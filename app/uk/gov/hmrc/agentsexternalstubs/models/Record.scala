package uk.gov.hmrc.agentsexternalstubs.models
import play.api.libs.json._

trait Record {
  def keys: Seq[String]
}

object Record {

  val KEYS = "_keys"
  val TYPE = "_type"

  val reads: Reads[Record] = new Reads[Record] {
    override def reads(json: JsValue): JsResult[Record] = json match {
      case obj: JsObject => {
        (obj \ TYPE).asOpt[String] match {
          case Some("RelationshipRecord") => RelationshipRecord.formats.reads(obj)
          case Some(_)                    => JsError("Record type not supported")
          case None                       => JsError("Missing record type field")
        }
      }
      case o => JsError(s"Cannot parse Record from $o, must be JsObject.")
    }
  }

  val writes: Writes[Record] = new Writes[Record] {
    override def writes(record: Record): JsValue = {
      val json = record match {
        case x: RelationshipRecord => RelationshipRecord.formats.writes(x)
      }
      json match {
        case obj: JsObject =>
          obj.+(KEYS -> JsArray(record.keys.map(JsString))).+(TYPE -> JsString(record.getClass.getSimpleName))
        case o => throw new IllegalStateException(s"Record must be serialized to JsObject, got $o instead")
      }
    }
  }

  implicit val formats: Format[Record] = Format[Record](reads, writes)

}
