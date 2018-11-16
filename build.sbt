
//Imports
import Common._
import Docker._
import ModuleNames._
import Dependencies._

//Add all the command alias's
CommandAlias.allCommandAlias

lazy val quireagent = (project in file(quireAgentKey))
  .settings(quireAgentSettings: _*)
  .settings(libraryDependencies ++= quireDependencies)
  .enablePlugins(Artifactory)
  .settings(quireAgentDockerSettings)
  .enablePlugins(AshScriptPlugin)
  .enablePlugins(AssemblyPlugin)