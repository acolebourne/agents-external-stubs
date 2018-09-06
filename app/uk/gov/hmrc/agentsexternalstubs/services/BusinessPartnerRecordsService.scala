package uk.gov.hmrc.agentsexternalstubs.services

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr}
import uk.gov.hmrc.agentsexternalstubs.models.BusinessPartnerRecord
import uk.gov.hmrc.agentsexternalstubs.repository.RecordsRepository
import uk.gov.hmrc.http.BadRequestException

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BusinessPartnerRecordsService @Inject()(val recordsRepository: RecordsRepository) extends RecordsService {

  def store(record: BusinessPartnerRecord, autoFill: Boolean, planetId: String)(
    implicit ec: ExecutionContext): Future[String] =
    BusinessPartnerRecord
      .validate(record)
      .fold(
        errors => Future.failed(new BadRequestException(errors.mkString(", "))),
        _ => {
          val entity = if (autoFill) BusinessPartnerRecord.sanitize(record.safeId)(record) else record
          recordsRepository.store(entity, planetId)
        }
      )

  def getBusinessPartnerRecord(arn: Arn, planetId: String)(
    implicit ec: ExecutionContext): Future[Option[BusinessPartnerRecord]] =
    findByKey[BusinessPartnerRecord](BusinessPartnerRecord.agentReferenceNumberKey(arn.value), planetId)
      .map(_.headOption)

  def getBusinessPartnerRecord(utr: Utr, planetId: String)(
    implicit ec: ExecutionContext): Future[Option[BusinessPartnerRecord]] =
    findByKey[BusinessPartnerRecord](BusinessPartnerRecord.utrKey(utr.value), planetId).map(_.headOption)

}
