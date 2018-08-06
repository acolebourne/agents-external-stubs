package uk.gov.hmrc.agentsexternalstubs.connectors

import org.joda.time.LocalDate
import org.scalatest.Suite
import org.scalatestplus.play.ServerProvider
import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticatedSession, UserGenerator}
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support.{ServerBaseISpec, TestRequests}
import uk.gov.hmrc.domain.Nino

class CitizenDetailsConnectorISpec extends ServerBaseISpec with TestRequests with TestStubs {
  this: Suite with ServerProvider =>

  val url = s"http://localhost:$port"
  val wsClient = app.injector.instanceOf[WSClient]
  val connector = app.injector.instanceOf[CitizenDetailsConnector]

  "CitizenDetailsConnector" when {

    "getCitizenDateOfBirth" should {
      "return dateOfBirth" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession("foo")
        Users.update(UserGenerator
          .individual(userId = "foo", nino = "HW 82 78 56 C", name = "Alan Brian Foo-Foe", dateOfBirth = "1975-12-18"))

        val result = await(connector.getCitizenDateOfBirth(Nino("HW827856C")))
        result.flatMap(_.dateOfBirth) shouldBe Some(LocalDate.parse("1975-12-18"))
      }
    }
  }
}
