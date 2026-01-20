# HyperPerms

A production-grade permissions management plugin for Hytale servers.

## Features

- **Contextual Permissions** - World, game mode, time-based permissions
- **Wildcard Support** - Use wildcards like `plugin.*` for permission matching
- **Timed Permissions** - Grant temporary permissions that auto-expire
- **Chat Formatting** - Prefix/suffix support with full color codes
- **Tab List Formatting** - Display ranks in the player tab list
- **Group Inheritance** - Hierarchical permission groups with weight system
- **Tracks/Ladders** - Promotion/demotion tracks for ranks
- **Web Editor** - Browser-based permission editor
- **Backup System** - Automatic and manual backups
- **Tebex Integration** - Full support for offline player rank assignment
- **HyFactions Integration** - Faction prefix support
- **WerChat Integration** - Channel-aware chat formatting

## Installation

1. Download the latest release from [Releases](https://github.com/ZenithDevHQ/HyperPerms/releases)
2. Place the JAR in your server's `mods` folder
3. Restart your server
4. Configure in `config/hyperperms/config.json`

## Commands

| Command | Description |
|---------|-------------|
| `/hp help` | Show help |
| `/hp user <player> addgroup <group>` | Add player to group |
| `/hp user <player> removegroup <group>` | Remove player from group |
| `/hp user <player> setprimarygroup <group>` | Set display group |
| `/hp group <group> setperm <permission>` | Set group permission |
| `/hp editor` | Open web editor |
| `/hp reload` | Reload configuration |

## Tebex Integration

Use these commands in your Tebex packages:
```
hp user addgroup {id} vip
hp user setprimarygroup {id} vip
```

Use `{id}` (UUID) for offline player support.

## Links

- [Releases](https://github.com/ZenithDevHQ/HyperPerms/releases)
- [Issues](https://github.com/ZenithDevHQ/HyperPerms/issues)

## License

All rights reserved. This software is proprietary.
