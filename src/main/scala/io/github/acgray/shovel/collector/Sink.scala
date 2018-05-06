package io.github.acgray.shovel.collector

import com.snowplowanalytics.snowplow.enrich.common.outputs.EnrichedEvent
import scalaz.Validation

trait Sink {
  def submit(events: Array[Byte],
             streamName: String): Validation[Exception, Unit]
}
