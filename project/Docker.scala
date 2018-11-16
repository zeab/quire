
//Imports
import Common._
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._
import sbt.Def
import ModuleNames.quireAgentKey

object Docker {

  val repo: Option[String] = Some("zeab")

  //Image List
  val I = new {
    val openjdk8Alpine: String = "openjdk:8-jdk-alpine"
    val openjdk8Slim: String = "openjdk:8-jdk-slim"
  }

  //Base
  val baseDockerSettings: Seq[Def.Setting[_]] = Seq(
    dockerBaseImage := I.openjdk8Alpine,
    dockerRepository := repo,
    dockerUpdateLatest := true
  )

  val quireAgentDockerSettings: Seq[Def.Setting[_]] = Seq(
    dockerBaseImage := I.openjdk8Alpine,
    dockerRepository := repo,
    dockerLabels := mapDockerLabels(quireAgentKey, quireAgentVersion, buildTime),
    dockerUpdateLatest := true
  )

}
