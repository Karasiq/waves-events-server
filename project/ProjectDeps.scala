import sbt._

object ProjectDeps {
  type Deps = Seq[ModuleID]
  private[this] implicit def implicitSbtDepToDeps(m: ModuleID): Deps = Seq(m)

  object akka {
    val version = "2.5.20"
    val httpVersion = "10.1.7"

    def actors: Deps = Seq(
      "com.typesafe.akka" %% "akka-actor" % version,
      "com.typesafe.akka" %% "akka-actor-typed" % version,
    )

    def streams: Deps = Seq(
      "com.typesafe.akka" %% "akka-stream" % version,
      "com.typesafe.akka" %% "akka-stream-typed" % version
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

  val ficus: Deps = "com.iheart" %% "ficus" % "1.4.2"

  val monix = Def.setting(
    Seq(
      // exclusion and explicit dependency can likely be removed when monix 3 is released
      ("io.monix" %% "monix" % "3.0.0-RC1").exclude("org.typelevel", "cats-effect_2.12"),
      "org.typelevel" %% "cats-effect" % "0.10.1"
    ))

  val wavesNode: Deps = "com.github.karasiq" %% "waves" % "0.15.2"

  def enableScalaMeta = addCompilerPlugin(
    ("org.scalameta" % "paradise" % "3.0.0-M11").cross(CrossVersion.full))
}
