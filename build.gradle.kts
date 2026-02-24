tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

subprojects {
    group = "dev.rikka.tools.autoresconfig"
    version = "1.3.0"

    plugins.withId("java") {
        println("- Configuring `java`")

        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
        tasks.register("sourcesJar", type = Jar::class) {
            archiveClassifier.set("sources")
            from(project.extensions.getByType<SourceSetContainer>().getByName("main").allSource)
        }
        tasks.register("javadocJar", type = Jar::class) {
            archiveClassifier.set("javadoc")
            from(tasks["javadoc"])
        }
        tasks.withType(Javadoc::class) {
            isFailOnError = false
        }
    }
    plugins.withId("maven-publish") {
        println("- Configuring `publishing`")

        afterEvaluate {
            extensions.configure<PublishingExtension> {
                publications {
                    withType(MavenPublication::class) {
                        version = project.version.toString()
                        group = project.group.toString()

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
                        url =
                            uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2")
                        credentials(PasswordCredentials::class.java)
                    }
                }
            }
        }
        plugins.withId("signing") {
            println("- Configuring `signing`")

            afterEvaluate {
                extensions.configure<SigningExtension> {
                    if (findProperty("signing.gnupg.keyName") != null) {
                        useGpgCmd()

                        val signingTasks =
                            sign(extensions.getByType<PublishingExtension>().publications)
                        tasks.withType(AbstractPublishToMaven::class) {
                            dependsOn(signingTasks)
                        }
                    }
                }
            }
        }
    }
}
tasks.named<UpdateDaemonJvm>("updateDaemonJvm") {
    languageVersion = JavaLanguageVersion.of(21)
    vendor = JvmVendorSpec.ADOPTIUM
}
