package com.hyperperms.registry;

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Manages permission aliases for compatibility between HyperPerms simplified
 * permission nodes and Hytale's actual permission structure.
 * <p>
 * Hytale uses hierarchical permission paths like:
 * <ul>
 *   <li>{@code hytale.command.player.gamemode} (not {@code hytale.command.gamemode})</li>
 *   <li>{@code hytale.command.player.inventory.give} (not {@code hytale.command.give})</li>
 *   <li>{@code hytale.editor.builderTools} (camelCase, not {@code buildertools})</li>
 * </ul>
 * <p>
 * This class provides bidirectional aliasing so that:
 * <ol>
 *   <li>When a user is granted {@code hytale.command.gamemode}, they also get {@code hytale.command.player.gamemode}</li>
 *   <li>Wildcards like {@code hytale.command.*} properly expand to include all actual Hytale permissions</li>
 * </ol>
 */
public final class PermissionAliases {

    private static PermissionAliases instance;

    /**
     * Maps simplified/friendly permissions to their actual Hytale equivalents.
     * One simplified permission can map to multiple actual permissions.
     */
    private final Map<String, Set<String>> aliasToActual = new HashMap<>();

    /**
     * Maps actual Hytale permissions back to their simplified aliases.
     * Used for reverse lookups.
     */
    private final Map<String, Set<String>> actualToAlias = new HashMap<>();

    /**
     * Gets the singleton instance.
     */
    @NotNull
    public static PermissionAliases getInstance() {
        if (instance == null) {
            instance = new PermissionAliases();
        }
        return instance;
    }

    private PermissionAliases() {
        registerAllAliases();
    }

    /**
     * Registers all permission aliases.
     */
    private void registerAllAliases() {
        // ==================== Legacy System Command Format ====================
        // Format: hytale.system.command.* → hytale.command.* (used by some older configs)
        alias("hytale.system.command.gamemode", "hytale.command.player.gamemode", "hytale.command.gamemode", "hytale.command.gamemode.self");
        alias("hytale.system.command.give", "hytale.command.player.inventory.give", "hytale.command.give");
        alias("hytale.system.command.kick", "hytale.command.server.kick", "hytale.command.kick");
        alias("hytale.system.command.ban", "hytale.command.server.ban", "hytale.command.ban");
        alias("hytale.system.command.unban", "hytale.command.server.unban", "hytale.command.unban");
        alias("hytale.system.command.kill", "hytale.command.player.kill", "hytale.command.kill");
        alias("hytale.system.command.heal", "hytale.command.player.effect.apply", "hytale.command.heal");
        alias("hytale.system.command.tp", "hytale.command.player.teleport", "hytale.command.tp");
        alias("hytale.system.command.teleport", "hytale.command.player.teleport", "hytale.command.teleport");
        alias("hytale.system.command.spawn", "hytale.command.world.spawnblock", "hytale.command.spawn");
        alias("hytale.system.command.stop", "hytale.command.server.stop", "hytale.command.stop");
        alias("hytale.system.command.op", "hytale.command.op", "hytale.command.op");
        alias("hytale.system.command.opadd", "hytale.command.op.add", "hytale.command.opadd");
        alias("hytale.system.command.opremove", "hytale.command.op.remove", "hytale.command.opremove");
        alias("hytale.system.command.who", "hytale.command.server.who", "hytale.command.who");
        alias("hytale.system.command.help", "hytale.command.utility.help", "hytale.command.help");
        alias("hytale.system.command.version", "hytale.command.debug.version", "hytale.command.version");
        alias("hytale.system.command.ping", "hytale.command.debug.ping", "hytale.command.ping");
        alias("hytale.system.command.time", "hytale.command.world.time", "hytale.command.time");
        alias("hytale.system.command.weather", "hytale.command.world.weather", "hytale.command.weather");
        alias("hytale.system.command.clear", "hytale.command.player.inventory.clear", "hytale.command.clear");
        alias("hytale.system.command.effect", "hytale.command.player.effect.apply", "hytale.command.effect");
        alias("hytale.system.command.emote", "hytale.command.emote");
        alias("hytale.system.command.plugin", "hytale.command.plugin");
        alias("hytale.system.command.backup", "hytale.command.utility.backup", "hytale.command.backup");

        // ==================== Hytale Editor Permissions ====================
        // Case sensitivity matters!
        alias("hytale.editor.buildertools", "hytale.editor.builderTools");

        // Additional editor permissions from JAR
        alias("hytale.editor.asset", "hytale.editor.asset");
        alias("hytale.editor.packs.create", "hytale.editor.packs.create");
        alias("hytale.editor.packs.edit", "hytale.editor.packs.edit");
        alias("hytale.editor.packs.delete", "hytale.editor.packs.delete");
        alias("hytale.editor.brush.config", "hytale.editor.brush.config");
        alias("hytale.editor.prefab.manage", "hytale.editor.prefab.manage");
        alias("hytale.editor.selection.modify", "hytale.editor.selection.modify");

        // ==================== Player Commands ====================
        // gamemode - Hytale checks for hytale.command.gamemode.self
        alias("hytale.command.gamemode", "hytale.command.player.gamemode", "hytale.command.gamemode.self");
        alias("hytale.command.gamemode.self", "hytale.command.player.gamemode");
        alias("hytale.command.gamemode.others", "hytale.command.player.gamemode");
        alias("hytale.command.gamemode.survival", "hytale.command.player.gamemode", "hytale.command.gamemode.self");
        alias("hytale.command.gamemode.creative", "hytale.command.player.gamemode", "hytale.command.gamemode.self");
        alias("hytale.command.gamemode.adventure", "hytale.command.player.gamemode", "hytale.command.gamemode.self");
        alias("hytale.command.gamemode.spectator", "hytale.command.player.gamemode", "hytale.command.gamemode.self");

        // give / inventory
        alias("hytale.command.give", "hytale.command.player.inventory.give");
        alias("hytale.command.give.self", "hytale.command.player.inventory.give");
        alias("hytale.command.give.others", "hytale.command.player.inventory.give");
        alias("hytale.command.clear", "hytale.command.player.inventory.clear");

        // kill / damage
        alias("hytale.command.kill", "hytale.command.player.kill");
        alias("hytale.command.damage", "hytale.command.player.damage");
        alias("hytale.command.heal", "hytale.command.player.effect.apply");

        // hide
        alias("hytale.command.hide", "hytale.command.player.hide");

        // ==================== Server Commands ====================
        alias("hytale.command.kick", "hytale.command.server.kick");
        alias("hytale.command.stop", "hytale.command.server.stop");
        alias("hytale.command.who", "hytale.command.server.who");
        alias("hytale.command.maxplayers", "hytale.command.server.maxplayers");

        // ==================== Utility Commands ====================
        alias("hytale.command.backup", "hytale.command.utility.backup");
        alias("hytale.command.help", "hytale.command.utility.help");

        // ==================== World Commands ====================
        alias("hytale.command.spawn", "hytale.command.world.spawnblock");
        alias("hytale.command.spawn.teleport", "hytale.command.world.spawnblock");
        alias("hytale.command.spawn.set", "hytale.command.world.spawnblock");

        // ==================== Permissions Commands ====================
        alias("hytale.command.op", "hytale.command.op");
        alias("hytale.command.op.self", "hytale.command.op.self");
        alias("hytale.command.op.add", "hytale.command.op.add");
        alias("hytale.command.op.remove", "hytale.command.op.remove");
        alias("hytale.command.perm", "hytale.command.perm");

        // ==================== Plugin Commands ====================
        alias("hytale.command.plugin", "hytale.command.plugin");
        alias("hytale.command.plugin.list", "hytale.command.plugin.list");
        alias("hytale.command.plugin.load", "hytale.command.plugin.load");
        alias("hytale.command.plugin.unload", "hytale.command.plugin.unload");
        alias("hytale.command.plugin.reload", "hytale.command.plugin.reload");

        // ==================== Debug Commands ====================
        alias("hytale.command.version", "hytale.command.debug.version");
        alias("hytale.command.ping", "hytale.command.debug.ping");

        // ==================== Other Commands ====================
        alias("hytale.command.emote", "hytale.command.emote");
        alias("hytale.command.whereami", "hytale.command.player.whereami");
        alias("hytale.command.whoami", "hytale.command.player.whoami");

        // ==================== Wildcard Mappings ====================
        // These ensure that wildcards expand to include all relevant permissions
        aliasWildcard("hytale.command.gamemode.*",
            "hytale.command.player.gamemode");
        aliasWildcard("hytale.command.give.*",
            "hytale.command.player.inventory.give",
            "hytale.command.player.inventory.givearmor");
        aliasWildcard("hytale.command.inventory.*",
            "hytale.command.player.inventory",
            "hytale.command.player.inventory.give",
            "hytale.command.player.inventory.givearmor",
            "hytale.command.player.inventory.clear",
            "hytale.command.player.inventory.backpack",
            "hytale.command.player.inventory.item",
            "hytale.command.player.inventory.see",
            "hytale.command.player.inventory.itemstate");
        aliasWildcard("hytale.command.player.*",
            "hytale.command.player",
            "hytale.command.player.gamemode",
            "hytale.command.player.damage",
            "hytale.command.player.kill",
            "hytale.command.player.hide",
            "hytale.command.player.reset",
            "hytale.command.player.respawn",
            "hytale.command.player.zone",
            "hytale.command.player.refer",
            "hytale.command.player.sudo",
            "hytale.command.player.whereami",
            "hytale.command.player.whoami",
            "hytale.command.player.toggleblockplacementoverride",
            "hytale.command.player.camera",
            "hytale.command.player.camera.reset",
            "hytale.command.player.camera.sidescroller",
            "hytale.command.player.camera.topdown",
            "hytale.command.player.effect",
            "hytale.command.player.effect.apply",
            "hytale.command.player.effect.clear",
            "hytale.command.player.inventory",
            "hytale.command.player.inventory.give",
            "hytale.command.player.inventory.givearmor",
            "hytale.command.player.inventory.clear",
            "hytale.command.player.inventory.backpack",
            "hytale.command.player.inventory.item",
            "hytale.command.player.inventory.see",
            "hytale.command.player.inventory.itemstate",
            "hytale.command.player.stats",
            "hytale.command.player.stats.add",
            "hytale.command.player.stats.get",
            "hytale.command.player.stats.set",
            "hytale.command.player.stats.reset",
            "hytale.command.player.stats.dump",
            "hytale.command.player.stats.settomax",
            "hytale.command.player.viewradius",
            "hytale.command.player.viewradius.get",
            "hytale.command.player.viewradius.set");
        aliasWildcard("hytale.command.server.*",
            "hytale.command.server",
            "hytale.command.server.kick",
            "hytale.command.server.stop",
            "hytale.command.server.who",
            "hytale.command.server.maxplayers",
            "hytale.command.server.auth",
            "hytale.command.server.auth.login",
            "hytale.command.server.auth.logout",
            "hytale.command.server.auth.status",
            "hytale.command.server.auth.cancel",
            "hytale.command.server.auth.select",
            "hytale.command.server.auth.persistence");
        aliasWildcard("hytale.command.utility.*",
            "hytale.command.utility.backup",
            "hytale.command.utility.help",
            "hytale.command.utility.convertprefabs",
            "hytale.command.utility.eventtitle",
            "hytale.command.utility.notify",
            "hytale.command.utility.stash",
            "hytale.command.utility.validatecpb",
            "hytale.command.utility.lighting",
            "hytale.command.utility.lighting.get",
            "hytale.command.utility.lighting.info",
            "hytale.command.utility.lighting.send",
            "hytale.command.utility.lighting.calculation",
            "hytale.command.utility.lighting.invalidate",
            "hytale.command.utility.lighting.sendtoggle",
            "hytale.command.utility.sleep",
            "hytale.command.utility.sleep.offset",
            "hytale.command.utility.sleep.test",
            "hytale.command.utility.sound",
            "hytale.command.utility.sound.play2d",
            "hytale.command.utility.sound.play3d",
            "hytale.command.utility.worldmap",
            "hytale.command.utility.worldmap.discover",
            "hytale.command.utility.worldmap.undiscover",
            "hytale.command.utility.worldmap.reload",
            "hytale.command.utility.worldmap.clearmarkers",
            "hytale.command.utility.git.update",
            "hytale.command.utility.git.updateassets",
            "hytale.command.utility.git.updateprefabs",
            "hytale.command.utility.metacommands.commands",
            "hytale.command.utility.metacommands.dumpcommands",
            "hytale.command.utility.net.network");
        aliasWildcard("hytale.command.world.*",
            "hytale.command.world.spawnblock",
            "hytale.command.world.chunk",
            "hytale.command.world.chunk.info",
            "hytale.command.world.chunk.load",
            "hytale.command.world.chunk.unload",
            "hytale.command.world.chunk.loaded",
            "hytale.command.world.chunk.resend",
            "hytale.command.world.chunk.regenerate",
            "hytale.command.world.chunk.tint",
            "hytale.command.world.chunk.lighting",
            "hytale.command.world.chunk.tracker",
            "hytale.command.world.chunk.forcetick",
            "hytale.command.world.chunk.marksave",
            "hytale.command.world.chunk.maxsendrate",
            "hytale.command.world.chunk.fixheightmap",
            "hytale.command.world.entity",
            "hytale.command.world.entity.clean",
            "hytale.command.world.entity.clone",
            "hytale.command.world.entity.count",
            "hytale.command.world.entity.dump",
            "hytale.command.world.entity.effect",
            "hytale.command.world.entity.remove",
            "hytale.command.world.entity.resend",
            "hytale.command.world.entity.tracker",
            "hytale.command.world.entity.lod",
            "hytale.command.world.entity.nameplate",
            "hytale.command.world.entity.intangible",
            "hytale.command.world.entity.invulnerable",
            "hytale.command.world.entity.makeinteractable",
            "hytale.command.world.entity.hidefromadventureplayers",
            "hytale.command.world.worldgen",
            "hytale.command.world.worldgen.reload",
            "hytale.command.world.worldgen.benchmark");
        aliasWildcard("hytale.command.debug.*",
            "hytale.command.debug.version",
            "hytale.command.debug.ping",
            "hytale.command.debug.log",
            "hytale.command.debug.assets",
            "hytale.command.debug.assetsduplicates",
            "hytale.command.debug.assettags",
            "hytale.command.debug.packetstats",
            "hytale.command.debug.pidcheck",
            "hytale.command.debug.hitdetection",
            "hytale.command.debug.hudmanagertest",
            "hytale.command.debug.tagpattern",
            "hytale.command.debug.debugplayerposition",
            "hytale.command.debug.showbuildertoolshud",
            "hytale.command.debug.stopnetworkchunksending",
            "hytale.command.debug.messagetranslationtest",
            "hytale.command.debug.packs",
            "hytale.command.debug.packs.list",
            "hytale.command.debug.server",
            "hytale.command.debug.server.gc",
            "hytale.command.debug.server.dump",
            "hytale.command.debug.server.stats",
            "hytale.command.debug.stresstest",
            "hytale.command.debug.stresstest.start",
            "hytale.command.debug.stresstest.stop",
            "hytale.command.debug.component.hitboxcollision",
            "hytale.command.debug.component.hitboxcollision.add",
            "hytale.command.debug.component.hitboxcollision.remove",
            "hytale.command.debug.component.repulsion",
            "hytale.command.debug.component.repulsion.add",
            "hytale.command.debug.component.repulsion.remove");
        aliasWildcard("hytale.editor.*",
            "hytale.editor.asset",
            "hytale.editor.builderTools",
            "hytale.editor.history",
            "hytale.editor.brush.use",
            "hytale.editor.brush.config",
            "hytale.editor.prefab.use",
            "hytale.editor.prefab.manage",
            "hytale.editor.selection.use",
            "hytale.editor.selection.clipboard",
            "hytale.editor.selection.modify",
            "hytale.editor.packs.create",
            "hytale.editor.packs.edit",
            "hytale.editor.packs.delete");

        // ==================== Additional Web Editor Permissions ====================
        // Moderation commands
        alias("hytale.command.ban", "hytale.command.server.ban");
        alias("hytale.command.unban", "hytale.command.server.unban");
        alias("hytale.command.opadd", "hytale.command.op.add");
        alias("hytale.command.opremove", "hytale.command.op.remove");
        alias("hytale.command.opself", "hytale.command.op.self");

        // Whitelist commands
        alias("hytale.command.whitelist", "hytale.command.server.whitelist");
        alias("hytale.command.whitelist.add", "hytale.command.server.whitelist.add");
        alias("hytale.command.whitelist.remove", "hytale.command.server.whitelist.remove");
        alias("hytale.command.whitelist.list", "hytale.command.server.whitelist.list");
        alias("hytale.command.whitelist.on", "hytale.command.server.whitelist.on");
        alias("hytale.command.whitelist.off", "hytale.command.server.whitelist.off");

        // Teleportation commands
        alias("hytale.command.tp", "hytale.command.player.teleport");
        alias("hytale.command.teleport", "hytale.command.player.teleport");
        alias("hytale.command.tpall", "hytale.command.player.teleport.all");
        alias("hytale.command.tpback", "hytale.command.player.teleport.back");
        alias("hytale.command.tpforward", "hytale.command.player.teleport.forward");
        alias("hytale.command.tptop", "hytale.command.player.teleport.top");
        alias("hytale.command.tphome", "hytale.command.player.teleport.home");
        alias("hytale.command.spawnset", "hytale.command.world.spawnblock");

        // Building/Editor commands (simplified -> actual)
        alias("hytale.command.fill", "hytale.editor.selection.fill");
        alias("hytale.command.walls", "hytale.editor.selection.walls");
        alias("hytale.command.hollow", "hytale.editor.selection.hollow");
        alias("hytale.command.replace", "hytale.editor.selection.replace");
        alias("hytale.command.copy", "hytale.editor.selection.clipboard");
        alias("hytale.command.cut", "hytale.editor.selection.clipboard");
        alias("hytale.command.paste", "hytale.editor.selection.clipboard");
        alias("hytale.command.undo", "hytale.editor.history");
        alias("hytale.command.redo", "hytale.editor.history");
        alias("hytale.command.pos1", "hytale.editor.selection.use");
        alias("hytale.command.pos2", "hytale.editor.selection.use");
        alias("hytale.command.brush", "hytale.editor.brush.use");
        alias("hytale.command.prefab", "hytale.editor.prefab.use");

        // Entity commands
        alias("hytale.command.entity", "hytale.command.world.entity");
        alias("hytale.command.entity.spawn", "hytale.command.world.entity.spawn");
        alias("hytale.command.entity.remove", "hytale.command.world.entity.remove");
        alias("hytale.command.entity.clone", "hytale.command.world.entity.clone");
        alias("hytale.command.npc", "hytale.command.world.entity.npc");
        alias("hytale.command.mount", "hytale.command.player.mount");
        alias("hytale.command.dismount", "hytale.command.player.dismount");
        alias("hytale.command.hitbox", "hytale.command.debug.component.hitboxcollision");

        // World commands
        alias("hytale.command.worldgen", "hytale.command.world.worldgen");
        alias("hytale.command.chunk", "hytale.command.world.chunk");
        alias("hytale.command.warp", "hytale.command.world.warp");
        alias("hytale.command.time", "hytale.command.world.time");
        alias("hytale.command.time.set", "hytale.command.world.time.set");
        alias("hytale.command.time.get", "hytale.command.world.time.get");
        alias("hytale.command.weather", "hytale.command.world.weather");
        alias("hytale.command.weather.set", "hytale.command.world.weather.set");
        alias("hytale.command.weather.get", "hytale.command.world.weather.get");

        // Audio/Visual commands
        alias("hytale.command.sound", "hytale.command.utility.sound");
        alias("hytale.command.sound.play", "hytale.command.utility.sound.play2d", "hytale.command.utility.sound.play3d");
        alias("hytale.command.particle", "hytale.command.utility.particle");
        alias("hytale.command.ambience", "hytale.command.utility.ambience");
        alias("hytale.command.tint", "hytale.command.world.chunk.tint");
        alias("hytale.command.lighting", "hytale.command.utility.lighting");

        // Server/Debug commands
        alias("hytale.command.debug", "hytale.command.debug");
        alias("hytale.command.log", "hytale.command.debug.log");
        alias("hytale.command.network", "hytale.command.utility.net.network");
        alias("hytale.command.stresstest", "hytale.command.debug.stresstest");

        // Player state commands
        alias("hytale.command.player", "hytale.command.player");
        alias("hytale.command.inventory", "hytale.command.player.inventory");
        alias("hytale.command.sleep", "hytale.command.utility.sleep");
        alias("hytale.command.effect", "hytale.command.player.effect");
        alias("hytale.command.stats", "hytale.command.player.stats");

        // Instance/Auth commands
        alias("hytale.command.instance", "hytale.command.server.instance");
        alias("hytale.command.auth", "hytale.command.server.auth");
        alias("hytale.command.hub", "hytale.command.server.hub");
        alias("hytale.command.leave", "hytale.command.server.leave");

        // ==================== HyperPerms Plugin Permissions ====================
        // These ensure hyperperms.* wildcard properly expands to all HyperPerms permissions
        aliasWildcard("hyperperms.*",
            // Command wildcards
            "hyperperms.command.*",
            "hyperperms.admin.*",
            "hyperperms.chat.*",
            // User commands
            "hyperperms.command.user.info",
            "hyperperms.command.user.info.others",
            "hyperperms.command.user.permission",
            "hyperperms.command.user.group",
            "hyperperms.command.user.promote",
            "hyperperms.command.user.demote",
            "hyperperms.command.user.clear",
            "hyperperms.command.user.clone",
            "hyperperms.command.user.setprefix",
            "hyperperms.command.user.setsuffix",
            // Group commands
            "hyperperms.command.group.info",
            "hyperperms.command.group.list",
            "hyperperms.command.group.create",
            "hyperperms.command.group.delete",
            "hyperperms.command.group.permission",
            "hyperperms.command.group.parent",
            "hyperperms.command.group.modify",
            "hyperperms.command.group.rename",
            "hyperperms.command.group.setweight",
            "hyperperms.command.group.setprefix",
            "hyperperms.command.group.setsuffix",
            "hyperperms.command.group.setdisplayname",
            // Track commands
            "hyperperms.command.track.info",
            "hyperperms.command.track.list",
            "hyperperms.command.track.create",
            "hyperperms.command.track.delete",
            "hyperperms.command.track.modify",
            "hyperperms.command.track.append",
            "hyperperms.command.track.insert",
            "hyperperms.command.track.remove",
            // Admin commands
            "hyperperms.command.reload",
            "hyperperms.command.verbose",
            "hyperperms.command.cache",
            "hyperperms.command.export",
            "hyperperms.command.import",
            "hyperperms.command.listgroups",
            "hyperperms.command.listtracks",
            "hyperperms.command.check",
            "hyperperms.command.check.self",
            "hyperperms.command.check.others",
            // Editor commands
            "hyperperms.command.editor",
            "hyperperms.command.apply",
            // Debug commands
            "hyperperms.command.debug",
            "hyperperms.command.debug.tree",
            "hyperperms.command.debug.resolve",
            "hyperperms.command.debug.contexts",
            // Backup commands
            "hyperperms.command.backup",
            "hyperperms.command.backup.create",
            "hyperperms.command.backup.list",
            "hyperperms.command.backup.restore",
            "hyperperms.command.backup.delete",
            // Permission registry commands
            "hyperperms.command.perms",
            "hyperperms.command.perms.list",
            "hyperperms.command.perms.search",
            // Chat permissions
            "hyperperms.chat.color",
            "hyperperms.chat.color.hex",
            "hyperperms.chat.format",
            "hyperperms.chat.format.bold",
            "hyperperms.chat.format.italic",
            "hyperperms.chat.format.underline",
            "hyperperms.chat.format.strikethrough",
            "hyperperms.chat.format.magic",
            "hyperperms.chat.links",
            "hyperperms.chat.bypass.cooldown",
            "hyperperms.chat.bypass.filter",
            "hyperperms.chat.broadcast",
            "hyperperms.chat.pm",
            "hyperperms.chat.pm.receive",
            "hyperperms.chat.socialspy");

        aliasWildcard("hyperperms.command.*",
            "hyperperms.command.user.info",
            "hyperperms.command.user.info.others",
            "hyperperms.command.user.permission",
            "hyperperms.command.user.group",
            "hyperperms.command.user.promote",
            "hyperperms.command.user.demote",
            "hyperperms.command.user.clear",
            "hyperperms.command.user.clone",
            "hyperperms.command.user.setprefix",
            "hyperperms.command.user.setsuffix",
            "hyperperms.command.group.info",
            "hyperperms.command.group.list",
            "hyperperms.command.group.create",
            "hyperperms.command.group.delete",
            "hyperperms.command.group.permission",
            "hyperperms.command.group.parent",
            "hyperperms.command.group.modify",
            "hyperperms.command.group.rename",
            "hyperperms.command.group.setweight",
            "hyperperms.command.group.setprefix",
            "hyperperms.command.group.setsuffix",
            "hyperperms.command.group.setdisplayname",
            "hyperperms.command.track.info",
            "hyperperms.command.track.list",
            "hyperperms.command.track.create",
            "hyperperms.command.track.delete",
            "hyperperms.command.track.modify",
            "hyperperms.command.track.append",
            "hyperperms.command.track.insert",
            "hyperperms.command.track.remove",
            "hyperperms.command.reload",
            "hyperperms.command.verbose",
            "hyperperms.command.cache",
            "hyperperms.command.export",
            "hyperperms.command.import",
            "hyperperms.command.listgroups",
            "hyperperms.command.listtracks",
            "hyperperms.command.check",
            "hyperperms.command.check.self",
            "hyperperms.command.check.others",
            "hyperperms.command.editor",
            "hyperperms.command.apply",
            "hyperperms.command.debug",
            "hyperperms.command.debug.tree",
            "hyperperms.command.debug.resolve",
            "hyperperms.command.debug.contexts",
            "hyperperms.command.backup",
            "hyperperms.command.backup.create",
            "hyperperms.command.backup.list",
            "hyperperms.command.backup.restore",
            "hyperperms.command.backup.delete",
            "hyperperms.command.perms",
            "hyperperms.command.perms.list",
            "hyperperms.command.perms.search");

        aliasWildcard("hyperperms.command.user.*",
            "hyperperms.command.user.info",
            "hyperperms.command.user.info.others",
            "hyperperms.command.user.permission",
            "hyperperms.command.user.group",
            "hyperperms.command.user.promote",
            "hyperperms.command.user.demote",
            "hyperperms.command.user.clear",
            "hyperperms.command.user.clone",
            "hyperperms.command.user.setprefix",
            "hyperperms.command.user.setsuffix");

        aliasWildcard("hyperperms.command.group.*",
            "hyperperms.command.group.info",
            "hyperperms.command.group.list",
            "hyperperms.command.group.create",
            "hyperperms.command.group.delete",
            "hyperperms.command.group.permission",
            "hyperperms.command.group.parent",
            "hyperperms.command.group.modify",
            "hyperperms.command.group.rename",
            "hyperperms.command.group.setweight",
            "hyperperms.command.group.setprefix",
            "hyperperms.command.group.setsuffix",
            "hyperperms.command.group.setdisplayname");

        aliasWildcard("hyperperms.command.track.*",
            "hyperperms.command.track.info",
            "hyperperms.command.track.list",
            "hyperperms.command.track.create",
            "hyperperms.command.track.delete",
            "hyperperms.command.track.modify",
            "hyperperms.command.track.append",
            "hyperperms.command.track.insert",
            "hyperperms.command.track.remove");

        aliasWildcard("hyperperms.command.backup.*",
            "hyperperms.command.backup.create",
            "hyperperms.command.backup.list",
            "hyperperms.command.backup.restore",
            "hyperperms.command.backup.delete");

        aliasWildcard("hyperperms.command.debug.*",
            "hyperperms.command.debug.tree",
            "hyperperms.command.debug.resolve",
            "hyperperms.command.debug.contexts");

        aliasWildcard("hyperperms.command.perms.*",
            "hyperperms.command.perms.list",
            "hyperperms.command.perms.search");

        aliasWildcard("hyperperms.chat.*",
            "hyperperms.chat.color",
            "hyperperms.chat.color.hex",
            "hyperperms.chat.format",
            "hyperperms.chat.format.bold",
            "hyperperms.chat.format.italic",
            "hyperperms.chat.format.underline",
            "hyperperms.chat.format.strikethrough",
            "hyperperms.chat.format.magic",
            "hyperperms.chat.links",
            "hyperperms.chat.bypass.cooldown",
            "hyperperms.chat.bypass.filter",
            "hyperperms.chat.broadcast",
            "hyperperms.chat.pm",
            "hyperperms.chat.pm.receive",
            "hyperperms.chat.socialspy");

        // ==================== HyperHomes Plugin ====================
        alias("hyperhomes.homes", "com.hyperhomes.hyperhomes.command.homes");
        alias("hyperhomes.home", "com.hyperhomes.hyperhomes.command.home");
        alias("hyperhomes.sethome", "com.hyperhomes.hyperhomes.command.sethome");
        alias("hyperhomes.delhome", "com.hyperhomes.hyperhomes.command.delhome");
        alias("hyperhomes.gui", "com.hyperhomes.hyperhomes.command.homes.gui");
        alias("hyperhomes.use",
            "com.hyperhomes.hyperhomes.command.homes",
            "com.hyperhomes.hyperhomes.command.home",
            "com.hyperhomes.hyperhomes.command.homes.gui");
        alias("hyperhomes.list",
            "com.hyperhomes.hyperhomes.command.homes",
            "com.hyperhomes.hyperhomes.command.homes.list");
        alias("hyperhomes.set",
            "com.hyperhomes.hyperhomes.command.sethome",
            "com.hyperhomes.hyperhomes.command.homes.set");
        alias("hyperhomes.delete",
            "com.hyperhomes.hyperhomes.command.delhome",
            "com.hyperhomes.hyperhomes.command.homes.delete");
        alias("hyperhomes.teleport", "com.hyperhomes.hyperhomes.command.home");
        aliasWildcard("hyperhomes.*",
            "com.hyperhomes.hyperhomes.command.homes",
            "com.hyperhomes.hyperhomes.command.home",
            "com.hyperhomes.hyperhomes.command.sethome",
            "com.hyperhomes.hyperhomes.command.delhome",
            "com.hyperhomes.hyperhomes.command.homes.gui",
            "com.hyperhomes.hyperhomes.command.homes.list",
            "com.hyperhomes.hyperhomes.command.homes.set",
            "com.hyperhomes.hyperhomes.command.homes.delete");

        // ==================== HyperWarps Plugin ====================
        alias("hyperwarps.warp", "com.hyperwarps.hyperwarps.command.warp");
        alias("hyperwarps.warps", "com.hyperwarps.hyperwarps.command.warps");
        alias("hyperwarps.setwarp", "com.hyperwarps.hyperwarps.command.setwarp");
        alias("hyperwarps.delwarp", "com.hyperwarps.hyperwarps.command.delwarp");
        alias("hyperwarps.warpinfo", "com.hyperwarps.hyperwarps.command.warpinfo");
        alias("hyperwarps.spawn", "com.hyperwarps.hyperwarps.command.spawn");
        alias("hyperwarps.spawns", "com.hyperwarps.hyperwarps.command.spawns");
        alias("hyperwarps.setspawn", "com.hyperwarps.hyperwarps.command.setspawn");
        alias("hyperwarps.delspawn", "com.hyperwarps.hyperwarps.command.delspawn");
        alias("hyperwarps.spawninfo", "com.hyperwarps.hyperwarps.command.spawninfo");
        alias("hyperwarps.tpa", "com.hyperwarps.hyperwarps.command.tpa");
        alias("hyperwarps.tpahere", "com.hyperwarps.hyperwarps.command.tpahere");
        alias("hyperwarps.tpaccept", "com.hyperwarps.hyperwarps.command.tpaccept");
        alias("hyperwarps.tpdeny", "com.hyperwarps.hyperwarps.command.tpdeny");
        alias("hyperwarps.tpcancel", "com.hyperwarps.hyperwarps.command.tpcancel");
        alias("hyperwarps.tptoggle", "com.hyperwarps.hyperwarps.command.tptoggle");
        alias("hyperwarps.back", "com.hyperwarps.hyperwarps.command.back");
        aliasWildcard("hyperwarps.*",
            "com.hyperwarps.hyperwarps.command.warp",
            "com.hyperwarps.hyperwarps.command.warps",
            "com.hyperwarps.hyperwarps.command.setwarp",
            "com.hyperwarps.hyperwarps.command.delwarp",
            "com.hyperwarps.hyperwarps.command.warpinfo",
            "com.hyperwarps.hyperwarps.command.spawn",
            "com.hyperwarps.hyperwarps.command.spawns",
            "com.hyperwarps.hyperwarps.command.setspawn",
            "com.hyperwarps.hyperwarps.command.delspawn",
            "com.hyperwarps.hyperwarps.command.spawninfo",
            "com.hyperwarps.hyperwarps.command.tpa",
            "com.hyperwarps.hyperwarps.command.tpahere",
            "com.hyperwarps.hyperwarps.command.tpaccept",
            "com.hyperwarps.hyperwarps.command.tpdeny",
            "com.hyperwarps.hyperwarps.command.tpcancel",
            "com.hyperwarps.hyperwarps.command.tptoggle",
            "com.hyperwarps.hyperwarps.command.back");
    }

    /**
     * Registers a simple alias mapping.
     */
    private void alias(String simplified, String... actual) {
        String lowerSimplified = simplified.toLowerCase();
        Set<String> actuals = aliasToActual.computeIfAbsent(lowerSimplified, k -> new HashSet<>());
        for (String a : actual) {
            actuals.add(a.toLowerCase());
            actualToAlias.computeIfAbsent(a.toLowerCase(), k -> new HashSet<>()).add(lowerSimplified);
        }
    }

    /**
     * Registers a wildcard alias mapping.
     */
    private void aliasWildcard(String wildcardPattern, String... actual) {
        alias(wildcardPattern, actual);
    }

    /**
     * Gets all actual permissions that a simplified permission maps to.
     *
     * @param permission the permission (may be simplified or actual)
     * @return set of actual Hytale permissions, empty if no aliases exist
     */
    @NotNull
    public Set<String> getActualPermissions(@NotNull String permission) {
        String lowerPerm = permission.toLowerCase();
        Set<String> actuals = aliasToActual.get(lowerPerm);
        return actuals != null ? Collections.unmodifiableSet(actuals) : Collections.emptySet();
    }

    /**
     * Gets all simplified aliases for an actual Hytale permission.
     *
     * @param permission the actual Hytale permission
     * @return set of simplified aliases, empty if no aliases exist
     */
    @NotNull
    public Set<String> getAliases(@NotNull String permission) {
        String lowerPerm = permission.toLowerCase();
        Set<String> aliases = actualToAlias.get(lowerPerm);
        return aliases != null ? Collections.unmodifiableSet(aliases) : Collections.emptySet();
    }

    /**
     * Expands a permission to include all its aliases.
     * This should be called when building the expanded permission set.
     *
     * @param permission the permission to expand
     * @return set containing the original permission and all its aliases
     */
    @NotNull
    public Set<String> expand(@NotNull String permission) {
        Set<String> result = new HashSet<>();
        String lowerPerm = permission.toLowerCase();
        result.add(lowerPerm);

        // Add all actual permissions this alias maps to
        Set<String> actuals = aliasToActual.get(lowerPerm);
        if (actuals != null) {
            result.addAll(actuals);
        }

        return result;
    }

    /**
     * Checks if a permission has any aliases.
     *
     * @param permission the permission to check
     * @return true if aliases exist
     */
    public boolean hasAliases(@NotNull String permission) {
        String lowerPerm = permission.toLowerCase();
        return aliasToActual.containsKey(lowerPerm) || actualToAlias.containsKey(lowerPerm);
    }

    /**
     * Gets all registered alias mappings.
     *
     * @return unmodifiable map of simplified -> actual permissions
     */
    @NotNull
    public Map<String, Set<String>> getAllAliases() {
        Map<String, Set<String>> result = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : aliasToActual.entrySet()) {
            result.put(entry.getKey(), Collections.unmodifiableSet(entry.getValue()));
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Clears and reloads all aliases.
     */
    public void reload() {
        aliasToActual.clear();
        actualToAlias.clear();
        registerAllAliases();
    }
}
