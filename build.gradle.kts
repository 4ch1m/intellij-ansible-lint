import org.jetbrains.changelog.Changelog

fun properties(key: String) = project.findProperty(key).toString()

version = properties("pluginVersion")
description = properties("pluginDescription")

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.0.0"
    id("org.jetbrains.intellij") version "1.17.4"
    id("org.jetbrains.changelog") version "2.2.1"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0"
    id("org.jsonschema2pojo") version "1.2.1"
    id("com.github.ben-manes.versions") version "0.51.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.charleskorn.kaml:kaml-jvm:0.60.0")
    implementation("io.github.z4kn4fein:semver-jvm:2.0.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
}

sourceSets {
    main {
        java.srcDir("src/main/kotlin")
        java.srcDir("src/generated/java")
    }
}

intellij {
    pluginName.set(properties("pluginName"))
    version.set(properties("platformVersion"))
    updateSinceUntilBuild.set(false)
}

changelog {
    version.set(properties("pluginVersion"))
}

jsonSchema2Pojo {
    // JSON schema for "Static Analysis Results Interchange Format (SARIF) Version 2.1.0"; check:
    //  https://github.com/ansible/ansible-lint/blob/e4b9e555b5aa9d70ea1270aac9d8282850d5719a/src/ansiblelint/formatters/__init__.py#L203-L205

    sourceFiles = files("${projectDir}/src/main/resources/json/sarif.json")
    targetDirectory = file("${projectDir}/src/generated/java")
    targetPackage = "de.achimonline.ansible_lint.parser.sarif"
    removeOldOutput = true
}

kotlin {
    jvmToolchain(properties("kotlinJvmTarget").toInt())
}

tasks {
    properties("javaVersion").let {
        withType<JavaCompile> {
            sourceCompatibility = it
            targetCompatibility = it
        }
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        dependsOn(generateJsonSchema2Pojo)
    }

    dependencyUpdates {
        rejectVersionIf {
            (
                listOf("RELEASE", "FINAL", "GA").any { candidate.version.uppercase().contains(it) }
                ||
                "^[0-9,.v-]+(-r)?$".toRegex().matches(candidate.version)
            ).not()
        }
    }

    patchPluginXml {
        pluginDescription.set(properties("pluginDescription"))
        version.set(properties("pluginVersion"))
        sinceBuild.set(properties("pluginSinceBuild"))
        changeNotes.set(provider {
            changelog.renderItem(
                changelog.getLatest(),
                Changelog.OutputType.HTML
            )
        })
    }

    publishPlugin {
        if (project.hasProperty("JB_PLUGIN_PUBLISH_TOKEN")) {
            token.set(project.property("JB_PLUGIN_PUBLISH_TOKEN").toString())
        }
    }
}
