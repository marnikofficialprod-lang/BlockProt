# BlockProt for Fabric

Fabric port of the [BlockProt](https://github.com/spnda/BlockProt) Spigot plugin (1.2.6) by spnda,
for **Minecraft 26.3**. Fabric port made and maintained by mariotto. Licensed under the GPL-3.0,
like the original.

Report problems with the Fabric version at
[marnikofficialprod-lang/BlockProt](https://github.com/marnikofficialprod-lang/BlockProt/issues),
not on the original project.

The mod only runs on the server. The menus are normal chest and anvil screens, so vanilla
clients can join without installing anything.

## Installation

1. Install Fabric Loader 0.19.5+ and Fabric API on the server (Java 25).
2. Put `build/libs/blockprot-fabric-1.2.6+fabric.2.jar` into the `mods` folder.
3. Optional: install LuckPerms to manage the permissions.

## What stays the same

- All menus: lock/unlock, block info, friends (search, history, "the public", permissions
  read/write/manager), redstone/hopper/piston protection, copy/paste configuration, block name,
  inspect contents, user settings, statistics.
- Commands: `/blockprot` (alias `/bp`) with `settings`, `stats`/`statistics`, `about`,
  `reload`, `disablehints` and `debug`.
- The `config.yml` keys and the 15 translation files. Block names like `OAK_DOOR` still work;
  ids such as `minecraft:oak_door` or modded blocks also work.
- Protection against: other players, explosions, fire, pistons, hoppers, redstone (doors,
  trapdoors, fence gates), withers, the ender dragon, zombies breaking doors, falling anvils.
- Locked shulker boxes keep their protection when the owner breaks and places them again.
- Permissions: `blockprot.lock` (everyone), `blockprot.info` (operators), `blockprot.admin`,
  `blockprot.bypass`, `blockprot.lockmax`, `blockprot.locklimit.<number>`, `blockprot.debug`.

## What changed

| Spigot | Fabric |
| --- | --- |
| `plugins/BlockProt/config.yml` | `config/blockprot/config.yml` |
| `plugins/BlockProt/lang/` | `config/blockprot/lang/` |
| PersistentDataContainer (NBT-API) | Fabric data attachments on block entities, chunks and players |
| `blockprot_usercache.sqlite` | `blockprot_usercache.json` in the server folder |
| `excluded_worlds: world_nether` | Same names work, as do dimension ids like `minecraft:the_nether` |
| `blockprot.locklimit.<n>` from any permission plugin | Read from LuckPerms, or from the LuckPerms meta key `blockprot.locklimit` |
| Bukkit events (`BlockAccessEvent`, ...) | Fabric events: `BlockAccessEvent.EVENT`, `BlockAccessMenuEvent.EVENT`, `BlockLockOnPlaceEvent.EVENT` |

Removed: the Towny, WorldGuard, Lands and PlaceholderAPI integrations, bStats metrics, the
`/blockprot integrations` command and the update checker (`/blockprot update` and
`notify_op_of_updates`), which only knew about the Spigot releases. Existing locks of a Paper server are not migrated.

`replace_translations: true` now really overwrites the bundled translation files on every start,
as the config comment describes. Set it to `false` to edit them.

Small fixes over the plugin:
- A new block placed where a locked door/trapdoor was removed is no longer locked already.
- Managers (friends with the manager permission) can use the redstone settings.
- Friend permissions are applied to both halves of double chests and doors.
- Unlocking someone else's block as admin updates the owner's statistics.

## Building

Requires JDK 25.

```
./gradlew build                       # jar in build/libs
./gradlew runGameTest -Pgametest      # automated in-game tests
```
