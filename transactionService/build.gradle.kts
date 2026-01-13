plugins {
    java
    id("org.springframework.boot") version "3.2.5"
    id("io.spring.dependency-management") version "1.1.3"
    id("org.openapi.generator") version "7.5.0"
    id("org.flywaydb.flyway") version "11.3.4"
    id("maven-publish")
}
dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2023.0.3")
    }
}

group = "com.artem"
version = "0.0.1-SNAPSHOT"
description = "transactionService"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
    mavenLocal()
}
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            groupId = project.group.toString()
            artifactId = "transaction-service"
            version = project.version.toString()
        }
    }

    repositories {
        maven {
            name = "nexus"

            val releasesRepoUrl = uri("http://localhost:8081/repository/maven-releases/")
            val snapshotsRepoUrl = uri("http://localhost:8081/repository/maven-snapshots/")

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
    tasks.bootJar {
        enabled = false
    }

    tasks.jar {
        enabled = true
    }

}



openApiGenerate {
    generatorName.set("spring")
    inputSpec.set("$projectDir/src/main/resources/api/openapi.yaml")
    outputDir.set("$buildDir/generated")
    apiPackage.set("com.artem.transaction.api")
    modelPackage.set("com.artem.transaction.model")
    configOptions.set(
        mapOf(
            "interfaceOnly" to "true",
            "useSpringBoot3" to "true",
            "useTags" to "true",
            "dateLibrary" to "java8",
            "serializableModel" to "true",
            "serializationLibrary" to "jackson",
            "useSpringController" to "true",
            "generateApiTests" to "false",
            "generateModelTests" to "false"
        )
    )
    globalProperties.set(
        mapOf(
            "apis" to "",
            "models" to "",
            "supportingFiles" to "ApiUtil.java"
        )
    )
}


sourceSets {
    main {
        java {
            srcDir("$buildDir/generated/src/main/java")
        }
    }
}


tasks.withType<JavaCompile> {
    dependsOn(tasks.openApiGenerate)
}


dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("com.fasterxml.jackson.module:jackson-module-parameter-names")

    implementation("io.swagger.core.v3:swagger-annotations:2.2.20")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("org.openapitools:jackson-databind-nullable:0.2.6")

    implementation("ch.qos.logback.contrib:logback-json-classic:0.1.5")

    implementation("org.apache.shardingsphere:shardingsphere-jdbc:5.5.2")
    implementation("com.zaxxer:HikariCP:5.0.1")


    implementation("org.flywaydb:flyway-core:10.20.1")
    implementation("org.flywaydb:flyway-database-postgresql:10.20.1")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")

    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    implementation("org.mapstruct:mapstruct:1.6.3")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")
    implementation("com.fasterxml.jackson.core:jackson-core:2.18.0")


    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:kafka")
    testImplementation("org.testcontainers:postgresql")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}



tasks.withType<Test> {
    useJUnitPlatform()
}