# Hex Casting: Legacy

Hex Casting: Legacy is a Forge port of Hex Casting for Minecraft 1.12.2. It
preserves the `hexcasting` mod ID while adapting the mod's casting system,
items, blocks, effects, documentation, and integrations to the 1.12.2
environment.

This project is based on the upstream [Hex Casting](https://github.com/FallingColors/HexMod)
project and is maintained as a legacy branch for 1.12.2.

## Requirements

### Required mods

- Minecraft 1.12.2
- Minecraft Forge
- [Patchouli](https://www.curseforge.com/minecraft/mc-mods/patchouli)
- [Raids Backport](https://www.curseforge.com/minecraft/mc-mods/raids-backport)
- [Caves & Not Cliffs](https://modrinth.com/mod/caves-not-cliffs-backported) or [Farmer's Future Delight](https://www.curseforge.com/minecraft/mc-mods/farmers-future-delight) (install one)

Caves & Not Cliffs and Farmer's Delight
Legacy are alternative required resource providers; if both are installed,
Caves & Not Cliffs takes precedence.

### Optional integrations

The following mods are supported as optional integrations. They are not
required to launch Hex Casting: Legacy unless a specific integration is being
used:

- [Just Enough Items](https://www.curseforge.com/minecraft/mc-mods/jei)
- [BaublesEX](https://www.curseforge.com/minecraft/mc-mods/baubles-ex)
- [Unseen's Nether Backport](https://www.curseforge.com/minecraft/mc-mods/unseens-nether-backport)
- [BambooDecor](https://www.curseforge.com/minecraft/mc-mods/bamboodecor)
- [Cherry_on_1.12.2](https://www.curseforge.com/minecraft/mc-mods/cherry-on-1-12-2)

Optional integrations are only active when their corresponding mod is
installed.

## Project status

This is an active 1.12.2 port. Some behavior is adapted from the modern
upstream implementation because Minecraft, Forge, rendering, data, and
dependency APIs differ between versions.

## Contributing

Bug reports and focused pull requests are welcome. When reporting a problem,
include the Minecraft version, Forge version, installed integration mods, and
the relevant `latest.log` excerpt when available.

Please keep changes scoped to the 1.12.2 port and preserve the `hexcasting`
mod ID for compatibility.

## Credits

Thanks to the Hex Casting development team and the authors of the compatible
dependencies and integration mods.
