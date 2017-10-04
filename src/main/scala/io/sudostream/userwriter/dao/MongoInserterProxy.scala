package io.sudostream.userwriter.dao

import io.sudostream.timetoteach.messages.systemwide.model.User
import org.mongodb.scala.Completed

import scala.concurrent.Future

trait MongoInserterProxy {

  def insertUser(userToInsert: User): Future[Completed]
  
}
