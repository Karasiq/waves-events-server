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

object NodeTxFeed {
  // Types
  type SubscriptionMap = Map[Subscription, Set[ActorRef[Transactions]]]
  type TransactionsSeq = Seq[Transaction]

  sealed trait Subscription
  object Subscription {
    final case class Address(address: AddressOrAlias) extends Subscription
    final case class DataKey(key: String) extends Subscription
  }

  final case class Transactions(tx: TransactionsSeq) extends AnyVal

  // Messages
  trait Message

  final case class Subscribe(ref: ActorRef[Transactions], subscription: Subscription) extends Message
  final case class Unsubscribe(ref: ActorRef[Transactions], subscription: Subscription) extends Message
  final case class UnsubscribeAll(ref: ActorRef[Transactions]) extends Message

  private final case class RequestNewTransactions(newHeight: Int) extends Message
  private final case class ProcessNewTransactions(blocks: TransactionsSeq) extends Message
  private final case class SetNewHeight(newHeight: Height) extends Message
  private final case class Failure(exc: Throwable) extends Message

  private case object UpdateHeight extends Message

  // Timers
  private object Timers {
    case object UpdateHeight
  }

  // Behaviors
  def behavior(target: ActorRef[Transactions], after: FiniteDuration)
              (implicit mat: Materializer): Behavior[Message] = {
    Behaviors.withTimers { timers =>
      timers.startPeriodicTimer(Timers.UpdateHeight, UpdateHeight, after)
      active(timers, after)(0, Map.empty)
    }
  }

  private def active(timers: TimerScheduler[Message], after: FiniteDuration)
                    (height: Height, subscriptions: SubscriptionMap)
                    (implicit mat: Materializer): Behavior[Message] = {
    Behaviors.receive[Message] { (ctx, msg) =>
      import ctx.executionContext

      msg match {
        case Subscribe(actor, subscription) =>
          val current = subscriptions.getOrElse(subscription, Set.empty)
          ctx.watchWith(actor, UnsubscribeAll(actor))
          active(timers, after)(height, subscriptions + (subscription -> (current + actor)))

        case Unsubscribe(actor, subscription) =>
          subscriptions.get(subscription) match {
            case Some(actors) =>
              val newSet = actors - actor
              if (newSet.isEmpty) active(timers, after)(height, subscriptions - subscription)
              else active(timers, after)(height, subscriptions + (subscription -> newSet))

            case None =>
              Behaviors.same
          }

        case UnsubscribeAll(actor) =>
          val newSubscriptions = subscriptions
            .mapValues(_ - actor)
            .filterNot(_._2.isEmpty)

          active(timers, after)(height, newSubscriptions)

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

  private def processTransactions(subscriptions: SubscriptionMap)(transactions: TransactionsSeq): Unit = {
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
      def sendTransactions(txs: TransactionsSeq): Unit = {
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
