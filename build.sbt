version in ThisBuild := "1.0.0"

organization in ThisBuild := "com.wavesplatform"

scalaVersion in ThisBuild := "2.12.8"

lazy val commonSettings = Seq(
  libraryDependencies ++=
    ProjectDeps.akka.all ++
    ProjectDeps.cats.all ++
    ProjectDeps.monix.value ++
    ProjectDeps.ficus ++
    ProjectDeps.playJson ++
    ProjectDeps.wavesNode ++
    ProjectDeps.scalaTest.map(_ % "test"),

  // ProjectDeps.enableScalaMeta
)

lazy val clientSettings = Seq(
  name := "waves-events-client"
)

lazy val serverSettings = Seq(
  name := "waves-events-server"
)

lazy val common = project
  .settings(commonSettings)

lazy val client = project
  .settings(commonSettings, clientSettings)
  .dependsOn(common)

lazy val server = project
  .settings(commonSettings, serverSettings)
  .dependsOn(common, client)

lazy val tests = project
  .settings(commonSettings)
  .dependsOn(common, client, server)