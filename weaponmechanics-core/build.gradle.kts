import org.jreleaser.model.Active

plugins {
    `java-library`
    kotlin("jvm") version libs.versions.kotlin
    `maven-publish`
    id("org.jreleaser") version "1.18.0"
}

dependencies {
    // Core Minecraft dependencies
    compileOnly(libs.paper)
    compileOnly(libs.mechanicsCore)
    compileOnly(libs.packetEvents)

    // External "hooks" or plugins that we might interact with
    compileOnly(libs.placeholderApi)
    compileOnly(libs.mythicMobs)
    compileOnly(files("../libs/Vivecraft-Spigot-Extension-1.3.4-0.jar"))

    // Shaded dependencies
    compileOnly(libs.annotations)
    compileOnly(libs.bstats)
    compileOnly(libs.commandApi)
    compileOnly(libs.commandApiKotlin)
    compileOnly(libs.fastUtil)
    compileOnly(libs.foliaScheduler)
    compileOnly(libs.hikariCp)
    compileOnly(libs.jsonSimple)
    compileOnly(libs.xSeries)
    compileOnly(kotlin("stdlib"))

    // Testing dependencies
    testImplementation(libs.paper)
    testImplementation(libs.annotations)
    testImplementation(libs.foliaScheduler)
}


val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.named("javadoc").map { it.outputs.files })
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(sourcesJar)
            artifact(javadocJar)

            groupId = "com.cjcrafter"
            artifactId = "weaponmechanics"
            version = findProperty("version").toString()

            pom {
                name.set("WeaponMechanics")
                description.set("A new age of weapons in Minecraft")
                url.set("https://github.com/WeaponMechanics/WeaponMechanics")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("CJCrafter")
                        name.set("Collin Barber")
                        email.set("collinjbarber@gmail.com")
                    }
                    developer {
                        id.set("DeeCaaD")
                        name.set("DeeCaaD")
                        email.set("perttu.kangas@hotmail.fi")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/WeaponMechanics/WeaponMechanics.git")
                    developerConnection.set("scm:git:ssh://github.com/WeaponMechanics/WeaponMechanics.git")
                    url.set("https://github.com/WeaponMechanics/WeaponMechanics")
                }
            }
        }
    }

    // Deploy this repository locally for staging, then let the root project actually
    // upload the maven repo using jReleaser
    repositories {
        maven {
            name = "stagingDeploy"
            url = layout.buildDirectory.dir("staging-deploy").map { it.asFile.toURI() }.get()
        }
    }
}

jreleaser {
    gitRootSearch.set(true)

    project {
        name.set("WeaponMechanics")
        group = "com.cjcrafter"
        version = findProperty("version").toString()
        description = "A new age of weapons in Minecraft"
        authors.add("CJCrafter <collinjbarber@gmail.com>")
        authors.add("DeeCaaD <perttu.kangas@hotmail.fi>")
        license = "MIT" // SPDX identifier
        copyright = "Copyright © 2023-2025 CJCrafter, DeeCaaD"

        links {
            homepage.set("https://github.com/WeaponMechanics/WeaponMechanics")
            documentation.set("https://github.com/WeaponMechanics/WeaponMechanics#readme")
        }

        java {
            groupId = "com.cjcrafter"
            artifactId = "weaponmechanics"
            version = findProperty("version").toString()
        }

        snapshot {
            fullChangelog.set(true)
        }
    }

    signing {
        active.set(Active.ALWAYS)
        armored.set(true)
    }

    deploy {
        maven {
            mavenCentral {
                create("releaseDeploy") {
                    active.set(Active.RELEASE)
                    url.set("https://central.sonatype.com/api/v1/publisher")
                    // run ./gradlew weaponmechanics-core:publish before deployment
                    stagingRepository("build/staging-deploy")
                    // Credentials (JRELEASER_MAVENCENTRAL_USERNAME, JRELEASER_MAVENCENTRAL_PASSWORD or JRELEASER_MAVENCENTRAL_TOKEN)
                    // will be picked up from ~/.jreleaser/config.toml
                    maxRetries = 200
                }
            }

            nexus2 {
                create("sonatypeSnapshots") {
                    active.set(Active.SNAPSHOT)
                    url.set("https://central.sonatype.com/repository/maven-snapshots/")
                    snapshotUrl.set("https://central.sonatype.com/repository/maven-snapshots/")
                    applyMavenCentralRules = true
                    snapshotSupported = true
                    closeRepository = true
                    releaseRepository = true
                    stagingRepository("build/staging-deploy")
                }
            }
        }
    }

    distributions {
        create("weaponmechanics") {
            active.set(Active.ALWAYS)
            distributionType.set(org.jreleaser.model.Distribution.DistributionType.SINGLE_JAR)
            artifact {
                path.set(file("../weaponmechanics-build/build/libs/WeaponMechanics-${findProperty("version")}.jar"))
            }
        }
    }

    release {
        github {
            repoOwner.set("WeaponMechanics")
            name.set("WeaponMechanics")
            host.set("github.com")

            val version = findProperty("version").toString()
            val isSnapshot = version.endsWith("-SNAPSHOT")
            releaseName.set(if (isSnapshot) "SNAPSHOT" else "v$version")
            tagName.set("v{{projectVersion}}")
            draft.set(false)
            skipTag.set(isSnapshot)
            overwrite.set(false)
            update { enabled.set(isSnapshot) }

            prerelease {
                enabled.set(isSnapshot)
                pattern.set(".*-SNAPSHOT")
            }

            commitAuthor {
                name.set("Collin Barber")
                email.set("collinjbarber@gmail.com")
            }

            changelog {
                formatted.set(Active.ALWAYS)
                preset.set("conventional-commits")
                format.set("- {{commitShortHash}} {{commitTitle}}")
                contributors {
                    enabled.set(true)
                    format.set("{{contributorUsernameAsLink}}")
                }
                hide {
                    contributors.set(listOf("[bot]"))
                }
            }
        }
    }
}
