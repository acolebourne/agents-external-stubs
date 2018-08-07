/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.gov.hmrc.agentsexternalstubs.repository

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Ascending
import reactivemongo.bson.{BSONBoolean, BSONDocument, BSONObjectID}
import reactivemongo.core.errors.DatabaseException
import reactivemongo.play.json.ImplicitBSONHandlers
import uk.gov.hmrc.agentsexternalstubs.models.User
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.collection.Seq
import scala.concurrent.{ExecutionContext, Future}

case class DuplicateUserException(msg: String) extends IllegalStateException(msg)

@Singleton
class UsersRepository @Inject()(mongoComponent: ReactiveMongoComponent)
    extends ReactiveRepository[User, BSONObjectID](
      "users",
      mongoComponent.mongoConnector.db,
      User.formats,
      ReactiveMongoFormats.objectIdFormats) with StrictlyEnsureIndexes[User, BSONObjectID] {

  import ImplicitBSONHandlers._

  private final val UsersIndexName = "Users"
  private final val NinosIndexName = "Ninos"
  private final val TTLIndexName = "TTL"

  override def indexes = Seq(
    Index(Seq(User.user_index_key -> Ascending), Some(UsersIndexName), unique = true),
    Index(Seq(User.nino_index_key -> Ascending), Some(NinosIndexName), unique = true, sparse = true),
    Index(
      Seq("planetId" -> Ascending),
      Some(TTLIndexName),
      partialFilter = Some(BSONDocument("isPermanent" -> BSONDocument("$eq" -> BSONBoolean(false)))),
      options = BSONDocument("expireAfterSeconds" -> 43200)
    )
  )

  def findByUserId(userId: String, planetId: String)(implicit ec: ExecutionContext): Future[Option[User]] =
    find(Seq(User.user_index_key -> Option(User.userIndexKey(userId, planetId))).map(option =>
      option._1 -> toJsFieldJsValueWrapper(option._2.get)): _*).map {
      case Nil      => None
      case x :: Nil => Some(x)
      case _ :: _   => throw DuplicateUserException(s"Duplicated userId $userId for $planetId")
    }

  def findByNino(nino: String, planetId: String)(implicit ec: ExecutionContext): Future[Option[User]] =
    find(Seq(User.nino_index_key -> Option(User.ninoIndexKey(nino, planetId))).map(option =>
      option._1 -> toJsFieldJsValueWrapper(option._2.get)): _*).map {
      case Nil      => None
      case x :: Nil => Some(x)
      case _ :: _   => throw DuplicateUserException(s"Duplicated nino $nino for $planetId")
    }

  def create(user: User, planetId: String)(implicit ec: ExecutionContext): Future[Unit] =
    insert(user.copy(planetId = Some(planetId), isPermanent = explicitFlag(user.isPermanent))).map(_ => ()).recover {
      case e: DatabaseException if e.code.contains(11000) =>
        throw DuplicateUserException(transformMessage(e.getMessage(), user, planetId))
    }

  def update(user: User, planetId: String)(implicit ec: ExecutionContext): Future[Unit] =
    (User.formats.writes(user.copy(planetId = Some(planetId), isPermanent = explicitFlag(user.isPermanent))) match {
      case u @ JsObject(_) =>
        collection.update(Json.obj(User.user_index_key -> User.userIndexKey(user.userId, planetId)), u, upsert = true)
      case _ =>
        Future.failed[WriteResult](new Exception("Cannot update User"))
    }).map(_ => ()).recover {
      case e: DatabaseException if e.code.contains(11000) =>
        throw DuplicateUserException(transformMessage(e.getMessage(), user, planetId))
    }

  def delete(userId: String, planetId: String)(implicit ec: ExecutionContext): Future[WriteResult] =
    remove(User.user_index_key -> Option(User.userIndexKey(userId, planetId)))

  val indexNameRegex = """\sindex\:\s(\w*?)\s""".r

  def transformMessage(msg: String, user: User, planetId: String): String =
    if (msg.contains("11000")) {
      indexNameRegex
        .findFirstMatchIn(msg)
        .map(_.group(1))
        .flatMap(i => duplicatedUserMessageByIndex.get(i).map(_(user)(planetId)))
        .getOrElse(s"Duplicated user setup on $planetId")
    } else msg

  val duplicatedUserMessageByIndex: Map[String, User => String => String] = Map(
    UsersIndexName -> (u => p => s"Duplicated userId ${u.userId} on $p"),
    NinosIndexName -> (u => p => s"Duplicated NINO ${u.nino.get} on $p")
  )

  private def explicitFlag(flag: Option[Boolean]): Option[Boolean] = flag match {
    case Some(true) => Some(true)
    case _          => Some(false)
  }

}
