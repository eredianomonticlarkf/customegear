# CustomGear

A Spigot/Paper plugin (1.20+) for renaming items, changing armor trims, and saving custom kits.

## Commands

| Command | What it does |
|---|---|
| `/rename <name>` | Rename the item in your hand. Supports `&` color codes. No name = reset. |
| `/trim <pattern> <material> [hand\|all]` | Change armor trim on everything you wear (`all`, default) or the piece in your hand (`hand`). |
| `/kit list` | List saved kits. |
| `/kit <name>` | Get a kit. |
| `/kit save <name>` | Save your current inventory, armor and offhand as a kit (ops only). |
| `/kit delete <name>` | Delete a kit (ops only). |

## Permissions

- `customgear.rename` (default: everyone)
- `customgear.trim` (default: everyone)
- `customgear.kit.use` (default: everyone)
- `customgear.kit.admin` (default: op)

## Config

`replace-inventory: false` adds kit items to your inventory. Set it to `true` to replace your whole inventory with the saved layout.

## Build

Needs JDK 17+ and Maven:

```bash
mvn package
```

The jar is `target/CustomGear-1.1.0.jar`. Put it in your server's `plugins` folder. Pushing to GitHub also builds it automatically: open the **Actions** tab and download the jar from Artifacts.
