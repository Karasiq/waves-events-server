package com.wavesplatform.events.server

import akka.NotUsed
import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.scaladsl.Source
import akka.stream.typed.scaladsl.ActorSource
import akka.stream.{Materializer, OverflowStrategy}
import com.wavesplatform.events.client.actors.NodeTxFeed

import scala.language.postfixOps

class EventsRoutes(implicit as: ActorSystem, mat: Materializer) extends Directives {
  private[this] type SSEStream = Source[ServerSentEvent, NotUsed]

  private[this] def getStream(actor: ActorRef[NodeTxFeed.Message], subscription: NodeTxFeed.Subscription): SSEStream = {
    ActorSource.actorRef[NodeTxFeed.Transactions](PartialFunction.empty, PartialFunction.empty, 128, OverflowStrategy.dropHead)
      .mapConcat(_.tx.toVector)
      .map(_ => ServerSentEvent(???, ???))
      .mapMaterializedValue(actor ! NodeTxFeed.Subscribe(_, subscription))
      .mapMaterializedValue(_ => NotUsed)
  }


  val ssePaths: Route = path("transactions") {
    path("address" / Base58Address) { address =>
      complete(getStream(???, NodeTxFeed.Subscription.Address(address)))
    } ~
    path("data-key" / Segment) { dataKey =>
      complete(getStream(???, NodeTxFeed.Subscription.DataKey(dataKey)))
    }
  }

  val routes: Route = get(ssePaths)
}
