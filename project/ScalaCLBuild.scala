import sbt._
import Keys._

import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtScalariform.scalariformSettings
import com.typesafe.sbt.SbtScalariform._

object ScalaCLBuild extends Build {
  // See https://github.com/mdr/scalariform
  ScalariformKeys.preferences := FormattingPreferences()
    .setPreference(MultilineScaladocCommentsStartOnFirstLine, true)
    .setPreference(PreserveDanglingCloseParenthesis, true)
    .setPreference(AlignSingleLineCaseStatements, true)
    .setPreference(DoubleIndentClassDeclaration, true)
    .setPreference(PreserveDanglingCloseParenthesis, false)
    
	val sharedSettings = Defaults.defaultSettings ++ Seq(
		organization := "com.nativelibs4java",
    version := "0.3-SNAPSHOT",
    
    fork := true,
    
    scalaVersion := "2.10.0",
    scalacOptions ++= Seq(
      "-language:experimental.macros",
      "-deprecation"
      //"-Ymacro-debug-lite"
      //"-Xlog-free-terms", 
      //"-unchecked",
    ),
    
		resolvers += "Sonatype OSS Repository" at "http://oss.sonatype.org/content/repositories/snapshots",
		
		libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-compiler" % _),
    libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-reflect" % _),
      
		libraryDependencies ++= Seq(
      "com.nativelibs4java" % "javacl" % "1.0-SNAPSHOT",
      "com.nativelibs4java" %% "scalaxy-components" % "0.3-SNAPSHOT",
      "com.novocode" % "junit-interface" % "0.5" % "test->default"
    ),    

    libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-compiler" % _)
	)

	lazy val ScalaCL = Project(
		id = "ScalaCL",
		base = file("."),
		settings = sharedSettings ++ scalariformSettings ++ Seq(
		  name := "scalacl"
		)
	)
}
