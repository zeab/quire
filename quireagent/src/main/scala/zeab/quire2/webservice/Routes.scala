package zeab.quire2.webservice

//Imports
import java.util.UUID
import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.util.Timeout
import akka.{Done, NotUsed}
import zeab.quire2.internalkeeper.InternalKeeperMessages.{AddQueue, GetQueue, GetQueues}
import zeab.quire2.queue.QueueActor
import zeab.quire2.queue.QueueMessages.{Add, GetNext, Next}
import zeab.quire2.webservice.http.PostTopicProduceRequestBody
import zeab.quire2.webservice.ws.WebSocketMessages
import zeab.quire2.webservice.ws.WebSocketMessages.Msg
import zeab.quire2.websocketinterface.WebSocketInterfaceActor

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{FiniteDuration, _}
import scala.util.{Failure, Success}
//Circe and Akka-Http plugin
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import io.circe.syntax._
//Circe
import io.circe.generic.auto._
import io.circe.parser.decode

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

  def hookQueueToWs(webSocketInterface:ActorRef, queue: ActorRef): Flow[Message, Message, NotUsed] = {

    //Set up the incoming flow
    val incomingMessages: Sink[Message, NotUsed] =
      Flow[Message].map {
        case TextMessage.Strict(text) =>
          decode[Msg](text) match {
            case Right(msg) => msg
            case Left(ex) => ex.toString
          }
        case _ => "Unsupported Msg Type"
      }.to(Sink.actorRef(webSocketInterface, PoisonPill))

    //Set up the outgoing flow
    val outgoingMessages: Source[Message, NotUsed] =
      Source.actorRef[String](100, OverflowStrategy.dropTail)
        .mapMaterializedValue { outActor =>
          // give the user actor a way to send messages out
          val senderId: String = UUID.randomUUID().toString
          webSocketInterface ! (senderId, outActor, queue)
          outActor ! WebSocketMessages.Hello(senderId).asJson.noSpaces
          NotUsed
        }
        .map { outMsg => TextMessage(outMsg) }

    // then combine both to a flow
    Flow.fromSinkAndSource(incomingMessages, outgoingMessages)

  }

  //Routes dealing with basic ingress checks
  def topicRoute(implicit actorSystem: ActorSystem, internalKeeper: ActorRef): Route = {
    pathPrefix("topic") {
      get {
        path("ws") {
          parameters("name") { name =>
            onComplete(actorSystem.actorSelection("user/" + name).resolveOne()) {
              case Success(queue) =>
                val webSocketInterface: ActorRef = actorSystem.actorOf(Props[WebSocketInterfaceActor])
                handleWebSocketMessages(hookQueueToWs(webSocketInterface, queue))
              case Failure(exception) => complete(StatusCodes.InternalServerError, exception.toString)
            }
          }
        } ~
          path("info") {
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
            msg <- (actor ? GetNext).mapTo[Next]
          } yield msg
          onComplete(getNext) {
            case Success(next) => complete(StatusCodes.OK, next.msg)
            case Failure(ex) => complete(StatusCodes.InternalServerError, ex.toString)
          }
        }
      }
    }
  }

}
