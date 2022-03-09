buildscript {
    repositories {
        mavenCentral()
        google()
    }
}

allprojects {
    group = "dev.rikka.tools.autoresconfig"
    version = "1.1.0"
}

task("clean", type = Delete::class) {
    delete(buildDir)
}

subprojects {
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    afterEvaluate {
        println("- Add publishing to module '${this.name}'")

        val artifactName: String = when (this.name) {
            "gradle-plugin" -> "$group.gradle.plugin"
            else -> this.name
        }

        val sourceSets = extensions.getByType(SourceSetContainer::class.java)

        val sourcesJar = tasks.register("sourcesJar", type = Jar::class) {
            archiveClassifier.set("sources")
            from(sourceSets.named("main").get().allSource)
        }
        val javadocJar = tasks.register("javadocJar", type = Jar::class) {
            archiveClassifier.set("javadoc")
            from(tasks["javadoc"])
        }

        val publishing = extensions.getByType(PublishingExtension::class.java).apply {
            publications {
                create("maven", type = MavenPublication::class) {
                    this.artifactId = artifactName
                    this.version = project.version.toString()

                    from(components["java"])

                    artifact(javadocJar)
                    artifact(sourcesJar)

                    pom {
                        name.set("AutoResConfig")
                        description.set("AutoResConfig")
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
                mavenLocal()
                maven {
                    name = "ossrh"
                    url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2")
                    credentials(PasswordCredentials::class.java)
                }
            }
        }

        extensions.configure(SigningExtension::class) {
            val signingKey = findProperty("signingKey") as? String
            val signingPassword = findProperty("signingPassword") as? String
            val secretKeyRingFile = findProperty("signing.secretKeyRingFile") as? String

            if ((secretKeyRingFile != null && file(secretKeyRingFile).exists()) || signingKey != null) {
                if (signingKey != null) {
                    useInMemoryPgpKeys(signingKey, signingPassword)
                }

                val task = sign(publishing.publications)

                tasks["publishMavenPublicationToOssrhRepository"].dependsOn(task)
            }
        }
    }
}
