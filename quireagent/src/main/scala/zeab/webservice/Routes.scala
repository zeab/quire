package zeab.webservice

//Imports
import java.util.UUID
import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.util.Timeout
import akka.{Done, NotUsed}
import zeab.internalkeeper.InternalKeeperMessages.{AddQueue, GetQueue, GetQueues}
import zeab.queue.QueueActor
import zeab.queue.QueueMessages.{Add, GetNext}
import zeab.webservice.http.PostTopicProduceRequestBody
import zeab.webservice.ws.WebSocketMessages

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{FiniteDuration, _}
import scala.util.{Failure, Success}
//Circe and Akka-Http plugin
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import io.circe.syntax._

object Routes {

  implicit val timeout: Timeout = Timeout(FiniteDuration(1, TimeUnit.SECONDS))

  //Collection of all the routes together in 1 big route
  def allRoutes(implicit actorSystem: ActorSystem, actorMaterializer: ActorMaterializer, executionContext: ExecutionContext, internalKeeper: ActorRef): Route =
    topicRoute ~ produceRoute ~ consumeRoute

  //topic
  //this creates, edit, list(details) and delete topics
  //topic/produce
  //add a message to a queue
  //topic/consume
  //get the next message from a queue

  //Routes dealing with basic ingress checks
  def topicRoute(implicit actorSystem: ActorSystem, internalKeeper: ActorRef): Route = {
    pathPrefix("topic") {
      get {
        path ("ws"){
          complete(StatusCodes.Created, s"11111")
        } ~
        path ("info"){
          parameters("name".?) {
              case Some(n) =>
                onComplete(internalKeeper ? GetQueue(n)) {
                  case Success(info) => complete(StatusCodes.Accepted, info.toString)
                  case Failure(ex) => complete(StatusCodes.InternalServerError, ex.toString)
                }
              case None =>
                onComplete(internalKeeper ? GetQueues) {
                  case Success(info) => complete(StatusCodes.Accepted, info.toString)
                  case Failure(ex) => complete(StatusCodes.InternalServerError, ex.toString)
                }

          }
        }
      }
    } ~
      pathPrefix("topic") {
        post {
          parameters("name") { name =>
            val masterQueueName: String = s"$name-MasterQueue-${UUID.randomUUID}"
            val queue: ActorRef = actorSystem.actorOf(Props(classOf[QueueActor], 100), masterQueueName)
            internalKeeper ! AddQueue(masterQueueName, queue)
            complete(StatusCodes.Created, s"Created $masterQueueName")
          }
        }
      }
  }

  def produceRoute(implicit actorSystem: ActorSystem): Route = {
    pathPrefix("produce") {
      post {
        parameters("name") { name =>
          decodeRequest {
            entity(as[PostTopicProduceRequestBody]) { msg =>
              onComplete(actorSystem.actorSelection("user/" + name).resolveOne()) {
                case Success(queue) =>
                  queue ! Add(msg.msg)
                  complete(StatusCodes.Accepted, "Message queued")
                case Failure(ex) => complete(StatusCodes.InternalServerError, ex.toString)
              }
            }
          }
        }
      }
    }
  }

  def consumeRoute(implicit actorSystem: ActorSystem, executionContext: ExecutionContext): Route = {
    implicit val timeout: Timeout = Timeout(5.second)
    pathPrefix("consume") {
      get {
        parameters("name") { name =>
          val getNext = for {
            actor <- actorSystem.actorSelection("user/" + name).resolveOne()
            msg <- (actor ? GetNext).mapTo[String]
          } yield msg
          onComplete(getNext) {
            case Success(next) => complete(StatusCodes.OK, next)
            case Failure(ex) => complete(StatusCodes.InternalServerError, ex.toString)
          }
        }
      }
    }
  }


  //Routes dealing with basic ingress checks
  def topicRoute1(implicit actorSystem: ActorSystem, actorMaterializer: ActorMaterializer): Route = {
    pathPrefix("topic") {
      pathPrefix("produce") {
        post {
          parameters("name") { name =>
            decodeRequest {
              entity(as[PostTopicProduceRequestBody]) { msg =>
                onComplete(actorSystem.actorSelection("user/" + name).resolveOne()) {
                  case Success(queue) =>
                    queue ! msg
                    complete(StatusCodes.Accepted, "message queued")
                  case Failure(ex) => complete(StatusCodes.InternalServerError, ex.toString)
                }
              }
            }
          }
        }
      } ~
        get {
          //Used to query all the known queues running in this quire
          complete(StatusCodes.InternalServerError, "asdasd")
        } ~
        post {
          parameters("name") { name =>
            val masterQueueName: String = s"$name-MasterQueue-${UUID.randomUUID}"
            actorSystem.actorOf(Props(classOf[QueueActor], 100), masterQueueName)
            complete(StatusCodes.Created, s"Created $masterQueueName")
          }
        } ~
        put {
          complete(StatusCodes.Accepted, s"Get topic")
        } ~
        delete {
          complete(StatusCodes.Accepted, s"Get topic")
        }
    }
  }

  //Routes dealing with basic ingress checks
  def topicRoute2(implicit actorSystem: ActorSystem, actorMaterializer: ActorMaterializer): Route = {
    pathPrefix("topic") {
      get {
        onComplete(actorSystem.actorSelection("user/" + "MasterQueue").resolveOne()) {
          case Success(queue) => // logic with the actorRef
            def hookQueueToWs(router: ActorRef): Flow[Message, Message, NotUsed] = {

              //Set up the incoming flow
              val incomingMessages: Sink[Message, NotUsed] =
                Flow[Message].map {
                  case TextMessage.Strict(text) => text
                  case _ => "Unsupported Msg Type"
                }.to(Sink.actorRef(router, Done))

              //Set up the outgoing flow
              val outgoingMessages: Source[Message, NotUsed] =
                Source.actorRef[String](100, OverflowStrategy.dropTail)
                  .mapMaterializedValue { outActor =>
                    // give the user actor a way to send messages out
                    val uuid: String = UUID.randomUUID().toString
                    router ! (uuid, outActor)
                    outActor ! WebSocketMessages.Hello(uuid).asJson.noSpaces
                    NotUsed
                  }
                  .map { outMsg => TextMessage(outMsg) }

              // then combine both to a flow
              Flow.fromSinkAndSource(incomingMessages, outgoingMessages)

            }

            handleWebSocketMessages(hookQueueToWs(queue))

          case Failure(ex) =>
            complete(StatusCodes.InternalServerError, ex.toString)
        }
      } ~
        post {
          actorSystem.actorOf(Props(classOf[QueueActor], 100), "MasterQueue")
          complete(StatusCodes.Accepted, s"Post topic")
        } ~
        put {
          complete(StatusCodes.Accepted, s"Get topic")
        } ~
        delete {
          complete(StatusCodes.Accepted, s"Get topic")
        }
    }
  }

}
