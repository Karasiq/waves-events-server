package com.wavesplatform.events

import play.api.libs.json._

final case class JsonBlock(json: JsObject, transactions: Seq[JsonTransaction])

object JsonBlock {
  implicit val format = Format[JsonBlock](
    Reads {
      case o: JsObject => JsSuccess(JsonBlock(o, (o \ "transactions").as[Seq[JsonTransaction]]))
      case _ => JsError("Object required")
    },
    Writes(_.json)
  )
}
