import org.gradle.wrapper.WrapperExecutor

group = "com.github.oasalonen"
version = "0.4.0-SNAPSHOT"

task<Wrapper>("wrap") {
    gradleVersion = "4.9"
    distributionType = Wrapper.DistributionType.ALL
}

plugins {
    kotlin("jvm").version("1.2.60")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish") version "0.9.10"
}

val kotlinVersion: String? by extra {
    buildscript.configurations["classpath"]
            .resolvedConfiguration.firstLevelModuleDependencies
            .find { it.moduleName == "org.jetbrains.kotlin.jvm.gradle.plugin" }?.moduleVersion
}
val junitVersion = "5.2.0"
val wiremockVersion = "2.18.0"

repositories {
    jcenter()
    maven("http://packages.confluent.io/maven/")
}

dependencies {
    compileOnly(gradleApi())
    implementation("org.jetbrains.kotlin", "kotlin-stdlib", kotlinVersion)
    implementation("io.confluent", "kafka-avro-serializer", "3.2.1")
            .exclude("org.slf4j", "slf4j-log4j12")
    implementation("org.apache.avro", "avro", "1.8.2")

    testImplementation(gradleTestKit())
    testImplementation("junit", "junit", "4.12")
    testImplementation("org.assertj", "assertj-core", "3.6.2")
    testImplementation("org.mockito", "mockito-all", "1.10.19")
    testImplementation("com.github.tomakehurst", "wiremock-standalone", wiremockVersion)
}

val registryPluginName = "com.github.oasalonen.kafka-schema-registry-gradle-plugin"
gradlePlugin {
    plugins.invoke {
        "schema-registry" {
            id = registryPluginName
            implementationClass = "com.github.imflog.schema.registry.SchemaRegistryPlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/oasalonen/schema-registry-plugin"
    vcsUrl = "https://github.com/oasalonen/schema-registry-plugin.git"

    (plugins) {
        "schemaRegistryPlugin" {
            id = registryPluginName
            description = "A plugin to download, register and test schemas from a Kafka Schema Registry. Forked from com.github.ImFlog.kafka-schema-registry-gradle-plugin"
            displayName = "Kafka schema registry gradle plugin"
            tags = listOf("schema", "registry", "schema-registry", "kafka")
            version = version
        }
    }
}