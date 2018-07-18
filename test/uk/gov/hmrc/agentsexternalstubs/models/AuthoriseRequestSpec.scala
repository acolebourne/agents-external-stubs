package uk.gov.hmrc.agentsexternalstubs.models

import play.api.libs.json.Json
import uk.gov.hmrc.play.test.UnitSpec
import org.mockito.Mockito._

class AuthoriseRequestSpec extends UnitSpec {

  "AuthoriseRequest" should {
    "parse empty authorise request" in {
      Json.parse(s"""{
                    |"authorise": [],
                    |"retrieve": []
                    |}""".stripMargin).as[AuthoriseRequest] shouldBe AuthoriseRequest.empty
    }

    "serialize empty authorise request" in {
      Json.toJson(AuthoriseRequest.empty).toString() shouldBe """{"authorise":[],"retrieve":[]}"""
    }

    "parse authorise request with enrolment predicate" in {
      Json.parse(s"""{
                    |"authorise": [
                    | { "enrolment": "FOO"
                    | }
                    |],
                    |"retrieve": []
                    |}""".stripMargin).as[AuthoriseRequest] shouldBe AuthoriseRequest(
        Seq(EnrolmentPredicate("FOO")),
        Seq.empty)
    }

    "serialize authorise request with enrolment predicate" in {
      Json
        .toJson(AuthoriseRequest(Seq(EnrolmentPredicate("FOO")), Seq.empty))
        .toString() shouldBe """{"authorise":[{"enrolment":"FOO"}],"retrieve":[]}"""
    }

    "parse authorise request with enrolment with identifiers predicate" in {
      Json.parse(s"""{
                    |"authorise": [
                    | { "enrolment": "FOO",
                    |   "identifiers": [{ "key": "UTR", "value": "123" }, { "key": "ABC", "value": "AGGHGs3892183" }]
                    | }
                    |],
                    |"retrieve": []
                    |}""".stripMargin).as[AuthoriseRequest] shouldBe AuthoriseRequest(
        Seq(EnrolmentPredicate("FOO", Some(Seq(Identifier("UTR", "123"), Identifier("ABC", "AGGHGs3892183"))))),
        Seq.empty)
    }

    "serialize authorise request with enrolment with identifiers predicate" in {
      Json
        .toJson(
          AuthoriseRequest(
            Seq(EnrolmentPredicate("FOO", Some(Seq(Identifier("UTR", "123"), Identifier("ABC", "AGGHGs3892183"))))),
            Seq.empty))
        .toString() shouldBe
        """{"authorise":[{"enrolment":"FOO","identifiers":[{"key":"UTR","value":"123"},{"key":"ABC","value":"AGGHGs3892183"}]}],"retrieve":[]}"""
    }

    "parse authorise request with authProviders predicate" in {
      Json.parse(s"""{
                    |"authorise": [
                    | { "authProviders": [
                    |     "FOO"
                    |   ]
                    | }
                    |],
                    |"retrieve": []
                    |}""".stripMargin).as[AuthoriseRequest] shouldBe AuthoriseRequest(
        Seq(AuthProviders(Seq("FOO"))),
        Seq.empty)
    }

    "serialize authorise request with authProviders predicate" in {
      Json
        .toJson(AuthoriseRequest(Seq(AuthProviders(Seq("FOO"))), Seq.empty))
        .toString() shouldBe """{"authorise":[{"authProviders":["FOO"]}],"retrieve":[]}"""
    }

    "parse authorise request with credentialStrength predicate" in {
      Json.parse(s"""{
                    |"authorise": [
                    | { "credentialStrength": "bar" }
                    |],
                    |"retrieve": []
                    |}""".stripMargin).as[AuthoriseRequest] shouldBe AuthoriseRequest(
        Seq(CredentialStrength("bar")),
        Seq.empty)
    }

    "serialize authorise request with credentialStrength predicate" in {
      Json
        .toJson(AuthoriseRequest(Seq(CredentialStrength("foobar")), Seq.empty))
        .toString() shouldBe """{"authorise":[{"credentialStrength":"foobar"}],"retrieve":[]}"""
    }

    "parse authorise request with confidenceLevel predicate" in {
      Json.parse(s"""{
                    |"authorise": [
                    | { "confidenceLevel": 333 }
                    |],
                    |"retrieve": []
                    |}""".stripMargin).as[AuthoriseRequest] shouldBe AuthoriseRequest(
        Seq(ConfidenceLevel(333)),
        Seq.empty)
    }

    "serialize authorise request with confidenceLevel predicate" in {
      Json
        .toJson(AuthoriseRequest(Seq(ConfidenceLevel(892)), Seq.empty))
        .toString() shouldBe """{"authorise":[{"confidenceLevel":892}],"retrieve":[]}"""
    }

    "parse authorise request with affinityGroup predicate" in {
      Json.parse(s"""{
                    |"authorise": [
                    | { "affinityGroup": "Organisation" }
                    |],
                    |"retrieve": []
                    |}""".stripMargin).as[AuthoriseRequest] shouldBe AuthoriseRequest(
        Seq(AffinityGroup("Organisation")),
        Seq.empty)
    }

    "serialize authorise request with affinityGroup predicate" in {
      Json
        .toJson(AuthoriseRequest(Seq(AffinityGroup("Agent")), Seq.empty))
        .toString() shouldBe """{"authorise":[{"affinityGroup":"Agent"}],"retrieve":[]}"""
    }
  }

  "EnrolmentPredicate" should {
    "accept if user has the expected enrolment with key" in {
      val context = mock(classOf[AuthoriseContext])
      when(context.principalEnrolments).thenReturn(Seq(Enrolment("foo")))
      val predicate = EnrolmentPredicate("foo")
      predicate.validate(context) shouldBe Right(())
    }

    "accept if user has the expected enrolment with key and identifiers" in {
      val context = mock(classOf[AuthoriseContext])
      when(context.principalEnrolments).thenReturn(Seq(Enrolment("foo", Some(Seq(Identifier("bar", "1234556789"))))))
      val predicate = EnrolmentPredicate("foo", Some(Seq(Identifier("bar", "1234556789"))))
      predicate.validate(context) shouldBe Right(())
    }

    "reject if user has the enrolment key expected but doesn't match" in {
      val context = mock(classOf[AuthoriseContext])
      when(context.principalEnrolments).thenReturn(Seq(Enrolment("bar")))
      val predicate = EnrolmentPredicate("foo")
      predicate.validate(context) shouldBe Left("InsufficientEnrolments")
    }

    "reject if user has the enrolment key matches but identifiers key doesn't match" in {
      val context = mock(classOf[AuthoriseContext])
      when(context.principalEnrolments).thenReturn(Seq(Enrolment("foo", Some(Seq(Identifier("bar", "1234556789"))))))
      val predicate = EnrolmentPredicate("foo", Some(Seq(Identifier("rab", "1234556789"))))
      predicate.validate(context) shouldBe Left("InsufficientEnrolments")
    }

    "reject if user has the enrolment and identifier keys match but identifiers value doesn't match" in {
      val context = mock(classOf[AuthoriseContext])
      when(context.principalEnrolments).thenReturn(Seq(Enrolment("foo", Some(Seq(Identifier("bar", "1234556789"))))))
      val predicate = EnrolmentPredicate("foo", Some(Seq(Identifier("bar", "987654321"))))
      predicate.validate(context) shouldBe Left("InsufficientEnrolments")
    }
  }

}
