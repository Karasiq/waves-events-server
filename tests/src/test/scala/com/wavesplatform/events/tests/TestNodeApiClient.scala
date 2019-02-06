package com.wavesplatform.events.tests

import akka.stream.scaladsl.Source
import com.wavesplatform.block.Block
import com.wavesplatform.events.client.NodeApiClient
import com.wavesplatform.events.streams.BlockStream
import com.wavesplatform.events.{Height, JsonBlock}

import scala.concurrent.Future
import scala.language.implicitConversions

class TestNodeApiClient extends NodeApiClient {
  object state {
    var height = 0
    var blocks = Seq.empty[Block]
  }

  override def height(): Future[Height] = {
    Future.successful(state.height)
  }

  override def blocks(fromHeight: Height, toHeight: Height): BlockStream = {
    def toJsonBlock(b: Block): JsonBlock = JsonBlock.format.reads(b.json()).get
    Source(state.blocks.slice(fromHeight, toHeight).toVector.map(toJsonBlock))
  }

  def generateBlocks(): Unit = {
    state.height = 5
    state.blocks = for (_ <- 1 to state.height) yield {
      val transfers = for (_ <- 1 to 5; tx <- TransactionGen.transferV2Gen.sample) yield tx
      val datas = for (_ <- 1 to 5; tx <- TransactionGen.dataTransactionGen.sample) yield tx
      TestBlock.create(transfers ++ datas)
    }
  }
}
