import org.jetbrains.changelog.Changelog

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.8.0"
    id("org.jetbrains.intellij") version "1.13.1" // TODO wait for upgrade until "1.13.3" is released ("1.13.2" has an issue, that would break unit-tests: https://github.com/JetBrains/gradle-intellij-plugin/issues/1346)
    id("org.jetbrains.changelog") version "2.0.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.4.20"
    id("org.jsonschema2pojo") version "1.2.1"
    id("com.github.ben-manes.versions") version "0.46.0"
}

repositories {
    mavenCentral()
}

jsonSchema2Pojo {
    // JSON schema for "Static Analysis Results Interchange Format (SARIF) Version 2.1.0"; check:
    //  https://github.com/ansible/ansible-lint/blob/e4b9e555b5aa9d70ea1270aac9d8282850d5719a/src/ansiblelint/formatters/__init__.py#L203-L205

    sourceFiles = files("${projectDir}/src/main/resources/json/sarif.json")
    targetDirectory = file("${projectDir}/src/generated/java")
    targetPackage = "de.achimonline.ansible_lint.parser.sarif"
    removeOldOutput = true
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

dependencies {
    implementation("com.charleskorn.kaml:kaml-jvm:0.53.0")
    implementation("io.github.z4kn4fein:semver-jvm:1.4.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
}

changelog {
    version.set(properties("pluginVersion"))
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        dependsOn(generateJsonSchema2Pojo)
        kotlinOptions.jvmTarget = properties("kotlinJvmTarget")
    }

    patchPluginXml {
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
