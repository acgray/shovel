package io.github.acgray.snowplow.serverless.collector

import org.apache.http.client.utils.URLEncodedUtils
import org.apache.http.message.BasicNameValuePair

import scala.collection.JavaConversions._

object Utils {
  def mapToQueryString(params: Map[String, String]): String = {
    val pairs = Option(params) match {
      case (None) => List.empty
      case Some(p) => p.map {
        case (k, v) => new BasicNameValuePair(k, v)
      }
    }

    URLEncodedUtils.format(pairs.toList, "utf-8")
  }
}
