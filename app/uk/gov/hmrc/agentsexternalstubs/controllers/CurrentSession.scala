package uk.gov.hmrc.agentsexternalstubs.controllers
import play.api.Logger
import play.api.libs.json.{JsValue, Json, OWrites, Writes}
import play.api.mvc.{Request, Result, Results}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticatedSession, User}
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, UsersService}
import uk.gov.hmrc.auth.core.AuthorisationException
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpException, NotFoundException}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

trait CurrentSession extends HttpHelpers {

  def authenticationService: AuthenticationService

  val errorHandler: PartialFunction[Throwable, Result] = {
    case e: NotFoundException      => notFound("NOT_FOUND", e.getMessage)
    case e: BadRequestException    => badRequest("BAD_REQUEST", e.getMessage)
    case e: HttpException          => Results.Status(e.responseCode)(errorMessage("SERVER_ERROR", Some(e.getMessage)))
    case e: AuthorisationException => forbidden(e.getMessage)
    case NonFatal(e) =>
      e.printStackTrace()
      internalServerError("SERVER_ERROR", e.getMessage)
  }

  def withCurrentOrExternalSession[T](body: AuthenticatedSession => Future[Result])(
    ifSessionNotFound: => Future[Result])(
    implicit request: Request[T],
    ec: ExecutionContext,
    hc: HeaderCarrier): Future[Result] =
    request.headers.get(HeaderNames.AUTHORIZATION) match {
      case None =>
        Logger(getClass).info(s"Missing Authorization HTTP header.")
        ifSessionNotFound
      case Some(BearerToken(authToken)) =>
        (for {
          maybeSession <- authenticationService.findByAuthTokenOrLookupExternal(authToken)
          result <- maybeSession match {
                     case Some(session) => body(session)
                     case _ =>
                       Logger(getClass).info(s"Authorization $authToken not found locally nor externally.")
                       ifSessionNotFound
                   }
        } yield result)
          .recover(errorHandler)
    }

  def withCurrentSession[T](body: AuthenticatedSession => Future[Result])(
    ifSessionNotFound: => Future[Result])(implicit request: Request[T], ec: ExecutionContext): Future[Result] =
    request.headers.get(HeaderNames.AUTHORIZATION) match {
      case None =>
        Logger(getClass).info(s"Missing Authorization HTTP header.")
        ifSessionNotFound
      case Some(BearerToken(authToken)) =>
        (for {
          maybeSession <- authenticationService.findByAuthToken(authToken)
          result <- maybeSession match {
                     case Some(session) => body(session)
                     case _ =>
                       Logger(getClass).info(s"Authorization $authToken not found.")
                       ifSessionNotFound
                   }
        } yield result)
          .recover(errorHandler)
    }

  def withCurrentUser[T](body: (User, AuthenticatedSession) => Future[Result])(ifSessionNotFound: => Future[Result])(
    implicit request: Request[T],
    ec: ExecutionContext,
    usersService: UsersService): Future[Result] =
    withCurrentSession { session =>
      usersService.findByUserId(session.userId, session.planetId).flatMap {
        case None       => Future.successful(Results.NotFound("CURRENT_USER_NOT_FOUND"))
        case Some(user) => body(user, session).recover(errorHandler)
      }
    }(SessionRecordNotFound)
}

/*
     When stubbing DES request we can't just rely on the `Authorization` header
     because it is not the same token value issued by MTDP Auth,
     instead we have to exploit fact that `HeaderCarrierConverter` copy over sessionId
     from HTTP session as `X-Session-ID` header and lookup session by its ID.
 */
trait DesCurrentSession extends DesHttpHelpers {

  def authenticationService: AuthenticationService

  case class DesErrorResponse(code: String, reason: Option[String])
  object DesErrorResponse {
    implicit val writes: Writes[DesErrorResponse] = Json.writes[DesErrorResponse]
  }

  override def errorMessage(code: String, reason: Option[String]): JsValue =
    Json.toJson(DesErrorResponse(code, reason))

  val errorHandler: PartialFunction[Throwable, Result] = {
    case e: NotFoundException      => notFound("NOT_FOUND", e.getMessage)
    case e: BadRequestException    => badRequest("BAD_REQUEST", e.getMessage)
    case e: HttpException          => Results.Status(e.responseCode)(errorMessage("SERVER_ERROR", Some(e.getMessage)))
    case e: AuthorisationException => forbidden(e.getMessage)
    case NonFatal(e)               => internalServerError("SERVER_ERROR", e.getMessage)
  }

  def withCurrentSession[T](body: AuthenticatedSession => Future[Result])(
    ifSessionNotFound: => Future[Result])(implicit request: Request[T], ec: ExecutionContext): Future[Result] =
    // When DES request comes from authenticated browser session
    request.headers.get(uk.gov.hmrc.http.HeaderNames.xSessionId) match {
      case Some(sessionId) =>
        (for {
          maybeSession <- authenticationService.findBySessionId(sessionId)
          result <- maybeSession match {
                     case Some(session) =>
                       body(session)
                     case _ =>
                       Logger(getClass).warn(
                         s"AuthenticatedSession for sessionIs=$sessionId not found, cannot guess planetID for DES stubs")
                       ifSessionNotFound
                   }
        } yield result)
          .recover(errorHandler)
      case None =>
        // When DES request comes through api gateway
        request.session.get("X-Client-ID") match {
          case Some(apiClientId) =>
            (for {
              maybeSession <- authenticationService.findByPlanetId(apiClientId)
              result <- maybeSession match {
                         case Some(session) =>
                           body(session)
                         case _ =>
                           Logger(getClass).warn(
                             s"AuthenticatedSession for apiClientId=$apiClientId not found, cannot guess planetID for DES stubs")
                           ifSessionNotFound
                       }
            } yield result)
              .recover(errorHandler)
          case None =>
            Logger(getClass).warn(
              s"None of 'X-Session-ID' request header or 'X-Client-ID' session attribute found, cannot guess planetID for DES stubs")
            ifSessionNotFound
        }
    }

}
