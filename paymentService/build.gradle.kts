plugins {
    java
    id("org.springframework.boot") version "4.0.0-M1"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.openapi.generator") version "7.23.0"
    id("org.flywaydb.flyway") version "11.3.4"
    id("maven-publish")
}

group = "com.artem"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }      // обязательно для 4.0.0-M1
    maven { url = uri("https://repo.spring.io/snapshot") }      // для снапшотов (опционально)
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

val jacksonDatabindNullableVersion: String by project
val swaggerAnnotationsVersion: String by project
val mapstructVersion: String by project
val wiremockStandaloneVersion: String by project
val lombokMapstructBindingVersion: String by project

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
//    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.flywaydb:flyway-core")
    implementation("io.swagger.core.v3:swagger-annotations:${swaggerAnnotationsVersion}")
    implementation("org.openapitools:jackson-databind-nullable:${jacksonDatabindNullableVersion}")
    implementation("org.mapstruct:mapstruct:${mapstructVersion}")
    annotationProcessor("org.mapstruct:mapstruct-processor:${mapstructVersion}")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("org.springframework:spring-webflux") // явно, чтобы избежать проблем

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:${lombokMapstructBindingVersion}")
    runtimeOnly("org.postgresql:postgresql")

    // === ТЕСТОВЫЕ ЗАВИСИМОСТИ (исправляем ошибку с AutoConfigureMockMvc) ===
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // Явно добавляем автоконфигурации для тестов
    testImplementation("org.springframework.boot:spring-boot-test-autoconfigure")
    testImplementation("org.springframework:spring-test")
    testImplementation("org.springframework.boot:spring-boot-test")

    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.wiremock:wiremock-standalone:${wiremockStandaloneVersion}")
    testImplementation("org.testcontainers:testcontainers:2.0.2")
    testImplementation("org.testcontainers:postgresql:1.20.6")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

openApiGenerate {

    generatorName.set("spring")

    inputSpec.set(file("src/main/resources/api/openapi.yaml").absolutePath)

    outputDir.set(layout.buildDirectory.dir("generated").get().asFile.absolutePath)

    apiPackage.set("com.artem.paymentservice.api")
    modelPackage.set("com.artem.paymentservice.dto")

    generateApiTests.set(false)
    generateModelTests.set(false)

    configOptions.set(
        mapOf(
            "interfaceOnly" to "true",
            "useSpringBoot4" to "true",
            "useJakartaEe" to "true",
            "useTags" to "true",
            "dateLibrary" to "java8",
            "openApiNullable" to "false"
        )
    )
}
tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generateFakeProviderClient") {

    generatorName.set("java")

    library.set("restclient")

    inputSpec.set(
        file("src/main/resources/fake-provider/openapi.yaml").absolutePath
    )

    outputDir.set(
        layout.buildDirectory.dir("generated-fakeprovider").get().asFile.absolutePath
    )

    apiPackage.set("com.artem.fakepaymentprovider.client.api")

    modelPackage.set("com.artem.fakepaymentprovider.client.dto")

    invokerPackage.set("com.artem.fakepaymentprovider.client")

    generateApiTests.set(false)
    generateModelTests.set(false)

    configOptions.set(
        mapOf(
            "dateLibrary" to "java8",
            "serializationLibrary" to "jackson",
            "useJakartaEe" to "true",
            "openApiNullable" to "false"
        )
    )
}


////////////////////////////////////////////////////////

sourceSets["main"].java {
    srcDir(layout.buildDirectory.dir("generated/src/main/java"))
    srcDir(layout.buildDirectory.dir("generated-fakeprovider/src/main/java"))
}

tasks.compileJava {
    dependsOn(tasks.openApiGenerate)
    dependsOn("generateFakeProviderClient")
}

tasks.register<Jar>("clientJar") {
    archiveClassifier.set("")
    from(sourceSets.main.get().output) {
        include("com/artem/paymentservice/**")
    }
    dependsOn(tasks.compileJava)
}

tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from("src/main/java")
    from(layout.buildDirectory.dir("generated/src/main/java"))
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
            artifactId = "payment-service-client"
            version = project.version.toString()
        }
    }
    repositories {
        maven {
            name = "nexus"
            val releasesRepoUrl = uri("http://localhost:8081/repository/maven-releases/")
            val snapshotsRepoUrl = uri("http://localhost:8081/repository/maven-snapshots/")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
            isAllowInsecureProtocol = true
            credentials {
                username = "admin"
                password = "admin123"
            }
        }
    }
}