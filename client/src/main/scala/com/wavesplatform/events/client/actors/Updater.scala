package com.wavesplatform.events.client.actors

import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.Materializer
import akka.stream.typed.scaladsl.ActorSink
import com.wavesplatform.account.{Address, AddressOrAlias, Alias}
import com.wavesplatform.events.Height
import com.wavesplatform.events.client.NodeApiClient
import com.wavesplatform.lang.v1.traits.domain.Recipient
import com.wavesplatform.lang.v1.traits.domain.Tx.MassTransfer
import com.wavesplatform.transaction.lease.LeaseTransaction
import com.wavesplatform.transaction.transfer.TransferTransaction
import com.wavesplatform.transaction.{DataTransaction, Transaction}

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.language.implicitConversions

object Updater {
  sealed trait Subscription
  object Subscription {
    final case class Address(address: AddressOrAlias) extends Subscription
    final case class DataKey(key: String) extends Subscription
  }

  trait Msg
  final case class Subscribe(subscription: Subscription, ref: ActorRef[Transactions]) extends Msg
  final case class Unsubscribe(subscription: Subscription, ref: ActorRef[Transactions]) extends Msg
  private final case class RequestNewTransactions(newHeight: Int) extends Msg
  private final case class ProcessNewTransactions(blocks: Seq[Transaction]) extends Msg
  private final case class SetNewHeight(newHeight: Height) extends Msg
  private final case class Failure(exc: Throwable) extends Msg

  final case class Transactions(tx: Seq[Transaction])

  private object Timers {
    case object UpdateHeight
  }
  private case object UpdateHeight extends Msg

  def behavior(target: ActorRef[Transactions], after: FiniteDuration)
              (implicit mat: Materializer): Behavior[Msg] = {
    Behaviors.withTimers { timers =>
      timers.startPeriodicTimer(Timers.UpdateHeight, UpdateHeight, after)
      active(timers, after)(0, Map.empty)
    }
  }

  private def active(timers: TimerScheduler[Msg], after: FiniteDuration)
                    (height: Height, subscriptions: Map[Subscription, Set[ActorRef[Transactions]]])
                    (implicit mat: Materializer): Behavior[Msg] = {
    Behaviors.receive[Msg] { (ctx, msg) =>
      import ctx.executionContext

      msg match {
        case Subscribe(subscription, actor) =>
          val current = subscriptions.getOrElse(subscription, Set.empty)
          active(timers, after)(height, subscriptions + (subscription -> (current + actor)))

        case Unsubscribe(subscription, actor) =>
          subscriptions.get(subscription) match {
            case Some(actors) =>
              val newSet = actors - actor
              if (newSet.isEmpty) active(timers, after)(height, subscriptions - subscription)
              else active(timers, after)(height, subscriptions + (subscription -> newSet))

            case None =>
              Behaviors.same
          }

        case UpdateHeight =>
          Future.successful(23).foreach(height => ctx.self ! RequestNewTransactions(height))
          Behaviors.same

        case RequestNewTransactions(newHeight) =>
          if (newHeight > height) {
            ctx.log.debug("Requesting new blocks from {}", newHeight)
            NodeApiClient.blocks(height, newHeight)
              .map(block => ProcessNewTransactions(block.transactionData))
              .runWith(ActorSink.actorRef(ctx.self, SetNewHeight(newHeight), Failure))
            Behaviors.same
          } else {
            Behaviors.same
          }

        case ProcessNewTransactions(transactions) =>
          processTransactions(subscriptions)(transactions)
          Behaviors.same

        case SetNewHeight(newHeight) =>
          ctx.log.debug("Setting new height: {}", newHeight)
          active(timers, after)(newHeight, Map.empty)

        case Failure(exc) =>
          ctx.log.error(exc, "Waves updater error")
          Behaviors.same
      }
    }
  }

  private def processTransactions(subscriptions: Map[Subscription, Set[ActorRef[Transactions]]])(transactions: Seq[Transaction]): Unit = {
    implicit def recipientToAddressOrAlias(r: Recipient): AddressOrAlias = (r match {
      case Recipient.Address(bytes) => Address.fromBytes(bytes.arr)
      case Recipient.Alias(alias) => Alias.fromString(alias)
    }).right.getOrElse(throw new IllegalArgumentException(s"Invalid recipient: $r"))

    val byAddress = transactions.collect {
      case tt: TransferTransaction =>
        (tt.recipient, tt) :: Nil

      case lt: LeaseTransaction =>
        (lt.recipient, lt) :: Nil

      case mt: MassTransfer =>
        mt.transfers.map(ti => (ti.recipient: AddressOrAlias, mt))
    }

    val byAddressMap = byAddress
      .flatten
      .groupBy(_._1)
      .mapValues(_.map(_._2))

    val byDataKey = transactions.collect {
      case dt: DataTransaction => dt.data.map(de => (de.key, dt))
    }

    val byDataKeyMap = byDataKey
      .flatten
      .groupBy(_._1)
      .mapValues(_.map(_._2))

    subscriptions.foreach { case (subscription, actors) =>
      def sendTransactions(txs: Seq[Transaction]): Unit = {
        actors.foreach(_ ! Transactions(txs))
      }

      subscription match {
        case Subscription.Address(address) =>
          val txs = byAddressMap(address)
          sendTransactions(txs)

        case Subscription.DataKey(key) =>
          val txs = byDataKeyMap(key)
          sendTransactions(txs)
      }
    }
  }
}
