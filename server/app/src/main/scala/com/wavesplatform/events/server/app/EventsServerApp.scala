package com.wavesplatform.events.server.app

import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.wavesplatform.events.client.NodeApiClient
import com.wavesplatform.events.client.actors.NodeTxFeed
import com.wavesplatform.events.server.EventsRoutes
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.NameMapper

object EventsServerApp extends App {
  import net.ceedubs.ficus.readers.ArbitraryTypeReader._
  implicit val readConfigInHyphen: NameMapper = net.ceedubs.ficus.readers.namemappers.implicits.hyphenCase // IDEA bug

  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer()
  import actorSystem.dispatcher
  val config = actorSystem.settings.config.as[EventsServerAppConfig]("waves.events")

  val client = NodeApiClient(config.client)
  val feed = actorSystem.spawn(NodeTxFeed.behavior(client), "node-tx-feed")
  val server = new EventsRoutes(feed)

  Http().bindAndHandle(server.routes, "0.0.0.0", config.server.port)
}
