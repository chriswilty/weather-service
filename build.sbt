organization in ThisBuild := "com.scottlogic"
version in ThisBuild := "1.0.0-SNAPSHOT"
scalaVersion in ThisBuild := "2.12.6"

lagomKafkaEnabled in ThisBuild := false
lagomCassandraEnabled in ThisBuild := false

lazy val `weather-service` = (project in file("."))
  .aggregate(`weather-service-api`, `weather-service-impl`, `owm-adapter-api`, `owm-adapter-impl`)

lazy val `weather-service-api` = (project in file("weather-service-api"))
  .settings(
    common,
    libraryDependencies ++= commonApiDeps
  )

lazy val `weather-service-impl` = (project in file("weather-service-impl"))
  .enablePlugins(LagomJava)
  .settings(
    common,
    libraryDependencies ++= commonImplDeps,
    testOptions += Tests.Argument(jupiterTestFramework, "-a", "-v")
  )
  .settings(lagomForkedTestSettings: _*)
  .dependsOn(`weather-service-api`, `owm-adapter-api`)

lazy val `owm-adapter-api` = (project in file("owm-adapter-api"))
  .settings(
    common,
    libraryDependencies ++= commonApiDeps
  )

lazy val `owm-adapter-impl` = (project in file("owm-adapter-impl"))
  .enablePlugins(LagomJava)
  .settings(
    common,
    libraryDependencies ++= commonImplDeps,
    testOptions += Tests.Argument(jupiterTestFramework, "-a", "-v")
  )
  .settings(lagomForkedTestSettings: _*)
  .dependsOn(`owm-adapter-api`)

val hamcrest = "org.hamcrest" % "hamcrest-library" % "1.3" % Test
val h2 = "com.h2database" % "h2" % "1.4.197" % Test
val junit5 = "org.junit" % "junit-bom" % "5.2.0" % Test
val lombok = "org.projectlombok" % "lombok" % "1.16.22"
val mockito = "org.mockito" % "mockito-core" % "2.18.3" % Test

lazy val commonApiDeps = Seq(
  lagomJavadslApi,
  lagomJavadslJackson,
  lagomJavadslPersistenceCassandra,
  lombok
)

lazy val commonImplDeps = Seq(
  lagomJavadslPersistenceCassandra,
  lagomJavadslTestKit,
  lagomLogback,
  hamcrest,
  h2,
  junit5,
  lombok,
  mockito
)

def common = Seq(
  javacOptions in compile += "-parameters"
)
