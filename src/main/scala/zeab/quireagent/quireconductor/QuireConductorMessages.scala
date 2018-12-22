package zeab.quireagent.quireconductor

object QuireConductorMessages {

  case class ProduceMsg(topic:String, msg:String)

  case class CreateTopic(topic:String)

  case class ConsumeTopic(topic:String)

}
