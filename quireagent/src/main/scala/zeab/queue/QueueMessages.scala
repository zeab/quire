package zeab.queue

object QueueMessages {

  case class Add(message:String)

  case object GetNext

}
