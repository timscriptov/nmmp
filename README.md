[![](https://jitpack.io/v/TimScriptov/nmmp.svg)](https://jitpack.io/#TimScriptov/nmmp)

## Add it in your root build.gradle at the end of repositories:
```groovy
    allprojects {
        repositories {
            //...
            maven { url 'https://jitpack.io' }
        }
    }
```

## Add the dependency
```groovy
    dependencies {
        implementation 'com.github.TimScriptov:nmmp:Tag'
    }
```

## Convert byte-code to native
```kotlin
    Nmmp(File("path/in.apk"), File("path/out.apk"), File("path/rules.txt"), File("path/mapping.txt"), null/*ApkLogger*/).obfuscate()
```

```java
    new Nmmp(new File("path/in.apk"), new File("path/out.apk"), new File("path/rules.txt"), new File("path/mapping.txt"), null/*ApkLogger*/).obfuscate();
```