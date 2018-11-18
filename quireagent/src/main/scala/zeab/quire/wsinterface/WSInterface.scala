package zeab.quire.wsinterface

//Imports
import akka.actor.{Actor, ActorRef}
import akka.event.{Logging, LoggingAdapter}

class WSInterface extends Actor {

  //Actor Settings
  val actorLog: LoggingAdapter = Logging(context.system, this)

  //Receive
  def receive: Receive = disconnected()

  //Behaviors
  def disconnected(): Receive = {
    case (senderId: String, webSocketOut: ActorRef, queue:ActorRef) => context.become(connected(senderId, webSocketOut, queue))
  }

  def connected(senderId: String, webSocketOut: ActorRef, queue:ActorRef): Receive = {
    case _ =>
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
