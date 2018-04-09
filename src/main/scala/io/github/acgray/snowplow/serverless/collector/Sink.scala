package io.github.acgray.snowplow.serverless.collector

import scalaz.Validation

trait Sink {
  def submit(payload: Array[Byte], streamName: String):  Validation[Exception, Unit]
}
