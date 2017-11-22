package io.sudostream.userwriter.dao

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.stream.Materializer
import io.sudostream.timetoteach.messages.systemwide.model.{User, UserPreferences}
import io.sudostream.userwriter.config.ActorSystemWrapper
import org.mongodb.scala.Completed

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

class MongoDbUserWriterDao(mongoFindQueriesProxy: MongoInserterProxy,
                           actorSystemWrapper: ActorSystemWrapper) extends UserWriterDao {

  implicit val system: ActorSystem = actorSystemWrapper.system
  implicit val executor: ExecutionContextExecutor = system.dispatcher
  implicit val materializer: Materializer = actorSystemWrapper.materializer
  val logger: LoggingAdapter = system.log

  override def insertUser(userToInsert: User) : Future[Completed] = {
    logger.info(s"Inserting User to Database: ${userToInsert.toString}")
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

  override def updateUserPreferences(tttUserId: String, newUserPreferences: UserPreferences): Future[Completed] = ???

}
