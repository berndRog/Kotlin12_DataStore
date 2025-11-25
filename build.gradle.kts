plugins {
   kotlin("jvm") version "2.2.21"
   kotlin("plugin.serialization") version "2.2.21"
}

group = "de.rogallab.mobile"
version = "1.0-SNAPSHOT"

repositories {
   mavenCentral()
}

dependencies {
   implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
   implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
   implementation("io.insert-koin:koin-core:4.1.1" )

   testImplementation("junit:junit:4.13.2")
   testImplementation(kotlin("test"))  // includes kotlin-test-junit for JUnit4
   testImplementation("app.cash.turbine:turbine:1.2.1")
   testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
   testImplementation("io.insert-koin:koin-test:4.1.1")

}

kotlin {
   jvmToolchain(21)
}