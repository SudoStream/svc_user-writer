package io.sudostream.userwriter.api.http

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives.{path, _}
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.util.Timeout
import io.sudostream.timetoteach.kafka.serializing.systemwide.model.{UserDeserializer, UserPreferencesDeserializer}
import io.sudostream.timetoteach.messages.systemwide.model.{User, UserPreferences}
import io.sudostream.userwriter.api.kafka.StreamingComponents
import io.sudostream.userwriter.config.ActorSystemWrapper
import io.sudostream.userwriter.dao.UserWriterDao

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

class HttpRoutes(userDao: UserWriterDao,
                 actorSystemWrapper: ActorSystemWrapper,
                 streamingComponents: StreamingComponents)
  extends Health {

  implicit val system: ActorSystem = actorSystemWrapper.system
  implicit val executor: ExecutionContextExecutor = system.dispatcher
  implicit val materializer: Materializer = actorSystemWrapper.materializer
  val logger: LoggingAdapter = system.log

  implicit val timeout: Timeout = Timeout(30 seconds)

  val routes: Route =
    path("api" / "users") {
      post {
        decodeRequest {
          entity(as[HttpEntity]) { entity =>
            val smallTimeout = 3000.millis
            val dataFuture = entity.toStrict(smallTimeout) map {
              httpEntity =>
                httpEntity.getData()
            }

            val userExtractedFuture: Future[User] = dataFuture map {
              databytes =>
                val bytesAsArray = databytes.toArray
                val userDeserializer = new UserDeserializer
                userDeserializer.deserialize("ignore", bytesAsArray)
            }

            val insertUserEventualFuture = for {
              theUser <- userExtractedFuture
              insertUserFuture = userDao.insertUser(theUser)
            } yield (insertUserFuture, theUser)

            val insertFutureCompleted = {
              insertUserEventualFuture map { tuple => tuple._1 }
            }.flatMap(fut => fut)

            onComplete(insertFutureCompleted) {
              case Success(insertCompleted) =>
                // TODO: To get here the user future must be completed and successful but my copmosing skills are lacking!
                val theUser = userExtractedFuture.value.get.get
                logger.info(s"Deserialised user: ${theUser.toString}")
                complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, s"timeToTeachId=${theUser.timeToTeachId}"))

              case Failure(ex) => logger.error(s"Failed to deserialse user, ${ex.getMessage} : ${ex.getStackTrace.toString}")
                complete(StatusCodes.InternalServerError, ex.getMessage)
            }
          }
        }
      }
    } ~ path("api" / "users" / Segment / "editprefs") { (tttUserId) =>
      post {
        decodeRequest {
          entity(as[HttpEntity]) { entity =>
            val smallTimeout = 3000.millis
            val dataFuture = entity.toStrict(smallTimeout) map {
              httpEntity =>
                httpEntity.getData()
            }

            val userPreferencesExtractedFuture: Future[UserPreferences] = dataFuture map {
              databytes =>
                val bytesAsArray = databytes.toArray
                val userPreferencesDeserializer = new UserPreferencesDeserializer
                userPreferencesDeserializer.deserialize("ignore", bytesAsArray)
            }

            val updateUserPreferencesEventualFuture = for {
              theUserPreferences <- userPreferencesExtractedFuture
              updateUserPreferencesFuture = userDao.updateUserPreferences(tttUserId, theUserPreferences)
            } yield (updateUserPreferencesFuture, theUserPreferences)

            val updateFutureCompleted = {
              updateUserPreferencesEventualFuture map { tuple => tuple._1 }
            }.flatMap(fut => fut)

            onComplete(updateFutureCompleted) {
              case Success(updateResult) =>
                // TODO: To get here the user future must be completed and successful but my copmosing skills are lacking!
                val theUserPreferences = userPreferencesExtractedFuture.value.get.get
                logger.info(s"Deserialised user preferences: ${theUserPreferences.toString}")
                complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, s"updateResult=${updateResult.toString}"))

              case Failure(ex) => logger.error(s"Failed to deserialse user, ${ex.getMessage} : ${ex.getStackTrace.toString}")
                complete(StatusCodes.InternalServerError, ex.getMessage)
            }
          }
        }
      }
    } ~ health

}