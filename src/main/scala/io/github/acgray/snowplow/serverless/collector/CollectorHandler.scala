package io.github.acgray.snowplow.serverless.collector

import com.amazonaws.regions.Regions
import com.amazonaws.services.kinesis.AmazonKinesisClient
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import io.github.acgray.snowplow.serverless.lambda.{LambdaProxyRequest, LambdaProxyResponse}
import scalaz.{Failure, Success}

import scala.collection.JavaConversions._
import scala.util.Properties

class OptionsHandler extends RequestHandler[LambdaProxyRequest, LambdaProxyResponse] {
  def handleRequest(input: LambdaProxyRequest,
                    context: Context): LambdaProxyResponse = {
    println(s"Received request headers: ${input.getHeaders}")

    input.getHttpMethod match {
      case "OPTIONS" =>
        LambdaProxyResponse(
          200, "", Map(
            "Access-Control-Allow-Origin" -> input.getHeaders.get("origin"),
            "Access-Control-Allow-Headers" -> "Content-Type",
            "Access-Control-Allow-Credentials" -> "true"))
      case _ =>
        LambdaProxyResponse(405, "", Map[String, String]())
    }
  }
}

class CollectorHandler(private val streamNameOverride: Option[String],
                       private val sinkOverride: Option[Sink] = None,
                       private val serializer: Serializer = new ThriftSerializer)
  extends RequestHandler[LambdaProxyRequest, LambdaProxyResponse] {

  private val streamName = streamNameOverride
    .orElse(Properties.envOrNone("KINESIS_STREAM_NAME"))
    .orElse(throw new RuntimeException("KINESIS_STREAM_NAME is required"))

  private val kinesisRegion = Properties.envOrNone("KINESIS_REGION") match {
    case Some(name) => Regions.fromName(name)
    case _ => Regions.EU_WEST_1
  }

  private final val pixel = "R0lGODlhAQABAPAAAP///wAAACH5BAEAAAAALAAAAAABAAEAAAICRAEAOw=="

  private val kinesis = AmazonKinesisClient.builder
    .withRegion(kinesisRegion)
    .build

  private final val sink = sinkOverride
    .getOrElse(new KinesisSink(kinesis))

  /**
    * Entrypoint for collector requests
    *
    * @param input   LambdaProxyRequest instance
    * @param context Context instance
    */
  def handleRequest(input: LambdaProxyRequest,
                    context: Context): LambdaProxyResponse = {

    val result = serializer.serialize(input) match {
      case Failure(e) =>
        throw new RuntimeException(e)
      case Success(bytes) =>
        sink.submit(bytes, streamName.get)
    }

    val headers = Map(
      "Content-Type" -> "image/gif",
      "Access-Control-Allow-Origin" -> input.getHeaders.get("origin"),
      "Access-Control-Allow-Credentials" -> "true")

    result match {
      case Failure(e) => throw new RuntimeException(e)
      case Success(_) =>
        LambdaProxyResponse(200, pixel, headers, base64Encoded = true)
    }
  }

}
