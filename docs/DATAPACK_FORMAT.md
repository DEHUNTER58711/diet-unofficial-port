# Diet data pack format (unofficial NeoForge 26.3 port)

Everything that defines food groups and diet effects is data-driven and can be added to or
overridden by data packs, mods and modpacks. Paths are relative to a data pack's `data/` folder.
Files whose name starts with `_` are ignored. All JSON files below may carry NeoForge
conditions (`"neoforge:conditions": [...]`, e.g. `neoforge:mod_loaded`); a file whose conditions
fail is skipped without errors.

Changes compared to Diet 2.1.1 (1.20.1) are marked **new**; everything else is unchanged.

## 1. Groups — `data/<namespace>/diet/groups/<group>.json`

The file name is the group name. Files with the same name from several namespaces are merged
(the `diet` namespace first, then all others, later values win).

```json
{
  "icon": "minecraft:apple",
  "color": "#d41c53",
  "order": 0,
  "default_value": 0.0,
  "gain_multiplier": 1.0,
  "decay_multiplier": 1.0,
  "beneficial": true,
  "items": ["minecraft:sweet_berries", "#c:foods/fruit", {"id": "othermod:kiwi", "required": false}],
  "remove": ["minecraft:enchanted_golden_apple"]
}
```

| Key | Meaning |
|---|---|
| `icon`, `color` (`#rrggbb`), `order`, `default_value` (0–1), `gain_multiplier`, `decay_multiplier`, `beneficial` | as in the original mod |
| `items` **new** | extra members (item ids, `#tags`, or `{"id", "required"}` objects; `required: false` = silently ignored if the item does not exist) |
| `remove` **new** | items/tags that are never in this group — wins over the tag and `items`, and the recipe-based value generator never assigns this group to them (fixes upstream #227) |

## 2. Group membership tags — `data/<namespace>/tags/item/<group>.json` (tag `diet:<group>`)

Unchanged: every group has the item tag `diet:<group>`. Note the vanilla folder name `tag/item/`
(it was `tags/items/` before 1.21). Use `"required": false` for items of optional mods. NeoForge
tag files also support `"remove": [...]`, but an item removed from all group tags is treated as
"unclassified" and may get groups again from its recipe (that was the cause of #227); use the
`remove` list of the group file or `group_entries` instead.

Special tags (unchanged): `diet:ingredients` (ignored when deriving groups from recipes),
`diet:special_food` (shows the group names without percentages).

## 3. Group entries — `data/<namespace>/diet/group_entries/<any>.json` **new**

Adds or removes members of several groups in one file — intended for mod compatibility packs.

```json
{
  "neoforge:conditions": [{"type": "neoforge:mod_loaded", "modid": "tofucraft"}],
  "groups": {
    "proteins": ["tofucraft:tofu_kinu", "#c:foods/tofu"],
    "vegetables": ["tofucraft:edamame_boild"]
  },
  "remove": {
    "sugars": ["tofucraft:tofu_strawberry"]
  }
}
```

Files are applied in id order after all group files; unknown group names are logged as warnings,
unknown required items too.

## 4. Suites — `data/<namespace>/diet/suites/<suite>.json`

Unchanged format; one **new** optional key per effect:

```json
{
  "replace": false,
  "groups": ["fruits", "grains", "proteins", "sugars", "vegetables"],
  "effects": [
    {
      "title": "diet.effect.sugar",
      "attributes": [{"name": "minecraft:movement_speed", "operation": "multiply_total", "amount": -0.1}],
      "status_effects": [{"name": "minecraft:hunger", "power": 0}],
      "conditions": [{"groups": ["sugars"], "match": "all", "above": 0.8, "below": 1.0}]
    }
  ]
}
```

- `title` **new** (#53): translation key or literal text shown in the effect tooltips, the Diet
  screen and the harmful-effect notice. Without a title the condition text is shown instead.
- Attribute names: vanilla attributes lost their `generic.`/`player.` prefix in 1.21.2
  (`minecraft:max_health`); old names are still accepted.
- `operation`: `add` (default), `multiply_base`, `multiply_total`.
- `match`: `all`, `any`, `every` (stacks per matching group), `average`.

## 5. Server config — `config/diet-server.toml`

`foodOverrides`, death penalty, gain/decay penalties, `generateGroupsForEmptyItems`,
`hideTooltipsUntilEaten` as before, plus **new** `creativeBypassesHiddenTooltips` (#212).
The file is now global (NeoForge "synced" config, sent to clients), not per world.

## 6. Built-in mod support (26.3)

`data/diet/diet/group_entries/` ships conditional files for mods that verifiably exist for
Minecraft 26.3 / NeoForge (see PORTING_NOTES.md §B1); the large upstream tag lists (with
`required: false` entries for ~130 older mods) are kept unchanged, so mods that get ported later
are covered automatically if their item ids did not change.
