name := "InterestingCVToy"

version := "1.0"

scalaVersion := "2.11.7"

scalacOptions ++= Seq("-feature", "-unchecked", "-deprecation", "-Xlint", "-language:implicitConversions", "-language:postfixOps", "-language:higherKinds")

// Platform classifier for native library dependencies
lazy val platform = org.bytedeco.javacpp.Loader.getPlatform
// Some dependencies like `javacpp` are packaged with maven-plugin packaging
classpathTypes += "maven-plugin"

val javacppVersion = "1.0"

val opencvVersion = "3.0.0"

libraryDependencies ++= Seq(
  "org.bytedeco"                 % "javacpp"         % javacppVersion,
  "org.bytedeco"                 % "javacv"          % javacppVersion,
  "org.bytedeco.javacpp-presets" % "flandmark" % ("1.07-" + javacppVersion) classifier "",
  "org.bytedeco.javacpp-presets" % "flandmark" % ("1.07-" + javacppVersion) classifier platform,
  "org.bytedeco.javacpp-presets" % "opencv" % (opencvVersion + "-" + javacppVersion) classifier "",
  "org.bytedeco.javacpp-presets" % "opencv" % (opencvVersion + "-" + javacppVersion) classifier platform,
  "org.scalafx" %% "scalafx" % "8.0.40-R8"
)

autoCompilerPlugins := true

// fork a new JVM for 'run' and 'test:run'
fork := true
// add a JVM option to use when forking a JVM for 'run'
javaOptions += "-Xmx2G"

// Set the prompt (for this build) to include the project id.
shellPrompt in ThisBuild := { state => "sbt:" + Project.extract(state).currentRef.project + "> "}