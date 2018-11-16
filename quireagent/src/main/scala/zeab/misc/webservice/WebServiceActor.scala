package zeab.misc.webservice

//Imports
import zeab.misc.webservice.WebServiceMessages.{StartService, StopService}
//Akka
import akka.actor.{Actor, ActorSystem}
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.pattern.pipe
import akka.stream.ActorMaterializer
//Scala
import scala.concurrent.ExecutionContext

/** A ready to go web server bundled inside a web server */
class WebServiceActor(implicit val actorMaterializer: ActorMaterializer) extends Actor {

  //Actor Settings
  val actorLog: LoggingAdapter = Logging(context.system, this)
  implicit val actorSystem: ActorSystem = context.system
  implicit val ec: ExecutionContext = context.dispatcher

  //Receive
  def receive: Receive = disconnected

  //Behaviors
  def disconnected: Receive = {
    case StopService => actorLog.warning(s"Web Server ${self.path.name} is already disconnected")
    case message: StartService =>
      actorLog.info(s"Web Server ${self.path.name} starting binding attempt")
      context.become(connecting)
      Http().bindAndHandle(message.routes, message.host, message.port.toInt).pipeTo(self)
  }

  def connected(webServer: Http.ServerBinding): Receive = {
    case StopService =>
      actorLog.info(s"Web Server ${self.path.name} offline ${webServer.localAddress}")
      webServer.unbind()
      context.become(disconnected)
    case _: StartService => actorLog.warning(s"Web Server ${self.path.name} already connected")
  }

  def connecting: Receive = {
    case StopService =>
      actorLog.warning(s"Web Server ${self.path.name} is disconnecting while connecting")
      context.become(disconnected)
      self ! StopService
    case message: Http.ServerBinding =>
      context.become(connected(message))
      actorLog.info(s"Web Server ${self.path.name} online ${message.localAddress}")
    case _: StartService => actorLog.warning(s"Web Server ${self.path.name} is already attempting to establish binding")
  }

  //Lifecycle Hooks
  /** Log Name on Start */
  override def preStart: Unit = {
    actorLog.debug(s"Starting ${this.getClass.getName}")
  }

  /** Log Name on Stop */
  override def postStop: Unit = {
    actorLog.debug(s"Stopping ${this.getClass.getName}")
  }

}
