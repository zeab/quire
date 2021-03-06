package zeab.quireagent.queue

//Imports
import zeab.quireagent.queue.QueueMessages.{Add, GetNext, Next}
//Akka
import akka.actor.{Actor, ActorRef}
import akka.event.{Logging, LoggingAdapter}

class QueueActor extends Actor{

  //Actor Settings
  val actorLog: LoggingAdapter = Logging(context.system, this)

  //Receive
  def receive: Receive = queue()

  //Behaviors
  def queue(q:List[String] = List.empty, consumers: Map[String, ActorRef] = Map.empty): Receive = {
    case m: Add =>
      actorLog.debug(s"Adding - size of queue ${q.size + 1}")
      context.become(queue(q ++ List(m.message)))
    case m: GetNext =>
      actorLog.debug(s"GetNext - size of queue ${q.size}")
      val next: String = q.headOption match {
        case Some(n) => n
        case None => "queue empty"
      }
      sender ! Next(next, m.senderId)
      context.become(queue(q.drop(1), consumers))
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
