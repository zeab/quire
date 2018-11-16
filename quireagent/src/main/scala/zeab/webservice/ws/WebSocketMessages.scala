package zeab.webservice.ws

object WebSocketMessages {

  case class Hello(uuid:String)

  case class Msg(text: String, uuid: Option[String])

}
