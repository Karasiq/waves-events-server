package com.wavesplatform.events.tests.client

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.scaladsl.adapter._
import com.wavesplatform.account.AddressOrAlias
import com.wavesplatform.events.client.actors.NodeTxFeed
import com.wavesplatform.events.client.actors.NodeTxFeed.{Subscription, UpdateHeight}
import com.wavesplatform.events.config.EventsClientConfig
import com.wavesplatform.events.tests.{DefaultSpec, TestNodeApiClient}
import com.wavesplatform.transaction.transfer.TransferTransaction

import scala.concurrent.duration._
import scala.language.postfixOps

class NodeTxFeedSpec extends DefaultSpec {
  val config = EventsClientConfig("localhost", 10, 1 second)
  val client = new TestNodeApiClient
  client.generateBlocks()
  val recipient: AddressOrAlias = client.state.blocks.head.transactionData.head.asInstanceOf[TransferTransaction].recipient

  "Node TX feed" should "fetch transactions" in {
    val inbox = TestProbe[NodeTxFeed.Transactions]("test-feed-receiver")
    val testFeed = system.spawn(NodeTxFeed.behavior(client, config), "test-feed")
    testFeed ! NodeTxFeed.Subscribe(inbox.ref, Subscription.Address(recipient))
    testFeed ! UpdateHeight

    val message = inbox.receiveMessage(10 seconds)
    message.tx shouldNot be (empty)
    message.tx.head should matchPattern { case tt: TransferTransaction if tt.recipient == recipient => () }
  }
}
