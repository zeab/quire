package zeab.quire2.internalkeeper

//Imports
import akka.actor.ActorRef

object InternalKeeperMessages {

  case class AddQueue(name:String, queue:ActorRef)
  case class RemoveQueue(name:String)
  case object GetQueues
  case class GetQueue(name:String)

}
