import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
	kotlin("jvm") version "1.9.23"
	kotlin("plugin.noarg") version "1.9.23"

	`java-library`

	id("com.github.johnrengelman.shadow") version "8.1.1"

	// auto update dependencies with 'useLatestVersions' task
	id("se.patrikerdes.use-latest-versions") version "0.2.18"
	id("com.github.ben-manes.versions") version "0.50.0"
}

repositories {
	mavenCentral()
	maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots/")
	google()
}

dependencies {
	compileOnly(files("jadx.jar"))

	implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
	implementation("com.google.code.gson:gson:2.10.1")

	testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	testImplementation(files("jadx.jar"))

	testImplementation("org.slf4j:slf4j-simple:2.0.11")
	testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
}

java {
	sourceCompatibility = JavaVersion.VERSION_11
	targetCompatibility = JavaVersion.VERSION_11
}

noArg {
	annotation("uk.oshawk.jadx.collaboration.NoArgs")
	invokeInitializers = true
}

version = System.getenv("VERSION") ?: "dev"

tasks {
	withType(Test::class) {
		useJUnitPlatform()
	}
	val shadowJar = withType(ShadowJar::class) {
		archiveClassifier.set("") // remove '-all' suffix
	}

	// copy result jar into "build/dist" directory
	register<Copy>("dist") {
		dependsOn(shadowJar)
		dependsOn(withType(Jar::class))

		from(shadowJar)
		into(layout.buildDirectory.dir("dist"))
	}
}
