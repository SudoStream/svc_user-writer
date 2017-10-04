package io.sudostream.userwriter.dao

import akka.event.Logging
import io.sudostream.timetoteach.messages.systemwide.model.User
import org.mongodb.scala.bson.{BsonArray, BsonString}
import org.mongodb.scala.{Completed, Document, MongoCollection}

import scala.concurrent.Future

class MongoInserterProxyImpl(mongoDbConnectionWrapper: MongoDbConnectionWrapper) extends MongoInserterProxy {

  val usersCollection: MongoCollection[Document] = mongoDbConnectionWrapper.getUsersCollection

  override def insertUser(userToInsert: User): Future[Completed] = {
    val userToInsertAsDocument = convertUserToDocument(userToInsert)
    val observable = usersCollection.insertOne(userToInsertAsDocument)
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
      "schools" -> schoolsAsDocuments
    )
  }

}
