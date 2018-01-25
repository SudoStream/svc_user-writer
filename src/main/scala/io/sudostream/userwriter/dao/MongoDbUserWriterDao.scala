package io.sudostream.userwriter.dao

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.stream.Materializer
import io.sudostream.timetoteach.messages.systemwide.model.{User, UserPreferences}
import io.sudostream.userwriter.config.ActorSystemWrapper
import org.mongodb.scala.Completed
import org.mongodb.scala.bson.{BsonDocument, BsonString}
import org.mongodb.scala.result.UpdateResult

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

class MongoDbUserWriterDao(mongoFindQueriesProxy: MongoInserterProxy,
                           actorSystemWrapper: ActorSystemWrapper) extends UserWriterDao {

  implicit val system: ActorSystem = actorSystemWrapper.system
  implicit val executor: ExecutionContextExecutor = system.dispatcher
  implicit val materializer: Materializer = actorSystemWrapper.materializer
  val logger: LoggingAdapter = system.log

  def userAlreadyExistsInTheDatabase(userToInsert: User): Future[Boolean] = {
    val maybeSocialIdDetails = userToInsert.socialNetworkIds.headOption
    val maybeEventualUserExists = for {
      socialIdDetailsWrapper <- maybeSocialIdDetails
      socialIdMatcher = BsonDocument(
        "socialNetwork" -> BsonString(socialIdDetailsWrapper.socialNetworkId.socialNetwork.toString.toUpperCase),
        "id" -> BsonString(socialIdDetailsWrapper.socialNetworkId.id)
      )
    } yield mongoFindQueriesProxy.doesUserExist(socialIdMatcher)

    maybeEventualUserExists match {
      case Some(eventualUserExists) =>
        eventualUserExists.map { userExists =>
          if (userExists) {
            logger.warning(s"User already exists the in the database : ${userToInsert.toString}")
          } else {
            logger.info(s"User can now be inserted as not currently signed up: ${userToInsert.toString}")
          }
        }
        eventualUserExists
      case None =>
        logger.info(s"Couldn't find the user in the database : ${userToInsert.toString}")
        Future {
          false
        }
    }
  }

  override def insertUser(userToInsert: User): Future[Completed] = {
    logger.info(s"Inserting User to Database: ${userToInsert.toString}")
    val eventualUserAlreadyExists = for {
      userAlreadyExists <- userAlreadyExistsInTheDatabase(userToInsert)
    } yield userAlreadyExists

    val eventualFutureCompleted = eventualUserAlreadyExists.map { userExists =>
      if (userExists) {
        val errorMsg = s"There is already a user with the same social ids : ${userToInsert.toString}"
        logger.warning(errorMsg)
        Future {
          Completed()
        }
      } else {
        val insertCompleted = mongoFindQueriesProxy.insertUser(userToInsert)
        insertCompleted.onComplete {
          case Success(completed) =>
            logger.info(s"Successfully inserted user ${userToInsert.fullName}")
          case Failure(t) =>
            val errorMsg = s"Failed to inserted user ${userToInsert.fullName}" +
              s" with error ${t.getMessage}. Full stack trace .... ${t.getStackTrace.toString}"
            logger.error(errorMsg)
        }
        insertCompleted
      }
    }

    eventualFutureCompleted.flatMap(res => res)
  }

  override def updateUserPreferences(tttUserId: String, newUserPreferences: UserPreferences): Future[UpdateResult] = {
    logger.info(s"Updating User Preferences to Database: ${
      newUserPreferences.toString
    }")
    val updateCompleted = mongoFindQueriesProxy.updateUserPreferences(tttUserId, newUserPreferences)

    updateCompleted.onComplete {
      case Success(completed) =>
        logger.info(s"Successfully updated user preferences ${
          newUserPreferences.toString
        }")
      case Failure(t) =>
        val errorMsg = s"Failed to inserted user ${
          newUserPreferences.toString
        }" +
          s" with error ${
            t.getMessage
          }. Full stack trace .... ${
            t.getStackTrace.toString
          }"
        logger.error(errorMsg)
    }

    updateCompleted
  }

}
