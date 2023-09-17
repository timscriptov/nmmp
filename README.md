[![](https://jitpack.io/v/TimScriptov/nmmp.svg)](https://jitpack.io/#TimScriptov/nmmp)

## Original REPO:
https://github.com/maoabc/nmmp

# Screenshots
![Main](/ART/Screenshot.png)

## Add it in your root build.gradle at the end of repositories:
```groovy
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

## Add the dependency:
```groovy
dependencies {
    implementation("com.github.TimScriptov:nmmp:Tag")
    implementation("com.github.TimScriptov:preferences:Tag")
}
```

## Init preferences:
```kotlin
 Preferences(File("path"), "name.json").init()
```

## Environment path:
```kotlin
Prefs.setSdkPath("path") // ANDROID_SDK_HOME
Prefs.setCmakePath("path") // CMAKE_PATH
Prefs.setNdkPath("path") // ANDROID_NDK_HOME
```

## Change lib name and class name:
```kotlin
Prefs.setRegisterNativesClassName("com/nmmedit/protect/NativeUtil")
Prefs.setVmName("nmmvm")
Prefs.setNmmpName("nmmp")
```

## Protect APK:
```kotlin
val input = File("input.apk")
val output = File("output.apk")
val rules = File("rules.txt")
val simpleRules = SimpleRules().apply {
    parse(InputStreamReader(FileInputStream(rules), StandardCharsets.UTF_8))
}
val filterConfig = SimpleConvertConfig(BasicKeepConfig(), simpleRules)
ApkProtect.Builder(ApkFolders(input, output)).apply {
    setInstructionRewriter(RandomInstructionRewriter())
    setFilter(filterConfig)
    setLogger(null)
    setClassAnalyzer(ClassAnalyzer())
}.build().run()
```

## Protect AAR:
```kotlin
val input = File("input.aar")
val output = File("output.aar")
val rules = File("rules.txt")
val simpleRules = SimpleRules().apply {
    parse(InputStreamReader(FileInputStream(rules), StandardCharsets.UTF_8))
}
val filterConfig = SimpleConvertConfig(BasicKeepConfig(), simpleRules)
AarProtect.Builder(AarFolders(input, output)).apply {
    setInstructionRewriter(RandomInstructionRewriter())
    setFilter(filterConfig)
    setLogger(null)
    setClassAnalyzer(ClassAnalyzer())
}.build().run()
```

## Protect AAB:
```kotlin
val input = File("input.aab")
val output = File("output.aab")
val rules = File("rules.txt")
val simpleRules = SimpleRules().apply {
    parse(InputStreamReader(FileInputStream(rules), StandardCharsets.UTF_8))
}
val filterConfig = SimpleConvertConfig(BasicKeepConfig(), simpleRules)
AabProtect.Builder(AabFolders(input, output)).apply {
    setInstructionRewriter(RandomInstructionRewriter())
    setFilter(filterConfig)
    setLogger(null)
    setClassAnalyzer(ClassAnalyzer())
}.build().run()
```
