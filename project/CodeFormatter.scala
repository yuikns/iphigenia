import com.typesafe.sbt.SbtScalariform._
import sbt.Keys._
import sbt._

object CodeFormatter extends AutoPlugin {
  import scalariform.formatter.preferences.{ DoubleIndentClassDeclaration, FormatXml, PreserveDanglingCloseParenthesis }

  lazy val BuildConfig = config("build") extend Compile
  lazy val BuildSbtConfig = config("buildsbt") extend Compile

  lazy val prefs = Seq(
    ScalariformKeys.preferences := ScalariformKeys.preferences.value
      .setPreference(FormatXml, false)
      .setPreference(DoubleIndentClassDeclaration, false)
      .setPreference(PreserveDanglingCloseParenthesis, true))

  override def trigger = allRequirements

  override def projectSettings = defaultScalariformSettings ++ prefs ++
    inConfig(BuildConfig)(configScalariformSettings) ++
    inConfig(BuildSbtConfig)(configScalariformSettings) ++
    Seq(scalaSource in BuildConfig := baseDirectory.value / "project",
      scalaSource in BuildSbtConfig := baseDirectory.value / "project",
      includeFilter in (BuildConfig, ScalariformKeys.format) := ("*.scala": FileFilter),
      includeFilter in (BuildSbtConfig, ScalariformKeys.format) := ("*.sbt": FileFilter),
      ScalariformKeys.format in Compile := {
        (ScalariformKeys.format in BuildSbtConfig).value
        (ScalariformKeys.format in BuildConfig).value
        (ScalariformKeys.format in Compile).value
      })
}
