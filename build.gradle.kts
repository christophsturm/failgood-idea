import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import java.util.Locale

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    id("com.github.ben-manes.versions") version "0.50.0"
    id("java")
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.gradleIntelliJPlugin) // Gradle IntelliJ Plugin
    alias(libs.plugins.changelog) // Gradle Changelog Plugin
    alias(libs.plugins.qodana) // Gradle Qodana Plugin
    alias(libs.plugins.kover) // Gradle Kover Plugin

    id("com.adarshr.test-logger") version "4.0.0"
    id("com.bnorm.power.kotlin-power-assert") version "0.13.0"
    id("com.ncorti.ktfmt.gradle") version "0.16.0"
}

group = properties("pluginGroup")

version = properties("pluginVersion")

// Configure project's dependencies
repositories { mavenCentral() }

@Suppress("UnstableApiUsage")
kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(17)
        vendor = JvmVendorSpec.JETBRAINS
    }
}

dependencies {
    implementation(enforcedPlatform("org.jetbrains.kotlin:kotlin-bom:1.9.21"))
    testRuntimeOnly(libs.junitJupiterEngine)
    testRuntimeOnly(libs.junitVintageEngine)
    testImplementation(libs.junitVintageEngine)
    //    compileOnly("org.jetbrains.kotlin:kotlin-compiler:1.9.10")
    testImplementation("dev.failgood:failgood:0.8.3")
}

// Configure Gradle IntelliJ Plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    pluginName.set(properties("pluginName"))
    version.set(properties("platformVersion"))
    type.set(properties("platformType"))

    // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
    plugins.set(
        properties("platformPlugins").split(',').map(String::trim).filter(String::isNotEmpty)
    )
}

// Configure Gradle Changelog Plugin - read more:
// https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.empty()
    repositoryUrl.set(properties("pluginRepositoryUrl"))
}

// Configure Gradle Qodana Plugin - read more: https://github.com/JetBrains/gradle-qodana-plugin
qodana {
    cachePath.set(projectDir.resolve(".qodana").canonicalPath)
    reportPath.set(projectDir.resolve("build/reports/inspections").canonicalPath)
    saveReport.set(true)
    showReport.set(System.getenv("QODANA_SHOW_REPORT")?.toBoolean() ?: false)
}

koverReport { defaults { xml { onCheck = true } } }

tasks {
    buildSearchableOptions { enabled = false }
    wrapper { gradleVersion = properties("gradleVersion") }

    patchPluginXml {
        version.set(properties("pluginVersion"))
        sinceBuild.set(properties("pluginSinceBuild"))
        untilBuild.set(properties("pluginUntilBuild"))

        //        val changelog = project.changelog // local variable for configuration cache
        // compatibility
        // Get the latest available change notes from the changelog file
        /*        changeNotes = properties("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }*/
    }

    // Configure UI tests plugin
    // Read more: https://github.com/JetBrains/intellij-ui-test-robot
    runIdeForUiTests {
        systemProperty("robot-server.port", "8082")
        systemProperty("ide.mac.message.dialogs.as.sheets", "false")
        systemProperty("jb.privacy.policy.text", "<!--999.999-->")
        systemProperty("jb.consents.confirmation.enabled", "false")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        dependsOn("patchChangelog")
        token.set(System.getenv("PUBLISH_TOKEN"))
        // pluginVersion is based on the SemVer (https://semver.org) and supports pre-release
        // labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel
        // automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels.set(
            listOf(
                properties("pluginVersion").split('-').getOrElse(1) { "default" }.split('.').first()
            )
        )
    }
    test {
        useJUnitPlatform()
        //        systemProperty("idea.home.path",
        // "/Users/christoph/Projects/ext/intellij-community")
        //        systemProperty("idea.force.use.core.classloader", "true")
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword =
        listOf("RELEASE", "FINAL", "GA").any { version.uppercase(Locale.getDefault()).contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks.named<DependencyUpdatesTask>("dependencyUpdates") {
    rejectVersionIf { isNonStable(candidate.version) && !isNonStable(currentVersion) }
    // optional parameters
    gradleReleaseChannel = "current"
    checkForGradleUpdate = true
    outputFormatter = "json"
    outputDir = "build/dependencyUpdates"
    reportfileName = "report"
}

tasks.getByName("classpathIndexCleanup") {
    dependsOn(tasks.getByName("compileTestKotlin"))
    dependsOn(tasks.getByName("compileKotlin"))
}

tasks.getByName("check").dependsOn(tasks.getByName("ktfmtCheck"))

ktfmt { kotlinLangStyle() }
