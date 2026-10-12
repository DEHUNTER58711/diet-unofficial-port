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
| Curios | compile dependency on Curios API | `CuriosScreen` detected by class name (Phase B: generalised to the `buttonScreens` config list) | Curios 26.3 is only a beta |
| Mod metadata | `logoFile` with upstream logo | `iconFile = diet_icon.png` (128×128): **a new icon made for this port** (not upstream artwork, not an official Diet logo); no update/issue/display URLs | NeoForge 26.3 reads `iconFile`/`bannerFile` in its mod list (`DefaultModDisplayInfo`); `logoFile` is still parsed by FML 12.0.8 (`ModInfo`) but logs "uses the deprecated `logoFile` property" (`LogoFileWarningsHandler`) |

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

---

# Phase B — improvements (new features and bug fixes)

Full data format: [docs/DATAPACK_FORMAT.md](docs/DATAPACK_FORMAT.md). All Phase B additions are
**additive**: every 1.20.1 data pack, config file and API call keeps working.

## B1. Food groups via data packs + mod support (new feature, fixes #227)

| What | Detail |
|---|---|
| `items` / `remove` in group files | members and exclusions directly in `data/<ns>/diet/groups/<group>.json`; `remove` beats tags and recipe generation |
| `data/<ns>/diet/group_entries/*.json` | new folder: one file adds/removes items/tags for several groups (`groups` / `remove`), applied in id order after the group files |
| NeoForge conditions | `neoforge:conditions` honoured on group, entry and suite files (`ConditionalOps`) → compat files for missing mods are skipped silently |
| `{"id", "required": false}` | optional item ids inside the lists (as in vanilla tags) |
| sync | added/removed lists are part of the `diet:groups` payload, so clients show the same groups |

Built-in conditional compat files (`data/diet/diet/group_entries/`), chosen from Modrinth's
NeoForge + 26.3 version lists (checked 2026-10-11 via the Modrinth API, jars loaded in a test
server and every food item dumped with its resulting groups):

| Mod (mod id) | 26.3 NeoForge build | Why a file |
|---|---|---|
| TofuCraftReload (`tofucraft`) | 26.3-28.0.6.0 | ~135 foods, most not covered by `c:` tags or recipes (tofu = proteins, edamame/soy = vegetables, sweets = sugars …). Soymilk bottles have 0 nutrition and stay without groups like vanilla milk. |
| Fish of Thieves (`fishofthieves`) | 26.3.0.1 | 32 fish items (raw/cooked) → proteins |
| UsefulFood Reborn (`usefulfood`) | 1.5.5-26.3 | 113 foods; sushi, oatmeal, fries etc. needed manual groups, rest derived from recipes |
| Ecologics (`ecologics`) | 2.7.1 | coconut/prickly pear are tagged, maple syrup etc. classified manually |
| Minecraft 26.x (`minecraft_26.json`) | — | fish buckets → proteins (as in upstream's 1.20 list, ids still valid) |

Checked and **not** included: Farmer's Delight, Brewin' and Chewin', Pam's HarvestCraft 2,
Croptopia — no NeoForge 26.3 build on Modrinth (Farmer's Delight's newest is 1.21.1). Crop and
Kettle (on 26.3) adds no food items of its own. The ~130 optional mod ids in the upstream tag files
were kept, so mods that get ported later with unchanged item ids work automatically.

## B2. GUI / HUD (new features, fixes #164, #215, #194, #190)

| Issue | Was | Now |
|---|---|---|
| #164 position not saved | `buttonX/Y` only editable in the file, SpectreLib reload issues | **Shift + drag** the button; position (relative to the inventory) is written to `diet-client.toml` (`buttonDragging` to turn off). NeoForge in-game config screen (Mods → Diet → Config) registered. |
| #215 background texture | hard-coded vanilla `demo_background.png` | nine-slice GUI sprite `diet:background` (`assets/diet/textures/gui/sprites/background.png` + `.mcmeta`), replaceable by resource packs |
| #194 Curios Menu V2 / #190 Inventory Profiles Next | button only on `InventoryScreen` + hard-coded Curios class; overlaps IPN | button attached in `ScreenEvent.Init.Post` to every screen listed in `buttonScreens` (class or interface names, default inventory + `CuriosScreen`/`CuriosScreenV2`), `addButton = false` disables it, positions movable to avoid overlaps |
| tooltips | | button tooltip shows the active effects (as before) plus the drag hint |

## B3. Where effects come from (new features, #178, #166, #53)

- Optional `title` per suite effect (#53); the builtin suite now ships "Balanced Diet",
  "Well Nourished" and "Too Much Sugar" (the 80–100 % sugar → Hunger + slowness, cause of #178).
- Suite effects are now synced to clients (`diet:suites` carries effects with conditions).
- Diet screen: the effects icon lists each effect with ✔/✘, its title and its condition
  (`groupEffectTooltip`); hovering a **group row** lists the effects linked to that group
  (`groupRowTooltips`).
- Inventory status-effect tooltip (NeoForge `GatherEffectScreenTooltipsEvent`): "Diet: Too Much
  Sugar" for effects caused by Diet (`effectSourceTooltips`).
- Action bar notice when a harmful diet effect starts (#166, `effectNotifications`).

## B4. Config + bug fixes

| Issue | What it was | Status on 26.3 |
|---|---|---|
| #209 | request: disable the "When eaten" tooltips | new client option `foodTooltips` |
| #212 | creative players cannot see groups with `hideTooltipsUntilEaten` | new server option `creativeBypassesHiddenTooltips` (default true) |
| #227 | `remove` in a group tag had no effect: an item removed from all tags is "ungrouped" and gets groups from its recipe again | **fixed**: `remove` lists in group/entry files exclude the item from the group and from generation (GameTest `group_entries`). Tag `remove` keeps vanilla semantics (documented). |
| #219 | death reset not working | caused by the per-world SpectreLib server config (edits to a global file had no effect / reset by worlds). Fixed by the global `config/diet-server.toml`; GameTests `death_penalty` and `death_penalty_disabled` (loss 0) |
| #216 | server toml not generated | same root cause (SpectreLib only wrote `serverconfig/` inside a loaded world). NeoForge now creates `config/diet-server.toml` at server start; verified on the dedicated server. Not reproducible anymore. |
| #217 (Fabric) | crash with `addButton = false` | Fabric-only (NPE in the Fabric screen hook); the NeoForge listener skips a null button. Not applicable. |

New client options: `buttonScreens`, `buttonDragging`, `foodTooltips`, `groupEffectTooltip`,
`groupRowTooltips`, `effectSourceTooltips`, `effectNotifications`. New server option:
`creativeBypassesHiddenTooltips`. All config entries have names/tooltips for the NeoForge config
screen (`gui.diet.config.<key>` + `.tooltip`; old `.name`/`.description` kept).

Alpha audit (Phase B): new text uses `Component` styles; button/sprite tints `0xFFFFFFFF`;
no raw `0xRRGGBB` colour reaches a draw call.

## In-game differences vs. 1.20.1 (summary)
- Server config is global (`config/diet-server.toml`), not per world.
- Diet button can be dragged and appears on configured screens; panel texture is a sprite.
- More tooltips (effect causes, group rows, status-effect source) and the harmful-effect notice
  (all switchable).
- Built-in effects have titles. Creative players see food groups even with hidden tooltips.

## Testing (Phase B)
- `./gradlew build` clean; `runGameTestServer`: **13 Diet GameTests** passed (Phase A tests plus
  `group_entries` — add/remove/conditional file/sync, `death_penalty_disabled`,
  `suite_sync_roundtrip`), with and without the five 26.3 food mods in `run/mods`.
- Dedicated server with the release jar: (a) with TofuCraftReload, Fish of Thieves, UsefulFood
  Reborn, Ecologics, Crop and Kettle (+ libs): 5 entry files loaded, 294 foods generated, no
  Diet warnings (the only ERROR is TofuCraft's own data map); (b) Diet only: 1 entry file, compat
  files silently skipped. `reload` and clean stop in both. Logs in `qa/logs/phaseB-*`.
- Client: compile + class-loading self-check only (no GL on this box). Manual checklist for a
  machine with a GPU: drag button with Shift and relog; resource pack replacing
  `diet:textures/gui/sprites/background.png`; eat sugar to 80 % → notice + status tooltip
  "Diet: Too Much Sugar"; hover group rows; `addButton=false`; Curios inventory.

## Group colours (follow-up)
Port artwork proposed Fruits `#E5383B`, Vegetables `#4CC04A`, Proteins `#C97B5A`, Grains `#F2C230`,
Sugars `#C45BD6`. Compared with upstream (`#d41c53`, `#31d41c`, `#d4781c`, `#d4a31c`, `#ff8fe9`)
the hue shifts are 17°/6°/12°/1°/21°, but proteins becomes a desaturated brown (S 0.77→0.51) and
sugars changes from light pastel pink to a darker purple (L 0.78→0.60, S 1.00→0.60) — a visibly
different look for the most-discussed group (#178). **Kept the upstream colours** (also what
existing screenshots, wikis and data packs show); modpacks can set `color` in a group file.
Drawing adds alpha (`| 0xFF000000` / `ARGB.color(255, …)`).

## Licence files (repository hygiene)
`LICENSE` is now the plain LGPL-3.0 text (identical to `COPYING.LESSER`) so GitHub detects the licence; upstream's 25-line `LICENSE` notice was moved verbatim into `NOTICE.md`. The jar still ships `LICENSE_diet`, `COPYING_diet`, `COPYING.LESSER_diet` and `NOTICE_diet.md` (releases after 2.1.1+…unofficial.1 contain the new `LICENSE`).

## Release 2.1.1+mc26.3-neoforge-unofficial.2 (metadata only, no gameplay changes)
- **Licence notice:** `NOTICE.md` (packed as `NOTICE_diet.md`) now states right above the verbatim
  upstream `LICENSE` quote that SpectreLib is **not** included: the jar contains no SpectreLib
  classes, no `META-INF/jarjar`, and `neoforge.mods.toml` only depends on `neoforge` and
  `minecraft`. The upstream quote is unchanged (historical). `LICENSE_diet` = LGPL-3.0 text.
- **Display name:** `Diet (Unofficial Port)` (was `Diet (Unofficial NeoForge Port)`).
- **Icon metadata (decision: `iconFile` only).** Evidence from NeoForge 26.3.0.51 / FML 12.0.8
  bytecode: `ModListScreen$ModsList$Entry` draws `ModDisplayInfo.icon()`;
  `DefaultModDisplayInfo.icon()` reads only `iconFile` (mod entry, then file level).
  `logoFile` is used solely by `banner()` (the wide image in `ModListScreen$ModInfoPanel`,
  after `bannerFile`) and makes `LogoFileWarningsHandler` log "uses the deprecated `logoFile`
  property". Adding `logoFile` would only show the square icon as a stretched banner and add the
  warning, so it is not set.
