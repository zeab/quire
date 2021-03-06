//Imports
import sbt._

object Dependencies {

  //List of Versions
  val V = new {
    val wabonki                     = "0.0.5"
  }

  //List of Dependencies
  val D = new {
    //Wabonki
    val wabonki                     = "zeab" %% "wabonki" % V.wabonki
  }

  val rootDependencies: Seq[ModuleID] = Seq(
    D.wabonki
  )

}
