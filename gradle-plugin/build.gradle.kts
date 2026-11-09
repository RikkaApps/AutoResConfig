plugins {
    `java-gradle-plugin`
    `maven-publish`
    signing
}

group = "dev.rikka.tools.autoresconfig"
version = "1.3.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    compileOnly(libs.android.gradle)
}

gradlePlugin {
    plugins {
        register("AutoResConfig") {
            id = group.toString()
            displayName = "AutoResConfig"
            description = "A gradle plugin generates resConfig & languages array from project res folder."
            implementationClass = "$group.AutoResConfigPlugin"
        }
    }
}

publishing {
    publications {
        withType(MavenPublication::class.java) {
            pom {
                name.convention("AutoResConfig")
                description.convention("AutoResConfig")
                url.set("https://github.com/RikkaApps/AutoResConfig")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://github.com/RikkaApps/AutoResConfig/blob/main/LICENSE")
                    }
                }
                developers {
                    developer {
                        name.set("RikkaW")
                        url.set("https://github.com/RikkaW")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/RikkaApps/AutoResConfig.git")
                    url.set("https://github.com/RikkaApps/AutoResConfig")
                }
            }
        }
    }
    repositories {
        maven {
            name = "ossrh"
            val host = "https://ossrh-staging-api.central.sonatype.com"
            url = uri("$host/service/local/staging/deploy/maven2/")
            credentials(PasswordCredentials::class.java)
        }
    }
}

signing {
    val signingKey = findProperty("signingKey") as String?
    val signingPassword = findProperty("signingPassword") as String?
    val secretKeyRingFile = findProperty("signing.secretKeyRingFile") as String?
    val gnuPgDir = File(System.getProperty("user.home") + "/.gnupg/private-keys-v1.d")

    if (secretKeyRingFile != null && file(secretKeyRingFile).exists()) {
        sign(publishing.publications)
    } else if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications)
    } else if (gnuPgDir.isDirectory && gnuPgDir.list()?.isNotEmpty() == true) {
        useGpgCmd()
        sign(publishing.publications)
    }
}
