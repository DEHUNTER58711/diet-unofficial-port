# Porting notes — Diet, unofficial NeoForge 26.3 port

Original: **Diet** by C4 / TheIllusiveC4 (Illusive Soulworks), LGPL-3.0-or-later,
https://github.com/illusivesoulworks/diet (the old URL TheIllusiveC4/Diet redirects there).
Base: branch `1.20.x` (newest branch; `main` is an old 1.16 branch from 2022), commit `c737eab`
("Fix publishing", 2023-10-23), version **2.1.1** (a release, not a beta).
Port: DEHunter58711, 2026. Version `2.1.1+mc26.3-neoforge-unofficial.1`, Minecraft `[26.3,26.4)`,
NeoForge `26.3.0.51-beta` or newer.

Upstream was a MultiLoader project (common/forge/fabric/quilt) and used SpectreLib for its
configs. The port is NeoForge-only: `common` + `forge` were merged into `src/main`, the
Fabric/Quilt modules and SpectreLib were dropped.

## 1. Port changes (Phase A — behaviour kept as close to 1.20.1 as possible)

| Area | 1.20.1 (Forge) | 26.3 port | Why |
|---|---|---|---|
| Build | MultiLoader, ForgeGradle, mixin refmap | ModDevGradle, one project, Java 25 | 26.3 toolchain; Mojang names at runtime, no refmap |
| Platform services | `java.util.ServiceLoader` + `META-INF/services` | direct instances in `Services`/`ClientServices` | only one loader left |
| Player data | Forge capability `diet:diet_tracker` | NeoForge data attachment `diet:diet_tracker` | capabilities for entity data were removed |
| Respawn / dimension | capability `reviveCaps` + copy | `PlayerEvent.Clone` copies (with death penalty), attachment re-synced on login/respawn/dimension change | same behaviour |
| Networking | SimpleChannel `diet:main`, 6 messages | 6 play payloads `diet:diet`, `diet:effects_info`, `diet:activate`, `diet:eaten`, `diet:groups`, `diet:suites` (StreamCodec) | SimpleChannel removed. `diet` and `eaten` are now length-prefixed instead of "read until buffer end" |
| Config | SpectreLib (`diet-server.toml` per world in `serverconfig/`, `diet-client.toml`) | NeoForge `ModConfigSpec`: `config/diet-server.toml` (type SYNCED, synced to clients) and `config/diet-client.toml` | SpectreLib not available; same keys and defaults |
| Groups / suites loading | `SimpleJsonResourceReloadListener` (Gson) | `SimplePreparableReloadListener` that scans the same folders (`data/<ns>/diet/groups`, `data/<ns>/diet/suites`) and parses with the original Gson code; groups are guaranteed to load before suites | 26.3 json listener is codec-based; suites resolve group names |
| Tags | datagen (`DietTagsProvider`) → `tags/items/*.json` | shipped JSON in `data/diet/tags/item/` (folder renamed by 1.21) | datagen dropped, output identical |
| Common tags | `#forge:…` | `#c:…` (e.g. `#forge:dyes` → `#c:dyes`); `#forge:heads` → `#minecraft:skulls`; `#minecraft:music_discs` → `#c:music_discs` | Forge tags no longer exist |
| Attributes | `minecraft:generic.max_health` etc. | `minecraft:max_health`; **old ids still accepted** (prefixes `generic.`, `player.`, `zombie.` are stripped when an id is unknown) | 1.21.2 renamed vanilla attributes |
| Attribute modifiers | UUID + name "Diet group effect" | id `diet:effect/<uuid>` (UUID still taken from the effect / config) | modifiers are Identifier-based since 1.21 |
| Operations | `addition`, `multiply_base`, `multiply_total` | same JSON words, mapped to `ADD_VALUE`, `ADD_MULTIPLIED_BASE`, `ADD_MULTIPLIED_TOTAL` | renamed enum |
| Effects / attributes in code | `MobEffect` / `Attribute` | `Holder<MobEffect>` / `Holder<Attribute>` (API changed, see §4) | 26.3 API |
| `MobEffects.CONFUSION` | | `MobEffects.NAUSEA` | renamed |
| Food | `Item#getFoodProperties`, saturation modifier | `minecraft:food` data component; saturation converted back to the old modifier (`saturation / (2 * nutrition)`) | gain formula unchanged |
| Natural regeneration attribute | static field, Forge registry | `DeferredRegister`, same id `diet:natural_regeneration`, still added to players | |
| Mixins | `FoodData.tick(Player)`, `ServerPlayerGameMode#useItemOn` wraps `BlockState#use` | `FoodData.tick(ServerPlayer)` (gamerule STORE), capture for the whole `useItemOn` (HEAD/RETURN) | `BlockState#use` was split in 1.20.5 |
| Recipe value generator | `Recipe#getIngredients`, `getResultItem` | `PlacementInfo#ingredients`, result from recipe displays | 26.3 recipe API |
| Command argument `diet:group` | `ArgumentTypeInfos.registerByClass` | registered in `minecraft:command_argument_type` | NeoForge requirement |
| `/diet` permission | level 2 | `Commands.LEVEL_GAMEMASTERS` (= level 2) | 26.3 permission API |
| `/diet clear` | removed modifiers named "Diet group effect" on dirty attributes | removes all `diet:effect/*` modifiers on all attributes | names were removed |
| IMC `diet:food_override` | `net.minecraft.util.Tuple<Item, BiFunction>` | `com.mojang.datafixers.util.Pair<Item, BiFunction>` or any `Map.Entry` | `Tuple` no longer exists |
| Diet screen background | `minecraft:textures/gui/demo_background.png` | own nine-slice GUI sprite `diet:background` (`assets/diet/textures/gui/sprites/background.png`) | vanilla removed the demo background in 26.x |
| Diet screen / button rendering | `GuiGraphics`, Tesselator `coloredBlit`, `ImageButton` | `GuiGraphicsExtractor`, tinted `blit`, `AbstractButton` drawing the same `icons.png` region, deferred tooltips | 26.3 render-state GUI |
| Key category | `key.diet.category` string | `KeyMapping.Category` `diet:diet` → new lang key `key.category.diet.diet` (value copied from `key.diet.category` in all languages) | 26.3 key categories |
| Curios | compile dependency on Curios API | `CuriosScreen` detected by class name | Curios 26.3 is only a beta |
| Mod metadata | | `iconFile = diet_icon.png` (instead of the deprecated `logoFile`), no update/issue/display URLs | |

### Alpha audit (26.3 drops text whose colour has alpha 0)
- `DietScreen`: title/group names use the configured `textColor` → `DietScreen.opaque()` adds
  `0xFF000000` when no alpha is given (the default `4210752` = `0x404040`).
- percent text: group colour `getRGB() | alpha`; outline `0xFF000000`.
- bar tint: `ARGB.color(255, r, g, b)`.
- Tooltips use `Component` styles (no raw ints).

## 2. API / data compatibility

| Element | Status |
|---|---|
| mod id `diet` | unchanged |
| data `data/<ns>/diet/groups/*.json`, `data/<ns>/diet/suites/*.json` | unchanged format and path |
| tags `diet:fruits`, `grains`, `proteins`, `sugars`, `vegetables`, `ingredients`, `special_food` | unchanged names; folder `tags/item/` (vanilla 1.21 rename, applies to all data packs) |
| attachment / old capability id `diet:diet_tracker` | same id; data layout of the tracker unchanged, nested under `Tracker`. 1.20.1 worlds cannot be loaded by 26.3 anyway, so no migration of old capability NBT |
| attribute `diet:natural_regeneration`, translation `diet.naturalRegeneration` | unchanged |
| config keys (`diet-server.toml`, `diet-client.toml`) | unchanged names/defaults; server file moved from `<world>/serverconfig/` to `config/` |
| commands `/diet …` | unchanged syntax |
| events `DietEvent.ConsumeItemStack`, `ApplyDecay`, `ApplyEffect` | same classes, now `ICancellableEvent`, fired on `NeoForge.EVENT_BUS` |
| `DietApi` | same methods; `getNaturalRegeneration()` now returns `Holder<Attribute>` |
| `IDietAttribute#getAttribute()` / `IDietStatusEffect#getEffect()` | return `Holder<…>` instead of the raw object |
| `IDietTracker#getModifiers()` | `Map<Holder<Attribute>, Set<UUID>>` |
| IMC `food_override` | `Pair`/`Map.Entry` instead of `Tuple` |
| network | new payload ids (client and server must both have the port) |

## 3. Testing (Phase A)
- `./gradlew build` clean.
- `./gradlew runGameTestServer`: 9 Diet GameTests (separate `gametest` source set, not in the
  release jar) — datapack loaded (5 groups, builtin suite, tags, generated values), eating bread
  (item path), eating cake (block path / FoodData mixin), NBT save/reload, effects at
  threshold (max health modifier, hunger from sugar) and removal below threshold, death penalty,
  clone without death, natural regeneration attribute, decay. All passed.
- dedicated test server (`qa/run-server-test.sh`): loads, "Done", `reload`, clean stop, no ERROR
  or WARN lines from Diet; `config/diet-server.toml` generated.
- `runClient` cannot create a window on this box (no GLX visual / no Vulkan surface under Xvfb),
  so the client part was only verified by compilation; a `-Ddiet.selfcheck=true` class-loading
  check is built into `DietClientMod` for machines with a GPU.
