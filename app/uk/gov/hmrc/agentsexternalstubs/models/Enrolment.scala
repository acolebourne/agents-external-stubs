package uk.gov.hmrc.agentsexternalstubs.models
import play.api.libs.json.{Format, Json}

case class Enrolment(key: String, identifiers: Option[Seq[Identifier]] = None) {

  def toEnrolmentKey: Option[String] =
    identifiers.map(ii => s"$key~${ii.sorted.map(i => s"${i.key}~${i.value}").mkString("~")}")

  def description: String =
    s"enrolment for service $key${identifiers.map(_.map(i => s"${i.key} ${i.value}").mkString(" and ")).map(x => s" with identifier $x").getOrElse("")}"
}
object Enrolment {
  implicit val format: Format[Enrolment] = Json.format[Enrolment]
}
