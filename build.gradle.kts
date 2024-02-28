plugins {
	kotlin("jvm") version "1.9.0"
}

group = "com.light672"
version = "0.1"

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