package com.wavesplatform.events

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.wavesplatform.block.Block
import com.wavesplatform.transaction.Transaction

package object streams {
  type BlockStream = Source[Block, NotUsed]
  type TxStream = Source[Transaction, NotUsed]
}
