package uk.gov.hmrc.agentsexternalstubs.models

import play.api.libs.json._
import uk.gov.hmrc.agentsexternalstubs.syntax.|>

trait Record {

  val id: Option[String]
  def withId(id: Option[String]): Record

  def lookupKeys: Seq[String]
  def uniqueKey: Option[String] = None
}

object Record {

  val ID = "_id"
  val UNIQUE_KEY = "_uniqueKey"
  val KEYS = "_keys"
  val TYPE = "_type"

  val reads: Reads[Record] = new Reads[Record] {
    override def reads(json: JsValue): JsResult[Record] = json match {
      case obj: JsObject =>
        ((obj \ TYPE).asOpt[String] match {
          case Some("RelationshipRecord")       => RelationshipRecord.formats.reads(obj)
          case Some("LegacyAgentRecord")        => LegacyAgentRecord.formats.reads(obj)
          case Some("LegacyRelationshipRecord") => LegacyRelationshipRecord.formats.reads(obj)
          case Some("BusinessDetailsRecord")    => BusinessDetailsRecord.formats.reads(obj)
          case Some(r)                          => JsError(s"Record type $r not supported")
          case None                             => JsError("Missing record type field")
        }).map(_.withId((obj \ ID \ "$oid").asOpt[String]))

      case o => JsError(s"Cannot parse Record from $o, must be JsObject.")
    }
  }

  val writes: Writes[Record] = new Writes[Record] {

    override def writes(record: Record): JsValue =
      (record match {
        case r: RelationshipRecord       => RelationshipRecord.formats.writes(r)
        case r: LegacyAgentRecord        => LegacyAgentRecord.formats.writes(r)
        case r: LegacyRelationshipRecord => LegacyRelationshipRecord.formats.writes(r)
        case r: BusinessDetailsRecord    => BusinessDetailsRecord.formats.writes(r)
        case _                           => throw new UnsupportedOperationException(s"Cannot serialize $record")
      }) match {
        case obj: JsObject =>
          obj
            .-("id")
            .+(TYPE -> JsString(record.getClass.getSimpleName))
            .+(KEYS -> JsArray(record.lookupKeys.map(JsString)))
            .|> { obj =>
              record.uniqueKey
                .map(uniqueKey => obj.+(UNIQUE_KEY -> JsString(uniqueKey)))
                .getOrElse(obj)
            }
            .|> { obj =>
              record.id
                .map(id => obj.+(ID -> Json.obj("$oid" -> JsString(id))))
                .getOrElse(obj)
            }
        case o => throw new IllegalStateException(s"Record must be serialized to JsObject, got $o instead")
      }
  }

  implicit val formats: Format[Record] = Format[Record](reads, writes)

}
