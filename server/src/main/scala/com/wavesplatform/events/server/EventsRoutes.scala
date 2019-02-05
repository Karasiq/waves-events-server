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
import play.api.libs.json.Json

import scala.language.postfixOps

class EventsRoutes(feed: ActorRef[NodeTxFeed.Message])(implicit as: ActorSystem, mat: Materializer) extends Directives {
  private[this] type SSEStream = Source[ServerSentEvent, NotUsed]

  val ssePaths: Route = path("transactions") {
    path("address" / Base58Address) { address =>
      complete(getJsonStream(NodeTxFeed.Subscription.Address(address)))
    } ~
    path("data-key" / Segment) { dataKey =>
      complete(getJsonStream(NodeTxFeed.Subscription.DataKey(dataKey)))
    }
  }

  val routes: Route = get(ssePaths)

  private[this] def getJsonStream(subscription: NodeTxFeed.Subscription): SSEStream = {
    ActorSource.actorRef[NodeTxFeed.Transactions](PartialFunction.empty, PartialFunction.empty, 128, OverflowStrategy.dropHead)
      .mapConcat(_.tx.toVector)
      .map(tx => ServerSentEvent(Json.stringify(tx.json())))
      .mapMaterializedValue(feed ! NodeTxFeed.Subscribe(_, subscription))
      .mapMaterializedValue(_ => NotUsed)
  }
}
