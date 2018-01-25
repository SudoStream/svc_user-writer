package io.sudostream.userwriter.dao

import io.sudostream.timetoteach.messages.systemwide.model.{User, UserPreferences}
import org.mongodb.scala.bson.{BsonArray, BsonDocument, BsonString}
import org.mongodb.scala.result.UpdateResult
import org.mongodb.scala.{Completed, Document, MongoCollection}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object MongoInserterProxyImpl {

  def convertListOfTuplesToMap(listOfTuples: List[(String, BsonDocument)]): Map[String, BsonArray] = {
    def mapBuilder(currentMap: Map[String, BsonArray],
                   tupleToAdd: (String, BsonDocument),
                   restOfTupleList: List[(String, BsonDocument)]
                  ): Map[String, BsonArray] = {
      if (restOfTupleList.isEmpty) addToMap(currentMap, tupleToAdd)
      else {
        val newMap: Map[String, BsonArray] = addToMap(currentMap, tupleToAdd)
        mapBuilder(newMap, restOfTupleList.head, restOfTupleList.tail)
      }
    }

    mapBuilder(Map(), listOfTuples.head, listOfTuples.tail)
  }

  private def addToMap(currentMap: Map[String, BsonArray], tupleToAdd: (String, BsonDocument)) = {
    val bsonArray = currentMap.get(tupleToAdd._1) match {
      case Some(currentValue) =>
        currentValue.add(tupleToAdd._2)
        println(s"the current val = ${currentValue.toString}")
        currentValue
      case None => BsonArray(tupleToAdd._2)
    }
    val newMap = currentMap + (tupleToAdd._1 -> bsonArray)
    newMap
  }
}

class MongoInserterProxyImpl(mongoDbConnectionWrapper: MongoDbConnectionWrapper) extends MongoInserterProxy {

  val usersCollection: MongoCollection[Document] = mongoDbConnectionWrapper.getUsersCollection

  override def insertUser(userToInsert: User): Future[Completed] = {
    val userToInsertAsDocument = convertUserToDocument(userToInsert)
    val observable = usersCollection.insertOne(userToInsertAsDocument)
    observable.toFuture()
  }

  override def updateUserPreferences(tttUserId: String, newUserPreferences: UserPreferences): Future[UpdateResult] = {
    val newUserPreferencesAsDocument = convertUserPreferencesToDocument(newUserPreferences)
    val observable = usersCollection.updateOne(
      BsonDocument("_id" -> BsonString(tttUserId)),
      BsonDocument(
        "$set" -> BsonDocument(
          "userPreferences" -> newUserPreferencesAsDocument
        )
      )
    )
    observable.toFuture()
  }

  private[dao] def convertUserToDocument(userToConvert: User): Document = {

    val socialNetworkIdsAsDocuments = for {
      socialNetworkId <- userToConvert.socialNetworkIds
      socialNetworkName = socialNetworkId.socialNetworkId.socialNetwork
      socialId = socialNetworkId.socialNetworkId.id
    } yield Document(
      "socialNetwork" -> BsonString(socialNetworkName.toString.toUpperCase),
      "id" -> BsonString(socialId))

    val emailsAsDocuments = for {
      emailDetails <- userToConvert.emails
      emailAddress = emailDetails.emailAddress
      emailValidated = emailDetails.validated
      emailPreferred = emailDetails.preferred
    } yield Document(
      "emailAddress" -> emailAddress,
      "validated" -> emailValidated,
      "preferred" -> emailPreferred
    )

    val schoolsAsDocuments = for {
      schoolWrapper <- userToConvert.schools
      school = schoolWrapper.school
      schoolId = school.id
      schoolName = school.name
      schoolAddress = school.address
      schoolPostCode = school.postCode
      schoolTelephone = school.telephone
      schoolLocalAuthority = school.localAuthority.toString.toUpperCase
      schoolCountry = school.country.toString.toUpperCase
    } yield Document(
      "_id" -> schoolId,
      "name" -> schoolName,
      "address" -> schoolAddress,
      "postCode" -> schoolPostCode,
      "telephone" -> schoolTelephone,
      "localAuthority" -> schoolLocalAuthority,
      "country" -> schoolCountry
    )

    Document(
      "_id" -> userToConvert.timeToTeachId,
      "socialNetworkIds" -> socialNetworkIdsAsDocuments,
      "fullName" -> userToConvert.fullName,
      "givenName" -> userToConvert.givenName,
      "familyName" -> userToConvert.familyName,
      "imageUrl" -> userToConvert.imageUrl,
      "emails" -> emailsAsDocuments,
      "userRole" -> userToConvert.userRole.toString.toUpperCase,
      "userAccountCreated" -> Document(
        "dateSignedUp_Iso8601" -> userToConvert.userAccountCreated.dateSignedUp_Iso8601,
        "timeSignedUp_Iso8601" -> userToConvert.userAccountCreated.timeSignedUp_Iso8601
      ),
      "schools" -> schoolsAsDocuments,
      "userPreferences" -> BsonDocument()
    )
  }

  private[dao] def convertUserPreferencesToDocument(userPrefsToConvert: UserPreferences): Document = {
    val allSchoolTimesAsDocuments = for {
      schoolTimes <- userPrefsToConvert.allSchoolTimes
      schoolId = schoolTimes.schoolId
      schoolStartTime = schoolTimes.schoolStartTime
      schoolEndTime = schoolTimes.schoolEndTime
      morningBreakStartTime = schoolTimes.morningBreakStartTime
      morningBreakEndTime = schoolTimes.morningBreakEndTime
      lunchStartTime = schoolTimes.lunchStartTime
      lunchEndTime = schoolTimes.lunchEndTime
    } yield Document(
      "schoolId" -> BsonString(schoolId),
      "schoolStartTime" -> BsonString(schoolStartTime),
      "morningBreakStartTime" -> BsonString(morningBreakStartTime),
      "morningBreakEndTime" -> BsonString(morningBreakEndTime),
      "lunchStartTime" -> BsonString(lunchStartTime),
      "lunchEndTime" -> BsonString(lunchEndTime),
      "schoolEndTime" -> BsonString(schoolEndTime)
    )

    Document(
      "allSchoolTimes" -> allSchoolTimesAsDocuments
    )
  }

  override def doesUserExist(matcher: BsonDocument): Future[Boolean] = {
    val usersFoundResult = usersCollection.find(matcher).toFuture
    usersFoundResult.map { usersFound =>
      if (usersFound.nonEmpty) {
        true
      } else {
        false
      }
    }
  }

}
