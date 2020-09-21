@file:Suppress(
  "UNCHECKED_CAST", "GradleDynamicVersion", "UnstableApiUsage",
  "SpellCheckingInspection", "SafeCastWithReturn",
  "NestedLambdaShadowedImplicitParameter"
)

buildscript {
  // parse versions.properties file and collect to Map<String, String>
  val versions = rootDir.resolve("versions.properties").readLines()
    .filter { it.contains("=") && !it.startsWith("#") }
    .map { it.substringBeforeLast("=") to it.substringAfterLast("=") }
    .toMap()

  // find newest dependency from the versions.properties file
  fun dep(group: String, artifact: String, versionKey: String? = null) =
    "$group:$artifact:" + versions[versionKey ?: "version.$group..$artifact"]

  repositories {
    gradlePluginPortal()
    jcenter()
    google()
    maven("https://dl.bintray.com/oh-rin/Mars/")
    maven("https://dl.bintray.com/kotlin/kotlin-eap/")
  }

  listOf(
    "com.mars.gradle.plugin:toolkit:0.6.7",
    "org.jlleitschuh.gradle:ktlint-gradle:9.1.1",
    "com.jfrog.bintray.gradle:gradle-bintray-plugin:1.8.5",
    "de.fayard.refreshVersions:refreshVersions:0.9.5",
    dep("org.jetbrains.kotlin", "kotlin-gradle-plugin"),
    dep("com.android.tools.build", "gradle")
  ).forEach { dependencies.classpath(it) }
}

setupToolkit {
  kotlinOptions {
    useIR = true
    jvmTarget = "1.8"
    apiVersion = "1.4"
    languageVersion = "1.4"
  }
  shareAndroidConfig {
    defaultConfig {
      versionCode = 1
      versionName = "1.3"
    }
  }
}

include(":conductor")
