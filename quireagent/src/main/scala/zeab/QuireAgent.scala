package zeab

//Imports
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import zeab.internalkeeper.InternalKeeperActor
import zeab.misc.AkkaConfigBuilder
import zeab.misc.webservice.WebServiceActor
import zeab.misc.webservice.WebServiceMessages.StartService
import zeab.webservice.Routes

import scala.concurrent.ExecutionContext

object QuireAgent extends QuireAgentLogging{

  def main(args: Array[String]): Unit = {

    //Akka
    implicit val actorSystem:ActorSystem = ActorSystem("QuireAgent", AkkaConfigBuilder.buildConfig())
    implicit val actorMaterializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContext = actorSystem.dispatcher

    //Internal Keeper
    implicit val internalKeeper: ActorRef = actorSystem.actorOf(Props[InternalKeeperActor])

    //Start the Web Service
    actorSystem.actorOf(Props(classOf[WebServiceActor], actorMaterializer), "QuireAgentWebService") ! StartService(Routes.allRoutes)

  }

}
