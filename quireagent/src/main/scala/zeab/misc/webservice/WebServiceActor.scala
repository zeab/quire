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
  val log: LoggingAdapter = Logging(context.system, this)
  implicit val actorSystem: ActorSystem = context.system
  implicit val ec: ExecutionContext = context.dispatcher

  //Receive
  def receive: Receive = disconnected

  //Behaviors
  def disconnected: Receive = {
    case StopService => log.warning(s"Web Server ${self.path.name} is already disconnected")
    case message: StartService =>
      log.info(s"Web Server ${self.path.name} starting binding attempt")
      context.become(connecting)
      Http().bindAndHandle(message.routes, message.host, message.port.toInt).pipeTo(self)
  }

  def connected(webServer: Http.ServerBinding): Receive = {
    case StopService =>
      log.info(s"Web Server ${self.path.name} offline ${webServer.localAddress}")
      webServer.unbind()
      context.become(disconnected)
    case _: StartService => log.warning(s"Web Server ${self.path.name} already connected")
  }

  def connecting: Receive = {
    case StopService =>
      log.warning(s"Web Server ${self.path.name} is disconnecting while connecting")
      context.become(disconnected)
      self ! StopService
    case message: Http.ServerBinding =>
      context.become(connected(message))
      log.info(s"Web Server ${self.path.name} online ${message.localAddress}")
    case _: StartService => log.warning(s"Web Server ${self.path.name} is already attempting to establish binding")
  }

  //Lifecycle Hooks
  /** Log Name on Start */
  override def preStart: Unit = {
    log.debug(s"Starting ${this.getClass.getName}")
  }

  /** Log Name on Stop */
  override def postStop: Unit = {
    log.debug(s"Stopping ${this.getClass.getName}")
  }

}
