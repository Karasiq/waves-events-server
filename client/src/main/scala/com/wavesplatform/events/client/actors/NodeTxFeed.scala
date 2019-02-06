package com.wavesplatform.events.client.actors

import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.Materializer
import akka.stream.typed.scaladsl.ActorSink
import com.wavesplatform.events.client.NodeApiClient
import com.wavesplatform.events.config.EventsClientConfig
import com.wavesplatform.events.{AddressedTransaction, DataTransaction, Height, JsonTransaction}

import scala.language.implicitConversions

object NodeTxFeed {
  // Types
  type SubscriptionMap = Map[Subscription, Set[ActorRef[Transactions]]]
  type TransactionsSeq = Seq[JsonTransaction]

  sealed trait Subscription
  object Subscription {
    final case class Address(address: String) extends Subscription
    final case class DataKey(key: String) extends Subscription
  }

  final case class Transactions(tx: TransactionsSeq) extends AnyVal

  // Messages
  trait Message

  final case class Subscribe(ref: ActorRef[Transactions], subscription: Subscription) extends Message
  final case class Unsubscribe(ref: ActorRef[Transactions], subscription: Subscription) extends Message
  final case class UnsubscribeAll(ref: ActorRef[Transactions]) extends Message

  case object UpdateHeight extends Message
  private final case class RequestNewTransactions(newHeight: Int) extends Message
  private final case class ProcessNewTransactions(blocks: TransactionsSeq) extends Message
  private final case class SetNewHeight(newHeight: Height) extends Message
  private final case class Failure(exc: Throwable) extends Message

  // Timers
  private object Timers {
    case object UpdateHeight
  }

  // Behaviors
  def behavior(nodeApiClient: NodeApiClient, config: EventsClientConfig)
              (implicit mat: Materializer): Behavior[Message] = {
    Behaviors.withTimers { timers =>
      timers.startPeriodicTimer(Timers.UpdateHeight, UpdateHeight, config.updateHeightInterval)
      active(timers, nodeApiClient)(0, Map.empty)
    }
  }

  private[this] def active(timers: TimerScheduler[Message], nodeApiClient: NodeApiClient)
                    (height: Height, subscriptions: SubscriptionMap)
                    (implicit mat: Materializer): Behavior[Message] = {
    Behaviors.receive[Message] { (ctx, msg) =>
      import ctx.executionContext

      msg match {
        case Subscribe(actor, subscription) =>
          val current = subscriptions.getOrElse(subscription, Set.empty)
          ctx.watchWith(actor, UnsubscribeAll(actor))
          active(timers, nodeApiClient)(height, subscriptions + (subscription -> (current + actor)))

        case Unsubscribe(actor, subscription) =>
          subscriptions.get(subscription) match {
            case Some(actors) =>
              val newSet = actors - actor
              if (newSet.isEmpty) active(timers, nodeApiClient)(height, subscriptions - subscription)
              else active(timers, nodeApiClient)(height, subscriptions + (subscription -> newSet))

            case None =>
              Behaviors.same
          }

        case UnsubscribeAll(actor) =>
          val newSubscriptions = subscriptions
            .mapValues(_ - actor)
            .filterNot(_._2.isEmpty)

          active(timers, nodeApiClient)(height, newSubscriptions)

        case UpdateHeight =>
          nodeApiClient.height().foreach(height => ctx.self ! RequestNewTransactions(height))
          Behaviors.same

        case RequestNewTransactions(newHeight) =>
          if (subscriptions.isEmpty) {
            ctx.log.info("Skipping blocks from {} to {}", height, newHeight)
            ctx.self ! SetNewHeight(newHeight)
            Behaviors.same
          } else if (newHeight > height) {
            ctx.log.info("Requesting new blocks from {}", newHeight)
            nodeApiClient.blocks(height, newHeight)
              .map(block => ProcessNewTransactions(block.transactions))
              .runWith(ActorSink.actorRef(ctx.self, SetNewHeight(newHeight), Failure))
            Behaviors.same
          } else {
            Behaviors.same
          }

        case ProcessNewTransactions(transactions) =>
          processTransactions(subscriptions)(transactions)
          Behaviors.same

        case SetNewHeight(newHeight) =>
          ctx.log.info("Setting new height: {}", newHeight)
          active(timers, nodeApiClient)(newHeight, subscriptions)

        case Failure(exc) =>
          ctx.log.error(exc, "Node transactions feed error")
          Behaviors.same
      }
    }
  }

  private def processTransactions(subscriptions: SubscriptionMap)(transactions: TransactionsSeq): Unit = {
    val byAddress = transactions.collect {
      case at: AddressedTransaction => (at.recipient, at)
    }

    val byAddressMap = byAddress
      .groupBy(_._1)
      .mapValues(_.map(_._2))
      .withDefaultValue(Nil)

    val byDataKey = transactions.collect {
      case dt: DataTransaction => dt.keys.map((_, dt))
    }

    val byDataKeyMap = byDataKey
      .flatten
      .groupBy(_._1)
      .mapValues(_.map(_._2))
      .withDefaultValue(Nil)

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
