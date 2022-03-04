# AutoResConfig

For Android application projects that accept user-submitted translations, the number of supported languages may be large and growing. When new languages are added, developers need to manually update `resConfig` (1) and language array xml/class (2). Manual means there could be human error.

(1) `resConfig` limits the final packaged resources. Libraries may carry languages resources which the language is not supported by the application itself. Set `resConfig` could reduce apk size.

(2) If the application has a "choose language" feature, there must be an array of supported languages (could be as an Android resource XML or a Java class).

This plugin collect locales from `values-` folders, set `resConfig`, generate language array xml/class.

## Usage

![gradle-plugin](https://img.shields.io/maven-central/v/dev.rikka.tools.autoresconfig/gradle-plugin?label=gradle-plugin)

Replace all the `<version>` below with the version shows here.

1. Add gradle plugin to root project

   ```groovy
   buildscript {
       repositories {
           mavenCentral()
       }
       dependencies {
           classpath 'dev.rikka.tools.autoresconfig:gradle-plugin:<version>'
       }
   }
   ```

2. Use the plugin in Android application module

   ```groovy
   plugins {
       id('dev.rikka.tools.autoresconfig')
   }

3. Config in Android application module

   ```groovy
   autoResConfig {
       generateClass = true
       generatedClassFullName = "rikka.autoresconfig.Locales"
       generateRes = true
       generatedResPrefix = null
       generatedArrayFirstItem = "SYSTEM"
   }
   ```