import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "MemoryWatch"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    "org.scala-tools" % "scala-stm_2.9.1" % "0.3",
    "org.mongodb" % "mongo-java-driver" % "2.11.2",
    "net.vz.mongodb.jackson" %% "play-mongo-jackson-mapper" % "1.1.0",
    javaCore,
    javaJdbc,
    javaEbean
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here      
  )

}
