package zeab.websocketinterface

//Imports
import akka.actor.{Actor, ActorRef}
import akka.event.{Logging, LoggingAdapter}
import zeab.queue.QueueMessages.{Add, GetNext}
import zeab.webservice.ws.WebSocketMessages.Msg

class WebSocketInterfaceActor extends Actor{

  //Actor Settings
  val actorLog: LoggingAdapter = Logging(context.system, this)

  //Receive
  def receive: Receive = inactive

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
  }

  //Behaviors
  def keeper(senderId: String = "", actor: Option[ActorRef] = None, queue: Option[ActorRef] = None): Receive = {
    case (senderId: String, actor: ActorRef, queue:ActorRef) =>
      context.become(keeper(senderId, Some(actor), Some(queue)))
    case m: String =>
      println(m)
    case m: Msg =>
      m.`type`.toLowerCase match {
        case "p" =>

        case "c" =>
      }
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
