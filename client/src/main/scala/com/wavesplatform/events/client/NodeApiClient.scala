package com.wavesplatform.events.client

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.wavesplatform.block.Block
import com.wavesplatform.events.Height
import com.wavesplatform.events.streams.BlockStream

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

//noinspection AccessorLikeMethodIsEmptyParen
trait NodeApiClient {
  def height(): Future[Height]
  def blocks(fromHeight: Height, toHeight: Height = Int.MaxValue): BlockStream
}

object NodeApiClient extends NodeApiClient {
  def height(): Future[Height] = ???

  def blocks(fromHeight: Height, toHeight: Height = Int.MaxValue): BlockStream = {
    Source(fromHeight to toHeight by 1000)
      .mapAsync(1)(height => Future.successful(Seq.empty[Block]))
      .takeWhile(_.nonEmpty)
      .mapConcat(identity)
  }

  def scheduleBlocksStream(): BlockStream = {
    Source.tick(1 second, 1 second, NotUsed)
      .mapAsync(1) { _ => Future.successful(123: Height) }
      .statefulMapConcat { () =>
        var height = 0

      { newHeight =>
        if (height == newHeight) {
          Nil
        } else {
          height = newHeight
          blocks(height, newHeight) :: Nil
        }
      }
      }
      .flatMapConcat(identity)
  }
}
