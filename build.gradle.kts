plugins {
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.17" apply false
    kotlin("jvm") version libs.versions.kotlin apply false
    base
    id("org.jreleaser") version "1.18.0"
}

allprojects {
    subprojects {
        pluginManager.withPlugin("java") {
            tasks.withType<JavaCompile>().configureEach {
                options.release.set(21)
                options.encoding = Charsets.UTF_8.name()
            }
        }

        pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
                kotlinOptions {
                    jvmTarget = "21"
                }
            }
        }
    }
}

jreleaser {
    project {
        name.set("WeaponMechanics")
        group = "com.cjcrafter"
        version = findProperty("weaponmechanics.version").toString()
        description = "A new age of weapons in Minecraft"
        authors.add("CJCrafter <collinjbarber@gmail.com>")
        authors.add("DeeCaaD <perttu.kangas@hotmail.fi>")
        license = "MIT" // SPDX identifier

        java {
            groupId = "com.cjcrafter"
            artifactId = "weaponmechanics"
            version = findProperty("weaponmechanics.version").toString()
        }
    }

    signing {
        active.set(org.jreleaser.model.Active.ALWAYS)
        armored.set(true)
    }

    deploy {
        maven {
            mavenCentral {
                create("releaseDeploy") {
                    active.set(org.jreleaser.model.Active.RELEASE)
                    url.set("https://central.sonatype.com/api/v1/publisher")
                    // run ./gradlew mechanicscore-core:publish before deployment
                    stagingRepository("weaponmechanics-core/build/staging-deploy")
                    // Credentials (JRELEASER_MAVENCENTRAL_USERNAME, JRELEASER_MAVENCENTRAL_PASSWORD or JRELEASER_MAVENCENTRAL_TOKEN)
                    // will be picked up from ~/.jreleaser/config.toml
                }
            }

            nexus2 {
                create("sonatypeSnapshots") {
                    active.set(org.jreleaser.model.Active.SNAPSHOT)
                    url.set("https://central.sonatype.com/repository/maven-snapshots/")
                    snapshotUrl.set("https://central.sonatype.com/repository/maven-snapshots/")
                    applyMavenCentralRules = true
                    snapshotSupported = true
                    closeRepository = true
                    releaseRepository = true
                    stagingRepository("mechanicscore-core/build/staging-deploy")
                }
            }
        }
    }

    // TODO consider replacing github release with this
    /*
    release {
        github { // Assuming your SCM is GitHub, based on POM
            // owner.set("WeaponMechanics") // Auto-detected from SCM URL if possible
            // name.set("MechanicsCore")   // Auto-detected
            tagName.set("v{{projectVersion}}")
            // You might want to configure changelog generation here
            changelog {
                formatted.set("ALWAYS")
                preset.set("conventionalcommits")
                // format.set("- {{commitShortHash}} {{commitTitle}}") // Customize format
            }
        }
    }
     */
}
