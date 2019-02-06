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
    ProjectDeps.scalaTest.map(_ % "test"),

  // ProjectDeps.enableScalaMeta
)

lazy val common = project
  .settings(commonSettings)

lazy val client = project
  .settings(commonSettings, name := "waves-events-client")
  .dependsOn(common)

lazy val server = project
  .settings(commonSettings, name := "waves-events-server")
  .dependsOn(common, client)

lazy val serverApp = (project in file("server") / "app")
  .settings(commonSettings, name := "waves-events-server-app")
  .dependsOn(server)

lazy val tests = project
  .settings(commonSettings, libraryDependencies ++= ProjectDeps.wavesNode)
  .dependsOn(common, client, server)