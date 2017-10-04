package io.sudostream.userwriter.dao

import java.net.URI
import javax.net.ssl.{HostnameVerifier, SSLSession}

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.stream.Materializer
import com.mongodb.connection.ClusterSettings
import com.typesafe.config.ConfigFactory
import io.sudostream.userwriter.config.ActorSystemWrapper
import org.mongodb.scala.connection.{NettyStreamFactoryFactory, SslSettings}
import org.mongodb.scala.{Document, MongoClient, MongoClientSettings, MongoCollection, MongoDatabase, ServerAddress}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContextExecutor

sealed class MongoDbConnectionWrapperImpl(actorSystemWrapper: ActorSystemWrapper) extends MongoDbConnectionWrapper {

  implicit val system: ActorSystem = actorSystemWrapper.system
  implicit val executor: ExecutionContextExecutor = system.dispatcher
  implicit val materializer: Materializer = actorSystemWrapper.materializer
  val log: LoggingAdapter = system.log

  private val config = ConfigFactory.load()
  private val mongoDbUriString = config.getString("mongodb.connection_uri")
  private val mongoDbUri = new URI(mongoDbUriString)
  private val usersDatabaseName = config.getString("user-writer.database_name")
  private val usersCollectionName = config.getString("user-writer.users_collection")

  private val isLocalMongoDb: Boolean = try {
    if (sys.env("LOCAL_MONGO_DB") == "true") true else false
  } catch {
    case e: Exception => false
  }

  log.info(s"Running Local = $isLocalMongoDb")

  def getUsersCollection: MongoCollection[Document] = {
    def createMongoClient: MongoClient = {
      if (isLocalMongoDb) {
        buildLocalMongoDbClient
      } else {
        log.info(s"connecting to mongo db at '${mongoDbUri.getHost}:${mongoDbUri.getPort}'")
        System.setProperty("org.mongodb.async.type", "netty")
        MongoClient(mongoDbUriString)
      }
    }

    val mongoClient = createMongoClient
    val database: MongoDatabase = mongoClient.getDatabase(usersDatabaseName)
    database.getCollection(usersCollectionName)
  }

  private def buildLocalMongoDbClient = {
    val mongoKeystorePassword = try {
      sys.env("MONGODB_KEYSTORE_PASSWORD")
    } catch {
      case e: Exception => ""
    }

    val mongoDbHost = mongoDbUri.getHost
    val mongoDbPort = mongoDbUri.getPort
    println(s"mongo host = '$mongoDbHost'")
    println(s"mongo port = '$mongoDbPort'")

    val clusterSettings: ClusterSettings = ClusterSettings.builder().hosts(
      List(new ServerAddress(mongoDbHost, mongoDbPort)).asJava).build()

    val mongoSslClientSettings = MongoClientSettings.builder()
      .sslSettings(SslSettings.builder()
        .enabled(true)
        .invalidHostNameAllowed(true)
        .build())
      .streamFactoryFactory(NettyStreamFactoryFactory())
      .clusterSettings(clusterSettings)
      .build()

    MongoClient(mongoSslClientSettings)
  }

}

class AcceptAllHostNameVerifier extends HostnameVerifier {
  override def verify(s: String, sslSession: SSLSession) = true
}
