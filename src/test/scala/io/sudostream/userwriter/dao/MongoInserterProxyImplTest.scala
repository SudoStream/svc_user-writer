package io.sudostream.userwriter.dao

import java.util

import io.sudostream.timetoteach.messages.systemwide.model._
import org.bson.BsonValue
import org.mongodb.scala.Document
import org.mongodb.scala.bson.{BsonArray, BsonBoolean, BsonString}
import org.scalatest.FunSuite
import org.scalatest.mockito.MockitoSugar

import scala.collection.immutable

class MongoInserterProxyImplTest extends FunSuite with MockitoSugar {

  val connectionWrapperMock = mock[MongoDbConnectionWrapper]

  test("Test Convert User To Document Happy path") {
    val mongoInserterProxy = new MongoInserterProxyImpl(connectionWrapperMock)

    val user: User = createHappyPathUser()
    val userDoc: Document = mongoInserterProxy.convertUserToDocument(user)
    assert(userDoc.getString("_id") === "id12345")

    println("-------------------")
    println(userDoc.toString())
    println("-------------------")
  }

  def createHappyPathUser(): User = {
    User(
      timeToTeachId = "id12345",
      socialNetworkIds = List(SocialNetworkIdWrapper(SocialNetworkId(
        socialNetwork = SocialNetwork.FACEBOOK,
        id = "facebook123"
      ))),
      fullName = "Andy Boyle",
      givenName = Some("Andy"),
      familyName = Some("Boyle"),
      imageUrl = Some("https://some/photo"),
      emails = List(
        EmailDetails(
          emailAddress = "andy@g.com",
          validated = true,
          preferred = true
        )
      ),
      userRole = UserRole.TEACHER,
      schools = List(
        SchoolWrapper(
          School(id = "school1234321",
            name = "MySchool",
            address = "123 Some Street, And A Town",
            postCode = "AB1 2WQ",
            telephone = "123211",
            localAuthority = LocalAuthority.SCOTLAND__ABERDEEN_CITY,
            country = Country.SCOTLAND
          )
        )
      )
    )
  }

}
