plugins {
    java
    id("org.springframework.boot") version "3.2.5"
    id("io.spring.dependency-management") version "1.1.4"
    id("org.openapi.generator") version "7.5.0"
    id("org.flywaydb.flyway") version "11.3.4"
    id("maven-publish")
}

group = "com.artem"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")

    // ✅ FIX
    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("org.flywaydb:flyway-core")

    implementation("io.swagger.core.v3:swagger-annotations:2.2.20")
    implementation("org.openapitools:jackson-databind-nullable:0.2.6")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    runtimeOnly("org.postgresql:postgresql")

    // ✅ FIX ТЕСТЫ
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")

    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

openApiGenerate {
    generatorName.set("spring")

    // ✅ FIX
    inputSpec.set(file("src/main/resources/api/openapi.yaml").absolutePath)

    outputDir.set(layout.buildDirectory.dir("generated").get().asFile.absolutePath)

    apiPackage.set("com.artem.fakepaymentprovider.api")
    modelPackage.set("com.artem.fakepaymentprovider.dto")

    generateApiTests.set(false)
    generateModelTests.set(false)

    configOptions.set(
        mapOf(
            "interfaceOnly" to "true",
            "useSpringBoot3" to "true",
            "useTags" to "true",
            "dateLibrary" to "java8",
            "serializationLibrary" to "jackson",
            "openApiNullable" to "false"
        )
    )
}

// ✅ FIX sourceSets
sourceSets["main"].java {
    srcDir(layout.buildDirectory.dir("generated/src/main/java"))
}

// чтобы генерация шла до компиляции
tasks.compileJava {
    dependsOn(tasks.openApiGenerate)
}

//
// 🔥 CLIENT JAR
//
tasks.register<Jar>("clientJar") {
    archiveClassifier.set("")

    from(sourceSets.main.get().output) {
        // ✅ FIX ПАКЕТЫ
        include("com/artem/fakepaymentprovider/api/**")
        include("com/artem/fakepaymentprovider/dto/**")
    }

    dependsOn(tasks.compileJava)
}

//
// 🔥 SOURCES JAR
//
tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
    dependsOn(tasks.openApiGenerate)
}

tasks.named<Jar>("jar") {
    enabled = false
}

publishing {
    publications {
        create<MavenPublication>("client") {

            artifact(tasks.named("clientJar"))
            artifact(tasks.named("sourcesJar"))

            groupId = project.group.toString()

            // ⚠️ можно оставить, но лучше переименовать
            artifactId = "fake-payment-client"

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

            url = if (version.toString().endsWith("SNAPSHOT"))
                snapshotsRepoUrl else releasesRepoUrl

            isAllowInsecureProtocol = true

            credentials {
                username = "admin"
                password = "admin123"
            }
        }
    }
}