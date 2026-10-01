# Hex Casting Legacy CraftTweaker examples

These scripts target Hex Casting Legacy for Minecraft 1.12.2 and
CraftTweaker 4.1.20.703.

## Installation

1. Install CraftTweaker and Hex Casting Legacy.
2. Copy `hexcasting.zs` and `custom_spells.zs` into the instance's
   `scripts/` directory.
3. Copy the `examples/resourcepack/assets/` directory into a resource pack,
   or copy the language entries into an existing pack.
4. Restart the game or run `/ct reload`.

The example recipe registrations are active. Removal calls are commented out
so the file can be copied as-is. The `forced_mishap` action is also active but
only runs when its pattern is drawn.

## Registered data examples

| API | Active example | What to test |
| --- | --- | --- |
| Craft Phial | diamond + emerald -> gold ingot | Drop the diamond, hold the emerald, and cast the Craft Phial pattern. |
| Edify | oak sapling | Cast Edify with the registered input. |
| Brainsweep | zombie and fletcher villager | Use obsidian/zombie or stone/fletcher villager. The villager requires career level 3 or higher. |
| Media | rotten flesh = 12000 | Put it in a valid media source and inspect its tooltip/get_media result. |

## Registered custom actions

The custom action pattern is defined by a start direction and a relative angle
string. The spell name is localized by `hexcasting.action.<namespace>:<path>`.
For example, `example:damage_target` uses start direction `EAST` and angle
string `qweqweqwe`.

The `damage_target` stack order is an Entity Iota below the damage number. A
typical source spell is:

```text
get_caster
entity_pos/eye
get_caster
get_entity_look
raycast/entity
5.0
example:damage_target
```

The `example:iota_factory`, `example:nested_double`, `example:stack_state`,
`example:consume_media`, `example:great_number`, and
`example:parenthesized_number` entries each demonstrate a separate part of
the public callback API. Great actions require enlightenment. The examples
under the final comments in `custom_spells.zs` are reference snippets for
continuations, parenthesis control, escape state, and Mishap stack effects.

## Villager localization

`Brainsweep.addWithNbt` accepts a final translation key. The example uses
`my_pack.hexcasting.brainsweep.fletcher`; the supplied resource-pack language
files translate it to Fletcher/制箭师. `CareerLevel: 3` is interpreted as level
3 or higher, so the tooltip can show both the profession and minimum level.
