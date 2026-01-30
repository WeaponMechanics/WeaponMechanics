package me.deecaad.weaponmechanics

import com.cjcrafter.foliascheduler.TaskImplementation
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.BlockPredicateArgument
import dev.jorel.commandapi.arguments.BooleanArgument
import dev.jorel.commandapi.arguments.LocationArgument
import dev.jorel.commandapi.arguments.MultiLiteralArgument
import dev.jorel.commandapi.arguments.TimeArgument
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.booleanArgument
import dev.jorel.commandapi.kotlindsl.chatColorArgument
import dev.jorel.commandapi.kotlindsl.commandAPICommand
import dev.jorel.commandapi.kotlindsl.doubleArgument
import dev.jorel.commandapi.kotlindsl.entityExecutor
import dev.jorel.commandapi.kotlindsl.entitySelectorArgumentManyEntities
import dev.jorel.commandapi.kotlindsl.entitySelectorArgumentManyPlayers
import dev.jorel.commandapi.kotlindsl.entitySelectorArgumentOnePlayer
import dev.jorel.commandapi.kotlindsl.entityTypeArgument
import dev.jorel.commandapi.kotlindsl.floatArgument
import dev.jorel.commandapi.kotlindsl.greedyStringArgument
import dev.jorel.commandapi.kotlindsl.integerArgument
import dev.jorel.commandapi.kotlindsl.locationArgument
import dev.jorel.commandapi.kotlindsl.multiLiteralArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.stringArgument
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.jorel.commandapi.kotlindsl.timeArgument
import me.deecaad.core.MechanicsCore
import me.deecaad.core.commands.CommandHelpBuilder
import me.deecaad.core.commands.CustomMapArgument
import me.deecaad.core.compatibility.CompatibilityAPI
import me.deecaad.core.compatibility.HitBox
import me.deecaad.core.compatibility.entity.EntityCompatibility.EntityMeta
import me.deecaad.core.compatibility.entity.FakeEntity
import me.deecaad.core.file.simple.BooleanSerializer
import me.deecaad.core.file.simple.CsvSerializer
import me.deecaad.core.file.simple.EnumValueSerializer
import me.deecaad.core.file.simple.IntSerializer
import me.deecaad.core.file.simple.StringSerializer
import me.deecaad.core.utils.EntityTransform
import me.deecaad.core.utils.EnumUtil
import me.deecaad.core.utils.LogLevel
import me.deecaad.core.utils.NumberUtil
import me.deecaad.core.utils.NumberUtil.approximately
import me.deecaad.core.utils.NumberUtil.toTime
import me.deecaad.core.utils.Quaternion
import me.deecaad.core.utils.RandomUtil.element
import me.deecaad.core.utils.RegistryUtil
import me.deecaad.core.utils.StringUtil.colorBukkit
import me.deecaad.core.utils.TableBuilder
import me.deecaad.core.utils.Transform
import me.deecaad.core.utils.ray.RayTrace
import me.deecaad.weaponmechanics.weapon.explode.BlockDamage
import me.deecaad.weaponmechanics.weapon.explode.Explosion
import me.deecaad.weaponmechanics.weapon.explode.Flashbang
import me.deecaad.weaponmechanics.weapon.explode.exposures.ExplosionExposure
import me.deecaad.weaponmechanics.weapon.explode.exposures.ExplosionExposures
import me.deecaad.weaponmechanics.weapon.explode.regeneration.RegenerationData
import me.deecaad.weaponmechanics.weapon.explode.shapes.CubeExplosion
import me.deecaad.weaponmechanics.weapon.explode.shapes.DefaultExplosion
import me.deecaad.weaponmechanics.weapon.explode.shapes.ExplosionShape
import me.deecaad.weaponmechanics.weapon.explode.shapes.ParabolaExplosion
import me.deecaad.weaponmechanics.weapon.explode.shapes.SphereExplosion
import me.deecaad.weaponmechanics.weapon.projectile.weaponprojectile.Projectile
import me.deecaad.weaponmechanics.weapon.projectile.weaponprojectile.ProjectileSettings
import me.deecaad.weaponmechanics.weapon.reload.ammo.AmmoConfig
import me.deecaad.weaponmechanics.weapon.shoot.SelectiveFireState
import me.deecaad.weaponmechanics.weapon.shoot.recoil.RecoilProfile
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.event.HoverEventSource
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Color
import org.bukkit.EntityEffect
import org.bukkit.FireworkEffect
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.block.Block
import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.FireworkMeta
import org.bukkit.util.Vector
import java.util.Random
import java.util.function.Consumer
import java.util.function.Predicate
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

@Suppress("UNCHECKED_CAST")
object WeaponMechanicsCommand {
    const val WIKI: String = "https://cjcrafter.gitbook.io/weaponmechanics/"

    @JvmStatic
    fun registerCommands() {
        val weaponDataMapArgument =
            CustomMapArgument(
                "data",
                mapOf(
                    "ammo" to IntSerializer(0, null),
                    "firemode" to EnumValueSerializer(SelectiveFireState::class.java, false),
                    "skipMainhand" to BooleanSerializer(),
                    "slot" to IntSerializer(0, 40),
                    "durability" to IntSerializer(1, null),
                    "maxDurability" to IntSerializer(1, null),
                    "skin" to StringSerializer(),
                    "attachments" to CsvSerializer(StringSerializer()),
                ),
            )
        weaponDataMapArgument.setOptional(true)

        commandAPICommand("wm") {
            withAliases("weaponmechanics")
            withPermission("weaponmechanics.admin")
            withShortDescription("WeaponMechanics main command")

            subcommand("give") {
                withPermission("weaponmechanics.commands.give")
                withShortDescription("Gives the target(s) requested weapon(s)")

                entitySelectorArgumentManyPlayers("target")
                stringArgument("weapon") {
                    replaceSuggestions(
                        ArgumentSuggestions.strings {
                            (
                                WeaponMechanics.getInstance().weaponHandler.infoHandler.sortedWeaponList +
                                    listOf(
                                        "*",
                                        "**",
                                        "*r",
                                    )
                            ).toTypedArray()
                        },
                    )
                }
                integerArgument("amount", 1, 64, optional = true)
                withArguments(weaponDataMapArgument)

                anyExecutor { player, args ->
                    val targets = args["target"] as List<Entity>
                    val weaponTitle = args["weapon"] as String
                    val amount = args["amount"] as? Int ?: 1
                    val data = args["data"] as? Map<String, Any> ?: mutableMapOf()

                    give(player, targets, weaponTitle, amount, data.toMutableMap())
                }
            }

            subcommand("get") {
                withPermission("weaponmechanics.commands.get")
                withShortDescription("Gives you the requested weapon(s)")

                stringArgument("weapon") {
                    replaceSuggestions(
                        ArgumentSuggestions.strings {
                            (
                                WeaponMechanics.getInstance().weaponHandler.infoHandler.sortedWeaponList +
                                    listOf(
                                        "*",
                                        "**",
                                        "*r",
                                    )
                            ).toTypedArray()
                        },
                    )
                }
                integerArgument("amount", 1, 64, optional = true)
                withArguments(weaponDataMapArgument)

                playerExecutor { player, args ->
                    val weapon = args["weapon"] as String
                    val amount = args["amount"] as? Int ?: 1
                    val data = mutableMapOf<String, Any>()

                    give(player, listOf(player), weapon, amount, data)
                }
            }

            subcommand("ammo") {
                withPermission("weaponmechanics.commands.ammo")
                withShortDescription("Gets ammo for held weapon")

                integerArgument("amount", 1, 64, optional = true)

                playerExecutor { player, args ->
                    val amount = args["amount"] as? Int ?: 64
                    giveAmmo(player, amount)
                }
            }

            subcommand("giveammo") {
                withPermission("weaponmechanics.commands.giveammo")
                withShortDescription("Gives ammo of a certain type to a player")

                entitySelectorArgumentOnePlayer("player")
                stringArgument("ammo")
                booleanArgument("magazine", optional = true)
                integerArgument("amount", 1, 64, optional = true)

                anyExecutor { sender, args ->
                    val target = args["player"] as Player
                    val ammoType = args["ammo"] as String
                    val magazine = args["magazine"] as? Boolean ?: false
                    val amount = args["amount"] as? Int ?: 64

                    giveAmmo(sender, target, ammoType, magazine, amount)
                }
            }

            subcommand("info") {
                withPermission("weaponmechanics.commands.info")
                withShortDescription("Displays info about WM")

                anyExecutor { sender, _ ->
                    info(sender)
                }
            }

            subcommand("list") {
                withPermission("weaponmechanics.commands.list")
                withShortDescription("Shows a table of weapons")

                integerArgument("page", 1, optional = true)

                anyExecutor { sender, args ->
                    val page = args["page"] as? Int ?: 1
                    list(sender, page)
                }
            }

            subcommand("wiki") {
                withPermission("weaponmechanics.commands.wiki")
                withShortDescription("Gives wiki links")

                anyExecutor { sender, _ ->
                    wiki(sender)
                }
            }

            subcommand("reload") {
                withPermission("weaponmechanics.commands.reload")
                withShortDescription("Reloads config")

                anyExecutor { sender, _ ->
                    WeaponMechanics.getInstance().reload().thenAccept {
                        sender.sendMessage("${ChatColor.GREEN}Reloaded configuration")
                    }
                }
            }

            subcommand("test") {
                withPermission("weaponmechanics.commands.test")
                withShortDescription("Testing dev commands")

                subcommand("nbt") {
                    withPermission("weaponmechanics.commands.test.nbt")
                    withShortDescription("Shows every NBT tag for an item")

                    entitySelectorArgumentManyPlayers("target") {
                        setOptional(true)
                    }

                    anyExecutor { sender, args ->
                        val target = args["target"] as? Entity
                        nbt(sender, target)
                    }
                }

                // Each explosion shares these arguments
                val commonExplosionArguments =
                    mutableListOf<Argument<*>>(
                        LocationArgument("origin"),
                        MultiLiteralArgument(
                            "exposure",
                            *ExplosionExposures.REGISTRY.map { it.key.toString() }.toTypedArray(),
                        ),
                        BooleanArgument("break-blocks"),
                        BlockPredicateArgument("blacklist"),
                        TimeArgument("regen"),
                    )

                subcommand("explosion") {
                    withPermission("weaponmechanics.commands.test.explosion")
                    withShortDescription("Spawns in an explosion that regenerates")

                    subcommand("sphere") {
                        doubleArgument("radius", optional = true)
                        withOptionalArguments(commonExplosionArguments)

                        playerExecutor { entity, args ->
                            val origin = args["origin"] as? Location ?: entity.location
                            val radius = args["radius"] as? Double ?: 5.0
                            val exposure = args["exposure"] as? String ?: "default"
                            val breakBlocks = args["break-blocks"] as? Boolean ?: true
                            val blackList = args["blacklist"] as? Predicate<Block> ?: Predicate { false }
                            val regen = args["regen"] as? Int ?: 200

                            val shape = SphereExplosion(radius)
                            explode(entity as LivingEntity, shape, origin, exposure, breakBlocks, blackList, regen)
                        }
                    }

                    subcommand("cube") {
                        doubleArgument("width", optional = true)
                        doubleArgument("height", optional = true)
                        withOptionalArguments(commonExplosionArguments)

                        playerExecutor { entity, args ->
                            val origin = args["origin"] as? Location ?: entity.location
                            val width = args["width"] as? Double ?: 5.0
                            val height = args["height"] as? Double ?: 5.0
                            val exposure = args["exposure"] as? String ?: "default"
                            val breakBlocks = args["break-blocks"] as? Boolean ?: true
                            val blackList = args["blacklist"] as? Predicate<Block> ?: Predicate { false }
                            val regen = args["regen"] as? Int ?: 200

                            val shape = CubeExplosion(width, height)
                            explode(entity as LivingEntity, shape, origin, exposure, breakBlocks, blackList, regen)
                        }
                    }

                    subcommand("parabola") {
                        doubleArgument("angle", optional = true)
                        doubleArgument("depth", optional = true)
                        withOptionalArguments(commonExplosionArguments)

                        playerExecutor { entity, args ->
                            val origin = args["origin"] as? Location ?: entity.location
                            val depth = args["depth"] as? Double ?: -5.0
                            val angle = args["angle"] as? Double ?: 0.25
                            val exposure = args["exposure"] as? String ?: "default"
                            val breakBlocks = args["break-blocks"] as? Boolean ?: true
                            val blackList = args["blacklist"] as? Predicate<Block> ?: Predicate { false }
                            val regen = args["regen"] as? Int ?: 200

                            val shape = ParabolaExplosion(depth, angle)
                            explode(entity as LivingEntity, shape, origin, exposure, breakBlocks, blackList, regen)
                        }
                    }

                    subcommand("vanilla") {
                        doubleArgument("yield", optional = true)
                        integerArgument("rays", 1, optional = true)
                        withOptionalArguments(commonExplosionArguments)

                        playerExecutor { player, args ->
                            val origin = args["origin"] as? Location ?: player.location
                            val yield = args["yield"] as? Double ?: 4.0
                            val rays = args["rays"] as? Int ?: 16
                            val exposure = args["exposure"] as? String ?: "default"
                            val breakBlocks = args["break-blocks"] as? Boolean ?: true
                            val blackList = args["blacklist"] as? Predicate<Block> ?: Predicate { false }
                            val regen = args["regen"] as? Int ?: 200

                            val shape = DefaultExplosion(yield, rays)
                            explode(player as LivingEntity, shape, origin, exposure, breakBlocks, blackList, regen)
                        }
                    }
                }

                subcommand("fakeentity") {
                    withPermission("weaponmechanics.commands.test.fakeentity")
                    withShortDescription("Spawns in a fake entity")

                    entityTypeArgument("entity")
                    locationArgument("location")
                    multiLiteralArgument(nodeName = "move", "none", "spin", "flash", "x", "y", "z")
                    timeArgument("time", optional = true)
                    booleanArgument("gravity", optional = true)
                    greedyStringArgument("name", optional = true)

                    playerExecutor { player, args ->
                        val type = args["entity"] as EntityType
                        val location = args["location"] as Location
                        val move = args["move"] as String
                        val time = args["time"] as? Int ?: 0
                        val gravity = args["gravity"] as? Boolean ?: false
                        val name = args["name"] as? String

                        spawn(player, location, type, move, time, gravity, name)
                    }
                }

                subcommand("meta") {
                    withPermission("weaponmechanics.commands.test.meta")
                    withShortDescription("Overrides the entity metadata of nearby entities for a moment")

                    entitySelectorArgumentManyEntities("targets")
                    val metaFlags = EnumUtil.getOptions(EntityMeta::class.java).toTypedArray()
                    multiLiteralArgument(nodeName = "flag", literals = metaFlags)
                    timeArgument("time", optional = true)

                    playerExecutor { player, args ->
                        val targets = args["targets"] as List<Entity>
                        val flag = args["flag"] as String
                        val time = args["time"] as? Int ?: 200

                        meta(player, targets, EnumUtil.parseEnums(EntityMeta::class.java, flag)[0], time)
                    }
                }

                subcommand("firework") {
                    withPermission("weaponmechanics.commands.test.firework")
                    withShortDescription("Spawns in a firework")

                    locationArgument("location")
                    timeArgument("time", optional = true)
                    val fireworkEffects = EnumUtil.getOptions(FireworkEffect.Type::class.java).toTypedArray()
                    multiLiteralArgument(nodeName = "shape", literals = fireworkEffects, optional = true)
                    chatColorArgument("color", optional = true)
                    chatColorArgument("fade", optional = true)
                    booleanArgument("flicker", optional = true)
                    booleanArgument("trail", optional = true)

                    anyExecutor { sender, args ->
                        val location = args["location"] as Location
                        val time = args["time"] as? Int ?: 60
                        val shape =
                            EnumUtil.parseEnums(FireworkEffect.Type::class.java, args["shape"] as? String ?: "BALL")[0]
                        val color = args["color"] as? ChatColor ?: ChatColor.WHITE
                        val fade = args["fade"] as? ChatColor ?: ChatColor.WHITE
                        val flicker = args["flicker"] as? Boolean ?: false
                        val trail = args["trail"] as? Boolean ?: false

                        val colorBukkit = Color.fromRGB(color.asBungee().color.rgb)
                        val fadeBukkit = Color.fromRGB(fade.asBungee().color.rgb)
                        firework(location, time, shape, colorBukkit, fadeBukkit, flicker, trail)
                    }
                }

                subcommand("hitbox") {
                    withPermission("weaponmechanics.commands.test.hitbox")
                    withShortDescription("Shows the hitboxes of nearby entities")

                    entitySelectorArgumentManyEntities("targets")
                    timeArgument("time", optional = true)

                    playerExecutor { player, args ->
                        val targets = args["targets"] as List<Entity>
                        val time = args["time"] as? Int ?: 200

                        hitbox(player, targets, time)
                    }
                }

                subcommand("ray") {
                    withPermission("weaponmechanics.commands.test.ray")
                    withShortDescription("Ray traces blocks/entities")

                    booleanArgument("highlight-box", optional = true)
                    doubleArgument("size", 0.0, optional = true)
                    integerArgument("distance", 1, optional = true)
                    timeArgument("time", optional = true)

                    playerExecutor { player, args ->
                        val highlightBox = args["highlight-box"] as? Boolean ?: false
                        val size = args["size"] as? Double ?: 0.5
                        val distance = args["distance"] as? Int ?: 100
                        val time = args["time"] as? Int ?: 200

                        ray(player, highlightBox, size, distance, time)
                    }
                }

                subcommand("recoil") {
                    withPermission("weaponmechanics.commands.test.recoil")
                    withShortDescription("Applies recoil to the player")

                    floatArgument("recoilMeanX", optional = true)
                    floatArgument("recoilMeanY", optional = true)
                    floatArgument("recoilVarianceX", optional = true)
                    floatArgument("recoilVarianceY", optional = true)
                    floatArgument("recoilSpeed", optional = true)
                    floatArgument("damping", optional = true)
                    floatArgument("dampingRecovery", optional = true)
                    floatArgument("smoothingFactor", optional = true)
                    floatArgument("maxRecoilAccum", optional = true)
                    integerArgument("interval", 1, optional = true)
                    integerArgument("time", 1, optional = true)

                    playerExecutor { player, args ->
                        val recoilMeanX = args["recoilMeanX"] as? Float ?: 0.1f
                        val recoilMeanY = args["recoilMeanY"] as? Float ?: 0.8f
                        val recoilVarianceX = args["recoilVarianceX"] as? Float ?: 0.2f
                        val recoilVarianceY = args["recoilVarianceY"] as? Float ?: 0.2f
                        val recoilSpeed = args["recoilSpeed"] as? Float ?: 1f
                        val damping = args["damping"] as? Float ?: 0.1f
                        val dampingRecovery = args["dampingRecovery"] as? Float ?: 0.1f
                        val smoothingFactor = args["smoothingFactor"] as? Float ?: 0.9f
                        val maxRecoilAccum = args["maxRecoilAccum"] as? Float ?: 360f
                        val interval = args["interval"] as? Int ?: 4
                        val time = args["time"] as? Int ?: 80

                        recoil(
                            player, recoilMeanX, recoilMeanY, recoilVarianceX, recoilVarianceY, recoilSpeed,
                            damping, dampingRecovery, smoothingFactor, maxRecoilAccum, interval, time,
                        )
                    }
                }

                subcommand("shoot") {
                    withPermission("weaponmechanics.commands.test.shoot")
                    withShortDescription("Shoots a projectile")

                    doubleArgument("speed", optional = true)
                    doubleArgument("gravity", optional = true)
                    entityTypeArgument("disguise", optional = true)

                    playerExecutor { entity, args ->
                        val speed = args["speed"] as? Double ?: 1.0
                        val gravity = args["gravity"] as? Double ?: 0.0
                        val disguise = args["disguise"] as? EntityType

                        shoot(entity as LivingEntity, speed, gravity, disguise)
                    }
                }

                subcommand("stats") {
                    withPermission("weaponmechanics.commands.test.stats")
                    withShortDescription("Shows the stats of a player or weapon")

                    multiLiteralArgument(nodeName = "type", "player", "weapon")
                    entitySelectorArgumentOnePlayer("target")
                    stringArgument("weapon", optional = true) {
                        replaceSuggestions(
                            ArgumentSuggestions.strings {
                                WeaponMechanics.getInstance().weaponHandler.infoHandler.sortedWeaponList.toTypedArray()
                            },
                        )
                    }

                    playerExecutor { player, args ->
                        val type = args["type"] as String
                        val target = args["target"] as Player
                        val weapon = args["weapon"] as? String

                        stats(player, type, target, weapon)
                    }
                }

                subcommand("transform") {
                    withPermission("weaponmechanics.commands.test.transform")
                    withShortDescription("Transforms the player")

                    integerArgument("children", 1, optional = true)
                    timeArgument("time", optional = true)
                    doubleArgument("speed", 0.0, optional = true)
                    doubleArgument("radius", 0.0, optional = true)
                    booleanArgument("particles", optional = true)

                    playerExecutor { player, args ->
                        val children = args["children"] as? Int ?: 16
                        val time = args["time"] as? Int ?: 200
                        val speed = args["speed"] as? Double ?: (2 * Math.PI)
                        val radius = args["radius"] as? Double ?: 2.0
                        val particles = args["particles"] as? Boolean ?: false

                        transform(player, children, time, speed, radius, particles)
                    }
                }
            }

            val helpBuilder = CommandHelpBuilder(Style.style(NamedTextColor.GOLD), Style.style(NamedTextColor.GRAY))
            helpBuilder.register(this)
        }
    }

    fun stats(
        sender: CommandSender,
        type: String,
        target: Player?,
        weapon: String?,
    ) {
        if (target == null) {
            sender.sendMessage(ChatColor.RED.toString() + "No target found")
            return
        }

        val wrapper = WeaponMechanics.getInstance().getPlayerWrapper(target)
        val statsData = wrapper.statsData

        if (statsData == null) {
            sender.sendMessage(ChatColor.RED.toString() + "Stats are disabled or not yet synced...")
            return
        }

        if ("player" == type) {
            val playerData = statsData.playerData
            if (playerData == null) {
                sender.sendMessage(ChatColor.RED.toString() + "No player stats found from " + target.name)
                return
            }

            sender.sendMessage(ChatColor.GOLD.toString() + "Showing stats of " + target.name + ":")
            for (msg in playerData) {
                sender.sendMessage(msg)
            }
            return
        }

        if (weapon == null) {
            sender.sendMessage(ChatColor.RED.toString() + "Weapon title has to be defined when trying to fetch weapon stats")
            return
        }

        val weaponData = statsData.getWeaponData(weapon)
        if (weaponData == null) {
            sender.sendMessage(ChatColor.RED.toString() + "No weapon stats found from " + weapon + " of player " + target.name)
            return
        }

        sender.sendMessage(ChatColor.GOLD.toString() + "Showing " + target.name + " stats of " + weapon + ":")
        for (msg in weaponData) {
            sender.sendMessage(msg)
        }
    }

    fun give(
        sender: CommandSender,
        targets: List<Entity>,
        weaponTitle: String?,
        amount: Int,
        data: MutableMap<String, Any>,
    ) {
        var weaponTitle = weaponTitle
        if (targets.isEmpty()) {
            sender.sendMessage(ChatColor.RED.toString() + "No entities were found")
            return
        }

        // Used by the WeaponGenerateEvent
        data["sender"] = sender

        val info = WeaponMechanics.getInstance().weaponHandler.infoHandler
        val entitiesGiven: MutableList<Entity> = ArrayList()
        val weaponsGiven: MutableSet<String> = HashSet()

        // Handle random weapon key "*r"
        if ("*r".equals(weaponTitle, ignoreCase = true)) weaponTitle = element(info.sortedWeaponList)

        for (loop in targets) {
            if (!loop.type.isAlive) continue

            val entity = loop as LivingEntity
            val isPlayer = entity is Player
            val player = if (isPlayer) entity as Player else null

            // Loop through each possible weapon and give each gun to the player.
            if ("*".equals(weaponTitle, ignoreCase = true) || "**".equals(weaponTitle, ignoreCase = true)) {
                // Non-players can only be given 1 weapon at a time.

                if (!isPlayer) continue

                entitiesGiven.add(entity)
                for (title in info.sortedWeaponList) {
                    if ("**".equals(weaponTitle, ignoreCase = true) || player!!.inventory.firstEmpty() != -1) {
                        info.giveOrDropWeapon(title, player, amount, data)
                        weaponsGiven.add(title)
                    }
                }
            } else {
                weaponTitle = info.getWeaponTitle(weaponTitle)
                if (info.giveOrDropWeapon(weaponTitle, entity, amount, data)) {
                    entitiesGiven.add(entity)
                    weaponsGiven.add(weaponTitle)
                }
            }
        }

        // Probably only happens when somebody uses a complicated targeter, like
        // @e[type=wither_skeleton] while there are no wither skeletons nearby.
        if (entitiesGiven.isEmpty() || weaponsGiven.isEmpty()) {
            sender.sendMessage(ChatColor.RED.toString() + "No entities were given any weapons...")
            return
        }

        val targetInfo = if (entitiesGiven.size == 1) entitiesGiven[0].name else entitiesGiven.size.toString()
        val weaponInfo =
            if (weaponsGiven.size == 1) {
                "$amount ${weaponsGiven.stream().findAny().get()}${if (amount > 1) "s" else ""}"
            } else {
                weaponsGiven.size.toString() + " weapons"
            }

        // Show each target. This may be useful in case the user accidentally
        // gave the weapon(s) to too many people, and needs to check who got it.
        val targetHover: HoverEventSource<*>?
        if (entitiesGiven.size == 1 && entitiesGiven[0] is HoverEventSource<*>) {
            targetHover = entitiesGiven[0] as HoverEventSource<*>
        } else if (entitiesGiven.isEmpty()) {
            targetHover = null
        } else {
            val builder = text().append(text(entitiesGiven[0].name))
            for (i in 1..<entitiesGiven.size) {
                builder.append(text(", ")).append(text(entitiesGiven[i].name))
            }
            targetHover = builder.build()
        }

        val style = Style.style(NamedTextColor.GREEN)
        val builder =
            text()
                .append(text("Gave ", style))
                .append(text().content(targetInfo).style(style).hoverEvent(targetHover))
                .append(text(" ", style))
                .append(text().content(weaponInfo).style(style))

        sender.sendMessage(builder)
    }

    fun giveAmmo(
        sender: Player,
        amount: Int,
    ) {
        val weaponMechanics = WeaponMechanics.getInstance();
        val info = weaponMechanics.weaponHandler.infoHandler
        val title = info.getWeaponTitle(sender.inventory.itemInMainHand, false)
        if (title == null) {
            sender.sendMessage(ChatColor.RED.toString() + "Not holding gun")
            return
        }

        val ammo = weaponMechanics.weaponConfigurations.getObject("$title.Reload.Ammo", AmmoConfig::class.java)
        if (ammo == null) {
            sender.sendMessage(ChatColor.RED.toString() + title + " does not use ammo")
            return
        }

        ammo.giveAmmo(sender.inventory.itemInMainHand, weaponMechanics.getPlayerWrapper(sender), amount, 64)
        sender.sendMessage(ChatColor.GREEN.toString() + "Sent ammo")
    }

    fun giveAmmo(
        sender: CommandSender,
        player: Player,
        ammoName: String,
        magazine: Boolean,
        amount: Int,
    ) {
        val item: ItemStack?
        try {
            item = WeaponMechanicsAPI.generateAmmo(ammoName, magazine)
        } catch (ex: Throwable) {
            sender.sendMessage(ChatColor.RED.toString() + ex.message)
            return
        }

        if (item == null) {
            sender.sendMessage(ChatColor.RED.toString() + "No such ammo exists")
            return
        }

        item.amount = amount

        val overflow: Map<Int, ItemStack> = player.inventory.addItem(item)
        if (overflow.isNotEmpty()) {
            sender.sendMessage(ChatColor.RED.toString() + player.name + "'s inventory was full")
        } else {
            sender.sendMessage(ChatColor.GREEN.toString() + player.name + " recieved " + amount + " " + ammoName)
        }
    }

    enum class RepairMode {
        HAND,
        INVENTORY,
    }

    fun info(sender: CommandSender) {
        val weaponMechanics = WeaponMechanics.getInstance()

        val desc = weaponMechanics.description
        val mechanicsCoreVersion = MechanicsCore.getInstance().description.version

        // Gather supported plugin names
        val softDepends =
            LinkedHashSet(desc.softDepend).apply {
                addAll(MechanicsCore.getInstance().description.softDepend)
                remove("MechanicsCore")
                removeIf { name -> Bukkit.getPluginManager().getPlugin(name) == null }
                if (isEmpty()) add("No supported plugins installed")
            }

        // Build everything as a single multi-line component
        val bigComponent =
            text()
                // Line 1: "WeaponMechanics, vX.Y.Z"
                .append(
                    text("Weapon", NamedTextColor.GOLD, TextDecoration.BOLD),
                    text("Mechanics", NamedTextColor.GRAY, TextDecoration.BOLD),
                    text(", v", NamedTextColor.GRAY),
                    text(desc.version, NamedTextColor.GRAY, TextDecoration.ITALIC),
                )
                .appendNewline()
                // Line 2: Authors
                .append(
                    text("  ➢ ", NamedTextColor.GRAY),
                    text("Authors: ", NamedTextColor.GOLD),
                    text(desc.authors.joinToString(", "), NamedTextColor.GRAY),
                )
                .appendNewline()
                // Line 3: Command
                .append(
                    text("  ➢ ", NamedTextColor.GRAY),
                    text("Command: ", NamedTextColor.GOLD),
                    text("/weaponmechanics", NamedTextColor.GRAY)
                        .hoverEvent(HoverEvent.showText(text("Click to run the command!")))
                        .clickEvent(ClickEvent.runCommand("/weaponmechanics")),
                )
                .appendNewline()
                // Line 4: Server
                .append(
                    text("  ➢ ", NamedTextColor.GRAY),
                    text("Server: ", NamedTextColor.GOLD),
                    text("${Bukkit.getName()} ${Bukkit.getVersion()}", NamedTextColor.GRAY),
                )
                .appendNewline()
                // Line 5: MechanicsCore
                .append(
                    text("  ➢ ", NamedTextColor.GRAY),
                    text("MechanicsCore: ", NamedTextColor.GOLD),
                    text(mechanicsCoreVersion, NamedTextColor.GRAY),
                )
                .appendNewline()
                // Line 6: Java
                .append(
                    text("  ➢ ", NamedTextColor.GRAY),
                    text("Java: ", NamedTextColor.GOLD),
                    text(System.getProperty("java.version"), NamedTextColor.GRAY),
                )
                .appendNewline()
                // Line 7: Supported plugins
                .append(
                    text("  ➢ ", NamedTextColor.GRAY),
                    text("Supported plugins: ", NamedTextColor.GOLD),
                    text(softDepends.joinToString(", "), NamedTextColor.GRAY),
                )
                .build()

        sender.sendMessage(bigComponent)
    }

    fun list(
        sender: CommandSender?,
        requestedPage: Int,
    ) {
        val info = WeaponMechanics.getInstance().getWeaponHandler().infoHandler
        val weapons = info.sortedWeaponList

        // «»
        val gold = Style.style(NamedTextColor.GOLD)
        val gray = Style.style(NamedTextColor.GRAY)
        val table =
            TableBuilder()
                .withConstraints(TableBuilder.DEFAULT_CONSTRAINTS.setColumns(3).setPixels(310)) // 10 pixel buffer
                .withElementChar('-')
                .withElementCharStyle(gold)
                .withFillChar('=')
                .withFillCharStyle(Style.style(NamedTextColor.GRAY, TextDecoration.STRIKETHROUGH))
                .withHeader("Weapons [Page " + requestedPage + "/" + ((weapons.size - 1) / (3 * 8) + 1) + "]")
                .withHeaderStyle(gold)
                .withElementStyle(gray)
                .withLeft(
                    text().content("«").style(gold)
                        .clickEvent(ClickEvent.runCommand("/wm list " + (requestedPage - 1)))
                        .hoverEvent(text("Click to go to page " + (requestedPage - 1), gray))
                        .build(),
                )
                .withRight(
                    text().content("»").style(gold)
                        .clickEvent(ClickEvent.runCommand("/wm list " + (requestedPage + 1)))
                        .hoverEvent(text("Click to go to page " + (requestedPage + 1), gray))
                        .build(),
                )
                .withAttemptSinglePixelFix()
                .withSupplier { i: Int ->
                    val index = i + (requestedPage - 1) * 3 * 8
                    if (weapons.size <= index) return@withSupplier Component.empty()

                    val title = weapons[index]
                    val item = info.generateWeapon(title, 1)
                    text().content(title.uppercase())
                        .clickEvent(ClickEvent.runCommand("/wm get $title"))
                        .hoverEvent(
                            LegacyComponentSerializer.legacySection().deserialize(
                                item.itemMeta!!.displayName,
                            ),
                        )
                        .build()
                }
                .build()

        sender!!.sendMessage(table)
    }

    fun wiki(sender: CommandSender?) {
        val pages: List<String> =
            mutableListOf(
                "Info", "Shoot", "Scope", "Reload",
                "Skin", "Projectile", "Explosion", "Damage", "Firearm_Action", "Melee",
            )

        val gold = Style.style(NamedTextColor.GOLD)
        val gray = Style.style(NamedTextColor.GRAY)
        val table =
            TableBuilder()
                .withConstraints(TableBuilder.DEFAULT_CONSTRAINTS.setPixels(300))
                .withElementChar('-')
                .withElementCharStyle(gold)
                .withElementStyle(gray)
                .withAttemptSinglePixelFix()
                .withSupplier { i: Int ->
                    if (i >= pages.size) {
                        Component.empty()
                    } else {
                        text().content(pages[i].uppercase())
                            .clickEvent(ClickEvent.openUrl(WIKI + "/weapon-modules/" + pages[i].lowercase()))
                            .hoverEvent(text("Click to go to the wiki", gray))
                            .build()
                    }
                }
                .build()

        sender!!.sendMessage(table)
    }

    fun nbt(
        sender: CommandSender,
        target: Entity?,
    ) {
        var target = target
        val entity: LivingEntity

        // When target is null, the command sender should be used as the target
        if (target == null) {
            if (sender is LivingEntity) {
                target = sender
            } else {
                sender.sendMessage(ChatColor.RED.toString() + "NBT is an Entity only command!")
                return
            }
        }

        if (target is LivingEntity) {
            entity = target
        } else {
            sender.sendMessage(ChatColor.RED.toString() + "Target must be a creature, player, or armor stand! Got: " + target.type)
            return
        }

        if (entity.equipment == null) {
            sender.sendMessage(ChatColor.RED.toString() + entity.name + " did not have any equipment")
            return
        }

        val item = entity.equipment!!.itemInMainHand
        if (!item.hasItemMeta()) {
            sender.sendMessage(ChatColor.RED.toString() + entity.name + "'s " + item.type + " did not have any NBT data.")
            return
        }

        val tags = CompatibilityAPI.getNBTCompatibility().getNBTDebug(item)
        WeaponMechanics.getInstance().debugger.info(tags)
        sender.sendMessage(colorBukkit(tags))
    }

    fun explode(
        cause: LivingEntity,
        shape: ExplosionShape,
        origin: Location,
        exposureString: String,
        isBreakBlocks: Boolean,
        blackList: Predicate<Block>,
        regen: Int,
    ) {
        cause.sendMessage(ChatColor.GREEN.toString() + "Spawning explosion in 5 seconds")

        WeaponMechanics.getInstance().foliaScheduler.region(origin).runDelayed(
            Runnable {
                val regeneration = RegenerationData(regen, max(1.0, shape.area / 100.0).toInt(), 1)
                val blockDamage =
                    object : BlockDamage(0.0, 1, 1, Material.AIR, BreakMode.BREAK, mapOf()) {
                        override fun getBreakMode(block: Block): BreakMode {
                            return if (blackList.test(block)) BreakMode.BREAK else BreakMode.CRACK
                        }
                    }

                val exposure = RegistryUtil.matchAny(ExplosionExposures.REGISTRY, exposureString)
                val explosion =
                    Explosion(
                        shape, exposure, blockDamage, regeneration, null, 0.0, 1.0,
                        null, null, Flashbang(10.0, null), null,
                    )
                explosion.explode(cause, origin, null)
            },
            100L,
        )
    }

    fun spawn(
        player: Player?,
        location: Location?,
        type: EntityType,
        moveType: String?,
        time: Int,
        gravity: Boolean,
        name: String?,
    ) {
        val entity =
            CompatibilityAPI.getEntityCompatibility().generateFakeEntity(
                location,
                type,
                if (type == EntityType.ITEM) {
                    ItemStack(
                        Material.STONE_AXE,
                    )
                } else {
                    null
                },
            )
        entity.setGravity(gravity)
        entity.setDisplay(name)
        entity.show(player!!)
        entity.setMotion(0.0, 0.0, 0.0)

        WeaponMechanics.getInstance().foliaScheduler.async()
            .runAtFixedRate(
                object : Consumer<TaskImplementation<Void>> {
                    // Some temp vars for the different move types
                    var ticksAlive: Int = 0
                    var flash: Boolean = true

                    override fun accept(task: TaskImplementation<Void>) {
                        if (ticksAlive++ >= time) {
                            entity.remove()
                            task.cancel()
                            return
                        }

                        when (moveType) {
                            "spin" -> entity.setRotation(entity.yaw + 5.0f, entity.yaw / 2.0f)
                            "flash" ->
                                if (ticksAlive % 10 == 0) {
                                    flash = !flash
                                    entity.isInvisible = !flash
                                    entity.isGlowing = flash
                                    entity.updateMeta()
                                }

                            "x" -> entity.setPosition(entity.x + 0.1, entity.y, entity.z)
                            "y" -> entity.setPosition(entity.x, entity.y + 0.1, entity.z)
                            "z" -> entity.setPosition(entity.x, entity.y, entity.z + 0.1)
                        }
                    }
                },
                0,
                0,
            )
    }

    fun meta(
        sender: Player,
        targets: List<Entity?>,
        flag: EntityMeta,
        ticks: Int,
    ) {
        val compatibility = CompatibilityAPI.getEntityCompatibility()

        sender.sendMessage(ChatColor.GREEN.toString() + "Making " + targets.size + " targets " + flag)

        for (entity in targets) {
            val packet = compatibility.generateMetaPacket(entity)
            compatibility.modifyMetaPacket(packet, flag, true)

            CompatibilityAPI.getCompatibility().sendPackets(sender, packet)
        }

        WeaponMechanics.getInstance().foliaScheduler.entity(sender).runDelayed(
            Runnable {
                sender.sendMessage(ChatColor.GREEN.toString() + "Resetting META...")
                for (entity in targets) {
                    // TODO check if entity is in the same region
                    val packet = compatibility.generateMetaPacket(entity)
                    compatibility.modifyMetaPacket(packet, flag, false)

                    CompatibilityAPI.getCompatibility().sendPackets(sender, packet)
                }
            },
            ticks.toLong(),
        )
    }

    fun hitbox(
        sender: CommandSender,
        targets: List<Entity>,
        ticks: Int,
    ) {
        sender.sendMessage(ChatColor.GREEN.toString() + "Showing hitboxes of " + targets.size + " entities for " + ticks + " ticks.")

        val basicConfiguration = WeaponMechanics.getInstance().configuration
        WeaponMechanics.getInstance().foliaScheduler.async()
            .runAtFixedRate(
                object : Consumer<TaskImplementation<Void>> {
                    var ticksPassed: Int = 0

                    override fun accept(task: TaskImplementation<Void>) {
                        for (entity in targets) {
                            if (entity !is LivingEntity) continue
                            if (entity == sender) continue

                            val type = entity.getType().name

                            val head = basicConfiguration.getDouble("Entity_Hitboxes.$type.HEAD", -1.0)
                            val body = basicConfiguration.getDouble("Entity_Hitboxes.$type.BODY", -1.0)
                            val legs = basicConfiguration.getDouble("Entity_Hitboxes.$type.LEGS", -1.0)
                            val feet = basicConfiguration.getDouble("Entity_Hitboxes.$type.FEET", -1.0)

                            if (head == -1.0 || body == -1.0 || legs == -1.0 || feet == -1.0) {
                                WeaponMechanics.getInstance().debugger.severe(
                                    "Entity type $type is missing some of its damage point values, please add it",
                                    "Located at file /WeaponMechanics/config.yml in Entity_Hitboxes.$type in configurations",
                                    "Its missing one of these: HEAD, BODY, LEGS or FEET",
                                )
                                continue
                            }
                            val sumOf = head + body + legs + feet
                            if (!approximately(sumOf, 1.0)) {
                                WeaponMechanics.getInstance().debugger.severe(
                                    "Entity type $type hit box values sum doesn't match 1.0",
                                    "Located at file /WeaponMechanics/config.yml in Entity_Hitboxes.$type in configurations",
                                    "Now the total sum was $sumOf, please make it 1.0.",
                                )
                                continue
                            }

                            val box: HitBox = HitBox.getHitbox(entity) ?: continue
                            val max = box.maxY
                            val height = box.height

                            val headY = max - (height * head)
                            val bodyY = max - (height * (head + body))
                            val legsY = max - (height * (head + body + legs))
                            val feetY =
                                max - (height * (head + body + legs + feet)) // this could also be just box.getMinY()

                            var x = box.minX
                            while (x <= box.maxX) {
                                var z = box.minZ
                                while (z <= box.maxZ) {
                                    if (head > 0.0) {
                                        entity.getWorld().spawnParticle(
                                            Particle.DUST,
                                            x,
                                            headY,
                                            z,
                                            1,
                                            0.0,
                                            0.0,
                                            0.0,
                                            0.0001,
                                            Particle.DustOptions(
                                                Color.RED,
                                                1.0f,
                                            ),
                                            true,
                                        )
                                    }
                                    if (body > 0.0) {
                                        entity.getWorld().spawnParticle(
                                            Particle.DUST,
                                            x,
                                            bodyY,
                                            z,
                                            1,
                                            0.0,
                                            0.0,
                                            0.0,
                                            0.0001,
                                            Particle.DustOptions(
                                                Color.ORANGE,
                                                1.0f,
                                            ),
                                            true,
                                        )
                                    }
                                    if (legs > 0.0) {
                                        entity.getWorld().spawnParticle(
                                            Particle.DUST,
                                            x,
                                            legsY,
                                            z,
                                            1,
                                            0.0,
                                            0.0,
                                            0.0,
                                            0.0001,
                                            Particle.DustOptions(
                                                Color.YELLOW,
                                                1.0f,
                                            ),
                                            true,
                                        )
                                    }
                                    if (feet > 0.0) {
                                        entity.getWorld().spawnParticle(
                                            Particle.DUST,
                                            x,
                                            feetY,
                                            z,
                                            1,
                                            0.0,
                                            0.0,
                                            0.0,
                                            0.0001,
                                            Particle.DustOptions(
                                                Color.GREEN,
                                                1.0f,
                                            ),
                                            true,
                                        )
                                    }
                                    z += 0.25
                                }
                                x += 0.25
                            }
                        }

                        ticksPassed += 5
                        if (ticksPassed >= ticks) {
                            task.cancel()
                        }
                    }
                },
                0L,
                5L,
            )
    }

    fun firework(
        location: Location?,
        time: Int,
        type: FireworkEffect.Type?,
        color: Color,
        fade: Color,
        flicker: Boolean,
        trail: Boolean,
    ) {
        val itemStack = ItemStack(Material.FIREWORK_ROCKET)
        val meta = itemStack.itemMeta as FireworkMeta?
        val effect =
            FireworkEffect.builder()
                .with(type!!)
                .withColor(color)
                .withFade(fade)
                .flicker(flicker)
                .trail(trail)
                .build()
        meta!!.addEffect(effect)
        itemStack.setItemMeta(meta)

        val random = Random()

        val fakeEntity =
            CompatibilityAPI.getEntityCompatibility()
                .generateFakeEntity(location, EntityType.FIREWORK_ROCKET, itemStack)
        fakeEntity.setMotion(random.nextGaussian() * 0.001, 0.3, random.nextGaussian() * 0.001)
        fakeEntity.show()
        if (time == 0) {
            fakeEntity.playEffect(EntityEffect.FIREWORK_EXPLODE)
            fakeEntity.remove()
            return
        }
        WeaponMechanics.getInstance().foliaScheduler.region(location!!).runDelayed(
            Runnable {
                fakeEntity.playEffect(EntityEffect.FIREWORK_EXPLODE)
                fakeEntity.remove()
            },
            time.toLong(),
        )
    }

    fun ray(
        sender: LivingEntity,
        box: Boolean,
        size: Double,
        distance: Int,
        ticks: Int,
    ) {
        sender.sendMessage(
            ChatColor.GREEN.toString() + "Showing hitboxes in distance " + distance + " for " +
                toTime(
                    ticks / 20,
                ),
        )
        val rayTrace =
            RayTrace()
                .withEntityFilter { entity: LivingEntity -> entity.entityId == sender.entityId }
                .withRaySize(size)
        if (box) {
            rayTrace.withOutlineHitBox(sender)
        } else {
            rayTrace.withOutlineHitPosition(sender)
        }

        WeaponMechanics.getInstance().foliaScheduler.entity(sender)
            .runAtFixedRate(
                object : Consumer<TaskImplementation<Void>> {
                    var ticker: Int = 0

                    override fun accept(task: TaskImplementation<Void>) {
                        val location = sender.eyeLocation
                        val direction = location.direction

                        rayTrace.cast(sender.world, location.toVector(), direction, distance.toDouble())

                        if (++ticker >= ticks) {
                            task.cancel()
                        }
                    }
                },
                1,
                1,
            )
    }

    fun recoil(
        player: Player,
        recoilMeanX: Float,
        recoilMeanY: Float,
        recoilVarianceX: Float,
        recoilVarianceY: Float,
        recoilSpeed: Float,
        damping: Float,
        dampingRecovery: Float,
        smoothingFactor: Float,
        maxRecoilAccum: Float,
        rate: Int,
        time: Int,
    ) {
        val playerWrapper = WeaponMechanics.getInstance().getPlayerWrapper(player)
        val recoil =
            RecoilProfile(
                recoilMeanX, recoilMeanY, recoilVarianceX, recoilVarianceY, recoilSpeed,
                damping, dampingRecovery, smoothingFactor, maxRecoilAccum, 1f
            )
        val controller = playerWrapper.recoilController

        player.sendMessage(
            ChatColor.GREEN.toString() + "Starting recoil every " + rate +
                " ticks for " + toTime(time / 20),
        )

        WeaponMechanics.getInstance().foliaScheduler.entity(player)
            .runAtFixedRate(
                object : Consumer<TaskImplementation<Void>> {
                    var ticks: Int = 0

                    override fun accept(task: TaskImplementation<Void>) {
                        player.sendActionBar(text("Recoil: $ticks"))
                        controller.onShotFired(recoil, null, null, null, null)

                        ticks += rate
                        if (ticks > time) {
                            task.cancel()
                        }
                    }
                },
                1,
                rate.toLong(),
            )
    }

    fun shoot(
        sender: LivingEntity,
        speed: Double,
        gravity: Double,
        entity: EntityType?,
    ) {
        val projectileSettings =
            ProjectileSettings(
                entity, null,
                gravity, false, -1.0, false,
                -1.0, 0.99, 0.96, 0.98, false, 600, -1.0, 0.1,
            )
        val projectile = Projectile(projectileSettings, null, null, null, null)
        projectile.shoot(sender, sender.eyeLocation, sender.location.direction.multiply(speed), null, null, null)
    }

    fun transform(
        sender: Entity,
        children: Int,
        time: Int,
        speed: Double,
        radius: Double,
        particles: Boolean,
    ) {
        val compatibility = CompatibilityAPI.getEntityCompatibility()

        val parent = Transform()
        parent.parent = EntityTransform(sender)

        val entities: MutableList<FakeEntity> = ArrayList(children * children / 2)
        for (i in 0..<children) {
            val transform = Transform(parent)
            var angle = i * NumberUtil.TAU_DOUBLE / children
            var x = cos(angle) * radius
            var z = sin(angle) * radius

            transform.localPosition = Vector(x, 0.0, z)
            transform.forward = transform.localPosition.normalize().crossProduct(Vector(0, 1, 0)).normalize()

            for (j in 0..<children / 2) {
                angle = j / (children.toDouble() / 2) * NumberUtil.TAU_DOUBLE
                x = cos(angle) * radius / 3.0
                z = sin(angle) * radius / 3.0

                val local = Transform(transform)
                local.localPosition = Vector(x, 0.0, z)

                val entity =
                    compatibility.generateFakeEntity(
                        transform.position.toLocation(sender.world),
                        ItemStack(Material.DIAMOND),
                    )

                entity.setGravity(false)
                entity.show()

                entities.add(entity)
            }
        }

        WeaponMechanics.getInstance().foliaScheduler.entity(sender)
            .runAtFixedRate(
                object : Consumer<TaskImplementation<Void>> {
                    var ticks: Int = 0

                    override fun accept(task: TaskImplementation<Void>) {
                        if (particles) parent.parent.debug(sender.world)

                        val deltaTime = 1.0 / 20.0
                        val rotationSpeed = ticks * speed / time * deltaTime
                        val spin = Quaternion.angleAxis(rotationSpeed, parent.up)
                        // parent.setForward(loc.getDirection());
                        parent.applyRotation(spin)

                        for (i in 0..<children) {
                            val transform = parent.getChild(i)

                            if (particles) transform.debug(sender.world)

                            for (j in 0..<children / 2) {
                                val local = transform.getChild(j)
                                if (particles) local.debug(sender.world)

                                val position = local.position
                                val rotation = local.rotation.eulerAngles

                                val entity = entities[i * children / 2 + j]
                                entity.setPosition(position, rotation.x.toFloat(), rotation.y.toFloat())
                            }
                        }

                        if (ticks++ >= time) {
                            entities.forEach(Consumer { obj: FakeEntity -> obj.remove() })
                            task.cancel()
                        }
                    }
                },
                1,
                1,
            )
    }
}
