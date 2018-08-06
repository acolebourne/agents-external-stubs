package uk.gov.hmrc.agentsexternalstubs.models

import org.joda.time.LocalDate
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.test.UnitSpec

class UserSanitizerSpec extends UnitSpec {

  "UserSanitizer" should {
    "add missing name to the Individual" in {
      UserSanitizer.sanitize(User("foo", affinityGroup = Some(User.AG.Individual))).name shouldBe Some(
        "Kaylee Phillips")
      UserSanitizer.sanitize(User("boo", affinityGroup = Some(User.AG.Individual))).name shouldBe Some(
        "Nicholas Isnard")
    }

    "add missing dateOfBirth to the Individual" in {
      UserSanitizer.sanitize(User("foo", affinityGroup = Some(User.AG.Individual))).dateOfBirth shouldBe Some(
        LocalDate.parse("1939-09-24"))
      UserSanitizer.sanitize(User("boo", affinityGroup = Some(User.AG.Individual))).dateOfBirth shouldBe Some(
        LocalDate.parse("1966-04-24"))
    }

    "add missing NINO to the Individual" in {
      UserSanitizer.sanitize(User("foo", affinityGroup = Some(User.AG.Individual))).nino shouldBe Some(
        Nino("XC 93 60 45 D"))
      UserSanitizer.sanitize(User("boo", affinityGroup = Some(User.AG.Individual))).nino shouldBe Some(
        Nino("HW 82 78 56 C"))
    }

    "add missing ConfidenceLevel to the Individual" in {
      UserSanitizer.sanitize(User("foo", affinityGroup = Some(User.AG.Individual))).confidenceLevel shouldBe Some(50)
      UserSanitizer.sanitize(User("boo", affinityGroup = Some(User.AG.Individual))).confidenceLevel shouldBe Some(50)
    }

    "add missing CredentialRole if appropriate" in {
      UserSanitizer.sanitize(User("foo", affinityGroup = Some(User.AG.Individual))).credentialRole shouldBe Some(
        User.CR.User)
      UserSanitizer.sanitize(User("foo", affinityGroup = Some(User.AG.Agent))).credentialRole shouldBe Some(
        User.CR.User)
      UserSanitizer.sanitize(User("foo", affinityGroup = Some(User.AG.Organisation))).credentialRole shouldBe None
      UserSanitizer.sanitize(User("foo", affinityGroup = None)).credentialRole shouldBe None
    }
  }

}
