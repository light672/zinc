plugins {
	kotlin("jvm") version "1.9.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
	mavenCentral()
}

dependencies {
	testImplementation(kotlin("test"))
	implementation("org.apache.commons:commons-lang3:3.14.0")
}

tasks.test {
	useJUnitPlatform()
}

kotlin {
	jvmToolchain(8)
}