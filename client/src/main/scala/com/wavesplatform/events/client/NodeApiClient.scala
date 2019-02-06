package com.wavesplatform.events.client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Accept
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import com.wavesplatform.events.config.EventsClientConfig
import com.wavesplatform.events.streams.BlockStream
import com.wavesplatform.events.{Height, JsonBlock}
import play.api.libs.json._

import scala.concurrent.Future
import scala.language.postfixOps

//noinspection AccessorLikeMethodIsEmptyParen
trait NodeApiClient {
  def height(): Future[Height]
  def blocks(fromHeight: Height, toHeight: Height = Int.MaxValue): BlockStream
}

object NodeApiClient {
  def apply(config: EventsClientConfig)(implicit system: ActorSystem, mat: Materializer): NodeApiClient = {
    new NodeApiClientImpl(config)
  }
}

private[client] class NodeApiClientImpl(config: EventsClientConfig)(implicit system: ActorSystem, mat: Materializer) extends NodeApiClient {
  import system.dispatcher
  private[this] val http = Http()

  private[this] final case class HeightWrapper(height: Height)
  private[this] implicit val heightWrapperFormat: Format[HeightWrapper] = Json.format[HeightWrapper]

  def height(): Future[Height] = {
    apiRequest[HeightWrapper]("blocks/height").map(_.height)
  }

  def blocks(fromHeight: Height, toHeight: Height = Int.MaxValue): BlockStream = {
    Source(fromHeight to toHeight by config.blocksBatchSize)
      .mapAsync(1)(height => apiRequest[Seq[JsonBlock]](s"blocks/seq/$height/${(height + config.blocksBatchSize) min toHeight}"))
      .takeWhile(_.nonEmpty)
      .mapConcat(_.toVector)
  }

  private def apiRequest[R: Reads](path: String): Future[R] = {
    http.singleRequest(HttpRequest(uri = s"http://${config.nodeAddress}/$path", headers = List(Accept(MediaRange(MediaTypes.`application/json`)))))
      .flatMap(_.entity.dataBytes.fold(ByteString.empty)(_ ++ _).map(bs => Json.parse(bs.toArray).as[R]).runWith(Sink.head))
  }
}
