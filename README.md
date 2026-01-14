# HyperPerms

A production-grade permissions management plugin for Hytale servers.

## Features

- **Contextual Permissions** - Per-world, per-region, per-server permission contexts
- **Wildcard Support** - Full wildcard matching (`plugin.command.*` matches `plugin.command.home`)
- **Timed Permissions** - Temporary permissions with automatic expiration cleanup
- **Group Inheritance** - Weight-based priority system with cycle detection
- **Track System** - Promotion/demotion tracks for rank progression
- **Pluggable Storage** - JSON, SQLite, and MySQL support
- **LRU Caching** - High-performance caching with smart invalidation
- **Event System** - Full event bus for permission changes and checks
- **Async Operations** - Non-blocking storage operations

## Installation

1. Download the latest release JAR
2. Place in your server's `plugins` folder
3. Start the server
4. Configure in `plugins/HyperPerms/config.yml`

## Building from Source

Requirements:
- Java 21+ (for building)
- Java 25 (for running on Hytale server)

```bash
./gradlew build
```

The built JAR will be in `build/libs/HyperPerms-<version>.jar`

## Configuration

```yaml
# Storage type: json, sqlite, mysql
storage:
  type: json

# Cache settings
cache:
  max-size: 10000
  expire-after-access: 10m

# Default group for new players
default-group: default
```

## API Usage

```java
// Get the API instance
HyperPermsAPI api = HyperPerms.getApi();

// Check permissions
User user = api.getUserManager().getUser(uuid).join();
boolean canBuild = user.hasPermission("world.build");

// Add permission with context
Node node = Node.builder("world.build")
    .value(true)
    .withContext("world", "creative")
    .build();
user.addPermission(node);

// Create a group
Group admin = Group.builder("admin")
    .weight(100)
    .addPermission(Node.builder("*").build())
    .build();
api.getGroupManager().createGroup(admin);

// Track-based promotion
Track staffTrack = api.getTrackManager().getTrack("staff").join();
api.getTrackManager().promote(user, staffTrack);
```

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/hp reload` | Reload configuration | `hyperperms.admin.reload` |
| `/hp info` | Plugin information | `hyperperms.admin.info` |
| `/hpuser <player> info` | View player permissions | `hyperperms.user.info` |
| `/hpuser <player> permission set <perm>` | Set permission | `hyperperms.user.permission.set` |
| `/hpuser <player> parent add <group>` | Add to group | `hyperperms.user.parent.add` |
| `/hpgroup create <name>` | Create group | `hyperperms.group.create` |
| `/hpgroup <group> permission set <perm>` | Set group permission | `hyperperms.group.permission.set` |
| `/hptrack create <name>` | Create track | `hyperperms.track.create` |
| `/hptrack <track> promote <player>` | Promote player | `hyperperms.track.promote` |

## Architecture

```
com.hyperperms
├── api/                 # Public API interfaces
│   ├── context/         # Context system
│   └── events/          # Event bus and events
├── cache/               # LRU permission cache
├── config/              # Configuration handling
├── manager/             # User, Group, Track managers
├── model/               # Core data models
├── resolver/            # Permission resolution engine
├── storage/             # Storage providers
│   └── json/            # JSON storage implementation
├── task/                # Background tasks
└── util/                # Utility classes
```

## License

MIT License

## Contributing

1. Fork the repository
2. Create a feature branch
3. Submit a pull request

## Support

For issues and feature requests, please use the GitHub issue tracker.
