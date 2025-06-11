plugins {
    `java-library`
    id("io.papermc.paperweight.userdev")
}

repositories {
    maven(url = "https://central.sonatype.com/repository/maven-snapshots/") // MechanicsCore Snapshots
}

dependencies {
    compileOnly(project(":weaponmechanics-core"))

    compileOnly(libs.mechanicsCore)
    compileOnly(libs.adventureApi)
    compileOnly(libs.foliaScheduler)

    paperweight.paperDevBundle("1.21.4-R0.1-SNAPSHOT")
}
