package zeab.quireagent.queue

object QueueMessages {

  case class Add(message:String)

  case class GetNext(senderId: String = "")

  case class Next(msg: String, senderId: String = "")

}
