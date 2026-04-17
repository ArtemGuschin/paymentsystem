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
val jacksonDatabindNullableVersion: String by project
val swaggerAnnotationsVersion: String by project
val mapstructVersion: String by project
val wiremockStandaloneVersion: String by project
val lombokMapstructBindingVersion: String by project

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.flywaydb:flyway-core")
    implementation("io.swagger.core.v3:swagger-annotations:${swaggerAnnotationsVersion}")
    implementation("org.openapitools:jackson-databind-nullable:${jacksonDatabindNullableVersion}")
    implementation("org.mapstruct:mapstruct:${mapstructVersion}")
    annotationProcessor("org.mapstruct:mapstruct-processor:${mapstructVersion}")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml")
    implementation("org.hibernate.orm:hibernate-core")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:${lombokMapstructBindingVersion}")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.wiremock:wiremock-standalone:${wiremockStandaloneVersion}")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

openApiGenerate {
    generatorName.set("spring")

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

sourceSets["main"].java {
    srcDir(layout.buildDirectory.dir("generated/src/main/java"))
}

tasks.compileJava {
    dependsOn(tasks.openApiGenerate)
}


tasks.register<Jar>("clientJar") {
    archiveClassifier.set("")

    from(sourceSets.main.get().output) {
        include("com/artem/fakepaymentprovider/api/**")
        include("com/artem/fakepaymentprovider/dto/**")
    }

    dependsOn(tasks.compileJava)
}


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