package io.github.acgray.shovel.collector

import com.amazonaws.services.lambda.runtime.Context
import com.github.mirreck.FakeFactory
import io.github.acgray.shovel.UnitSpec
import io.github.acgray.shovel.lambda.LambdaProxyRequest
import org.scalamock.scalatest.MockFactory
import scalaz.{Failure, Success}

import scala.collection.JavaConversions._

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

  it should "throw an exception when KINESIS_STREAM_NAME environment" +
    "variable is missing" in {
    val thrown = intercept[Exception] {
      val invalidHandler = new CollectorHandler
    }

    assert(thrown.isInstanceOf[RuntimeException])
    assert(thrown.getMessage == "KINESIS_STREAM_NAME is required")
  }
}
