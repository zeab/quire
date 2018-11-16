package zeab.queue

//Imports
import zeab.queue.QueueMessages.{Add, GetNext}
import zeab.webservice.ws.WebSocketMessages.Msg
//Akka
import akka.actor.{Actor, ActorRef}
import akka.event.{Logging, LoggingAdapter}
//Circe
import io.circe.generic.auto._
import io.circe.parser.decode

class QueueActor(maxQueueSize:Int) extends Actor{

  //Actor Settings
  val actorLog: LoggingAdapter = Logging(context.system, this)

  //Receive
  def receive: Receive = queue()

  //Behaviors
  def queue(q:List[String] = List.empty, consumers: Map[String, ActorRef] = Map.empty): Receive = {
    case (uuid: String, actor: ActorRef) =>
      context.become(queue(q, consumers ++ Map(uuid -> actor)))
    case m : String =>
      decode[Msg](m) match {
        case Right(msg) =>
          msg.uuid match {
            case Some(id) =>
              val next: String = q.headOption match {
                case Some(n) => n
                case None => "nothing to give"
              }
              consumers.find(_._1==id) match {
                case Some(x) =>
                  val (_, actor) = x
                  actor ! next
                  context.become(queue(q.drop(1), consumers))
                case None => //do nothing
              }
            case None =>
              //If I dont have an id then we assume this is a producer and ... no
              context.become(queue(q ++ List(msg.text), consumers))
          }
        case Left(ex) => //Something happened during decoding so dont do anything to the queue
      }
    case m: Add =>
      actorLog.debug(s"Adding - size of queue ${q.size}")
      context.become(queue(q ++ List(m.message)))
    case GetNext =>
      actorLog.debug(s"GetNext - size of queue ${q.size}")
      val next: String = q.headOption match {
        case Some(n) => n
        case None => "nothing to give"
      }
      sender ! next
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
