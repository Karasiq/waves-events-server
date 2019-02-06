package com.wavesplatform.events

import akka.NotUsed
import akka.stream.scaladsl.Source

package object streams {
  type BlockStream = Source[JsonBlock, NotUsed]
  type TxStream = Source[JsonTransaction, NotUsed]
}
