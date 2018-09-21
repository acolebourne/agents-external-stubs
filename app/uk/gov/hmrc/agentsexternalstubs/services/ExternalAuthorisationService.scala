package uk.gov.hmrc.agentsexternalstubs.services
import java.net.URL
import java.util.UUID

import javax.inject.{Inject, Named, Singleton}
import play.api.Logger
import uk.gov.hmrc.agentsexternalstubs.TcpProxiesConfig
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.http.{HeaderCarrier, HttpPost, Upstream4xxResponse}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ExternalAuthorisationService @Inject()(
  usersService: UsersService,
  tcpProxiesConfig: TcpProxiesConfig,
  http: HttpPost,
  @Named("auth-baseUrl") authBaseUrl: URL) {

  final def maybeExternalSession(
    planetId: String,
    createNewAuthentication: AuthenticateRequest => Future[Option[AuthenticatedSession]])(
    implicit ec: ExecutionContext,
    hc: HeaderCarrier): Future[Option[AuthenticatedSession]] =
    if (tcpProxiesConfig.isProxyMode) {
      Future.successful(None)
    } else {
      Logger(getClass).info(s"Looking for external authorisation using ${hc.authorization}")
      val authRequest = AuthoriseRequest(
        Seq.empty,
        Seq(
          "credentials",
          "allEnrolments",
          "affinityGroup",
          "confidenceLevel",
          "credentialStrength",
          "credentialRole",
          "nino",
          "groupIdentifier",
          "name",
          "dateOfBirth",
          "agentInformation"
        )
      )
      http
        .POST(s"${authBaseUrl.toExternalForm}/auth/authorise", authRequest)
        .map {
          _.json match {
            case null => None
            case body => body.asOpt[AuthoriseResponse]
          }
        }
        .recover {
          case _ @Upstream4xxResponse(_, 401, _, _) => None
        }
        .flatMap {
          case Some(response) =>
            val creds = response.credentials.getOrElse(throw new Exception("Missing credentials"))
            val userId = creds.providerId
            val user = User(
              userId = userId,
              groupId = response.groupIdentifier,
              affinityGroup = response.affinityGroup,
              confidenceLevel = response.confidenceLevel,
              credentialStrength = response.credentialStrength,
              credentialRole = response.credentialRole,
              nino = response.nino,
              principalEnrolments = response.allEnrolments,
              name = response.name.map(_.toString),
              dateOfBirth = response.dateOfBirth,
              agentCode = response.agentInformation.flatMap(_.agentCode),
              agentFriendlyName = response.agentInformation.flatMap(_.agentFriendlyName),
              agentId = response.agentInformation.flatMap(_.agentId)
            )
            for {
              maybeSession <- createNewAuthentication(
                               AuthenticateRequest(
                                 sessionId = hc.sessionId.map(_.value).getOrElse(UUID.randomUUID().toString),
                                 userId = userId,
                                 password = "p@ssw0rd",
                                 providerType = creds.providerType,
                                 planetId = planetId,
                                 authTokenOpt = hc.authorization.map(_.value)
                               ))
              _ <- maybeSession match {
                    case Some(session) =>
                      Logger(getClass).info(s"New session ${session.authToken} created based on external.")
                      usersService.findByUserId(userId, planetId).flatMap {
                        case Some(_) =>
                          usersService.updateUser(session.userId, session.planetId, existing => merge(user, existing))
                        case None =>
                          usersService.createUser(user.copy(session.userId), session.planetId)
                      }
                    case _ => Future.successful(None)
                  }
            } yield maybeSession
          case None => Future.successful(None)
        }
    }

  private def merge(newUser: User, existing: User): User = User(
    userId = newUser.userId,
    groupId = newUser.groupId.orElse(existing.groupId),
    affinityGroup = newUser.affinityGroup.orElse(existing.affinityGroup),
    confidenceLevel = newUser.confidenceLevel.orElse(existing.confidenceLevel),
    credentialStrength = newUser.credentialStrength.orElse(existing.credentialStrength),
    credentialRole = newUser.credentialRole.orElse(existing.credentialRole),
    nino = newUser.nino.orElse(existing.nino),
    principalEnrolments = (newUser.principalEnrolments ++ existing.principalEnrolments).distinct,
    delegatedEnrolments = (newUser.delegatedEnrolments ++ existing.delegatedEnrolments).distinct,
    name = newUser.name.orElse(existing.name),
    dateOfBirth = newUser.dateOfBirth.orElse(existing.dateOfBirth),
    agentCode = newUser.agentCode.orElse(existing.agentCode),
    agentFriendlyName = newUser.agentFriendlyName.orElse(existing.agentFriendlyName),
    agentId = newUser.agentId.orElse(existing.agentId)
  )

}
