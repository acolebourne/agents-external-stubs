package uk.gov.hmrc.agentsexternalstubs.controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.JsValue
import play.api.mvc.Action
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, BusinessDetailsRecordsService, LegacyRelationshipRecordsService, VatCustomerInformationRecordsService}
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

@Singleton
class RecordsController @Inject()(
  businessDetailsRecordsService: BusinessDetailsRecordsService,
  legacyRelationshipRecordsService: LegacyRelationshipRecordsService,
  vatCustomerInformationRecordsService: VatCustomerInformationRecordsService,
  val authenticationService: AuthenticationService)
    extends BaseController with CurrentSession {

  def storeBusinessDetails(autoFill: Boolean): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withCurrentSession { session =>
      withPayload[BusinessDetailsRecord](
        record =>
          businessDetailsRecordsService
            .store(record, autoFill, session.planetId)
            .map(_ =>
              Created.withHeaders(
                HeaderNames.LOCATION -> routes.DesStubController.getBusinessDetails("mtdbsa", record.mtdbsa).url)))
    }(SessionRecordNotFound)
  }

  def storeLegacyAgent(autoFill: Boolean): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withCurrentSession { session =>
      withPayload[LegacyAgentRecord](
        record =>
          legacyRelationshipRecordsService
            .store(record, autoFill, session.planetId)
            .map(_ => Created))
    }(SessionRecordNotFound)
  }

  def storeLegacyRelationship(autoFill: Boolean): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withCurrentSession { session =>
      withPayload[LegacyRelationshipRecord](
        record =>
          legacyRelationshipRecordsService
            .store(record, autoFill, session.planetId)
            .map(
              _ =>
                Created.withHeaders(
                  HeaderNames.LOCATION -> record.nino
                    .map(nino => routes.DesStubController.getLegacyRelationshipsByNino(nino).url)
                    .orElse(record.utr.map(utr => routes.DesStubController.getLegacyRelationshipsByUtr(utr).url))
                    .getOrElse(""))))
    }(SessionRecordNotFound)
  }

  def storeVatCustomerInformation(autoFill: Boolean): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withCurrentSession { session =>
      withPayload[VatCustomerInformationRecord](
        record =>
          vatCustomerInformationRecordsService
            .store(record, autoFill, session.planetId)
            .map(_ =>
              Created.withHeaders(
                HeaderNames.LOCATION -> routes.DesStubController.getVatCustomerInformation(record.vrn).url)))
    }(SessionRecordNotFound)
  }

}
