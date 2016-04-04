package opendataflow

import sbt._
import Keys._

object Dependencies {
  lazy val scalaTestVersion = settingKey[String]("The version of ScalaTest to use.")
  lazy val scalaStmVersion = settingKey[String]("The version of ScalaSTM to use.")
  lazy val scalaCheckVersion = settingKey[String]("The version of ScalaCheck to use.")
  lazy val sparkVersion = settingKey[String]("The version of spark to use")


  val Versions = Seq(
    crossScalaVersions := Seq("2.11.7"), //"2.12.0-M2"
    scalaVersion := crossScalaVersions.value.head,
    scalaStmVersion := sys.props.get("opendataflow.build.scalaStmVersion").getOrElse("0.7"),
    scalaCheckVersion := sys.props.get("opendataflow.build.scalaCheckVersion").getOrElse("1.11.6"),
    scalaTestVersion := (if (scalaVersion.value == "2.12.0-M2") "2.2.5-M2" else "2.2.4"),
    sparkVersion := sys.props.get("opendataflow.build.scalaStmVersion").getOrElse("1.5.1")
  )

  object Compile {
    val config = "com.typesafe" % "config" % "1.3.0"
    val osgiCore = "org.osgi" % "org.osgi.core" % "4.3.1" // ApacheV2
    // val osgiCompendium= "org.osgi"                    % "org.osgi.compendium"          % "4.3.1"       // ApacheV2

    val sparkCore = "org.apache.spark" %% "spark-core" % "1.5.1"
    val sparkSql = Def.setting {
      "org.apache.spark" %% "spark-sql" % sparkVersion.value % "compile"
    }
    val sparkHive = Def.setting {
      "org.apache.spark" %% "spark-hive" % sparkVersion.value % "compile"
    }
    val sparkStreaming = Def.setting {
      "org.apache.spark" %% "spark-streaming" % sparkVersion.value % "compile"
    }

    val scopt = "com.github.scopt" %% "scopt" % "3.3.0"
    val slf4j = "org.slf4j" % "slf4j-api" % "1.7.12"

    object Docs {
      val sprayJson = "io.spray" %% "spray-json" % "1.3.2" % "test"
      val gson = "com.google.code.gson" % "gson" % "2.3.1" % "test"
    }

    object Test {
      val commonsMath = "org.apache.commons" % "commons-math" % "2.2" % "test"
      // ApacheV2
      val commonsIo = "commons-io" % "commons-io" % "2.4" % "test"
      // ApacheV2
      val commonsCodec = "commons-codec" % "commons-codec" % "1.10" % "test"
      // ApacheV2
      val junit = "junit" % "junit" % "4.12" % "test"
      // Common Public License 1.0
      val logback = "ch.qos.logback" % "logback-classic" % "1.1.3" % "test"
      // EPL 1.0 / LGPL 2.1
      val mockito = "org.mockito" % "mockito-all" % "1.10.19" % "test"
      // MIT
      // changing the scalatest dependency must be reflected in akka-docs/rst/dev/multi-jvm-testing.rst
      val scalatest = Def.setting {
        "org.scalatest" %% "scalatest" % scalaTestVersion.value % "test"
      }
      // ApacheV2
      val scalacheck = Def.setting {
        "org.scalacheck" %% "scalacheck" % scalaCheckVersion.value % "test"
      }
      // New BSD
      val pojosr = "com.googlecode.pojosr" % "de.kalpatec.pojosr.framework" % "0.2.1" % "test"
      // ApacheV2
      val tinybundles = "org.ops4j.pax.tinybundles" % "tinybundles" % "1.0.0" % "test"
      // ApacheV2
      val log4j = "log4j" % "log4j" % "1.2.14" % "test"
      // ApacheV2
      val junitIntf = "com.novocode" % "junit-interface" % "0.11" % "test"
      // MIT
      val scalaXml = "org.scala-lang.modules" %% "scala-xml" % "1.0.4" % "test"

      // metrics, measurements, perf testing
      val metrics = "com.codahale.metrics" % "metrics-core" % "3.0.2" % "test"
      // ApacheV2
      val metricsJvm = "com.codahale.metrics" % "metrics-jvm" % "3.0.2" % "test"
      // ApacheV2
      val latencyUtils = "org.latencyutils" % "LatencyUtils" % "1.0.3" % "test"
      // Free BSD
      val hdrHistogram = "org.hdrhistogram" % "HdrHistogram" % "1.1.4" % "test"
      // CC0
      val metricsAll = Seq(metrics, metricsJvm, latencyUtils, hdrHistogram)

      // sigar logging
      val slf4jJul = "org.slf4j" % "jul-to-slf4j" % "1.7.12" % "test"
      // MIT
      val slf4jLog4j =  "org.slf4j" % "log4j-over-slf4j" % "1.7.12" % "test" //
      val slf4jSimple = "org.slf4j" % "slf4j-simple" % "1.7.12" % "test "
    }


    object Provided {

    }

  }

  import Compile._

  val l = libraryDependencies
  val core = l ++= Seq(config, slf4j ,  Test.scalatest.value, Test.slf4jSimple)
  val odfspark = l ++= Seq(config, sparkCore, sparkSql.value, Test.scalatest.value, Test.slf4jLog4j)
  val cli = l ++= Seq(config, slf4j, scopt, Test.scalatest.value, Test.slf4jSimple)
}

