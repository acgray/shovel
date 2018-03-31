package io.github.acgray.snowplow.serverless.collector

import java.nio.ByteBuffer
import java.util.Base64

import com.amazonaws.services.kinesis.AmazonKinesisClient
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.snowplowanalytics.snowplow.CollectorPayload.thrift.model1.CollectorPayload
import org.apache.http.client.utils.URLEncodedUtils
import org.apache.http.message.BasicNameValuePair
import org.apache.thrift.TSerializer
import scalaz.BuildInfo

import scala.collection.JavaConversions._
import scala.util.Random

class OptionsHandler extends RequestHandler[LambdaProxyRequest, LambdaProxyResponse] {
  def handleRequest(input: LambdaProxyRequest,
                    context: Context): LambdaProxyResponse = {
    println(s"Received request headers: ${input.getHeaders}")
    LambdaProxyResponse(
      200, "", Map(
        "Access-Control-Allow-Origin" -> input.getHeaders.get("origin"),
        "Access-Control-Allow-Headers" -> "Content-Type",
        "Access-Control-Allow-Credentials" -> "true"))
  }
}

class CollectorHandler extends RequestHandler[LambdaProxyRequest, LambdaProxyResponse] {

  private final val pixel = "R0lGODlhAQABAPAAAP///wAAACH5BAEAAAAALAAAAAABAAEAAAICRAEAOw=="
  private final val kinesisStreamName = System.getenv("KINESIS_STREAM_NAME")

  private final val kinesis = AmazonKinesisClient.builder.build

  def handleRequest(input: LambdaProxyRequest,
                    context: Context): LambdaProxyResponse = {

    val serializer = new TSerializer

    val payload = new CollectorPayload(
      "iglu:com.snowplowanalytics.snowplow/CollectorPayload/thrift/1-0-0",
      input.getRequestContext.getIdentity.getSourceIp,
      System.currentTimeMillis,
      "UTF-8",
      s"serverless-collector-${BuildInfo.version}-kinesis")

    val queryParams =
        Option(input.getQueryStringParameters) match {
          case (None) => List.empty
          case Some(params) => params.map {
              case (k, v) => new BasicNameValuePair(k, v)
            }
        }

    payload.querystring = URLEncodedUtils.format(
      queryParams.toList, "utf-8")
    payload.body = input.getBody
    payload.path = input.getPath
    payload.userAgent = input.getHeaders.get("User-Agent")
    payload.headers = input.getHeaders.map { case (k, v) => s"$k: $v" }.toList
    payload.contentType = input.getHeaders.get("content-type")

    println(s"Received $payload")

    kinesis.putRecord(
      kinesisStreamName,
      ByteBuffer.wrap(serializer.serialize(payload)),
      Random.nextInt.toString)

    val headers = Map(
      "Content-Type" -> "image/gif",
      "Access-Control-Allow-Origin" -> input.getHeaders.get("origin"),
      "Access-Control-Allow-Credentials" -> "true")

      LambdaProxyResponse(200, pixel, headers, true)
    }
}
