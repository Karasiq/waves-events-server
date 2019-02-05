version in ThisBuild := "1.0.0"

organization in ThisBuild := "com.wavesplatform"

scalaVersion in ThisBuild := "2.12.8"

val commonSettings = Seq(
  libraryDependencies ++=
    ProjectDeps.akka.all ++
    ProjectDeps.cats.all ++
    ProjectDeps.playJson ++
    ProjectDeps.scalaTest.map(_ % "test")
)

val clientSettings = Seq(
  name := "waves-events-client"
)

val serverSettings = Seq(
  name := "waves-events-server"
)

val common = project
  .settings(commonSettings)

val client = project
  .settings(commonSettings, clientSettings)
  .dependsOn(common)

val server = project
  .settings(commonSettings, serverSettings)
  .dependsOn(common, client)

val tests = project
  .settings(commonSettings)
  .dependsOn(common, client, server)