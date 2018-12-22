package zeab.quireagent.quireconductor

import akka.Done
import akka.actor.{Actor, ActorRef, Props}
import akka.event.{Logging, LoggingAdapter}
import zeab.quireagent.queue.QueueActor
import zeab.quireagent.quireconductor.QuireConductorMessages.CreateTopic

class QuireConductorActor extends Actor{

  //Actor Settings
  val actorLog: LoggingAdapter = Logging(context.system, this)

  //Receive
  def receive: Receive = conductor()

  //Behaviors
  def conductor(queues:Map[String, ActorRef] = Map.empty): Receive = {
    case m: CreateTopic =>
      val queue: ActorRef = context.actorOf(Props[QueueActor], s"${m.topic}")
      sender ! Done
      context.become(conductor(queues ++ Map(m.topic -> queue)))
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
