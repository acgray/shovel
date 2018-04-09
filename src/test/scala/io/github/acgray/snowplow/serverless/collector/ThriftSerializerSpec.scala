package io.github.acgray.snowplow.serverless.collector

import com.github.mirreck.FakeFactory
import com.snowplowanalytics.snowplow.CollectorPayload.thrift.model1.CollectorPayload
import io.github.acgray.snowplow.serverless.UnitSpec
import io.github.acgray.snowplow.serverless.lambda.LambdaProxyRequest
import org.apache.thrift.{TDeserializer, TSerializer}
import scalaz.{BuildInfo, Failure, Success}

import scala.collection.JavaConversions._


class ThriftSerializerSpec extends UnitSpec {
  private final val fake = new FakeFactory
  private final val fakeHostname = fake.words(1).mkString("")

  private final val serializer = new ThriftSerializer

  "serialize" should "serialize the request" in {
    val request = new LambdaProxyRequest
    val context = new LambdaProxyRequest.RequestContext()
    val identity = new LambdaProxyRequest.Identity()

    val userAgent = fake.words(1).mkString
    val extraHeaders = Map(
      s"X-${fake.words(1).mkString.capitalize}"
        -> fake.words(1).mkString)
    val path = fake.words(2).mkString("/")
    val body = fake.words(10).mkString(" ")
    val qsParams = Map(
      "a" -> "1",
      "b" -> "2",
      "c" -> "3")
    val userIp = "1.2.3.4"
    val hostname = fakeHostname
    val headers = Map[String, String](
      "User-Agent" -> userAgent,
      "content-type" -> "application/json",
      "Host" -> hostname) ++ extraHeaders

    identity.setSourceIp(userIp)
    context.setIdentity(identity)

    request.setRequestContext(context)
    request.setQueryStringParameters(qsParams)
    request.setBody(body)
    request.setPath(path)
    request.setHeaders(headers)

    val payloadData = serializer.serialize(request) match {
      case Failure(_) => fail()
      case Success(p) => p
    }

    val deserializer = new TDeserializer
    val payload = new CollectorPayload()

    deserializer.deserialize(payload, payloadData)

    assert(payload.querystring == "a=1&b=2&c=3")
    assert(payload.body == body)
    assert(payload.path == path)
    assert(payload.userAgent == userAgent)
    assert(payload.headers.toList ==  headers.map({ case (k, v) => s"$k: $v" }))
    assert(payload.contentType == "application/json")
    assert(payload.encoding == "UTF-8")
    assert(payload.collector == s"serverless-collector-${BuildInfo.version}-kinesis")
    assert(payload.hostname == hostname)
  }

}