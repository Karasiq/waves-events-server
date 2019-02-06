package com.wavesplatform.events.tests.client

import akka.actor.testkit.typed.scaladsl.{FishingOutcomes, TestProbe}
import akka.actor.typed.scaladsl.adapter._
import com.wavesplatform.events.client.actors.NodeTxFeed
import com.wavesplatform.events.client.actors.NodeTxFeed.Subscription
import com.wavesplatform.events.config.EventsClientConfig
import com.wavesplatform.events.tests.{DefaultSpec, TestNodeApiClient}
import com.wavesplatform.events.{AddressedTransaction, DataTransaction}
import com.wavesplatform.transaction.transfer.TransferTransaction

import scala.concurrent.duration._
import scala.language.postfixOps

class NodeTxFeedSpec extends DefaultSpec {
  val config = EventsClientConfig("localhost", 10, 1 second)
  val client = new TestNodeApiClient
  client.generateBlocks()
  val recipient = client.state.blocks.random.transactionData.random.asInstanceOf[TransferTransaction].recipient
  val Some(Seq(dataSubject, _*)) = client.state.blocks.random.transactionData.collectFirst { case dt: com.wavesplatform.transaction.DataTransaction => dt.data.map(_.key) }

  "Node TX feed" should "fetch transactions" in {
    val inbox = TestProbe[NodeTxFeed.Transactions]("test-feed-receiver")
    val testFeed = system.spawn(NodeTxFeed.behavior(client, config), "test-feed")
    testFeed ! NodeTxFeed.Subscribe(inbox.ref, Subscription.Address(recipient.stringRepr))
    testFeed ! NodeTxFeed.Subscribe(inbox.ref, Subscription.DataKey(dataSubject))
    // testFeed ! UpdateHeight

    var addressedSeen = false
    var dataSeen = false
    var failure = false

    inbox.fishForMessage(10 seconds) { message =>
      message.tx shouldNot be (empty)

      inside(message.tx.head) {
        case AddressedTransaction(rec, _) if rec == recipient.stringRepr => addressedSeen = true
        case DataTransaction(keys, _) if keys.contains(dataSubject) => dataSeen = true
        case _ => failure = true
      }

      if (failure) FishingOutcomes.fail("Invalid tx")
      else if (addressedSeen && dataSeen) FishingOutcomes.complete
      else FishingOutcomes.continue
    }
  }
}
