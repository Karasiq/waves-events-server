package com.wavesplatform.events

import play.api.libs.json._

trait JsonTransaction {
  def json: JsObject
}

object JsonTransaction {
  implicit val format = Format[JsonTransaction](
    Reads {
      case o: JsObject if o.value.contains("recipient") => JsSuccess(AddressedTransaction((o \ "recipient").as[String], o))
      case o: JsObject => JsSuccess(new JsonTransaction {
        override def json: JsObject = o
      })
      case _ => JsError("Object required")
    },
    Writes(_.json)
  )
}

final case class AddressedTransaction(recipient: String, json: JsObject) extends JsonTransaction
object AddressedTransaction {
  implicit val format = Json.format[AddressedTransaction]
}