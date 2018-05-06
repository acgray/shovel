package io.github.acgray.shovel.enrich

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.UUID

import com.amazonaws.services.kinesis.AmazonKinesisClient
import com.amazonaws.services.kinesis.model.{PutRecordsRequest, PutRecordsRequestEntry}
import com.amazonaws.services.lambda.runtime.events.KinesisEvent
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.snowplowanalytics.iglu.client.Resolver
import com.snowplowanalytics.iglu.client.repositories.{HttpRepositoryRef, RepositoryRefConfig}
import com.snowplowanalytics.snowplow.enrich.common._
import com.snowplowanalytics.snowplow.enrich.common.enrichments.EnrichmentRegistry
import com.snowplowanalytics.snowplow.enrich.common.enrichments.registry.Enrichment
import com.snowplowanalytics.snowplow.enrich.common.loaders.ThriftLoader
import com.snowplowanalytics.snowplow.enrich.common.outputs.{BadRow, EnrichedEvent}
import org.apache.commons.codec.binary.Base64
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import scalaz.Scalaz._
import scalaz._

import scala.collection.JavaConverters._
import scala.util.Random

class Handler extends RequestHandler[KinesisEvent, String] {

  lazy val log = LoggerFactory.getLogger(getClass)

  val enrichmentRegistry = new EnrichmentRegistry(
    Map[String, Enrichment]())

  val igluCentral = new HttpRepositoryRef(
    new RepositoryRefConfig(
      "Iglu Central",
      0,
      List("com.snowplowanalytics")
    ),
    "http://iglucentral.com"
  )

  val resolver = new Resolver(
    500,
    List(igluCentral),
    Some(0)
  )

  def tabSeparatedEnrichedEvent(output: EnrichedEvent): String = {
    output.getClass.getDeclaredFields
      .map { field =>
        field.setAccessible(true)
        Option(field.get(output)).getOrElse("")
      }.mkString("\t")
  }

  def getPropertyValue(ee: EnrichedEvent, property: String): String =
    property match {
      case "event_id" => ee.event_id
      case "event_fingerprint" => ee.event_fingerprint
      case "domain_userid" => ee.domain_userid
      case "network_userid" => ee.network_userid
      case "user_ipaddress" => ee.user_ipaddress
      case "domain_sessionid" => ee.domain_sessionid
      case "user_fingerprint" => ee.user_fingerprint
      case _ => UUID.randomUUID().toString
    }

  private val kinesis = AmazonKinesisClient.builder.build

  private final val goodStreamName = System.getenv("KINESIS_GOOD_STREAM_NAME")
  private final val badStreamName = System.getenv("KINESIS_BAD_STREAM_NAME")

  override def handleRequest(input: KinesisEvent, context: Context): String = {

    val events = for {
      record <- input.getRecords.asScala.toList
    } yield for {
      event <- EtlPipeline.processEvents(
        enrichmentRegistry,
        s"serverless-enrich-0.0.1-alpha",
        new DateTime(System.currentTimeMillis),
        ThriftLoader.toCollectorPayload(
          record.getKinesis.getData.array))(resolver)
    } yield event match {
      case Success(e) => e.success
      case Failure(errors) => (errors, record).fail
    }

    val flattened = events.flatten

    val goodEventsRecords = flattened.collect { case Success(e) => e }
      .map(e => {
        val entry = new PutRecordsRequestEntry()
        entry.setPartitionKey(e.event_id)
        entry.setData(ByteBuffer.wrap(tabSeparatedEnrichedEvent(e).getBytes(StandardCharsets.UTF_8)))
        entry
      })

    if (goodEventsRecords.nonEmpty) {
      val req = new PutRecordsRequest()
      req.setStreamName(goodStreamName)
      req.setRecords(goodEventsRecords.asJava)
      kinesis.putRecords(req)
      log.info("Wrote {} records to {}", goodStreamName.length, goodStreamName)
    }

    val badEventRecords = flattened.collect { case Failure(e) => e }
      .map(tuple => {
        val line = new String(
          Base64.encodeBase64(tuple._2.getKinesis.getData.array),
          StandardCharsets.UTF_8)
        val rowJson = BadRow(line, tuple._1).toCompactJson
        val entry = new PutRecordsRequestEntry()
        entry.setPartitionKey(Random.nextInt.toString)
        entry.setData(
          ByteBuffer.wrap(
            BadRow(line, tuple._1)
              .toCompactJson
              .getBytes(StandardCharsets.UTF_8)))
        entry
      })

    if (badEventRecords.nonEmpty) {
      val req = new PutRecordsRequest()
      req.setStreamName(badStreamName)
      req.setRecords(badEventRecords.asJava)
      kinesis.putRecords(req)
      log.info("Wrote {} records to {}", badEventRecords.length, badStreamName)
    }

    "OK"
  }
}
