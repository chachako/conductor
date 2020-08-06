@file:Suppress("SpellCheckingInspection")

import de.fayard.versions.bootstrapRefreshVersions

buildscript {
    // 从 versions.properties 文件中查找最新依赖
    val versions = File(rootDir, "versions.properties").readLines()
        .filter { it.contains("=") && !it.startsWith("#") }
        .map { it.substringBeforeLast("=") to it.substringAfterLast("=") }
        .toMap()

    fun dep(group: String, artifact: String, versionKey: String? = null) =
        "$group:$artifact:" + versions[versionKey ?: "version.$group..$artifact"]

    repositories {
        gradlePluginPortal()
        jcenter()
        google()
        maven("https://dl.bintray.com/kotlin/kotlin-eap/")
    }
    listOf(
        "de.fayard:refreshVersions:0.9.4",
        "com.vanniktech:gradle-maven-publish-plugin:0.12.0",
        "com.android.tools.build:gradle:4.2.0-alpha07",
        dep("org.jetbrains.kotlin", "kotlin-gradle-plugin", "version.org.jetbrains.kotlin..kotlin-stdlib-jdk8")
    ).forEach { dependencies.classpath(it) }
}

bootstrapRefreshVersions()

include(":conductor")
