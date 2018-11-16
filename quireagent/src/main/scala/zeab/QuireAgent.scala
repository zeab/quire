package zeab

//Imports
import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import zeab.misc.AkkaConfigBuilder
import zeab.misc.webservice.WebServiceActor
import zeab.misc.webservice.WebServiceMessages.StartService
import zeab.webservice.Routes

import scala.concurrent.ExecutionContext

object QuireAgent {

  def main(args: Array[String]): Unit = {

    //Akka
    implicit val actorSystem:ActorSystem = ActorSystem("QuireAgent", AkkaConfigBuilder.buildConfig())
    implicit val actorMaterializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContext = actorSystem.dispatcher

    //Start the Web Service
    actorSystem.actorOf(Props(classOf[WebServiceActor], actorMaterializer)) ! StartService(Routes.allRoutes)

  }

}
