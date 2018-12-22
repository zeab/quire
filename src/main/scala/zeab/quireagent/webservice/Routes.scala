package zeab.quireagent.webservice

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import zeab.quireagent.quireconductor.QuireConductorMessages.{CreateTopic, ProduceMsg}
import zeab.quireagent.webservice.http.{PostProduceRequestBody, PostTopicRequestBody}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

//Circe
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

object Routes {

  implicit val timeout: Timeout = Timeout(5.second)

  //Collection of all the routes together in 1 big route
  def allRoutes(implicit actorSystem: ActorSystem, actorMaterializer: ActorMaterializer, executionContext: ExecutionContext, quireConductor: ActorRef): Route =
    topicRoute ~ produceRoute ~ consumeRoute

  def topicRoute(implicit actorSystem: ActorSystem, quireConductor: ActorRef): Route = {
    pathPrefix("topic") {
      post {
        decodeRequest {
          entity(as[PostTopicRequestBody]) { topic =>
            onComplete(quireConductor ? CreateTopic(topic.topic)) {
              case Success(_) => complete(StatusCodes.Accepted, s"${topic.topic}")
              case Failure(ex) => complete(StatusCodes.InternalServerError, ex.toString)
            }
          }
        }
      }
    }
  }

  def produceRoute(implicit actorSystem: ActorSystem, quireConductor: ActorRef): Route = {
    pathPrefix("produce") {
      post {
        decodeRequest {
          entity(as[PostProduceRequestBody]) { msgToProduce =>
            onComplete(quireConductor ? ProduceMsg(msgToProduce.topic, msgToProduce.msg)) {
              case Success(_) => complete(StatusCodes.Accepted, "Message queued")
              case Failure(ex) => complete(StatusCodes.InternalServerError, ex.toString)
            }
          }
        }
      }
    }
  }

  def consumeRoute(implicit actorSystem: ActorSystem, quireConductor: ActorRef): Route = {
    pathPrefix("consume") {
      get {
        decodeRequest {
          entity(as[PostProduceRequestBody]) { msgToProduce =>
            onComplete(quireConductor ? ProduceMsg(msgToProduce.topic, msgToProduce.msg)) {
              case Success(next) => complete(StatusCodes.Accepted, s"$next")
              case Failure(ex) => complete(StatusCodes.InternalServerError, ex.toString)
            }
          }
        }
      }
    }
  }

}
