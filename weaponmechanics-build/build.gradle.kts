import xyz.jpenilla.resourcefactory.paper.PaperPluginYaml

plugins {
    `java-library`
    id("com.gradleup.shadow") version "8.3.5"
    id("xyz.jpenilla.resource-factory-paper-convention") version "1.3.1"
}

dependencies {
    // Main project code
    implementation(project(":weaponmechanics-core"))

    // Platform modules
    file("../weaponmechanics-platforms/paper").listFiles()?.forEach {
        implementation(project(":${it.name}"))
    }
}

paperPluginYaml {
    val versionProperty = findProperty("version") as? String
        ?: throw IllegalArgumentException("version was null")

    main = "me.deecaad.weaponmechanics.WeaponMechanics"
    name = "WeaponMechanics"
    version = versionProperty
    apiVersion = "1.21"
    foliaSupported = true

    authors = listOf("DeeCaaD", "CJCrafter")
    dependencies {
        // Server dependencies - controls load order during plugin initialization
        server("packetevents", required = true, load = PaperPluginYaml.Load.BEFORE)
        server("MechanicsCore", required = true, load = PaperPluginYaml.Load.BEFORE)
        server("WorldEdit", required = false)
        server("WorldGuard", required = false)
        server("PlaceholderAPI", required = false)
        server("MythicMobs", required = false, load = PaperPluginYaml.Load.BEFORE)
        server("Geyser-Spigot", required = false)
        server("Vivecraft-Spigot-Extension", required = false)
    }
}

tasks.shadowJar {
    val versionProperty = findProperty("version") as? String
        ?: throw IllegalArgumentException("version was null")
    archiveFileName.set("WeaponMechanics-$versionProperty.jar")

    val libPackage = "me.deecaad.core.lib"

    // the kotlin plugin adds kotlin-stdlib to the classpath, but we don't want it in the shadow jar
    exclude("org/jetbrains/kotlin/**")

    relocate("org.slf4j", "$libPackage.slf4j")
    relocate("org.bstats", "$libPackage.bstats")
    relocate("dev.jorel.commandapi", "$libPackage.commandapi")
    relocate("com.cjcrafter.foliascheduler", "$libPackage.scheduler")
    relocate("com.zaxxer.hikari", "$libPackage.hikari")
    relocate("kotlin.", "$libPackage.kotlin.")
    relocate("com.cryptomorin.xseries", "$libPackage.xseries")
}