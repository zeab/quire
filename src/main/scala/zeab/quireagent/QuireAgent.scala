package zeab.quireagent

//Imports
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import zeab.akkatools.akkaconfigbuilder.AkkaConfigBuilder
import zeab.akkatools.webservice.WebServiceActor
import zeab.akkatools.webservice.WebServiceMessages.StartService
import zeab.logging.Logging
import zeab.quireagent.quireconductor.QuireConductorActor
import zeab.quireagent.webservice.Routes

import scala.concurrent.ExecutionContext

object QuireAgent extends Logging {

  def main(args: Array[String]): Unit = {

    //Akka
    implicit val actorSystem:ActorSystem = ActorSystem("QuireAgent", AkkaConfigBuilder.buildConfig())
    implicit val actorMaterializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContext = actorSystem.dispatcher

    //Quire
    implicit val quireConductor: ActorRef = actorSystem.actorOf(Props[QuireConductorActor])

    //Web Service
    actorSystem.actorOf(Props(classOf[WebServiceActor], actorMaterializer), "QuireWebService") ! StartService(Routes.allRoutes)

  }

}
