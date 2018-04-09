package io.github.acgray.snowplow.serverless.collector

import io.github.acgray.snowplow.serverless.UnitSpec

class UtilsSpec extends UnitSpec {
  "encodeQueryString" should "handle empty map" in {
    val qs = Utils.mapToQueryString(Map[String, String]())
    assert(qs == "")
  }

  it should "encode a querystring" in {
    val qs = Utils.mapToQueryString(Map(
      "a" -> "1",
      "b" -> "2",
      "c" -> "3"))

    assert(qs == "a=1&b=2&c=3")
  }

  it should "encode special characters" in {
    val qs = Utils.mapToQueryString(Map(
      "a" -> "the quick brown عربى fox",
      "b" -> "jumped over the lazy عربى dog"
    ))

    assert(qs == "a=the+quick+brown+%D8%B9%D8%B1%D8%A8%D9%89+fox&b=jumped+over+the+lazy+%D8%B9%D8%B1%D8%A8%D9%89+dog")
  }
}
