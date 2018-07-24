resolvers += Resolver.jcenterRepo

addSbtPlugin("com.lightbend.lagom" % "lagom-sbt-plugin" % "1.4.6")
addSbtPlugin("net.aichler" % "sbt-jupiter-interface" % "0.7.0")

// For deploying with Lightbend ConductR
addSbtPlugin("com.lightbend.conductr" % "sbt-conductr" % "2.7.2")
