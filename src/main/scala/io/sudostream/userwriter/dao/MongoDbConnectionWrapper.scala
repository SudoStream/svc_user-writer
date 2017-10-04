package io.sudostream.userwriter.dao

import org.mongodb.scala.{Document, MongoCollection}

trait MongoDbConnectionWrapper {

  def getUsersCollection: MongoCollection[Document]

}
