package uk.gov.hmrc.agentsexternalstubs.models
import org.scalacheck.Gen
import uk.gov.hmrc.agentsexternalstubs.models.Validator.Validator

trait RecordUtils[T] {

  type Update = String => T => T

  val gen: Gen[T]

  val validate: Validator[T]

  val sanitizers: Seq[Update]

  final def seed(s: String): T = Generator.get(gen)(s).getOrElse(throw new Exception(s"Could not seed record with $s"))

  final def sanitize(s: String)(entity: T): T = sanitizers.foldLeft(entity)((u, fx) => fx(s)(u))

  final def generate(s: String): T = sanitize(s)(seed(s))
}
