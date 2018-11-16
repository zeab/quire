package zeab.internalkeeper

//Imports
import zeab.internalkeeper.InternalKeeperMessages.{AddQueue, GetQueue, GetQueues, RemoveQueue}
//Akka
import akka.actor.{Actor, ActorRef}
import akka.event.{Logging, LoggingAdapter}

class InternalKeeperActor extends Actor{

  //Actor Settings
  val actorLog: LoggingAdapter = Logging(context.system, this)

  //Receive
  def receive: Receive = keeper()

  //Behaviors
  def keeper(queues:Map[String, ActorRef] = Map.empty): Receive = {
    case m: AddQueue =>
      context.become(keeper(queues ++ Map(m.name -> m.queue)))
    case m: RemoveQueue =>
      //Remove a queue from the list
    case m: GetQueue =>
      //Get info for 1 specific queue
      sender ! queues.filter{queue =>
        val (k, _) = queue
        k == m.name
      }
    case GetQueues =>
      //Get info about all the queues and just return the lot
      sender ! queues
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
