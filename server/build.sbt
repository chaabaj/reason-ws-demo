
name := "server"

version := "0.1"

scalaVersion := "2.12.7"

val log4j2Version = "2.11.0"

scalacOptions ++=  Seq(
  "-Ypartial-unification",
  "-unchecked", // able additional warnings where generated code depends on assumptions
  "-deprecation", // emit warning for usages of deprecated APIs
  "-feature", // emit warning usages of features that should be imported explicitly
  // Features enabled by default
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:experimental.macros",
  "-language:existentials",
  // possibly deprecated options
  "-Ywarn-inaccessible",
  "-Xfatal-warnings",
  // Enables linter options
  "-Xlint:adapted-args", // warn if an argument list is modified to match the receiver
  "-Xlint:nullary-unit", // warn when nullary methods return Unit
  "-Xlint:inaccessible", // warn about inaccessible types in method signatures
  "-Xlint:nullary-override", // warn when non-nullary `def f()' overrides nullary `def f'
  "-Xlint:infer-any", // warn when a type argument is inferred to be `Any`
  "-Xlint:missing-interpolator", // a string literal appears to be missing an interpolator id
  "-Xlint:doc-detached", // a ScalaDoc comment appears to be detached from its element
  "-Xlint:private-shadow", // a private field (or class parameter) shadows a superclass field
  "-Xlint:type-parameter-shadow", // a local type parameter shadows a type already in scope
  "-Xlint:poly-implicit-overload", // parameterized overloaded implicit methods are not visible as view bounds
  "-Xlint:option-implicit", // Option.apply used implicit view
  "-Xlint:delayedinit-select", // Selecting member of DelayedInit
  "-Xlint:by-name-right-associative", // By-name parameter of right associative operator
  "-Xlint:package-object-classes", // Class or object defined in package object
  "-Xlint:unsound-match" // Pattern match may not be typesafe
)

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "1.4.0",
  "org.typelevel" %% "cats-kernel" % "1.4.0",
  "io.monix" %% "monix" % "3.0.0-RC1",
  "io.monix" %% "monix-eval" % "3.0.0-RC1",
  "com.typesafe.akka" %% "akka-stream" % "2.5.11",
  "com.typesafe.akka" %% "akka-http" % "10.1.5",
  "com.typesafe.akka" %% "akka-actor" % "2.5.11",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.5",
  "ch.megard" %% "akka-http-cors" % "0.3.0",
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % log4j2Version,
  "org.apache.logging.log4j" % "log4j-api" % log4j2Version,
  "org.apache.logging.log4j" % "log4j-core" % log4j2Version,
  "com.github.mpilquist" %% "simulacrum" % "0.13.0",
)

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

scalafmtConfig in ThisBuild := Some(file(".scalafmt.conf"))

enablePlugins(JavaAppPackaging)
