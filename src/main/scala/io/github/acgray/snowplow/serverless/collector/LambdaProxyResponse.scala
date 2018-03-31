package io.github.acgray.snowplow.serverless.collector

import scala.beans.BeanProperty

case class LambdaProxyResponse(@BeanProperty statusCode: Integer, @BeanProperty body: String,
                               @BeanProperty headers: java.util.Map[String, String], @BeanProperty base64Encoded: Boolean = false)
