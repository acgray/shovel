package io.github.acgray.snowplow.serverless.collector

import com.snowplowanalytics.snowplow.CollectorPayload.thrift.model1.CollectorPayload
import io.github.acgray.snowplow.serverless.lambda.LambdaProxyRequest
import org.apache.http.client.utils.URLEncodedUtils
import org.apache.http.message.BasicNameValuePair
import org.apache.thrift.TSerializer
import scalaz.{BuildInfo, Failure, Success, Validation}

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

class ThriftSerializer(private val ser: TSerializer = new TSerializer)
  extends Serializer {

  implicit class SerializableRequest(val original: LambdaProxyRequest) {

    def toCollectorPayload: CollectorPayload = {
      val payload = new CollectorPayload(
        "iglu:com.snowplowanalytics.snowplow/CollectorPayload/thrift/1-0-0",
        original.getRequestContext.getIdentity.getSourceIp,
        System.currentTimeMillis,
        "UTF-8",
        s"serverless-collector-${BuildInfo.version}-kinesis")

      payload.querystring = Utils.mapToQueryString(
        original
          .getQueryStringParameters
          .toMap)
      payload.body = original.getBody
      payload.path = original.getPath
      payload.userAgent = original.getHeaders.get("User-Agent")
      payload.headers = original.getHeaders
        .map { case (k, v) => s"$k: $v" }
        .toList
      payload.contentType = original.getHeaders.get("content-type")
      payload.hostname = original.getHeaders.get("Host")

      payload
    }
  }

  override def serialize(request: LambdaProxyRequest): Validation[Exception, Array[Byte]] =
    try {
      Success(ser.serialize(
        request.toCollectorPayload))
    } catch {
      case e: Exception => Failure(e)
    }
}
