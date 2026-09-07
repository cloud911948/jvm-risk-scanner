plugins { id("org.springframework.boot") version "4.0.8"; java }
java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }
dependencies {
  implementation("org.springframework.security:spring-security-web:7.0.6")
  implementation("org.springframework.graphql:spring-graphql:2.0.4")
  testImplementation("org.openjdk.jol:jol-core:0.17")
}
application { applicationDefaultJvmArgs = listOf("-javaagent:/opt/apm/agent.jar", "-XX:StartFlightRecording=disk=true") }
