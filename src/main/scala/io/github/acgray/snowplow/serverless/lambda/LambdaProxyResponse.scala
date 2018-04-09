package io.github.acgray.snowplow.serverless.lambda

import scala.beans.BeanProperty

case class LambdaProxyResponse(@BeanProperty statusCode: Integer, @BeanProperty body: String,
                               @BeanProperty headers: java.util.Map[String, String], @BeanProperty base64Encoded: Boolean = false)
