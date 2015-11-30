package opendataflow

import sbt.Keys._
import sbt._
import sbtunidoc.Plugin.ScalaUnidoc
import sbtunidoc.Plugin.UnidocKeys._

object OpenDataFlowBuild extends Build {
   val parallelExecutionByDefault = false // TODO: enable this once we're sure it doesn not break things

  lazy val buildSettings = Dependencies.Versions ++ Seq(
    organization        := "net.opendataflow",
    version             := "0.1-SNAPSHOT"
  )

  lazy val root = Project(
    id = "opendataflow",
    base = file("."),
    settings = parentSettings , 

    aggregate = Seq(core)
  )

  lazy val core = Project(
    id = "opendataflow-core",
    base = file("opendataflow-core")
  )

  override lazy val settings =
    super.settings ++
    buildSettings ++
    Seq(
      shellPrompt := { s => Project.extract(s).currentProject.id + " > " }
    ) ++
    resolverSettings

  lazy val baseSettings = Defaults.defaultSettings

  lazy val parentSettings = baseSettings ++ Seq(
    publishArtifact := false
  ) ++ dontPublishSettings

   val dontPublishSettings = Seq(
    publish := ()
  )

  val (mavenLocalResolver, mavenLocalResolverSettings) =
    System.getProperty("opendataflow.build.M2Dir") match {
      case null => (Resolver.mavenLocal, Seq.empty)
      case path =>
        // Maven resolver settings
        val resolver = Resolver.file("user-publish-m2-local", new File(path))
        (resolver, Seq(
          otherResolvers := resolver:: publishTo.value.toList,
          publishM2Configuration := Classpaths.publishConfig(packagedArtifacts.value, None, resolverName = resolver.name, checksums = checksums.in(publishM2).value, logging = ivyLoggingLevel.value)
        ))
    }

  lazy val resolverSettings = {
    // should we be allowed to use artifacts published to the local maven repository
    if(System.getProperty("opendataflow.build.useLocalMavenResolver", "false").toBoolean)
      Seq(resolvers += mavenLocalResolver)
    else Seq.empty
  } ++ {
    // should we be allowed to use artifacts from sonatype snapshots
    if(System.getProperty("opendataflow.build.useSnapshotSonatypeResolver", "false").toBoolean)
      Seq(resolvers += Resolver.sonatypeRepo("snapshots"))
    else Seq.empty
  } ++ Seq(
    pomIncludeRepository := (_ => false) // do not leak internal repositories during staging
  )

  private def allWarnings: Boolean = System.getProperty("opendataflow.allwarnings", "false").toBoolean

  lazy val defaultSettings = resolverSettings ++ Seq(
    // compile options
    scalacOptions in Compile ++= Seq("-encoding", "UTF-8", "-target:jvm-1.8", "-feature", "-unchecked", "-Xlog-reflective-calls", "-Xlint"),
    scalacOptions in Compile ++= (if (allWarnings) Seq("-deprecation") else Nil),
    scalacOptions in Test := (scalacOptions in Test).value.filterNot(opt =>
      opt == "-Xlog-reflective-calls" || opt.contains("genjavadoc")),
    // -XDignore.symbol.file suppresses sun.misc.Unsafe warnings
    javacOptions in compile ++= Seq("-encoding", "UTF-8", "-source", "1.8", "-target", "1.8", "-Xlint:unchecked", "-XDignore.symbol.file"),
    javacOptions in compile ++= (if (allWarnings) Seq("-Xlint:deprecation") else Nil),
    javacOptions in doc ++= Seq("-encoding", "UTF-8", "-source", "1.8"),
    incOptions := incOptions.value.withNameHashing(true),

    crossVersion := CrossVersion.binary,

    ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet,

    licenses := Seq(("Apache License, Version 2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))),
    homepage := Some(url("http://opendataflow.net/")),

    initialCommands :=
      """|import language.postfixOps
         |import opendataflow._
         |import scala.concurrent._
         |import com.typesafe.config.ConfigFactory
         |import scala.concurrent.duration._
         |""".stripMargin,

    /**
     * Test settings
     */

    // show full stack traces and test case durations
    testOptions in Test += Tests.Argument("-oDF"),

    // don't save test output to a file
    testListeners in (Test, test) := Seq(TestLogger(streams.value.log, {_ => streams.value.log }, logBuffered.value)),

    // -v Log "test run started" / "test started" / "test run finished" events on log level "info" instead of "debug".
    // -a Show stack traces and exception class name for AssertionErrors.
    testOptions += Tests.Argument(TestFrameworks.JUnit, "-v", "-a")
  ) ++
    mavenLocalResolverSettings 




}
