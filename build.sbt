import sbt.Keys._
import sbt._
import sbtrelease.Version

name := "snowplow-serverless"

resolvers ++= Seq(
  Resolver.sonatypeRepo("public"),
  // For Snowplow
  "Snowplow Analytics Maven releases repo" at "http://maven.snplow.com/releases/",
  "Snowplow Analytics Maven snapshot repo" at "http://maven.snplow.com/snapshots/",
  // For uaParser utils
  "user-agent-parser repo"                 at "https://clojars.org/repo/",
  // For user-agent-utils
  "user-agent-utils repo"                  at "https://raw.github.com/HaraldWalker/user-agent-utils/mvn-repo/"
)
scalaVersion := "2.11.11"
releaseNextVersion := { ver => Version(ver).map(_.bumpMinor.string).getOrElse("Error") }
assemblyJarName in assembly := "serverless-collector.jar"

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-lambda-java-events" % "1.3.0",
  "com.amazonaws" % "aws-lambda-java-core" % "1.1.0",
  "com.amazonaws" % "aws-java-sdk-kinesis" % "1.11.301",
  "com.snowplowanalytics" %% "snowplow-common-enrich"    % "0.31.0",
  "org.scalaz"    % "scalaz-core"    % "7.0.9"
)

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % Test
libraryDependencies += "org.scalamock" %% "scalamock" % "4.1.0" % Test
libraryDependencies += "org.typelevel" %% "scalaz-scalatest" % "1.1.2" % Test
libraryDependencies += "com.github.mirreck" % "java-fake-factory" % "1.0.2" % Test


scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Xfatal-warnings")
