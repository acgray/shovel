package io.github.acgray.snowplow.serverless.collector

import io.github.acgray.snowplow.serverless.lambda.LambdaProxyRequest
import scalaz.Validation

trait Serializer {
  def serialize(request: LambdaProxyRequest): Validation[Exception, Array[Byte]]
}
