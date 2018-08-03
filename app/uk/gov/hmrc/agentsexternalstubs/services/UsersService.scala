package uk.gov.hmrc.agentsexternalstubs.services

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agentsexternalstubs.models.{Enrolment, Identifier, User}
import uk.gov.hmrc.agentsexternalstubs.repository.UsersRepository
import uk.gov.hmrc.http.NotFoundException

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class UsersService @Inject()(usersRepository: UsersRepository) {

  def findByUserId(userId: String, planetId: String)(implicit ec: ExecutionContext): Future[Option[User]] =
    usersRepository.findByUserId(userId, planetId)

  def createUser(user: User, planetId: String)(implicit ec: ExecutionContext): Future[User] =
    for {
      _         <- usersRepository.create(user.copy(planetId = Some(planetId)))
      maybeUser <- findByUserId(user.userId, planetId)
      newUser = maybeUser.getOrElse(throw new Exception(s"User $user creation failed."))
    } yield newUser

  def tryCreateUser(user: User, planetId: String)(implicit ec: ExecutionContext): Future[Try[User]] =
    for {
      maybeUser <- findByUserId(user.userId, planetId)
      result <- maybeUser match {
                 case Some(_) => Future.successful(Failure(new Exception(s"User ${user.userId} already exists")))
                 case None =>
                   for {
                     _       <- usersRepository.create(user.copy(planetId = Some(planetId)))
                     newUser <- findByUserId(user.userId, planetId)
                   } yield newUser.map(Success.apply).getOrElse(Failure(new Exception(s"User creation failed")))
               }
    } yield result

  def updateUser(userId: String, planetId: String, modify: User => User)(implicit ec: ExecutionContext): Future[User] =
    for {
      maybeUser <- findByUserId(userId, planetId)
      updatedUser <- maybeUser match {
                      case Some(existingUser) =>
                        val modified = modify(existingUser).copy(userId = userId, planetId = Some(planetId))
                        if (modified != existingUser) for {
                          _         <- usersRepository.update(modified)
                          maybeUser <- usersRepository.findByUserId(userId, planetId)
                        } yield maybeUser.getOrElse(throw new Exception)
                        else Future.successful(existingUser)
                      case None => Future.failed(new NotFoundException(s"User $userId not found"))
                    }
    } yield updatedUser

  def addEnrolment(userId: String, planetId: String, service: String, identifierKey: String, identifierValue: String)(
    implicit ec: ExecutionContext): Future[User] =
    updateUser(
      userId,
      planetId,
      user =>
        user.copy(
          principalEnrolments = user.principalEnrolments :+ Enrolment(
            service,
            Some(Seq(Identifier(identifierKey, identifierValue)))))
    )

}
