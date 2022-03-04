plugins {
    java
    `java-gradle-plugin`
}

val pluginId = "$group"
val pluginClass = "$group.AutoResConfigPlugin"

repositories {
    mavenCentral()
    google()
}

dependencies {
    compileOnly(gradleApi())
    compileOnly("com.android.tools.build:gradle:7.0.4")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

gradlePlugin {
    plugins {
        create("AutoResConfig") {
            id = pluginId
            implementationClass = pluginClass
        }
    }
}
