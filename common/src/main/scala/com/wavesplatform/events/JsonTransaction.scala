package com.wavesplatform.events

import play.api.libs.json._

trait JsonTransaction {
  def json: JsObject
}

object JsonTransaction {
  implicit val format = Format[JsonTransaction](
    Reads {
      case obj: JsObject if obj.value.contains("recipient") => AddressedTransaction.format.reads(obj)
      case obj: JsObject if obj.value.contains("data") => DataTransaction.format.reads(obj)
      case obj: JsObject => JsSuccess(new JsonTransaction {
        override def json: JsObject = obj
      })
      case _ => JsError("Object required")
    },
    Writes(_.json)
  )
}

final case class AddressedTransaction(recipient: String, json: JsObject) extends JsonTransaction
object AddressedTransaction {
  implicit val format = Format[JsonTransaction](
    Reads {
      case obj: JsObject =>
        val recipient = (obj \ "recipient").as[String]
        JsSuccess(AddressedTransaction(recipient, obj))

      case _ => JsError("Object required")
    },
    Writes(_.json)
  )
}

final case class DataTransaction(keys: Seq[String], json: JsObject) extends JsonTransaction
object DataTransaction {
  implicit val format = Format[JsonTransaction](
    Reads {
      case obj: JsObject =>
        val data = (obj \ "data").as[Seq[JsObject]].map(entry => (entry \ "key").as[String])
        JsSuccess(DataTransaction(data, obj))

      case _ => JsError("Object required")
    },
    Writes(_.json)
  )
}