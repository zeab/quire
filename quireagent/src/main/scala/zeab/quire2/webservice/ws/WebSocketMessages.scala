package zeab.quire2.webservice.ws

object WebSocketMessages {

  case class Hello(senderId:String)

  case class Msg(msg: String, senderId: String, `type`: String)

}
