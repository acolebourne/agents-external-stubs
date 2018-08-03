package uk.gov.hmrc.agentsexternalstubs.models

import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.test.UnitSpec

class UserSpec extends UnitSpec {

  "User" should {
    "be valid only when affinityGroup is none or one of [Individual, Organisation, Agent]" in {
      User.validate(User("foo", affinityGroup = Some("Individual"))).isValid shouldBe true
      User.validate(User("foo", affinityGroup = Some("Organisation"))).isValid shouldBe true
      User.validate(User("foo", affinityGroup = Some("Agent"))).isValid shouldBe true
      User.validate(User("foo", affinityGroup = None)).isValid shouldBe true

      User.validate(User("foo", affinityGroup = Some("Foo"))).isValid shouldBe false
      User.validate(User("foo", affinityGroup = Some(""))).isValid shouldBe false
    }

    "be valid only when confidenceLevel is none, or one of [50,100,200,300] and user is Individual and NINO is not empty" in {
      User
        .validate(
          User("foo", confidenceLevel = Some(50), affinityGroup = Some("Individual"), nino = Some(Nino("HW827856C"))))
        .isValid shouldBe true
      User
        .validate(
          User("foo", confidenceLevel = Some(100), affinityGroup = Some("Individual"), nino = Some(Nino("HW827856C"))))
        .isValid shouldBe true
      User
        .validate(
          User("foo", confidenceLevel = Some(200), affinityGroup = Some("Individual"), nino = Some(Nino("HW827856C"))))
        .isValid shouldBe true
      User
        .validate(
          User("foo", confidenceLevel = Some(300), affinityGroup = Some("Individual"), nino = Some(Nino("HW827856C"))))
        .isValid shouldBe true
      User
        .validate(
          User("foo", confidenceLevel = Some(200), affinityGroup = Some("Agent"), nino = Some(Nino("HW827856C"))))
        .isValid shouldBe false
      User
        .validate(
          User(
            "foo",
            confidenceLevel = Some(200),
            affinityGroup = Some("Organisation"),
            nino = Some(Nino("HW827856C"))))
        .isValid shouldBe false

      User
        .validate(User("foo", confidenceLevel = Some(200), affinityGroup = Some("Individual"), nino = None))
        .isValid shouldBe false
      User
        .validate(
          User("foo", confidenceLevel = Some(55), affinityGroup = Some("Individual"), nino = Some(Nino("HW827856C"))))
        .isValid shouldBe false
      User
        .validate(
          User("foo", confidenceLevel = Some(0), affinityGroup = Some("Individual"), nino = Some(Nino("HW827856C"))))
        .isValid shouldBe false
    }

    "be valid only when credentialStrength is none, or one of [weak, strong]" in {
      User.validate(User("foo", credentialStrength = Some("weak"))).isValid shouldBe true
      User.validate(User("foo", credentialStrength = Some("strong"))).isValid shouldBe true
      User.validate(User("foo", credentialStrength = None)).isValid shouldBe true

      User.validate(User("foo", credentialStrength = Some("very strong"))).isValid shouldBe false
      User.validate(User("foo", credentialStrength = Some("little weak"))).isValid shouldBe false
      User.validate(User("foo", credentialStrength = Some(""))).isValid shouldBe false
    }

    "be valid only when credentialRole is none, or one of [User, Assistant] for Individual or Agent" in {
      User
        .validate(User("foo", credentialRole = Some("User"), affinityGroup = Some("Individual")))
        .isValid shouldBe true
      User.validate(User("foo", credentialRole = Some("User"), affinityGroup = Some("Agent"))).isValid shouldBe true
      User
        .validate(User("foo", credentialRole = Some("Assistant"), affinityGroup = Some("Individual")))
        .isValid shouldBe true
      User
        .validate(User("foo", credentialRole = Some("Assistant"), affinityGroup = Some("Agent")))
        .isValid shouldBe true
      User
        .validate(User("foo", credentialRole = None, affinityGroup = Some("Agent")))
        .isValid shouldBe true
      User
        .validate(User("foo", credentialRole = None, affinityGroup = Some("Individual")))
        .isValid shouldBe true

      User
        .validate(User("foo", credentialRole = Some("Assistant"), affinityGroup = Some("Organisation")))
        .isValid shouldBe false
    }

    "be valid only when nino is none or set for an Individual" in {
      User
        .validate(
          User("foo", nino = Some(Nino("HW827856C")), affinityGroup = Some("Individual"), confidenceLevel = Some(200)))
        .isValid shouldBe true
      User
        .validate(User("foo", nino = None, affinityGroup = Some("Individual")))
        .isValid shouldBe true

      User
        .validate(
          User("foo", nino = Some(Nino("HW827856C")), affinityGroup = Some("Individual"), confidenceLevel = None))
        .isValid shouldBe false
      User
        .validate(User("foo", nino = Some(Nino("HW827856C")), affinityGroup = Some("Agent")))
        .isValid shouldBe false
      User
        .validate(User("foo", nino = Some(Nino("HW827856C")), affinityGroup = Some("Organisation")))
        .isValid shouldBe false
    }

    "be valid only when delegatedEnrolments are empty or user is an Agent" in {
      User.validate(User("foo", delegatedEnrolments = Seq.empty)).isValid shouldBe true
      User
        .validate(User("foo", delegatedEnrolments = Seq(Enrolment("A")), affinityGroup = Some("Agent")))
        .isValid shouldBe true

      User
        .validate(User("foo", delegatedEnrolments = Seq(Enrolment("A")), affinityGroup = Some("Individual")))
        .isValid shouldBe false
      User
        .validate(User("foo", delegatedEnrolments = Seq(Enrolment("A")), affinityGroup = Some("Organisation")))
        .isValid shouldBe false
    }
  }

}
