// Comment to get more information during initialization
logLevel := Level.Info

resolvers ++= Seq(
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/", //  The Typesafe repository
  "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/" // The Sonatype snapshots repository
)

// Use the Scalariform plugin to reformat the code
addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.3.0")

// for publish to Sonatype
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "0.5.1")

// fot sbt-0.13.5 or higher
//addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")

// for package, visit https://github.com/sbt/sbt-assembly for detail
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.1")
