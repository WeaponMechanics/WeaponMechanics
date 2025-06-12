plugins {
    `java-library`
    id("com.gradleup.shadow") version "8.3.5"
    id("xyz.jpenilla.resource-factory-bukkit-convention") version "1.3.0"
}

dependencies {
    // Main project code
    implementation(project(":weaponmechanics-core"))

    // Platform modules
    file("../weaponmechanics-platforms/paper").listFiles()?.forEach {
        implementation(project(":${it.name}", "reobf"))
    }
}

bukkitPluginYaml {
    val versionProperty = findProperty("weaponmechanics.version") as? String
        ?: throw IllegalArgumentException("weaponmechanics.version was null")

    main = "me.deecaad.weaponmechanics.WeaponMechanics"
    name = "WeaponMechanics"
    version = versionProperty
    apiVersion = "1.13"  // Use 1.13, since apiVersion was added in 1.13
    foliaSupported = true

    authors = listOf("DeeCaaD", "CJCrafter")
    depend = listOf("MechanicsCore", "packetevents")
    softDepend = listOf("WorldEdit", "WorldGuard", "PlaceholderAPI", "MythicMobs", "Geyser-Spigot")
}

tasks.shadowJar {
    val versionProperty = findProperty("weaponmechanics.version") as? String
        ?: throw IllegalArgumentException("weaponmechanics.version was null")
    archiveFileName.set("WeaponMechanics-$versionProperty.jar")

    val libPackage = "me.deecaad.core.lib"

    // the kotlin plugin adds kotlin-stdlib to the classpath, but we don't want it in the shadow jar
    exclude("org/jetbrains/kotlin/**")

    relocate("org.slf4j", "$libPackage.slf4j")
    relocate("org.bstats", "$libPackage.bstats")
    relocate("net.kyori", "$libPackage.kyori")
    relocate("com.jeff_media.updatechecker", "$libPackage.updatechecker")
    relocate("dev.jorel.commandapi", "$libPackage.commandapi")
    relocate("com.cjcrafter.foliascheduler", "$libPackage.scheduler")
    relocate("com.zaxxer.hikari", "$libPackage.hikari")
    relocate("kotlin.", "$libPackage.kotlin.")
    relocate("com.cryptomorin.xseries", "$libPackage.xseries")
}