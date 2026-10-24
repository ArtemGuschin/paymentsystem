plugins {
    `java-library`
    `maven-publish`
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
}

group = "com.artem"
version = "1.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    api("org.apache.avro:avro:1.11.4")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("commonLibrary") {
            from(components["java"])

            groupId = "com.artem"
            artifactId = "common-library"
            version = project.version.toString()
        }
    }

    repositories {
        maven {
            name = "nexus"

            val releasesRepoUrl =
                uri("http://localhost:8081/repository/maven-releases/")
            val snapshotsRepoUrl =
                uri("http://localhost:8081/repository/maven-snapshots/")

            url = if (version.toString().endsWith("SNAPSHOT")) {
                snapshotsRepoUrl
            } else {
                releasesRepoUrl
            }

            isAllowInsecureProtocol = true

            credentials {
                username = "admin"
                password = "admin123"
            }
        }
    }
}