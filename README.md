# Conductor [![Travis Build](https://img.shields.io/bintray/v/oh-rin/Mars/conductor?color=f06292)](https://github.com/MarsPlanning/conductor)

对 [原 Conductor](https://github.com/bluelinelabs/Conductor) 做了部分修改以更贴合 Kotlin 和 Mars 相关项目的开发


## Installation

for build.gradle.kts (recommended)
```kotlin
repositories {
  // add Mars maven-repository
  maven(url = "https://dl.bintray.com/oh-rin/Mars")
  ....
}

dependencies {
  // add Conductor newest dependency
  implementation("com.mars.library:conductor:${LATEST_VERSION}")
  ....
}
```

<details><summary>or build.gradle</summary>

```groovy
repositories {
  // add Mars maven-repository
  maven {
    url 'https://dl.bintray.com/oh-rin/Mars'
  }
  ....
}

dependencies {
  // add Conductor newest dependency
  implementation "com.mars.library:conductor:${LATEST_VERSION}"
  ....
}
```
</details><br/>

## Changes
* migrated gradle file to kts file
* merge the [lifecycle-component](https://github.com/bluelinelabs/Conductor/tree/develop/conductor-modules/arch-components-lifecycle)
* `Controller.java` converted to `Controller.kt`
* added some like `Activity` api to `Controller`
* added a lifecycle `onInitialized()`, to support callbacks after the `constructor(...)` before `onCreateView()`
* added `ComponentController`, just like `ComponentActivity`, to support jetpack
* ...

## Lifecyle list
- constructor
- onInitialized (new)
- onCreateView
- onAttach
- onDetach
- onDestroyView
- onDestroy