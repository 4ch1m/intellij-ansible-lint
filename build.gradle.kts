import org.jetbrains.changelog.Changelog
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

fun property(key: String) = providers.gradleProperty(key).get()
fun env(key: String) = providers.environmentVariable(key).get()

group = property("pluginGroup")
version = property("pluginVersion")

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.0.21"
    id("org.jetbrains.intellij.platform") version "2.1.0"
    id("org.jetbrains.changelog") version "2.2.1"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21"
    id("org.jsonschema2pojo") version "1.2.2"
    id("com.github.ben-manes.versions") version "0.51.0"
}

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        create(
            providers.gradleProperty("platformType"),
            providers.gradleProperty("platformVersion")
        )

        instrumentationTools()
        pluginVerifier()
        zipSigner()
        testFramework(TestFrameworkType.Platform)
    }

    implementation("com.charleskorn.kaml:kaml-jvm:0.61.0")
    implementation("io.github.z4kn4fein:semver-jvm:2.0.0")

    testImplementation(kotlin("test"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testImplementation("io.mockk:mockk:1.13.13")

    testRuntimeOnly("junit:junit:4.13.2") // see: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-faq.html#junit5-test-framework-refers-to-junit4
}

sourceSets {
    main {
        java.srcDir("src/main/kotlin")
        java.srcDir("src/generated/java")
    }
}

intellijPlatform {
    pluginConfiguration {
        name = property("pluginName")
        version = property("pluginVersion")
        description = property("pluginDescription")
        changeNotes = provider {
            changelog.renderItem(
                changelog.getLatest(),
                Changelog.OutputType.HTML
            )
        }

        ideaVersion {
            sinceBuild = property("pluginSinceBuild")
            untilBuild = provider { null }
        }
    }

    signing {
        if (listOf(
                "JB_PLUGIN_SIGN_CERTIFICATE_CHAIN",
                "JB_PLUGIN_SIGN_PRIVATE_KEY",
                "JB_PLUGIN_SIGN_PRIVATE_KEY_PASSWORD").all { System.getenv(it) != null }) {
            certificateChainFile = file(env("JB_PLUGIN_SIGN_CERTIFICATE_CHAIN"))
            privateKeyFile = file(env("JB_PLUGIN_SIGN_PRIVATE_KEY"))
            password = file(env("JB_PLUGIN_SIGN_PRIVATE_KEY_PASSWORD")).readText()
        }
    }

    publishing {
        if (System.getenv("JB_PLUGIN_PUBLISH_TOKEN") != null) {
            token = file(env("JB_PLUGIN_PUBLISH_TOKEN")).readText().trim()
        }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

changelog {
    version = property("pluginVersion")
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
    jvmToolchain(property("kotlinJvmTarget").toInt())
}

tasks {
    property("javaVersion").let {
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
}
