# Snowplow serverless

Experimental Serverless version of the [Snowplow](https://www.snowplowanalytics.com)
stack, powered by AWS Lambda, Kinesis and API Gateway.

**THIS IS EXPERIMENTAL SOFTWARE AND ALMOST CERTAINLY NOT USABLE IN PRODUCTION.
See below for a full list of caveats**

*NOTE: This is an unofficial, community-contributed application not affiliated
with Snowplow Analytics Ltd or the official Snowplow project.*

## Quick start

Clone the repository:

```
git clone git@github.com:acgray/snowplow-serverless.git
```

Deploy the application:

```
sbt assembly && serverless deploy
```

That's it!  You can initialize trackers using the API gateway endpoint returned
by the `serverless` command (N.B. you must include the stage name, `dev` by
default.)  For example:

```html
<script type="text/javascript" async=1>
;(function(p,l,o,w,i,n,g){if(!p[i]){p.GlobalSnowplowNamespace=p.GlobalSnowplowNamespace||[];
p.GlobalSnowplowNamespace.push(i);p[i]=function(){(p[i].q=p[i].q||[]).push(arguments)
};p[i].q=p[i].q||[];n=l.createElement(o);g=l.getElementsByTagName(o)[0];n.async=1;
n.src=w;g.parentNode.insertBefore(n,g)}}(window,document,"script","//d1fc8wv8zag5ca.cloudfront.net/2.9.0/sp.js","snowplow"));
    window.snowplow('newTracker', 'default', 'XXXXXXX.execute-api.us-east-1.amazonaws.com/dev', {
        appId: 'test',
        forceSecureTracker: true,
        post: true
    });
    window.snowplow('trackPageView');
    </script>
```

## Caveats

This is alpha software and almost certainly not usable in production.

In particular, the following Snowplow features are not yet implemented:

- [ ] Custom Iglu schemas (only Iglu central events are supported)
- [ ] Custom enrichments
- [ ] GeoIP enrichment
- [ ] Webhooks
- [ ] Graceful handling of bad collector requests
- [ ] Graceful handling of Kinesis failures
- [ ] Snowplow monitoring
- [ ] 3rd party cookies (network_userid)
- [ ] Redirects
- [ ] Any sinks other than Kinesis