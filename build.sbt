scalaVersion := "2.12.6"

libraryDependencies ++= Seq(
  "com.typesafe.akka"                %% "akka-http"         % "10.0.11",
  "de.heikoseeberger"                %% "akka-http-json4s"  % "1.19.0",
  "com.typesafe.akka"                %% "akka-stream"       % "2.5.14",
  "org.json4s"                       %% "json4s-native"     % "3.5.3",
  "org.spire-math"                   %% "jawn-json4s"       % "0.11.0",
  "net.steppschuh.markdowngenerator" %  "markdowngenerator" % "1.3.1.1",
  "com.lihaoyi"                      %% "utest"             % "0.6.3"     % Test
)

testFrameworks += new TestFramework("utest.runner.Framework")

scalacOptions := Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-feature",
  "-unchecked",
  "-Ywarn-unused-import"
)