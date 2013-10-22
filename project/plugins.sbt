addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.2.0")

addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.2.0")

// ls.implicit.ly
addSbtPlugin("me.lessis" % "ls-sbt" % "0.1.2")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.10.0")

addSbtPlugin("org.ensime" % "ensime-sbt-cmd" % "0.1.2")

resolvers ++= Seq(
  Classpaths.sbtPluginReleases,
  Opts.resolver.sonatypeReleases
)
