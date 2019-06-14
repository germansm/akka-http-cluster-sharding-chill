organization := "com.ksmti.poc"

scalaVersion := "2.12.8"

version := "1.0.0"

scalacOptions in Compile ++= Seq("-deprecation",
                                 "-feature",
                                 "-unchecked",
                                 "-Xlog-reflective-calls",
                                 "-Xlint")

javacOptions in Compile ++= Seq("-Xlint:unchecked", "-Xlint:deprecation")

javaOptions in run ++= Seq("-Xms128m",
                           "-Xmx1024m",
                           "-Djava.library.path=./target/native")

lazy val akkaVersion = "2.5.22"

lazy val httpVersion = "10.1.7"

lazy val managementVersion = "1.0.1"

// Akka Actors / Remote / Cluster Libs
lazy val akkaLibs = Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-metrics" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion,
  "com.typesafe.akka" %% "akka-discovery" % akkaVersion,
)

// Akka Http Libs
lazy val akkaHttp = Seq("com.typesafe.akka" %% "akka-http" % httpVersion)
// Management Libs
lazy val managementLibs = Seq(
  "com.lightbend.akka.management" %% "akka-management" % managementVersion,
  "com.lightbend.akka.management" %% "akka-management-cluster-http" % managementVersion,
  "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % managementVersion,
  "com.lightbend.akka.discovery" %% "akka-discovery-aws-api" % managementVersion
)

// Chill-Akka / Kryo Serializer Libs
lazy val serializationLibs = Seq("com.twitter" %% "chill-akka" % "0.9.3")

// Monitoring Libs
lazy val monitoringLibs = Seq("io.kamon" % "sigar-loader" % "1.6.6-rev002")

// Testing Libs
lazy val testingLibs = Seq(
  "org.scalatest" %% "scalatest" % "3.0.3" % Test,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-multi-node-testkit" % akkaVersion % Test
)

libraryDependencies ++= (akkaLibs ++ akkaHttp ++ managementLibs ++ serializationLibs ++ monitoringLibs ++ testingLibs)

fork in run := true

mainClass in (Compile, run) := Some("com.ksmti.poc.MSPEngine")

// disable parallel tests
parallelExecution in Test := false

mainClass in assembly := Some("com.ksmti.poc.MSPEngine")

test in assembly := {}

headerLicense := Some(HeaderLicense.Custom("""| Copyright (C) 2015-2019 KSMTI
       |
       | <http://www.ksmti.com>
    """.stripMargin))
scalafmtOnCompile := true

lazy val `MSPEngine` = project.in(file(".")).enablePlugins(AutomateHeaderPlugin)
