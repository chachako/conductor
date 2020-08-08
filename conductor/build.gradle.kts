@file:Suppress("UsePropertyAccessSyntax", "SpellCheckingInspection")

import extensions.*
import dependencies.local.*
import com.vanniktech.maven.publish.custom.tasks.*

plugins {
  `android-library`; `kotlin-android`; `maven-publish`
  id("org.jlleitschuh.gradle.ktlint")
  id("com.jfrog.bintray")
}

android {
  setupWithConfig(this) {
    versionCode = 1
    versionName = "1.3"
    consumerProguardFile("proguard-rules.txt")
  }
}

dependencies {
  implementationOf(
    /** Kotlin Sdk */
    Kotlin.stdlib.jdk8,
    /** Kotlin 协程 */
    KotlinX.coroutines.core,
    KotlinX.coroutines.android,
    /** AndroidX Sdk */
    AndroidX.savedState,
    AndroidX.lifecycle.runtime,
    AndroidX.lifecycle.runtimeKtx,
    AndroidX.lifecycle.viewModelKtx,
    AndroidX.lifecycle.liveDataKtx,
    AndroidX.lifecycle.liveDataCoreKtx
  )
  compileOnlyOf(Libraries.tools.android)
  testImplementationOf(Testing.junit4, Testing.roboElectric)
}

ktlint {
  android.set(true)
  outputColorName.set("RED")
  enableExperimentalRules.set(true)
}

afterEvaluate {
  publishing {
    publications {
      create<MavenPublication>("maven") {
        groupId = "com.mars.library"
        artifactId = project.name
        version = android.defaultConfig.versionName
        from(project.components["release"])
        artifact(project.tasks.register("androidSourcesJar", AndroidSourcesJar::class.java).get())
      }
    }
  }
  bintray {
    user = "oh-rin"
    key = localProperties.getProperty("bintray.api.key")
    publish = true
    pkg.apply {
      repo = "Mars"
      name = project.name
      version.name = android.defaultConfig.versionName
      websiteUrl = "https://github.com/oh-Rin"
      issueTrackerUrl = "https://github.com/MarsPlanning/conductor/issues"
      vcsUrl = "https://github.com/MarsPlanning/conductor.git"
      desc = "一个更加契合 mars 项目或 kotlin 的 https://github.com/bluelinelabs/Conductor 分支"
      setLicenses("Apache-2.0")
    }
    setPublications("maven")
  }
}