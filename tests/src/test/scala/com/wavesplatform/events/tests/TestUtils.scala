package com.wavesplatform.events.tests

import scala.util.Random

trait TestUtils {
  implicit class IterableOps[T](iterable: Iterable[T]) {
    def random: T = {
      val seq = iterable.toSeq
      seq(Random.nextInt(seq.length))
    }
  }
}
