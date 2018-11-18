package zeab.quire

//Imports
import akka.actor.Props
import zeab.misc.AkkaConfigBuilder
import zeab.misc.webservice.WebServiceActor
import zeab.misc.webservice.WebServiceMessages.StartService
import zeab.quire.webservice.Routes
//Akka
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
//Scala
import scala.concurrent.ExecutionContext

object Quire {

  def main(args: Array[String]): Unit = {
    //Akka
    implicit val actorSystem:ActorSystem = ActorSystem("Quire", AkkaConfigBuilder.buildConfig())
    implicit val actorMaterializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContext = actorSystem.dispatcher

    //Start the Web Service
    actorSystem.actorOf(Props(classOf[WebServiceActor], actorMaterializer), "QuireWebService") ! StartService(Routes.allRoutes)

  }

}
