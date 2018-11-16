package zeab.misc.webservice

//Imports
import zeab.misc.EnvGrok

trait WebServiceEnvGrok extends EnvGrok{

  val httpServiceHostKey: String = getEnvGrok("HTTP_SERVICE_HOST", "0.0.0.0")
  val httpServicePortKey: String = getEnvGrok("HTTP_SERVICE_PORT", "8080")

}
