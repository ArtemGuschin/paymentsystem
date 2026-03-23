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

extra["springCloudVersion"] = "2023.0.1"

dependencies {
    val lombokVersion = "1.18.32"
    val mapstructVersion = "1.5.5.Final"

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")

    implementation("org.flywaydb:flyway-core")
    implementation("io.swagger.core.v3:swagger-annotations:2.2.20")
    implementation("org.openapitools:jackson-databind-nullable:0.2.6")

    implementation("net.javacrumbs.shedlock:shedlock-spring:5.13.0")
    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:5.13.0")

    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml")

    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")

    implementation("org.mapstruct:mapstruct:$mapstructVersion")
    annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.wiremock.integrations.testcontainers:wiremock-testcontainers-module:1.0-alpha-15")
    testImplementation("com.github.tomakehurst:wiremock-jre8:2.35.0")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

openApiGenerate {
    generatorName.set("spring")
    inputSpec.set("$projectDir/src/main/resources/api/openapi.yaml")
    outputDir.set(layout.buildDirectory.dir("generated").get().asFile.absolutePath)

    apiPackage.set("com.artem.currencyrateservice.api")
    modelPackage.set("com.artem.currencyrateservice.dto")

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

sourceSets {
    main {
        java {
            srcDir(layout.buildDirectory.dir("generated/src/main/java"))
        }
    }
}

tasks.compileJava {
    dependsOn(tasks.openApiGenerate)
}

//
// 🔥 CLIENT JAR (ТОЛЬКО API + DTO, ОСНОВНОЙ АРТЕФАКТ)
//
tasks.register<Jar>("clientJar") {
    archiveClassifier.set("") // ❗ важно: без classifier

    from(sourceSets.main.get().output) {
        include("com/artem/currencyrateservice/api/**")
        include("com/artem/currencyrateservice/dto/**")
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

//
// ❗ отключаем обычный jar (чтобы не публиковался весь проект)
//
tasks.named<Jar>("jar") {
    enabled = false
}

//
// 🔥 PUBLISHING (ТОЛЬКО CLIENT)
//
publishing {
    publications {
        create<MavenPublication>("currencyClient") {

            // ❗ публикуем только clientJar
            artifact(tasks.named("clientJar"))
            artifact(tasks.named("sourcesJar"))

            groupId = project.group.toString()
            artifactId = "currency-rate-client"
            version = project.version.toString()

            pom {
                name.set("Currency Rate Client")
                description.set("OpenAPI client for currency-rate-service")
            }
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