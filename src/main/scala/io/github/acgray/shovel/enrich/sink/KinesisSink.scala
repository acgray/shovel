package io.github.acgray.shovel.enrich.sink

import java.nio.ByteBuffer

import com.amazonaws.services.kinesis.AmazonKinesis
import com.amazonaws.services.kinesis.model.{PutRecordsRequest, PutRecordsRequestEntry}
import com.snowplowanalytics.snowplow.enrich.common.ValidatedEnrichedEvent
import com.snowplowanalytics.snowplow.enrich.common.outputs.EnrichedEvent
import io.github.acgray.shovel.enrich.Sink
import scalaz.{Failure, Success, Validation}

import scala.collection.JavaConversions._

class KinesisSink(val kinesis: AmazonKinesis,
                  val streamName: String) extends Sink {

  implicit class KinesisValidatedEvent(val original: EnrichedEvent) {

    private def asTsv: String = {
      getClass.getDeclaredFields
        .map { field =>
          field.setAccessible(true)
          Option(field.get(this)).getOrElse("")
        }.mkString("\t")
    }

    def prepareKinesis: PutRecordsRequestEntry = {
      val entry = new PutRecordsRequestEntry
      entry.setPartitionKey("111")
      entry.setData(ByteBuffer.wrap(asTsv.getBytes))
      entry
    }
  }

  override def submit(events: List[EnrichedEvent]): Validation[Exception, Unit] = {
    val req = new PutRecordsRequest
    req.setRecords(
      events.map(_.prepareKinesis))
    req.setStreamName(streamName)

    try {
      Success(kinesis.putRecords(req))
    } catch {
      case e: Exception => Failure(e)
    }
  }
}
