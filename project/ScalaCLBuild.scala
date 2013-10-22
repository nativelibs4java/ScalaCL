import sbt._
import Keys._
import ls.Plugin._

import sbtassembly.Plugin._ ; import AssemblyKeys._
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
    
  lazy val sonatypeSettings = Seq(
    publishMavenStyle := true,
    resolvers += Resolver.sonatypeRepo("snapshots"),
    publishTo <<= version { (v: String) =>
      val nexus = "https://oss.sonatype.org/"
      if (v.trim.endsWith("-SNAPSHOT"))
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    })

  lazy val infoSettings = Seq(
    organization := "com.nativelibs4java",
    version := "0.3-SNAPSHOT",
    licenses := Seq("BSD-3-Clause" -> url("http://www.opensource.org/licenses/BSD-3-Clause")),
    homepage := Some(url("https://github.com/ochafik/ScalaCL")),
    pomIncludeRepository := { _ => false },
    pomExtra := (
      <scm>
        <url>git@github.com:ochafik/ScalaCL.git</url>
        <connection>scm:git:git@github.com:ochafik/ScalaCL.git</connection>
      </scm>
      <developers>
        <developer>
          <id>ochafik</id>
          <name>Olivier Chafik</name>
          <url>http://ochafik.com/</url>
        </developer>
      </developers>
    ),
    (LsKeys.docsUrl in LsKeys.lsync) <<= homepage,
    (LsKeys.tags in LsKeys.lsync) :=
       Seq("opencl", "GPGPU", "macro", "GPU", "JavaCL"),
    (description in LsKeys.lsync) :=
      "OpenCL-powered and macro-powered data structures to store and transform data straight on the graphic card, using an API akin to Scala collections. Scala closures are transformed to OpenCL kernels automagically by macros, during compilation.",
    LsKeys.ghUser := Some("ochafik"),
    LsKeys.ghRepo := Some("ScalaCL"))

	lazy val standardSettings = 
	  Defaults.defaultSettings ++ 
	  scalariformSettings ++ 
	  sonatypeSettings ++
	  infoSettings ++
	  Seq(
      //resolvers += "Local Maven Repository" at "file://" + Path.userHome + "/.m2/repository",

      fork := true,

      scalaVersion := "2.10.3",
      scalacOptions ++= Seq(
        "-language:experimental.macros",
        "-encoding", "UTF-8",
        // "-optimise",
        "-deprecation",
        "-feature",
        // "-Xlog-free-types",
        // "-Ymacro-debug-lite",
        "-unchecked"
      ),

      libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-reflect" % _),
      libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-compiler" % _),
      libraryDependencies ++= Seq(
        "junit" % "junit" % "4.10" % "test",
        "com.novocode" % "junit-interface" % "0.8" % "test"
      )
    )

  def addLocalOrRemoteDependencies(project: Project, dependenciesAndTheirLocalPaths: List[(ModuleID, String, String)]): Project = {
    dependenciesAndTheirLocalPaths match {
      case Nil =>
        project
      case (dependency, dependencyProjectRoot, gitURL) :: rest =>
        addLocalOrRemoteDependencies(
          if (new File(dependencyProjectRoot).exists() && "1" != System.getenv("NO_LOCAL_DEPS")) {
            project.dependsOn(ProjectRef(file(dependencyProjectRoot), dependency.name))
          } else {
            println(dependencyProjectRoot + " does not exist. If you want to modify " + dependency.name + ", please clone it with:\n\tgit clone " + gitURL + " " + dependencyProjectRoot + "\n")
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

  lazy val shadeSettings =
    assemblySettings ++
    addArtifact(artifact in (Compile, assembly), assembly) ++
    Seq(
      publishArtifact in (Compile, packageBin) := false,
      excludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
        // Exclude scala-library and al.
        cp filter { j => {
          val n = j.data.getName
          n.startsWith("scala-") || n.equals("jfxrt.jar")
        } }
      },
      pomPostProcess := { (node: scala.xml.Node) =>
        // Since we publish the assembly (shaded) jar,
        // remove lagging scalaxy dependencies from pom ourselves.
        import scala.xml._; import scala.xml.transform._
        try {
          new RuleTransformer(new RewriteRule {
            override def transform(n: Node): Seq[Node] ={
              if ((n \ "artifactId" find { _.text.startsWith("scalaxy-") }) != None)
                Array[Node]()
              else
                n
            }
          })(node)
        } catch { case _: Throwable =>
          node
        }
      }
    )

  lazy val ScalaCL =
      Project(
        id = "ScalaCL",
        base = file("."),
        settings = standardSettings ++
          // shadeSettings ++
          Seq(
            name := "scalacl",
            scalacOptions ++= Seq(
              "-language:experimental.macros"
            ),
            // mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
            //   {
            //     case f if f.matches(""".*?\.(class|dll|so|dylib)""") => MergeStrategy.first
            //     case x => old(x)
            //   }
            // },
            libraryDependencies ++= Seq(
              "com.nativelibs4java" %% "scalaxy-components" % "0.3-SNAPSHOT",
              "com.nativelibs4java" %% "scalaxy-reified" % "0.3-SNAPSHOT",
              //"com.nativelibs4java" %% "scalaxy-reified-base" % "0.3-SNAPSHOT",
              "com.nativelibs4java" % "javacl" % "1.0-SNAPSHOT"
            ),
            fork in Test := true,
            //javaOptions in Test ++= Seq("-Xdebug", "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"),
            scalacOptions in Test ++= Seq(
              "-optimise",
              "-Yclosure-elim",
              "-Yinline"
            )
          ))

  // lazy val ScalaCL =
  //   addLocalOrRemoteDependencies(
  //     Project(
  //       id = "ScalaCL",
  //       base = file("."),
  //       settings = standardSettings ++ Seq(
  //         name := "scalacl",
  //         scalacOptions ++= Seq(
  //           "-language:experimental.macros"
  //         ),
  //         libraryDependencies ++= Seq(
  //           "com.nativelibs4java" %% "scalaxy-components" % "0.3-SNAPSHOT",
  //           "com.nativelibs4java" %% "scalaxy-reified" % "0.3-SNAPSHOT",
  //           "com.nativelibs4java" % "javacl" % "1.0-SNAPSHOT"
  //         )
  //       )
  //     ),
  //     List(
  //       (
  //         "com.nativelibs4java" %% "scalaxy-components" % "0.3-SNAPSHOT",
  //         "../Scalaxy",
  //         "git://github.com/ochafik/Scalaxy.git"
  //       ),
  //       (
  //         "com.nativelibs4java" %% "scalaxy-reified" % "0.3-SNAPSHOT",
  //         "../Scalaxy",
  //         "git://github.com/ochafik/Scalaxy.git"
  //       )
  //     )
  //   )
}
