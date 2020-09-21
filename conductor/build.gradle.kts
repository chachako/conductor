@file:Suppress("SpellCheckingInspection")

plugins {
  `android-library`; `kotlin-android`
  id("org.jlleitschuh.gradle.ktlint")
}

android {
  setupAndroidWithShares()
  defaultConfig {
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
  testImplementationOf(Testing.junit4, Testing.roboElectric)
}

ktlint {
  android.set(true)
  outputColorName.set("RED")
  enableExperimentalRules.set(true)
}

publishToBintray(artifact = project.name)