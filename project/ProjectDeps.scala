import sbt._

object ProjectDeps {
  type Deps = Seq[ModuleID]
  private[this] implicit def implicitSbtDepToDeps(m: ModuleID): Deps = Seq(m)

  object akka {
    val version = "2.5.20"
    val httpVersion = "10.1.7"

    def actors: Deps = Seq(
      "com.typesafe.akka" %% "akka-actor" % version,
      "com.typesafe.akka" %% "akka-typed" % version,
    )

    def streams: Deps = Seq(
      "com.typesafe.akka" %% "akka-stream" % version
    )

    def http: Deps = Seq(
      "com.typesafe.akka" %% "akka-http" % httpVersion
    )

    def persistence: Deps = Seq(
      "com.typesafe.akka" %% "akka-persistence" % version
    )

    def testKit: Deps = Seq(
      "com.typesafe.akka" %% "akka-testkit" % version,
      "com.typesafe.akka" %% "akka-stream-testkit" % version,
      "com.typesafe.akka" %% "akka-http-testkit" % httpVersion
    )

    def slf4j: Deps = Seq(
      "com.typesafe.akka" %% "akka-slf4j" % version
    )

    def all: Deps = {
      actors ++ streams ++ http ++ persistence // ++ testKit.map(_ % "test")
    }
  }

  object cats {
    val core: Deps = "org.typelevel" %% "cats-core" % "1.6.0"
    val mtl: Deps = "org.typelevel" %% "cats-mtl-core" % "0.4.0"
    val tagless: Deps = "org.typelevel" %% "cats-tagless-macros" % "0.1.0"
    val kittens: Deps = "org.typelevel" %% "kittens" % "1.2.0"
    val all: Deps = core ++ mtl ++ kittens // ++ tagless
  }

  def scalaTest: Deps = Seq(
    "org.scalatest" %% "scalatest" % "3.0.3"
  )

  def playJson: Deps = Seq(
    "com.typesafe.play" %% "play-json" % "2.6.7"
  )

  def enableScalaMeta = addCompilerPlugin(
    ("org.scalameta" % "paradise" % "3.0.0-M11").cross(CrossVersion.full))
}
