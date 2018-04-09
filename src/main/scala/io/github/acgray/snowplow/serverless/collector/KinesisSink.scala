package io.github.acgray.snowplow.serverless.collector

import java.nio.ByteBuffer

import com.amazonaws.services.kinesis.AmazonKinesis
import scalaz.{Failure, Success, Validation}

import scala.util.Random

class KinesisSink (kinesis: AmazonKinesis) extends Sink {

  override def submit(payload: Array[Byte], streamName: String): Validation[Exception, Unit] = {
    try {
      Success(kinesis.putRecord(
        streamName,
        ByteBuffer.wrap(payload),
        Random.nextInt.toString))
    } catch {
      case e: Exception => Failure(e)
    }
  }
}
