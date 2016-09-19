import sbt.Keys._
import sbt._
import sbtassembly.AssemblyKeys._
import sbtassembly.{MergeStrategy, PathList}

object BuildCSE8803 extends Build {
  lazy val id = "cse8803"
  lazy val projVersion = "0.0.1"
  lazy val projOrganization = "com.argcv"
  lazy val projScalaVersion = "2.11.8"
  lazy val hadoopVersion = "2.7.2"
  lazy val sparkVersion = "2.0.0"

  lazy val commonSettings = Seq(
    name := id,
    version := projVersion,
    organization := projOrganization,
    scalaVersion := projScalaVersion,
    licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT")),
    homepage := Some(url("https://github.com/yuikns/cse8803-hw"))
  )

  lazy val publishSettings = Seq(
    isSnapshot := false,
    publishMavenStyle := true,
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    publishArtifact in Test := false,
    pomIncludeRepository := { _ => false }
  )

  lazy val hadoopDependencies = Seq[ModuleID](
    "org.apache.hadoop" % "hadoop-common" % hadoopVersion,
    "org.apache.hadoop" % "hadoop-client" % hadoopVersion,
    "org.apache.hadoop" % "hadoop-hdfs" % hadoopVersion,
    "org.apache.hadoop" % "hadoop-mapreduce" % hadoopVersion pomOnly(),
    "org.apache.hadoop" % "hadoop-mapreduce-client-core" % hadoopVersion,
    "org.apache.hadoop" % "hadoop-mapreduce-client-common" % hadoopVersion,
    "org.apache.hadoop" % "hadoop-annotations" % hadoopVersion,
    "org.apache.hadoop" % "hadoop-yarn-api" % hadoopVersion,
    "org.apache.hadoop" % "hadoop-yarn-client" % hadoopVersion,
    "org.apache.hadoop" % "hadoop-streaming" % hadoopVersion,
    "org.apache.hadoop" % "hadoop-distcp" % hadoopVersion,
    "org.apache.hadoop" % "hadoop-aws" % hadoopVersion
  )

  lazy val sparkDependencies = Seq[ModuleID](
    "org.apache.spark" % "spark-core_2.11" % sparkVersion
  )

  lazy val dependenciesSettings = Seq(
    resolvers ++= Seq(
      "Atlassian Releases" at "https://maven.atlassian.com/public/",
      "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
      Resolver.sonatypeRepo("snapshots"),
      Classpaths.typesafeReleases,
      Classpaths.typesafeSnapshots
    ),
    libraryDependencies ++= Seq(
      //"com.nativelibs4java" %% "scalacl" % "0.3-SNAPSHOT",
      "com.nativelibs4java" % "javacl-core" % "1.0.0-RC4",
      "commons-pool" % "commons-pool" % "1.6", // pool for SockPool
      "net.liftweb" % "lift-webkit_2.11" % "3.0-M6", // a light weight framework for web
      //"com.google.guava" % "guava" % "18.0", // string process etc. (snake case for example)
      "org.scalaj" % "scalaj-http_2.11" % "2.2.0",
      "net.ruippeixotog" % "scala-scraper_2.11" % "1.0.0", //: https://github.com/ruippeixotog/scala-scraper
      "com.hierynomus" % "sshj" % "0.17.2", // for ssh
      "org.scalatest" % "scalatest_2.11" % "2.2.5" % "test"
    ) ++
      hadoopDependencies ++
      sparkDependencies,
    dependencyOverrides ++= Set(
      "org.scala-lang" % "scala-reflect" % projScalaVersion,
      "org.scala-lang" % "scala-compiler" % projScalaVersion,
      "org.scala-lang" % "scala-library" % projScalaVersion,
      "org.scala-lang.modules" % "scala-xml_2.11" % "1.0.4",
      "org.scala-lang.modules" % "scala-parser-combinators_2.11" % "1.0.4",
      "commons-logging" % "commons-logging" % "1.2",
      "commons-io" % "commons-io" % "2.4",
      "com.google.guava" % "guava" % "18.0"
    )
  )

  lazy val assemblySettings = Seq(
    assemblyJarName in assembly := s"$id-$projVersion-$projScalaVersion.jar",
    assemblyMergeStrategy in assembly := {
      case PathList("javax", "servlet", xs@_*) => MergeStrategy.last
      case PathList("javax", "activation", xs@_*) => MergeStrategy.last
      case PathList("org", "apache", xs@_*) => MergeStrategy.last
      case PathList("com", "google", xs@_*) => MergeStrategy.last
      case PathList("com", "esotericsoftware", xs@_*) => MergeStrategy.last
      case PathList("com", "codahale", xs@_*) => MergeStrategy.last
      case PathList("com", "yammer", xs@_*) => MergeStrategy.last
      case "about.html" => MergeStrategy.rename
      case "META-INF/ECLIPSEF.RSA" => MergeStrategy.discard
      case "META-INF/mailcap" => MergeStrategy.last
      case "META-INF/mimetypes.default" => MergeStrategy.last
      case "META-INF/MANIFEST.MF" => MergeStrategy.discard
      case "plugin.properties" => MergeStrategy.last
      case "log4j.properties" => MergeStrategy.last
      case x =>
        // p1
        //val oldStrategy = (assemblyMergeStrategy in assembly).value
        //oldStrategy(x)
        // p2

        // ignore
        if(x.startsWith("META-INF/") &&
          (x.endsWith(".DSA") || x.endsWith(".RSA") || x.endsWith(".SF"))) {
          MergeStrategy.discard
        } else {
          MergeStrategy.last
        }
    }
  )
  lazy val root = Project(id = id, base = file("."))
    .settings(commonSettings: _*)
    .settings(publishSettings: _*)
    .settings(dependenciesSettings: _*)
    .settings(assemblySettings: _*)
    .settings(launchSettings: _*)
    .dependsOn(valhalla)
    .aggregate(valhalla)
  lazy val valhalla = ProjectRef(file("modules/valhalla"), "valhalla")

  def launchSettings = Seq(
    // set the main class for 'sbt run'
    mainClass in(Compile, run) := Some("com.argcv.cse8803.mapreducebasic.FrequencyMapReducer")
  )

}

