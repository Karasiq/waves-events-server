package com.wavesplatform.events.client

import akka.stream.scaladsl.Source
import com.wavesplatform.block.Block
import com.wavesplatform.events.Height
import com.wavesplatform.events.config.EventsClientConfig
import com.wavesplatform.events.streams.BlockStream

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

//noinspection AccessorLikeMethodIsEmptyParen
trait NodeApiClient {
  def height(): Future[Height]
  def blocks(fromHeight: Height, toHeight: Height = Int.MaxValue): BlockStream
}

object NodeApiClient {
  def apply(config: EventsClientConfig)(implicit ec: ExecutionContext): NodeApiClient = {
    new NodeApiClientImpl(config)
  }
}

private[client] class NodeApiClientImpl(config: EventsClientConfig)(implicit ec: ExecutionContext) extends NodeApiClient {
  def height(): Future[Height] = Future.successful(100) // TODO: Actual api

  def blocks(fromHeight: Height, toHeight: Height = Int.MaxValue): BlockStream = {
    Source(fromHeight to toHeight by config.blocksBatchSize)
      .mapAsync(1)(height => Future.successful(Seq.empty[Block]))
      .takeWhile(_.nonEmpty)
      .mapConcat(_.toVector)
  }
}
