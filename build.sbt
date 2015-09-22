name := "akka-dumbdi"

licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

homepage := Some(url("https://github.com/KadekM/akka-dumbdi"))

organization := "com.marekkadek"

version := "0.0.2-SNAPSHOT"

scalaVersion := "2.11.7"

val akkaVersion = "2.4.0-RC3"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion  % Compile,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion  % Test,
  "org.scalatest" %% "scalatest" % "2.2.4" % Test
)
 
scalacOptions ++= Seq(
"-Xlint",
 "-deprecation"
)

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra :=
    <scm>
      <url>https://github.com/kadekm/akka-dumbdi</url>
      <connection>scm:git://github.com/kadekm/akka-dumbdi.git</connection>
    </scm>
    <developers>
      <developer>
        <id>kadekm</id>
        <name>Marek Kadek</name>
        <url>https://github.com/KadekM</url>
      </developer>
    </developers>
