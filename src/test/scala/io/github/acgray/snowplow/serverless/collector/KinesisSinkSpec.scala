package io.github.acgray.snowplow.serverless.collector

import java.nio.ByteBuffer

import com.amazonaws.{AmazonClientException, AmazonServiceException}
import com.amazonaws.services.kinesis.AmazonKinesis
import com.amazonaws.services.kinesis.model.PutRecordResult
import io.github.acgray.snowplow.serverless.UnitSpec
import org.scalatest.Matchers
import org.typelevel.scalatest.ValidationMatchers
import scalaz.{Failure, Success}

class KinesisSinkSpec extends UnitSpec with Matchers with ValidationMatchers  {

  "KinesisSink" should "submit a record to Kinesis and return Success" in {

    val fakeClient = mock[AmazonKinesis]
    val fakeStream = "fakeStreamName"
    val sink = new KinesisSink(fakeClient)

    val payload = "abcdefghijk".getBytes

    (fakeClient.putRecord (_: String, _: ByteBuffer, _: String))
      .expects(fakeStream, ByteBuffer.wrap(payload), *)
      .returns(new PutRecordResult())

    assert(sink.submit(payload, fakeStream).isSuccess)
  }

  it should "return Failure when a Kinesis error occurs" in {
    val fakeClient = mock[AmazonKinesis]
    val fakeStream = "fakeStreamName"
    val sink = new KinesisSink(fakeClient)

    val payload = "12345".getBytes

    val errorMessage = "uh oh"

    (fakeClient.putRecord (_: String, _: ByteBuffer, _: String))
      .expects(fakeStream, ByteBuffer.wrap(payload), *)
      .throws(new AmazonClientException(errorMessage))

    sink.submit(payload, fakeStream) match {
      case Success(_) => fail("Expected Failure but found Success")
      case Failure(e) => assert(e.getMessage == errorMessage)
    }
  }
}
