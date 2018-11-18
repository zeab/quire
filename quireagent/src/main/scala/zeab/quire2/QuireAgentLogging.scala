package zeab.quire2

import ch.qos.logback.classic.{Level, LoggerContext}
import org.slf4j.{Logger, LoggerFactory}
import zeab.misc.EnvGrok

trait QuireAgentLogging extends EnvGrok {

  //Logger
  val log: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
  val loggerContext: LoggerContext = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]

  val rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME)
  val rootLogLevel: Level = getEnvGrok("ROOT_LOG_LEVEL", "DEBUG").toUpperCase match {
    case "ERROR" => Level.ERROR
    case "WARN" | "WARNING" => Level.WARN
    case "INFO" => Level.INFO
    case "DEBUG" => Level.DEBUG
    case "OFF" => Level.OFF
  }
  rootLogger.setLevel(rootLogLevel)

  val akkaLogger = loggerContext.getLogger("akka")
  val akkaLogLevel: Level = getEnvGrok("AKKA_LOG_LEVEL", "DEBUG").toUpperCase match {
    case "ERROR" => Level.ERROR
    case "WARN" | "WARNING" => Level.WARN
    case "INFO" => Level.INFO
    case "DEBUG" => Level.DEBUG
    case "OFF" => Level.OFF
  }
  akkaLogger.setLevel(akkaLogLevel)

}
