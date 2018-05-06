package io.github.acgray.shovel.collector

import io.github.acgray.shovel.lambda.LambdaProxyRequest
import scalaz.Validation

trait Serializer {
  def serialize(request: LambdaProxyRequest): Validation[Exception, Array[Byte]]
}
