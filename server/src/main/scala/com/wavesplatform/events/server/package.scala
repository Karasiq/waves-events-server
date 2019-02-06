package com.wavesplatform.events

import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.server.PathMatcher.{Matched, Unmatched}
import akka.http.scaladsl.server.PathMatcher1
import com.wavesplatform.account.AddressOrAlias
import com.wavesplatform.common.state.ByteStr
import com.wavesplatform.common.utils.Base58
import com.wavesplatform.transaction.ValidationError.GenericError

import scala.util.Success

package object server {
  object Base58Segment extends PathMatcher1[ByteStr] {
    def apply(path: Path) = path match {
      case Path.Segment(segment, tail) ⇒
        Base58.decode(segment) match {
          case Success(value) => Matched(tail, Tuple1(ByteStr(value)))
          case _ => Unmatched
        }

      case _ ⇒ Unmatched
    }
  }

  object Base58Address extends PathMatcher1[AddressOrAlias] {
    def apply(path: Path) = path match {
      case Path.Segment(segment, tail) ⇒
        val result = for {
          base58 <- Base58.decode(segment).toEither.left.map(GenericError(_))
          address <- AddressOrAlias.fromBytes(base58, 0)
        } yield address._1

        result match {
          case Right(value) => Matched(tail, Tuple1(value))
          case _ => Unmatched
        }

      case _ ⇒ Unmatched
    }
  }

}
