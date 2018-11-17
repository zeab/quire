package zeab.websocketinterface

//Imports
import akka.actor.{Actor, ActorRef}
import akka.event.{Logging, LoggingAdapter}
import zeab.queue.QueueMessages.{Add, GetNext, Next}
import zeab.webservice.ws.WebSocketMessages.Msg

class WebSocketInterfaceActor extends Actor{

  //Actor Settings
  val actorLog: LoggingAdapter = Logging(context.system, this)

  //Receive
  def receive: Receive = inactive

  //Behaviors
  def inactive: Receive = {
    case (senderId: String, webSocketOut: ActorRef, queue:ActorRef) => context.become(active(senderId, webSocketOut, queue))
  }

  def active(senderId: String, webSocketOut: ActorRef, queue: ActorRef): Receive = {
    case m: String => println(m)
    case m: Msg =>
      m.`type`.toLowerCase match {
        case "p" =>
          queue ! Add(m.msg)
          webSocketOut ! "Msg Queued"
        case "c" =>
          queue ! GetNext
      }
    case m: Next =>
      webSocketOut ! m.msg
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
