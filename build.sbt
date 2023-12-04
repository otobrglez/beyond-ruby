scalaVersion := "3.3.1"
name         := "free-stations"
organization := "com.pinkstack.beyond"
version      := "0.0.1"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-effect"     % "3.5.2",
  "ch.qos.logback" % "logback-classic" % "1.4.11",
  "com.typesafe"   % "config"          % "1.4.3",
  "io.circe"      %% "circe-config"    % "0.10.1"
) ++ Seq(
  "co.fs2" %% "fs2-core",
  "co.fs2" %% "fs2-io"
).map(_ % "3.9.3") ++ Seq(
  "com.monovore" %% "decline",
  "com.monovore" %% "decline-effect"
).map(_ % "2.4.1") ++ Seq(
  "org.typelevel" %% "log4cats-core",
  "org.typelevel" %% "log4cats-slf4j"
).map(_ % "2.6.0") ++ Seq(
  "org.scalactic" %% "scalactic" % "3.2.17",
  "org.scalatest" %% "scalatest" % "3.2.17" % "test"
)

scalacOptions ++= Seq(
  "-deprecation",
  // "-source:future",
  "-language:adhocExtensions",
  "-language:implicitConversions"
)

resolvers ++= Resolver.sonatypeOssRepos("snapshots")
