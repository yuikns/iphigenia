import sbt.Keys._
import sbt.{ SbtExclusionRule, _ }
import sbtassembly.AssemblyKeys._
import sbtassembly.{ MergeStrategy, PathList }

object BuildIphigenia extends Build {
  lazy val id = "iphigenia"
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
    homepage := Some(url("https://github.com/yuikns/iphigenia"))
  )

  lazy val publishSettings = Seq(
    isSnapshot := false,
    publishMavenStyle := true,
    publishTo := Some(Resolver.file("file", new File(Path.userHome.absolutePath + "/.m2/repository"))),
    publishArtifact in Test := false,
    pomIncludeRepository := { _ => false }
  )

  lazy val hadoopDependencies = Seq[ModuleID](
    "org.apache.hadoop" % "hadoop-common" % hadoopVersion,
    "org.apache.hadoop" % "hadoop-client" % hadoopVersion,
    "org.apache.hadoop" % "hadoop-hdfs" % hadoopVersion,
    "org.apache.hadoop" % "hadoop-mapreduce" % hadoopVersion pomOnly (),
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
    "org.apache.spark" % "spark-core_2.11" % sparkVersion,
    "org.apache.spark" % "spark-mllib_2.11" % sparkVersion,
    "org.apache.spark" % "spark-sql_2.11" % sparkVersion,
    "org.apache.spark" % "spark-streaming_2.11" % sparkVersion,
    "org.apache.spark" % "spark-graphx_2.11" % sparkVersion,
    "org.apache.spark" % "spark-network-common_2.11" % sparkVersion,
    "org.apache.spark" % "spark-network-shuffle_2.11" % sparkVersion,
    "org.apache.spark" % "spark-yarn_2.11" % sparkVersion,
    "org.apache.spark" % "spark-launcher_2.11" % sparkVersion
  )

  lazy val tensorflowDependencies = Seq[ModuleID](
    //"org.tensorflow" % "libtensorflow" % "1.0.preview1"
  )

  lazy val linearDependencies = Seq[ModuleID](
    "de.bwaldvogel" % "liblinear" % "1.95"
  )

  lazy val xgboostDependencies = Seq[ModuleID](
    //"ml.dmlc" % "xgboost4j" % "0.7" % "provided",
    //"ml.dmlc" % "xgboost4j-spark" % "0.7" % "provided",
    //"ml.dmlc" % "xgboost4j-flink" % "0.7",
    "org.apache.commons" % "commons-lang3" % "3.4"
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
      "ch.qos.logback" % "logback-classic" % "1.1.2", // logger, can be ignored in play framwork
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
      sparkDependencies ++
      linearDependencies ++
      tensorflowDependencies ++
      xgboostDependencies,
    dependencyOverrides ++= Set(
      "org.scala-lang" % "scala-reflect" % projScalaVersion,
      "org.scala-lang" % "scala-compiler" % projScalaVersion,
      "org.scala-lang" % "scala-library" % projScalaVersion,
      "org.scala-lang.modules" % "scala-xml_2.11" % "1.0.4",
      "org.scala-lang.modules" % "scala-parser-combinators_2.11" % "1.0.4",
      //"commons-logging" % "commons-logging" % "1.2",
      "commons-io" % "commons-io" % "2.4",
      "com.google.guava" % "guava" % "18.0"
    ),
    excludeDependencies ++= Seq(
      SbtExclusionRule(organization = "org.slf4j", name = "slf4j-log4j12") // ignore default logger, use logback instead
    )
  )

  lazy val assemblySettings = Seq(
    assemblyJarName in assembly := s"$id-$projVersion-$projScalaVersion.jar",
    assemblyMergeStrategy in assembly := {
      case PathList("javax", "servlet", xs @ _*) => MergeStrategy.last
      case PathList("javax", "activation", xs @ _*) => MergeStrategy.last
      case PathList("org", "apache", xs @ _*) => MergeStrategy.last
      case PathList("com", "google", xs @ _*) => MergeStrategy.last
      case PathList("com", "esotericsoftware", xs @ _*) => MergeStrategy.last
      case PathList("com", "codahale", xs @ _*) => MergeStrategy.last
      case PathList("com", "yammer", xs @ _*) => MergeStrategy.last
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
        if (x.startsWith("META-INF/") &&
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
    mainClass in (Compile, run) := Some("Launcher")
  )

}

