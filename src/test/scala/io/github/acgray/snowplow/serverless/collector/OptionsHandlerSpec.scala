package io.github.acgray.snowplow.serverless.collector

import com.amazonaws.services.lambda.runtime.Context
import com.github.mirreck.FakeFactory
import io.github.acgray.snowplow.serverless.UnitSpec
import io.github.acgray.snowplow.serverless.lambda.LambdaProxyRequest
import org.scalamock.scalatest.MockFactory

import scala.collection.JavaConversions._


class OptionsHandlerSpec extends UnitSpec with MockFactory {
  private val factory = new FakeFactory
  private val handler = new OptionsHandler
  private val fakeContext = stub[Context]

  private val originDomain = s"https://${factory.words(1).get(0).toLowerCase}.com"

  private val fakeRequest = new LambdaProxyRequest()
  fakeRequest.setHeaders(Map("origin" -> originDomain))
  fakeRequest.setHttpMethod("OPTIONS")

  "OptionsHandler" should "return 200" in {
    val resp = handler.handleRequest(fakeRequest, fakeContext)
    assert(resp.statusCode == 200)
  }

  it should "return Access-Control-Allow-* headers" in {
    val resp = handler.handleRequest(fakeRequest, fakeContext)
    val wantHeaders: java.util.Map[String, String] = Map(
      "Access-Control-Allow-Origin" -> originDomain,
      "Access-Control-Allow-Headers" -> "Content-Type",
      "Access-Control-Allow-Credentials" -> "true")
    assert(resp.headers == wantHeaders)
  }

  it should "reject methods other than OPTIONS" in {
    val postRequest: LambdaProxyRequest = new LambdaProxyRequest()
    postRequest.setHttpMethod("POST")

    val resp = handler.handleRequest(postRequest, fakeContext)

    assert(resp.statusCode == 405)
  }
}
