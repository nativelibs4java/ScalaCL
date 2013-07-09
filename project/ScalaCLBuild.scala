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
    
	val sharedSettings = 
	  Defaults.defaultSettings ++ 
	  scalariformSettings ++ 
	  Seq(
      organization := "com.nativelibs4java",
      version := "0.3-SNAPSHOT",
      
      fork := true,
      
      scalaVersion := "2.10.2",
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
        //"com.nativelibs4java" %% "scalaxy-components" % "0.3-SNAPSHOT",
        "com.novocode" % "junit-interface" % "0.5" % "test->default"
      ),    
  
      libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-compiler" % _)
    )
	
  val scalaxyPath = "../Scalaxy"
  
  def log(s: String) = println("[scalacl] " + s)
  
	lazy val useLocalScalaxy = {
	  val exists = new File(scalaxyPath).exists
	  if (exists) {
	    log(scalaxyPath + " exists: building Scalaxy as part of ScalaCL")
	  } else {
	    log(scalaxyPath + " does not exist. If you want to modify it, please clone it with:\n\tgit clone git://github.com/ochafik/Scalaxy ../Scalaxy\n")
	  }
	  exists
	}
	
  def addLocalOrRemoteDependencies(project: Project, dependenciesAndTheirLocalPaths: List[(ModuleID, String)]): Project = {
    dependenciesAndTheirLocalPaths match {
      case Nil =>
        project
      case (dependency, dependencyProjectRoot) :: rest =>
        addLocalOrRemoteDependencies(
          if (new File(dependencyProjectRoot).exists()) {
            project.dependsOn(ProjectRef(file(dependencyProjectRoot), dependency.name))
          } else {
            project.copy(
              settings = (project: ProjectDefinition[_]).settings ++ Seq(
                libraryDependencies ++= Seq(dependency)
              )
            )
          },
          rest
        )
    }
  }
  
  lazy val ScalaCL = 
    addLocalOrRemoteDependencies(
      Project(
        id = "ScalaCL",
        base = file("."),
        settings = sharedSettings ++ Seq(
          name := "scalacl"
        )
      ),
      List(
        "com.nativelibs4java" %% "scalaxy-components" % "0.3-SNAPSHOT" -> "../Scalaxy"
      )
    )
}
