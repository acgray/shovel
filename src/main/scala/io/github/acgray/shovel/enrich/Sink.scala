package io.github.acgray.shovel.enrich

import com.snowplowanalytics.snowplow.enrich.common.outputs.EnrichedEvent
import scalaz.Validation

trait Sink {
  def submit(events: List[EnrichedEvent]): Validation[Exception, Unit]
}
