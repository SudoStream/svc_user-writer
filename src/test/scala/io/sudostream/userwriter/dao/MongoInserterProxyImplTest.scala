package io.sudostream.userwriter.dao

import java.util

import io.sudostream.timetoteach.messages.scottish.ScottishCurriculumLevel
import io.sudostream.timetoteach.messages.systemwide.model._
import org.bson.BsonValue
import org.mongodb.scala.Document
import org.mongodb.scala.bson.{BsonArray, BsonBoolean, BsonDocument, BsonString}
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

  test("Test Convert UserPreferences To Document Happy path") {
    val mongoInserterProxy = new MongoInserterProxyImpl(connectionWrapperMock)

    val userPrefs: UserPreferences = createHappyPathUserPreferences()
    val userPrefsDoc: Document = mongoInserterProxy.convertUserPreferencesToDocument(userPrefs)

    println("-------------------------")
    println("------ User Prefs -------")
    println(userPrefsDoc.toString())
    println("-------------------------")
    println("-------------------------")


    assert(userPrefsDoc.get[BsonArray]("allSchoolTimes").size == 1)
  }

  test("Happy test of convertListOfTuplesToMap") {
    val listOfTuples: List[(String, BsonDocument)] = List(
      ("apple",
        BsonDocument("hello" -> BsonString("there"))),
      ("apple",
        BsonDocument("another" -> BsonString("apple"))),
      ("banana",
        BsonDocument("okay" -> BsonString("dokes")))
    )

    val mapNow = MongoInserterProxyImpl.convertListOfTuplesToMap(listOfTuples)

    assert(mapNow.size == 2)
    assert(mapNow("apple").size() == 2)
    assert(mapNow("banana").size() == 1)
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
      userAccountCreated = UserAccountCreatedDetails(
        dateSignedUp_Iso8601 = "2017-02-25",
        timeSignedUp_Iso8601 = "19:07"
      ),
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
      ),
      userPreferences = None
    )
  }

  def createHappyPathUserPreferences(): UserPreferences = {
    UserPreferences(
      allSchoolTimes = List(
        SchoolTimes(
          schoolId = "school123",
          schoolStartTime = "09:00 AM",
          morningBreakStartTime = "10:30 AM",
          morningBreakEndTime = "10:45 AM",
          lunchStartTime = "12:00 PM",
          lunchEndTime = "01:00 PM",
          schoolEndTime = "3:00 PM",
          userTeachesTheseClasses = List(
            SchoolClass(
              className = "P1AB",
              curriculumLevels = List(
                CurriculumLevelWrapper(
                  CurriculumLevel(
                    country = Country.SCOTLAND,
                    scottishCurriculumLevel = Some(ScottishCurriculumLevel.EARLY)
                  )
                ),
                CurriculumLevelWrapper(
                  CurriculumLevel(
                    country = Country.SCOTLAND,
                    scottishCurriculumLevel = Some(ScottishCurriculumLevel.FIRST)
                  )
                )
              )
            )
          )
        )
      )
    )
  }


}
