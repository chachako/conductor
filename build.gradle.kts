@file:Suppress("SpellCheckingInspection", "UnstableApiUsage")

import extensions.*

buildscript {
    repositories {
        // 本地 Gradle 插件
        maven("${System.getenv("MARS_PROJECT_ROOT")}/internal/.release/")
        // 网络 Maven 仓库
        jcenter()
        google()
        mavenCentral()
    }
    dependencies.classpath("com.mars.gradle.plugin:global:1.0")
}

plugins {
    id("com.jfrog.bintray") version "1.8.5"
    id("org.jlleitschuh.gradle.ktlint") version "9.1.1"
}

allprojects {
    setupRepositories()
    configInject()
}

subprojects {
    configInject()
}

task<Delete>("clean") {
    delete(rootProject.buildDir)
}