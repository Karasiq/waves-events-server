package com.wavesplatform.events.tests

import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Inside, Matchers}

class DefaultSpec extends TestKit(ActorSystem("test")) with FlatSpecLike with Matchers with BeforeAndAfterAll with MockFactory with TestUtils with Inside {
  implicit val typedSystem = system.toTyped
  implicit val materializer = ActorMaterializer()

  override def afterAll(): Unit = {
    system.terminate()
  }
}
