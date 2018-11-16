//Imports
import sbt._

object Dependencies {

  //List of Versions
  val V = new {
    val akka                        = "2.5.14"
    val akkaHttp                    = "10.1.3"
    val akkaHttpCirce               = "1.21.0"
    val akkaKafka                   = "0.22"
    val circe                       = "0.9.3"
    val logbackJson                 = "5.2"
    val logback                     = "1.2.3"
    val scalaTest                   = "3.0.5"
    val scalaXML                    = "1.0.6"
    val jodaTime                    = "2.10"
  }

  //List of Dependencies
  val D = new {
    //Akka
    val akkaStream                  = "com.typesafe.akka" %% "akka-stream" % V.akka
    //Akka Http
    val akkaHttp                    = "com.typesafe.akka" %% "akka-http" % V.akkaHttp
    //Akka Kafka
    val akkaKafka                   = "com.typesafe.akka" %% "akka-stream-kafka" % V.akkaKafka
    //Json
    val circeCore                   = "io.circe" %% "circe-parser" % V.circe
    val circeParser                 = "io.circe" %% "circe-generic" % V.circe
    val akkaHttpCirce               = "de.heikoseeberger" %% "akka-http-circe" % V.akkaHttpCirce
    //Logging
    val akkaSlf4j                   = "com.typesafe.akka" %% "akka-slf4j" % V.akka
    val logback                     = "ch.qos.logback" % "logback-classic" % V.logback
    val logbackJson                 = "net.logstash.logback" % "logstash-logback-encoder" % V.logbackJson
    //Test
    val scalaTest                   = "org.scalatest" %% "scalatest" % V.scalaTest % "test"
    val akkaTestKit                 = "com.typesafe.akka" %% "akka-testkit" % V.akka % Test
    //Scala XML
    val scalaXML                    = "org.scala-lang.modules" %% "scala-xml" % V.scalaXML
    //Time
    val jodaTime                    = "joda-time" % "joda-time" % V.jodaTime
  }

  val quireDependencies: Seq[ModuleID] = Seq(
    D.akkaStream,
    D.akkaHttp,
    D.akkaHttpCirce,
    D.circeCore,
    D.circeParser,
    D.akkaSlf4j,
    D.logback,
    D.logbackJson,
    D.scalaTest,
    D.akkaTestKit
  )

}
