package zeab.quire.webservice

import java.util.UUID
import java.util.concurrent.TimeUnit

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.Timeout
import zeab.quire.queue.QueueActor
import zeab.quire.wsinterface.WSInterface
import zeab.quire2.webservice.ws.WebSocketMessages
import zeab.quire2.webservice.ws.WebSocketMessages.Msg

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}
//Circe and Akka-Http plugin
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.syntax._
import io.circe.generic.auto._
import io.circe.parser.decode

object Routes {

  implicit val timeout: Timeout = Timeout(FiniteDuration(1, TimeUnit.SECONDS))

  def allRoutes(implicit actorSystem: ActorSystem, actorMaterializer: ActorMaterializer, executionContext: ExecutionContext): Route =
    topicRoute

  def hookQueueToWs(webSocketInterface:ActorRef, queue: ActorRef): Flow[Message, Message, NotUsed] = {

    //Where to put the incoming messages
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
  def topicRoute(implicit actorSystem: ActorSystem): Route = {
    pathPrefix("topic") {
      get {
        path("ws") {
          parameters("name") { name =>
            onComplete(actorSystem.actorSelection("user/" + name).resolveOne()) {
              case Success(queue) =>
                val webSocketInterface: ActorRef = actorSystem.actorOf(Props[WSInterface])
                handleWebSocketMessages(hookQueueToWs(webSocketInterface, queue))
              case Failure(exception) => complete(StatusCodes.InternalServerError, exception.toString)
            }
          }
        }
      }
    } ~
      pathPrefix("topic") {
        post {
          parameters("name") { name =>
            val masterQueueName: String = s"$name-MasterQueue-${UUID.randomUUID}"
            val queue: ActorRef = actorSystem.actorOf(Props(classOf[QueueActor]), masterQueueName)
            complete(StatusCodes.Created, s"Created $masterQueueName")
          }
        }
      }
  }

}
