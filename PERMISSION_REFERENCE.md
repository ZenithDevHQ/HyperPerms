# HyperPerms - Comprehensive Permission Reference

## Current State Analysis

### Existing Permission Nodes (Permissions.java)

| Category | Permission | Purpose |
|----------|------------|---------|
| **Base** | `hyperperms.command` | Base permission for all commands |
| **Admin** | `hyperperms.*` | Full admin access |
| **User Commands** | `hyperperms.command.user.info` | View user information |
| | `hyperperms.command.user.permission` | Modify user permissions |
| | `hyperperms.command.user.group` | Modify user groups |
| | `hyperperms.command.user.promote` | Promote users |
| | `hyperperms.command.user.demote` | Demote users |
| | `hyperperms.command.user.clear` | Clear user permissions |
| | `hyperperms.command.user.clone` | Clone user permissions |
| **Group Commands** | `hyperperms.command.group.info` | View group information |
| | `hyperperms.command.group.create` | Create groups |
| | `hyperperms.command.group.delete` | Delete groups |
| | `hyperperms.command.group.permission` | Modify group permissions |
| | `hyperperms.command.group.parent` | Modify group parents |
| | `hyperperms.command.group.modify` | Modify group settings |
| **Track Commands** | `hyperperms.command.track.info` | View track information |
| | `hyperperms.command.track.create` | Create tracks |
| | `hyperperms.command.track.delete` | Delete tracks |
| | `hyperperms.command.track.modify` | Modify tracks |
| **Admin Commands** | `hyperperms.command.reload` | Reload plugin |
| | `hyperperms.command.verbose` | Verbose mode |
| | `hyperperms.command.cache` | Cache management |
| | `hyperperms.command.export` | Export data |
| | `hyperperms.command.import` | Import data |
| | `hyperperms.command.listgroups` | List all groups |
| | `hyperperms.command.listtracks` | List all tracks |
| **Web Editor** | `hyperperms.command.editor` | Use web editor |
| | `hyperperms.command.apply` | Apply web editor changes |
| **Chat** | `hyperperms.chat.color` | Use colors in chat |

### Existing Context Calculators

| Context Key | Values | Purpose |
|-------------|--------|---------|
| `world` | World name (e.g., `nether`, `end`) | World-specific permissions |
| `gamemode` | `survival`, `creative`, `adventure`, `spectator` | Game mode-specific permissions |
| `server` | Server name from config | Multi-server networks |

---

## Gap Analysis

### Missing Permission Categories

Based on Hytale's server architecture and standard voxel game requirements:

#### 1. Player Actions (HIGH PRIORITY)
- Movement permissions (fly, speed)
- Combat permissions (PvP, damage)
- Interaction permissions (use items, interact with blocks)

#### 2. World Modification (HIGH PRIORITY)
- Block placement/breaking
- World protection
- Build permissions

#### 3. Chat & Communication (MEDIUM PRIORITY)
- Chat formatting beyond colors
- Private messaging
- Broadcasts

#### 4. Economy/Trading (MEDIUM PRIORITY)
- Trading permissions
- Shop access
- Currency management

#### 5. Teleportation (HIGH PRIORITY)
- Teleport commands
- Warps
- Spawn access

#### 6. Entity Interactions (MEDIUM PRIORITY)
- NPC interactions
- Mob spawning
- Vehicle usage

#### 7. Inventory Management (MEDIUM PRIORITY)
- Inventory access
- Item restrictions
- Storage access

#### 8. Zone/Region Access (HIGH PRIORITY)
- Region entry/exit
- Zone-specific permissions
- Protected area access

#### 9. Server Administration (LOW PRIORITY - mostly covered)
- Server management
- Maintenance mode bypass
- Debug tools

---

## Comprehensive Permission Structure

### Convention
```
<namespace>.<category>.<action>[.<target>]
```

- **namespace**: Plugin identifier (`hyperperms` for internal, custom for gameplay)
- **category**: Permission category (e.g., `build`, `chat`, `teleport`)
- **action**: Specific action (e.g., `place`, `break`, `send`)
- **target**: Optional target specification (e.g., `stone`, `diamond`)

---

## Complete Permission Node Reference

### 1. HyperPerms Internal Permissions

These control HyperPerms itself:

```yaml
# === Admin Wildcards ===
hyperperms.*                           # Full admin access
hyperperms.admin.*                     # Administrative commands only

# === Command Base ===
hyperperms.command                     # Base command access
hyperperms.command.*                   # All command access

# === User Management ===
hyperperms.command.user.*              # All user commands
hyperperms.command.user.info           # /hp user info
hyperperms.command.user.info.others    # View other users' info
hyperperms.command.user.permission     # Set user permissions
hyperperms.command.user.permission.set # Set positive permissions
hyperperms.command.user.permission.unset # Remove permissions
hyperperms.command.user.group          # Modify user groups
hyperperms.command.user.group.add      # Add user to group
hyperperms.command.user.group.remove   # Remove user from group
hyperperms.command.user.promote        # Promote users on tracks
hyperperms.command.user.demote         # Demote users on tracks
hyperperms.command.user.clear          # Clear user data
hyperperms.command.user.clone          # Clone user permissions
hyperperms.command.user.setprefix      # Set user prefix
hyperperms.command.user.setsuffix      # Set user suffix

# === Group Management ===
hyperperms.command.group.*             # All group commands
hyperperms.command.group.info          # View group info
hyperperms.command.group.list          # List groups
hyperperms.command.group.create        # Create groups
hyperperms.command.group.delete        # Delete groups
hyperperms.command.group.rename        # Rename groups
hyperperms.command.group.permission    # Modify group permissions
hyperperms.command.group.permission.set
hyperperms.command.group.permission.unset
hyperperms.command.group.parent        # Modify inheritance
hyperperms.command.group.parent.add
hyperperms.command.group.parent.remove
hyperperms.command.group.modify        # Modify group metadata
hyperperms.command.group.setweight     # Set group weight
hyperperms.command.group.setprefix     # Set group prefix
hyperperms.command.group.setsuffix     # Set group suffix
hyperperms.command.group.setdisplayname # Set display name

# === Track Management ===
hyperperms.command.track.*             # All track commands
hyperperms.command.track.info          # View track info
hyperperms.command.track.list          # List tracks
hyperperms.command.track.create        # Create tracks
hyperperms.command.track.delete        # Delete tracks
hyperperms.command.track.modify        # Modify tracks
hyperperms.command.track.append        # Append group to track
hyperperms.command.track.insert        # Insert group in track
hyperperms.command.track.remove        # Remove group from track

# === Administrative ===
hyperperms.command.reload              # Reload configuration
hyperperms.command.verbose             # Enable verbose mode
hyperperms.command.verbose.receive     # Receive verbose output
hyperperms.command.cache               # Cache management
hyperperms.command.cache.clear         # Clear cache
hyperperms.command.cache.stats         # View cache statistics
hyperperms.command.export              # Export data
hyperperms.command.import              # Import data
hyperperms.command.backup              # Backup management
hyperperms.command.backup.create       # Create backups
hyperperms.command.backup.list         # List backups
hyperperms.command.backup.restore      # Restore backups
hyperperms.command.backup.delete       # Delete backups
hyperperms.command.check               # Check permissions
hyperperms.command.check.self          # Check own permissions
hyperperms.command.check.others        # Check others' permissions

# === Web Editor ===
hyperperms.command.editor              # Open web editor
hyperperms.command.apply               # Apply editor changes

# === Debugging ===
hyperperms.command.debug               # Debug commands
hyperperms.command.debug.tree          # View inheritance tree
hyperperms.command.debug.resolve       # Debug permission resolution
```

### 2. Chat Permissions

```yaml
# === Chat Formatting ===
hyperperms.chat.*                      # All chat permissions
hyperperms.chat.color                  # Use &X color codes
hyperperms.chat.color.hex              # Use hex colors (#RRGGBB)
hyperperms.chat.format                 # Use formatting codes
hyperperms.chat.format.bold            # Bold text (&l)
hyperperms.chat.format.italic          # Italic text (&o)
hyperperms.chat.format.underline       # Underlined text (&n)
hyperperms.chat.format.strikethrough   # Strikethrough (&m)
hyperperms.chat.format.magic           # Obfuscated text (&k)
hyperperms.chat.links                  # Post clickable links
hyperperms.chat.bypass.cooldown        # Bypass chat cooldown
hyperperms.chat.bypass.filter          # Bypass chat filter
hyperperms.chat.broadcast              # Send broadcasts
hyperperms.chat.announce               # Send announcements

# === Private Messaging ===
hyperperms.chat.pm                     # Send private messages
hyperperms.chat.pm.receive             # Receive private messages
hyperperms.chat.pm.bypass.ignore       # Message players who ignore you
hyperperms.chat.socialspy              # See others' private messages

# === Chat Channels (if implemented) ===
hyperperms.chat.channel.*              # All channel access
hyperperms.chat.channel.<name>         # Access specific channel
hyperperms.chat.channel.admin          # Manage channels
```

### 3. World/Building Permissions

```yaml
# === Block Interaction ===
build.*                                # All build permissions
build.place                            # Place any block
build.place.<block>                    # Place specific block (e.g., build.place.stone)
build.break                            # Break any block
build.break.<block>                    # Break specific block
build.interact                         # Interact with blocks
build.interact.<block>                 # Interact with specific block (doors, buttons)

# === World Protection ===
build.bypass                           # Bypass build protection
build.bypass.worldguard                # Bypass region protection
build.bypass.spawn                     # Build at spawn

# === Special Blocks ===
build.container                        # Access containers
build.container.chest                  # Access chests
build.container.shulker                # Access shulker boxes
build.sign                             # Place/edit signs
build.sign.color                       # Use colors on signs

# === World Edit (if integrated) ===
worldedit.*                            # WorldEdit access
worldedit.selection                    # Make selections
worldedit.clipboard                    # Use clipboard
worldedit.limit.bypass                 # Bypass block limits
```

### 4. Movement & Teleportation Permissions

```yaml
# === Basic Movement ===
movement.*                             # All movement permissions
movement.fly                           # Flight enabled
movement.fly.speed.<level>             # Flight speed (1-10)
movement.speed                         # Enhanced speed
movement.speed.<level>                 # Speed level (1-10)
movement.noclip                        # No-clip mode (creative)

# === Teleportation ===
teleport.*                             # All teleport permissions
teleport.self                          # Teleport yourself
teleport.other                         # Teleport others
teleport.here                          # Teleport others to you
teleport.coordinates                   # Teleport to coordinates
teleport.world                         # Cross-world teleportation
teleport.world.<worldname>             # Teleport to specific world
teleport.bypass.cooldown               # Bypass teleport cooldown
teleport.bypass.cost                   # Bypass teleport cost

# === Warps ===
warp.*                                 # All warp permissions
warp.use                               # Use warps
warp.use.<warpname>                    # Use specific warp
warp.create                            # Create warps
warp.delete                            # Delete warps
warp.modify                            # Modify warps
warp.list                              # List warps

# === Spawn ===
spawn.use                              # Use /spawn
spawn.set                              # Set spawn point
spawn.set.global                       # Set global spawn
spawn.bypass.cooldown                  # Bypass spawn cooldown

# === Home ===
home.*                                 # All home permissions
home.use                               # Use /home
home.set                               # Set homes
home.set.multiple                      # Set multiple homes
home.limit.<number>                    # Home limit (e.g., home.limit.5)
home.delete                            # Delete homes
home.other                             # Access others' homes
```

### 5. Player Action Permissions

```yaml
# === Combat ===
combat.*                               # All combat permissions
combat.pvp                             # PvP enabled
combat.damage.players                  # Damage players
combat.damage.mobs                     # Damage mobs
combat.damage.bypass.spawn             # Damage at spawn
combat.bypass.cooldown                 # Attack speed bypass

# === Items ===
item.*                                 # All item permissions
item.use                               # Use items
item.use.<item>                        # Use specific item
item.pickup                            # Pick up items
item.drop                              # Drop items
item.enchant                           # Enchant items
item.repair                            # Repair items
item.craft                             # Craft items
item.craft.<item>                      # Craft specific item

# === Interaction ===
interact.*                             # All interaction permissions
interact.npc                           # Interact with NPCs
interact.npc.<type>                    # Interact with specific NPC type
interact.vehicle                       # Use vehicles
interact.vehicle.mount                 # Mount vehicles
interact.vehicle.place                 # Place vehicles
interact.animal                        # Interact with animals
interact.animal.breed                  # Breed animals
interact.animal.tame                   # Tame animals
```

### 6. Entity Permissions

```yaml
# === Spawning ===
entity.*                               # All entity permissions
entity.spawn                           # Spawn entities
entity.spawn.<type>                    # Spawn specific entity
entity.spawn.mob                       # Spawn mobs
entity.spawn.animal                    # Spawn animals
entity.spawn.npc                       # Spawn NPCs
entity.limit.bypass                    # Bypass entity limits

# === Management ===
entity.kill                            # Kill entities
entity.kill.<type>                     # Kill specific type
entity.modify                          # Modify entities
entity.modify.name                     # Name entities
entity.modify.attributes               # Change attributes
```

### 7. Economy Permissions (if economy system exists)

```yaml
# === Currency ===
economy.*                              # All economy permissions
economy.balance                        # Check balance
economy.balance.other                  # Check others' balance
economy.pay                            # Pay other players
economy.withdraw                       # Withdraw money
economy.deposit                        # Deposit money

# === Trading ===
economy.trade                          # Trade with players
economy.trade.create                   # Create trade offers
economy.trade.accept                   # Accept trades

# === Shops ===
economy.shop.*                         # All shop permissions
economy.shop.use                       # Use shops
economy.shop.create                    # Create shops
economy.shop.admin                     # Admin shops
economy.shop.bypass.tax                # Bypass shop tax

# === Admin ===
economy.admin.*                        # Economy admin
economy.admin.give                     # Give currency
economy.admin.take                     # Take currency
economy.admin.set                      # Set balance
```

### 8. World/Zone Access Permissions

```yaml
# === World Access ===
world.*                                # All world permissions
world.access                           # Access any world
world.access.<worldname>               # Access specific world
world.bypass.whitelist                 # Bypass world whitelist
world.bypass.blacklist                 # Bypass world blacklist

# === Region/Zone Access ===
region.*                               # All region permissions
region.enter                           # Enter regions
region.enter.<region>                  # Enter specific region
region.exit                            # Exit regions
region.bypass                          # Bypass region restrictions
region.bypass.entry                    # Bypass entry restrictions
region.bypass.pvp                      # Bypass PvP flag
region.bypass.build                    # Bypass build flag
region.member.<region>                 # Region membership
region.owner.<region>                  # Region ownership
region.admin                           # Region administration
```

### 9. Server Administration Permissions

```yaml
# === Server Management ===
server.*                               # All server permissions
server.kick                            # Kick players
server.kick.bypass                     # Bypass kick protection
server.ban                             # Ban players
server.ban.temporary                   # Temporary bans
server.ban.permanent                   # Permanent bans
server.unban                           # Unban players
server.mute                            # Mute players
server.unmute                          # Unmute players

# === Maintenance ===
server.maintenance                     # Toggle maintenance
server.maintenance.bypass              # Join during maintenance
server.whitelist                       # Manage whitelist
server.whitelist.bypass                # Bypass whitelist

# === Join/Leave ===
server.join.full                       # Join full server
server.join.reserved                   # Reserved slot access
server.join.priority                   # Priority queue

# === Other ===
server.restart                         # Restart server
server.stop                            # Stop server
server.broadcast                       # Send broadcasts
server.announce                        # Send announcements
```

### 10. Utility Permissions

```yaml
# === Information ===
utility.*                              # All utility permissions
utility.help                           # Use /help
utility.rules                          # View rules
utility.motd                           # View MOTD
utility.ping                           # Check ping
utility.list                           # List players
utility.whois                          # Player information

# === Time/Weather (if controllable) ===
utility.time                           # Change time
utility.time.set                       # Set time
utility.time.lock                      # Lock time
utility.weather                        # Change weather
utility.weather.set                    # Set weather
utility.weather.lock                   # Lock weather

# === Vanish ===
utility.vanish                         # Toggle vanish
utility.vanish.see                     # See vanished players
utility.vanish.interact                # Interact while vanished

# === AFK ===
utility.afk                            # Toggle AFK
utility.afk.bypass.kick                # Don't get kicked for AFK
```

---

## Context-Based Permissions

### Available Contexts

| Context | Example Usage | Description |
|---------|---------------|-------------|
| `world` | `world=nether` | Apply only in specific world |
| `gamemode` | `gamemode=creative` | Apply only in specific game mode |
| `server` | `server=lobby` | Apply only on specific server |
| `region` | `region=spawn` | Apply only in specific region |
| `time` | `time=day` / `time=night` | Apply only at specific time |

### Context Usage Examples

```bash
# Grant permission only in creative mode
/hp user Player permission set build.* gamemode=creative

# Grant permission only in the nether
/hp group Builder permission set build.place world=nether

# Grant permission on specific server
/hp group VIP permission set server.join.full server=survival

# Time-based permission (requires TimeContextCalculator)
/hp group Night permission set movement.fly time=night

# Region-based permission (requires RegionContextCalculator)
/hp user Player permission set build.place region=plot_123
```

### Suggested Additional Context Calculators

```java
// 1. TimeContextCalculator - Day/Night permissions
public class TimeContextCalculator implements ContextCalculator {
    public static final String KEY = "time";
    // Values: "day", "night", "dawn", "dusk"
}

// 2. RegionContextCalculator - Region-based permissions
public class RegionContextCalculator implements ContextCalculator {
    public static final String KEY = "region";
    // Values: Region names from protection plugin
}

// 3. DimensionContextCalculator - More granular than world
public class DimensionContextCalculator implements ContextCalculator {
    public static final String KEY = "dimension";
    // Values: "overworld", "nether", "end", "custom"
}

// 4. BiomeContextCalculator - Biome-specific permissions
public class BiomeContextCalculator implements ContextCalculator {
    public static final String KEY = "biome";
    // Values: Biome names
}
```

---

## Implementation Recommendations

### Priority Order

1. **HIGH PRIORITY** - Core gameplay permissions:
   - `build.*` (block interaction)
   - `teleport.*` (movement)
   - `combat.*` (PvP)
   - `region.*` (zone access)

2. **MEDIUM PRIORITY** - Enhanced features:
   - Extended `chat.*` permissions
   - `economy.*` permissions
   - `entity.*` permissions

3. **LOW PRIORITY** - Utility features:
   - `utility.*` permissions
   - Additional context calculators
   - Debug/admin enhancements

### Default Group Setup Example

```yaml
groups:
  default:
    permissions:
      - build.interact
      - chat.color
      - teleport.spawn
      - home.use
      - home.set
      - home.limit.1

  member:
    parents: [default]
    permissions:
      - build.place
      - build.break
      - home.limit.3
      - warp.use

  builder:
    parents: [member]
    permissions:
      - build.*
      - home.limit.5
      - warp.create

  moderator:
    parents: [builder]
    permissions:
      - server.kick
      - server.mute
      - utility.vanish
      - chat.bypass.*
      - teleport.other

  admin:
    parents: [moderator]
    permissions:
      - server.*
      - hyperperms.command.*

  owner:
    permissions:
      - "*"
```

---

## API Usage for Plugin Developers

### Checking Permissions

```java
// Simple check
boolean canBuild = hyperPerms.hasPermission(uuid, "build.place");

// With context
ContextSet contexts = ContextSet.builder()
    .add("world", "nether")
    .add("gamemode", "survival")
    .build();
boolean canBuildInNether = hyperPerms.hasPermission(uuid, "build.place", contexts);

// Check specific permission with target
boolean canPlaceStone = hyperPerms.hasPermission(uuid, "build.place.stone");

// Wildcard check (automatically handles build.*)
boolean hasAllBuild = hyperPerms.hasPermission(uuid, "build.place"); // True if has build.*
```

### Registering Custom Context Calculator

```java
public class MyContextCalculator implements ContextCalculator {
    @Override
    public void calculate(UUID uuid, ContextSet.Builder builder) {
        // Add custom context based on your logic
        builder.add("custom-context", "value");
    }
}

// Register with HyperPerms
hyperPerms.getContextManager().registerCalculator(new MyContextCalculator());
```

---

---

## Hytale Built-in Command Permissions

These permissions control access to Hytale's native server commands. HyperPerms integrates with Hytale's `PermissionProvider` interface to enforce these.

> **Integration Note**: HyperPerms automatically integrates with Hytale's permission system via `PermissionsModule.get().addProvider()` in `HyperPermsPlugin.java`. No additional setup is required - just install HyperPerms and it will handle all permission checks for both HyperPerms and Hytale commands.

### Hytale Permission System Features

Hytale's built-in `PermissionsModule` supports:
- **`*` wildcard** - Grant all permissions
- **`-*` negation** - Deny all permissions  
- **`-permission.node` negation** - Deny specific permission
- **Intermediate wildcards** like `hytale.command.*` or `hytale.editor.*`

HyperPerms fully supports all these features through its wildcard matcher.

### Admin Commands

| Command | Permission Node | Description |
|---------|-----------------|-------------|
| `/op` | `hytale.command.op` | Give/revoke admin permissions |
| `/op self` | `hytale.command.op.self` | Op yourself |
| `/op [player]` | `hytale.command.op.others` | Op other players |
| `/auth` | `hytale.command.auth` | Manage server authentication |
| `/backup` | `hytale.command.backup` | Run universe backup |
| `/gamemode` | `hytale.command.gamemode` | Change game mode |
| `/gamemode [player]` | `hytale.command.gamemode.others` | Change others' game mode |
| `/give` | `hytale.command.give` | Give items to players |
| `/give [item] self` | `hytale.command.give.self` | Give items to yourself |
| `/give [item] [player]` | `hytale.command.give.others` | Give items to others |
| `/heal` | `hytale.command.heal` | Heal yourself |
| `/help` | `hytale.command.help` | View command help |
| `/hub` | `hytale.command.hub` | Return to Cosmos hub |
| `/hudtest` | `hytale.command.hudtest` | Toggle HUD visibility |
| `/inventory clear` | `hytale.command.inventory.clear` | Clear inventory |
| `/inventory see` | `hytale.command.inventory.see` | View others' inventory |
| `/kill` | `hytale.command.kill` | Kill players |
| `/kill self` | `hytale.command.kill.self` | Kill yourself |
| `/kill [player]` | `hytale.command.kill.others` | Kill other players |
| `/player respawn` | `hytale.command.respawn` | Force respawn |
| `/spawn set` | `hytale.command.spawn.set` | Set world spawn |
| `/spawn [player]` | `hytale.command.spawn.teleport` | Teleport to spawn |
| `/stop` | `hytale.command.stop` | Stop the server |
| `/tp` | `hytale.command.tp` | Teleport players |
| `/tp self` | `hytale.command.tp.self` | Teleport yourself |
| `/tp [player]` | `hytale.command.tp.others` | Teleport others |
| `/unstuck` | `hytale.command.unstuck` | Unstuck yourself |
| `/whereami` | `hytale.command.whereami` | Show current location |
| `/whoami` | `hytale.command.whoami` | Show player info |

### World/Building Commands

| Command | Permission Node | Description |
|---------|-----------------|-------------|
| `/block` | `hytale.command.block` | Modify block states |
| `/buildertoolslegend` | `hytale.command.buildertoolslegend` | Toggle builder tools legend |
| `/camshake` | `hytale.command.camshake` | Modify camera shake |
| `/checkpoint add` | `hytale.command.checkpoint.add` | Add checkpoints |
| `/checkpoint remove` | `hytale.command.checkpoint.remove` | Remove checkpoints |
| `/chunk` | `hytale.command.chunk` | Modify chunks |
| `/clearblocks` | `hytale.command.clearblocks` | Clear blocks in area |
| `/editprefab` | `hytale.command.editprefab` | Edit prefabs |
| `/fillblocks` | `hytale.command.fillblocks` | Fill area with blocks |
| `/mount` | `hytale.command.mount` | Mount entities |
| `/dismount` | `hytale.command.dismount` | Dismount entities |
| `/prefab save` | `hytale.command.prefab.save` | Save prefabs |
| `/prefab load` | `hytale.command.prefab.load` | Load prefabs |
| `/prefab delete` | `hytale.command.prefab.delete` | Delete prefabs |
| `/prefab list` | `hytale.command.prefab.list` | List prefabs |
| `/time` | `hytale.command.time` | Check world time |
| `/time set` | `hytale.command.time.set` | Set world time |
| `/weather set` | `hytale.command.weather.set` | Set weather |
| `/weather reset` | `hytale.command.weather.reset` | Reset weather |
| `/worldmap discover` | `hytale.command.worldmap.discover` | Discover zones |
| `/worldmap undiscover` | `hytale.command.worldmap.undiscover` | Undiscover zones |
| `/world reload` | `hytale.command.world.reload` | Reload world map |
| `/world clearmarkers` | `hytale.command.world.clearmarkers` | Clear map markers |

### Multiplayer Commands

| Command | Permission Node | Description |
|---------|-----------------|-------------|
| `/ban` | `hytale.command.ban` | Ban players |
| `/unban` | `hytale.command.unban` | Unban players |
| `/kick` | `hytale.command.kick` | Kick players |
| `/emote` | `hytale.command.emote` | Use emotes |
| `/max players` | `hytale.command.maxplayers` | Set max player count |
| `/who` | `hytale.command.who` | List online players |
| `/whitelist add` | `hytale.command.whitelist.add` | Add to whitelist |
| `/whitelist remove` | `hytale.command.whitelist.remove` | Remove from whitelist |
| `/whitelist enable` | `hytale.command.whitelist.enable` | Enable whitelist |
| `/whitelist disable` | `hytale.command.whitelist.disable` | Disable whitelist |
| `/whitelist list` | `hytale.command.whitelist.list` | View whitelist |

### Other Commands

| Command | Permission Node | Description |
|---------|-----------------|-------------|
| `/cursethis` | `hytale.command.cursethis` | Curse held item |
| `/damage` | `hytale.command.damage` | Damage players |
| `/fillsignature` | `hytale.command.fillsignature` | Fill signature energy |
| `/memories clear` | `hytale.command.memories.clear` | Clear memories |
| `/memories unlockall` | `hytale.command.memories.unlockall` | Unlock all memories |
| `/notify` | `hytale.command.notify` | Send notifications |

### Hytale Command Wildcards

```yaml
# All Hytale commands
hytale.command.*

# All admin commands
hytale.command.op.*
hytale.command.give.*
hytale.command.tp.*
hytale.command.inventory.*
hytale.command.kill.*
hytale.command.spawn.*

# All building commands
hytale.command.prefab.*
hytale.command.weather.*
hytale.command.worldmap.*
hytale.command.world.*
hytale.command.checkpoint.*

# All multiplayer commands
hytale.command.whitelist.*

# All memory commands
hytale.command.memories.*
```

### Example Group Setup with Hytale Commands

```yaml
groups:
  default:
    permissions:
      - hytale.command.help
      - hytale.command.whereami
      - hytale.command.whoami
      - hytale.command.emote
      - hytale.command.unstuck

  member:
    parents: [default]
    permissions:
      - hytale.command.spawn.teleport
      - hytale.command.hub

  builder:
    parents: [member]
    permissions:
      - hytale.command.gamemode
      - hytale.command.tp.self
      - hytale.command.block
      - hytale.command.prefab.*
      - hytale.command.editprefab
      - hytale.command.clearblocks
      - hytale.command.fillblocks
      - hytale.command.buildertoolslegend

  moderator:
    parents: [builder]
    permissions:
      - hytale.command.kick
      - hytale.command.tp.others
      - hytale.command.inventory.see
      - hytale.command.who
      - hytale.command.whitelist.list

  admin:
    parents: [moderator]
    permissions:
      - hytale.command.ban
      - hytale.command.unban
      - hytale.command.whitelist.*
      - hytale.command.give.*
      - hytale.command.kill.*
      - hytale.command.damage
      - hytale.command.time.*
      - hytale.command.weather.*
      - hytale.command.spawn.set
      - hytale.command.maxplayers
      - hytale.command.backup
      - hytale.command.notify

  owner:
    permissions:
      - "*"  # Full access including hytale.command.op and hytale.command.stop
```

---

## Hytale Editor & Camera Permissions

These permissions control access to Hytale's built-in editor tools and camera features. Discovered from decompiling `HytaleServer.jar`.

### Asset & Pack Management

| Permission | Description |
|------------|-------------|
| `hytale.editor.asset` | Access asset editing tools |
| `hytale.editor.packs.create` | Create new content packs |
| `hytale.editor.packs.edit` | Edit existing content packs |
| `hytale.editor.packs.delete` | Delete content packs |

### Builder Tools

| Permission | Description |
|------------|-------------|
| `hytale.editor.builderTools` | Access builder tool palette |
| `hytale.editor.brush.use` | Use brush tools |
| `hytale.editor.brush.config` | Configure brush settings |

### Prefab System

| Permission | Description |
|------------|-------------|
| `hytale.editor.prefab.use` | Use prefabs for building |
| `hytale.editor.prefab.manage` | Create/edit/delete prefabs |

### Selection Tools

| Permission | Description |
|------------|-------------|
| `hytale.editor.selection.use` | Make block selections |
| `hytale.editor.selection.clipboard` | Use clipboard (copy/paste) |
| `hytale.editor.selection.modify` | Modify selections (fill, replace, etc.) |

### History & Undo

| Permission | Description |
|------------|-------------|
| `hytale.editor.history` | Access undo/redo functionality |

### Camera Controls

| Permission | Description |
|------------|-------------|
| `hytale.camera.flycam` | Use free-flying camera mode |

### Editor Permission Wildcards

```yaml
# All editor permissions
hytale.editor.*

# All pack permissions
hytale.editor.packs.*

# All brush permissions
hytale.editor.brush.*

# All prefab permissions  
hytale.editor.prefab.*

# All selection permissions
hytale.editor.selection.*

# All camera permissions
hytale.camera.*
```

### Example Group Setup with Editor Permissions

```yaml
groups:
  # Players who can view but not edit
  viewer:
    permissions:
      - hytale.camera.flycam

  # Basic builders
  builder:
    parents: [viewer]
    permissions:
      - hytale.editor.builderTools
      - hytale.editor.brush.use
      - hytale.editor.selection.use
      - hytale.editor.selection.clipboard
      - hytale.editor.history
      - hytale.editor.prefab.use

  # Advanced builders/creators
  creator:
    parents: [builder]
    permissions:
      - hytale.editor.brush.config
      - hytale.editor.selection.modify
      - hytale.editor.prefab.manage
      - hytale.editor.asset

  # Content pack managers
  packmanager:
    parents: [creator]
    permissions:
      - hytale.editor.packs.*

  # Full editor access
  admin:
    permissions:
      - hytale.editor.*
      - hytale.camera.*
```

---

## Version History

| Version | Changes |
|---------|---------|
| 1.0.0 | Initial permission structure |
| 2.0.0 | Extended with comprehensive gameplay permissions |
| 2.1.0 | Added Hytale built-in command permissions |
| 2.2.0 | Added Hytale editor & camera permissions (discovered from bytecode analysis) |

---

## Notes for Production Release

1. **Permission Registration**: Other plugins should register their permissions with HyperPerms for verbose mode tracking.

2. **Default Permissions**: Consider which permissions should be granted by default to new players.

3. **Permission Inheritance**: Design group inheritance carefully to avoid permission loops.

4. **Performance**: Use permission caching for frequently-checked permissions.

5. **Documentation**: Keep this reference updated as new features are added.
