
//Imports
import Settings._
import Dependencies._
import Docker._
import ModuleNames._
import Resolvers._

//Add all the command alias's
CommandAlias.allCommandAlias

lazy val quireagent = (project in file("."))
  .settings(rootSettings: _*)
  .settings(libraryDependencies ++= rootDependencies)
  .enablePlugins(Artifactory)
  .settings(allResolvers: _*)
  .settings(rootDockerSettings)
  .enablePlugins(AshScriptPlugin)
  .enablePlugins(AssemblyPlugin)
