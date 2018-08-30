package uk.gov.hmrc.agentsexternalstubs.models
import org.scalacheck.Gen

trait RecordHelper[T] {

  type Update = T => T

  val gen: Gen[T]

  val sanitizers: Seq[Update]

  final def seed(s: String): T = Generator.get(gen)(s)

  final def sanitize(entity: T): T = sanitizers.foldLeft(entity)((u, fx) => fx(u))

  final def generate(s: String): T = sanitize(seed(s))
}
