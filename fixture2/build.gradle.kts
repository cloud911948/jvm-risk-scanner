plugins { alias(libs.plugins.spring.boot); java }
java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }
dependencies { implementation("org.springframework.boot:spring-boot-starter-security") }
