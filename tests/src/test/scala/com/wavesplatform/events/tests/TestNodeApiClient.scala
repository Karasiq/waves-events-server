package com.wavesplatform.events.tests

import akka.stream.scaladsl.Source
import com.wavesplatform.block.Block
import com.wavesplatform.events.Height
import com.wavesplatform.events.client.NodeApiClient
import com.wavesplatform.events.streams.BlockStream

import scala.concurrent.Future

class TestNodeApiClient extends NodeApiClient {
  object state {
    var height = 0
    var blocks = Seq.empty[Block]
  }

  override def height(): Future[Height] = {
    Future.successful(state.height)
  }

  override def blocks(fromHeight: Height, toHeight: Height): BlockStream = {
    Source(state.blocks.slice(fromHeight, toHeight).toVector)
  }

  def generateBlocks(): Unit = {
    state.height = 100
    state.blocks = for (_ <- 1 to 100) yield TestBlock.create(for (_ <- 1 to 100; tx <- TransactionGen.transferV2Gen.sample) yield tx)
  }
}
