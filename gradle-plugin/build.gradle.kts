plugins {
    java
    `java-gradle-plugin`
    `maven-publish`
    signing
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(libs.android.gradle)
}

gradlePlugin {
    plugins {
        create("AutoResConfig") {
            id = project.group.toString()
            displayName = "AutoResConfig"
            description = "A gradle plugin generates resConfig & languages array from project res folder."
            implementationClass = "$id.AutoResConfigPlugin"
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            named("pluginMaven", MavenPublication::class) {
                artifactId = "gradle-plugin"

                artifact(tasks["sourcesJar"])
                artifact(tasks["javadocJar"])
            }
        }
    }

    tasks.withType(Jar::class) {
        manifest {
            attributes(mapOf("Implementation-Version" to project.version.toString()))
        }
    }
}
