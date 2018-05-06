package io.github.acgray.snowplow.serverless.collector

import com.amazonaws.services.lambda.runtime.Context
import com.github.mirreck.FakeFactory
import com.snowplowanalytics.snowplow.CollectorPayload.thrift.model1.CollectorPayload
import io.github.acgray.snowplow.serverless.UnitSpec
import io.github.acgray.snowplow.serverless.lambda.LambdaProxyRequest
import org.apache.thrift.{TDeserializer, TSerializer}
import org.scalamock.scalatest.MockFactory
import org.scalatest.PrivateMethodTester
import scalaz.{BuildInfo, Failure, Success}

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

class CollectorHandlerSpec extends UnitSpec with MockFactory {

  private val fake = new FakeFactory()

  private def fakeHostname =
    fake.words(3).map(_.toLowerCase).mkString(".")

  private val fakeContext = stub[Context]
  private val fakeSink = mock[Sink]
  private val fakeStreamName = fake.words(1).mkString("")
  private val fakeSerializer = mock[Serializer]
  private val handler = new CollectorHandler(
    Some(fakeStreamName),
    Some(fakeSink),
    fakeSerializer)

  "handleRequest" should "submit record to a sink" in {
    val request = new LambdaProxyRequest()
    request.setHeaders(Map(
      "origin" -> fakeHostname))

    val data = "testdata".getBytes

    (fakeSerializer.serialize _)
      .expects(request)
      .returns(Success(data))

    (fakeSink.submit _).expects(
      data, fakeStreamName)
        .returns(Success(Unit))

    handler.handleRequest(request, fakeContext)
  }

  it should "return a 200 response with 1x1 pixel"

  it should "throw an exception serializing the request fails" in {
    val request = new LambdaProxyRequest()
    request.setHeaders(Map(
      "origin" -> fakeHostname))

    val data = "testdata".getBytes

    val wantException = new Exception("oh no")

    (fakeSerializer.serialize _)
      .expects(request)
      .returns(Failure(wantException))

    val thrownException = intercept[Exception] {
      handler.handleRequest(request, fakeContext)
    }

    assert(thrownException.isInstanceOf[RuntimeException])
    assert(thrownException.getCause == wantException)
  }

  it should "throw an exception when submitting to the sink fails" in {
    val request = new LambdaProxyRequest()
    request.setHeaders(Map(
      "origin" -> fakeHostname))

    val data = "testdata".getBytes

    val wantException = new Exception("oh no")

    (fakeSerializer.serialize _)
      .expects(request)
      .returns(Success(data))

    (fakeSink.submit _).expects(
      data, fakeStreamName)
      .returns(Failure(wantException))

    val thrownException = intercept[Exception] {
      handler.handleRequest(request, fakeContext)
    }

    assert(thrownException.isInstanceOf[RuntimeException])
    assert(thrownException.getCause == wantException)
  }
}
