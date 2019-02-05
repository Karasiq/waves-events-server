package com.wavesplatform.events.config

import scala.concurrent.duration.FiniteDuration

final case class EventsClientConfig(nodeAddress: String, blocksBatchSize: Int, updateHeightInterval: FiniteDuration) {
  require(nodeAddress.nonEmpty, "Invalid node address")
  require(blocksBatchSize > 0, s"Invalid blocks batch size: $blocksBatchSize")
}