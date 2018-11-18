package zeab.quire.queue

object QueueMessages {

  case class Add(msg:String)

  case class GetNext(senderId:String)

  case class Next(msg:String, senderId:String)

}
