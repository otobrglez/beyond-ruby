scalaVersion := "3.3.1"
name         := "bike-stations"
organization := "com.pinkstack.beyond"
version      := "0.0.1"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.4.14",
  "com.typesafe"   % "config"          % "1.4.3",
  "io.circe"      %% "circe-config"    % "0.10.1",
  "org.typelevel" %% "cats-effect"     % "3.5.2"
) ++ Seq(
  "co.fs2" %% "fs2-core",
  "co.fs2" %% "fs2-io"
).map(_ % "3.9.3") ++ Seq(
  "com.monovore" %% "decline",
  "com.monovore" %% "decline-effect",
  "com.monovore" %% "decline-refined"
).map(_ % "2.4.1") ++ Seq(
  "org.typelevel" %% "log4cats-core",
  "org.typelevel" %% "log4cats-slf4j"
).map(_ % "2.6.0") ++ Seq(
  "org.http4s" %% "http4s-ember-client",
  "org.http4s" %% "http4s-ember-server",
  "org.http4s" %% "http4s-dsl",
  "org.http4s" %% "http4s-circe"
).map(_ % "1.0.0-M40") ++ Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % "0.14.6") ++ Seq(
  "org.scalactic" %% "scalactic" % "3.2.17",
  "org.scalatest" %% "scalatest" % "3.2.17" % "test"
) ++ Seq(
  "eu.timepit" %% "refined"      % "0.11.0",
  "eu.timepit" %% "refined-cats" % "0.11.0" // optional
)

scalacOptions ++= Seq(
  "-deprecation",
  // "-source:future",
  "-language:adhocExtensions",
  "-language:implicitConversions"
)

resolvers ++= Resolver.sonatypeOssRepos("snapshots")
