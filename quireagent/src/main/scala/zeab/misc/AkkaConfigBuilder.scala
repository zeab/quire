package zeab.misc

import com.typesafe.config.{Config, ConfigFactory}

object AkkaConfigBuilder {

  /** Object used to define a Blocking Fixed Thread Dispatcher of a given name, thread size and throughput
    *
    * @param name       What the dispatcher can be called under
    * @param threadSize How many threads to dedicate to this thread pool
    * @param throughput How often the tread will switch to a new thread after executing a task */
  def buildFixedDispatcher(name: String, threadSize: String = "5120", throughput: String = "1"): String = {
    s"""$name {
       |  type = Dispatcher
       |  executor = "thread-pool-executor"
       |  thread-pool-executor {
       |    fixed-pool-size = $threadSize
       |  }
       |  throughput = $throughput
       |}"""
  }

  /** An easy way to build an akka configuration
    *
    * @param customDispatchers The list of dispatcher configures to be put into the main config
    **/
  def buildConfig(customDispatchers: List[String] = List.empty): Config = {
    val config: String =
      s"""
         |###Custom Dispatchers###
         |${customDispatchers.mkString("", "\n\n", "")}
         |
         |akka {
         | loggers = ["akka.event.slf4j.Slf4jLogger"]
         | logger-startup-timeout = 60s
         | loglevel = DEBUG
         |}
      """.stripMargin

    ConfigFactory.parseString(config)

  }

}
