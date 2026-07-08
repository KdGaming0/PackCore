# Implementation Log

Newest entries first. Keep under 500 lines; compact older entries when near the limit.

---

## Keep PackCore packs above a server resource pack (v5.0.8)

**Goal:** let a user's applied pack (e.g. "Hypixel SkyBlock Legacy") override Hypixel's own server
resource pack automatically, without manually dragging it to the top of the pack menu after every join.

### Root cause (MC 26.1 source)
A downloaded server pack is created by `DownloadedPackSource` with
`PackSelectionConfig(required = true, defaultPosition = TOP, fixedPosition = true)`. In
`PackRepository.rebuildSelected`, every `required` pack not already present is force-inserted via
`Pack.Position.insert`, which for TOP inserts at the **end** of the list (= highest priority) and makes
all other packs **skip over** any fixed-TOP pack. So no local pack can be ordered above it through the
pack screen. Confirmed by `javap` on `minecraft-merged-…-26.1.2`. (Catharsis only strips the GUI lock,
so the user still had to drag manually each join.)

### Join path (why a `rebuildSelected` hook suffices)
Accepting/loading the server pack runs
`Minecraft.reloadResourcePacks() → Options.loadSelectedResourcePacks() → PackRepository.setSelected()
→ rebuildSelected()`. Hooking `rebuildSelected`'s return re-asserts the desired order on **every**
join/reload — no SkyBlock-join detection needed.

### Changes
- `PackCoreConfig` — new visible enum `keepPacksAboveServerPack: KeepAboveServerPack` (default `ALWAYS`)
  with modes `ALWAYS` / `ON_APPLY_ONLY` / `OFF`, plus hidden `packsAboveServer` (comma-joined applied
  pack ids, priority order, highest last), both under a new `RESOURCE_PACKS` category.
- `ResourcePackManager` — records the packs it applied (available `packIds`, in order) into
  `packsAboveServer` and `MidnightConfig.write`s, so the mixin has a persisted list. Also sets a
  `volatile boolean applyingFromPackCore` around `setSelected` + `reloadResourcePacks()` (both run
  `rebuildSelected` synchronously before the returned future completes, so the `finally` that clears it
  covers both) — the signal the mixin uses to detect a PackCore-initiated reorder.
- `mixin/PackRepositoryMixin` — `@Inject(method="rebuildSelected", at=RETURN, cancellable=true)`. Bails
  on `OFF`; on `ON_APPLY_ONLY` bails unless `ResourcePackManager.applyingFromPackCore` (so ordinary
  reloads and the vanilla pack menu are untouched — persistence across joins is left to another mod);
  `ALWAYS` always proceeds. Then bails if the list is empty or no `PackSource.SERVER` pack is present;
  otherwise pulls the forced packs out of the rebuilt list (server pack and everything else keep their
  order) and re-appends them at the end in persisted priority order → highest priority = above the server
  pack. Registered in `packcore.mixins.json` client list.
- `en_us.json` — `category.resource_packs`, `keepPacksAboveServerPack` label + tooltip, and the three
  `enum.KeepAboveServerPack.*` value labels.

### Modes (why)
`ALWAYS` is self-contained (re-lifts on every `reload()`/join, since MC re-pins the server pack on top
each time — verified below). `ON_APPLY_ONLY` lifts only during a PackCore apply and never fights normal
reloads or the vanilla menu; it therefore requires the user to apply *while the server pack is present*
(else there is nothing to lift above) and relies on another mod to keep the order across joins/restarts.
`OFF` = vanilla.

### Verification
- `./gradlew 26.1:build` green.
- Mixin wiring verified against the **shipped jar** (`javap -v`): the project runs on a **named/Mojmap
  runtime** — the whole jar references MC by Mojang names with zero `class_`/`method_` intermediary and
  **no refmap**, exactly like the existing `PackRepository.setSelected`/`reload` calls; so
  `method=["rebuildSelected"]` (kept named in the jar) resolves directly at runtime. `defaultRequire: 1`
  would hard-fail at launch if the target were missing.
- Reorder logic traced against decompiled ordering semantics (server pack ends up at list end = highest
  priority; forced packs appended after it).
- Human validation zone (owner: user): join Hypixel SkyBlock, accept Hypixel's pack → Legacy overrides it
  **without opening the pack menu**, survives a relog; singleplayer / no-server-pack cases untouched;
  toggling the setting off restores vanilla behaviour. Catharsis is no longer required (no conflict if kept).

---

## Resource-pack page: priority ordering UX (two-section reorderable list) (v5.0.7)

**Goal:** let users see and control which selected pack wins conflicts (priority order), which was
previously invisible and arbitrary (selection was an unordered `HashSet`, and `apply` appended in
hash order). Chosen UX: a **Selected** section on top (priority order, `n.` + ▲/▼) and an
**Available** section below.

### Root cause of "arbitrary order"
Order was discarded at every layer: `WizardState.selectedResourcePacks` was a `HashSet`;
`MultiSelectList` tracked a `HashSet`; `ResourcePackManager.apply` appended in set-iteration order.
Additionally, the old `apply` *dedupe-preserved* an already-enabled pack in its existing position
(`if (excludeIds.contains(id) && !packIds.contains(id)) continue;`), so re-selecting a pack kept its
old priority — reordering could never take effect.

### Changes
- `WizardState` — `selectedResourcePacks` is now a `LinkedHashSet` (top-first priority order). Added
  `getResourcePackOrder()` and `moveResourcePackUp/Down` (swap with neighbour); `addResourcePack`
  appends (new pick = lowest priority).
- `ResourcePackManager.apply` — signature takes `Collection<String> packIds` (ordered); the
  keep-existing loop now drops **every** excluded id unconditionally, so the append order is
  authoritative for reordering. Single-arg `apply(Set)` overload unchanged → other callers unaffected.
- `ResourcePackStep.apply` — excludes **all** user-selectable packs (∪ Caxton) so order rebuilds from
  scratch; appends the selection **reversed** (top-first display → highest-priority-last, since last
  wins). Summary rows numbered in priority order. `version()` `2 → 3`.
- New `ReorderableSelectList<T>` component (`gui/component/`) — two sections in one scroll container,
  `PackRow` widgets with ▲/▼ sub-hitboxes (consumed even when disabled so they never fall through to a
  toggle) + `HeaderRow` components. Reuses `MultiSelectList.RowDescriptor` and `GuiColors`/`GuiHelper`
  styling; `MultiSelectList` (still used by `ScamScreenerPage`) untouched.
- `ResourcePackPage` — uses the new component; seeds enabled packs by reading `options.resourcePacks`
  **in reverse** (last = highest → top of the wizard order). Lang keys: `…resource_pack.selected_header`,
  `.available_header`, `.priority_hint`.

### Priority direction (validation zone)
The whole feature hinges on `options.resourcePacks` being **last = highest priority** (existing
`ResourcePackManager` comment + shipped append-at-end behaviour). Seed-in and apply-out both reverse,
so no-change round-trips (idempotent). If in-game testing shows it inverted, flip the two reversals
(`ResourcePackPage.seedEnabledPacks` loop + `ResourcePackStep.apply` `Collections.reverse`) — localized.

### Verification
- `./gradlew 26.1:build -x test` green.
- In-game pending (owner: user): set pack A as #1 → Apply → confirm A wins in vanilla RP screen;
  ▲/▼ reorder + end-disable; toggling moves rows between sections; reopening shows the same order.

---

## Resource-pack page: pre-select enabled packs + disable on uncheck (v5.0.7)

**Goal:** when the resource-pack wizard page opens, pre-highlight packs already enabled in-game so a
user reopening it to add more packs sees the current selection; and make unchecking a pack actually
disable it. Bump the page version so it reopens once on update.

### Changes
- `ResourcePackManager` — extracted the selectability rule as `isUserSelectable(Pack)` (was the page's
  private predicate) and added `availableUserSelectablePackIds()`. Single source of truth shared by the
  page and the step. Caxton packs use a `caxton:` namespaced id and are **not** `PackSource.DEFAULT`,
  so they are never user-selectable — the deselect logic and the Caxton exclude/re-add path stay
  fully separate.
- `ResourcePackPage` — `seedEnabledPacks(packs)` seeds `WizardState` with `options.resourcePacks ∩
  displayed rows` on first entry only (guarded by `seededEnabledPacks`), so `MultiSelectList` (which
  already highlights whatever ids it is given) shows them checked. Guard prevents re-adding a pack the
  user unchecks then navigates away from and back.
- `ResourcePackStep.apply` — `excludeIds = (availableUserSelectablePackIds − selected) ∪ caxtonPacks`,
  so every unchecked user-selectable pack is removed from the enabled order via the existing
  `ResourcePackManager.apply` exclude path. Non-user-selectable packs are never excluded → core
  mod/config packs preserved. `version()` `1 → 2`.

### Notes / verification
- `./gradlew 26.1:compileJava` clean.
- In-game validation pending (owner: user): enable 2 packs → reopen wizard → both pre-checked; uncheck
  one → Apply → that pack disabled, the other stays; a non-selectable core pack is never disabled; page
  reopens once after the version bump.

---

## One-shot forced config migrations — price tooltips: Skyblocker over SBE (v5.0.6)

**Goal:** on a modpack update, force existing users' price tooltips to come from Skyblocker (enable
its NPC/AvgBIN/LowestBIN/Bazaar lines) and disable Skyblock Enhancements' own `enablePriceTooltips`,
so the two mods stop stacking duplicate price lines. Must apply **once per updating user** and **not**
touch new installs (their shipped default configs already carry the intended values).

### Design
New lightweight one-shot migration framework in `migration/`, keyed by stable migration id:
- `ConfigMigration` — `record(String id, Runnable action)`.
- `ConfigMigrationRunner.run()` — run at `CLIENT_STARTED` (all mods' configs initialized):
  - **New vs updating** decided by `PackCorePreLaunch.getPreviousModpackVersion()` (the
    `lastSeenModpackVersion` captured at pre-launch before it's overwritten). Blank → first launch →
    new user. This is a pre-existing field, so there is no bootstrap ambiguity when the feature first
    ships: an updating user always has a prior non-blank value.
  - New user → record the migration id as applied **without running it** (baseline).
  - Updating user → run the action, then record the id.
  - Applied ids persisted in new hidden field `PackCoreConfig.appliedConfigMigrations`
    (comma-separated, `META` category). This set is the **sole "run once" guard** — a migration is
    marked applied even if its target mod was absent or the action threw, because the point is to
    never re-fight a player who later changes the setting back. Version magnitude is deliberately not
    compared; blank-previous handles new users and the applied-set handles once-only, so comparing
    versions would only reintroduce the "re-runs every update" problem.
- `PriceTooltipMigration` — the single registered migration `prices-skyblocker-over-sbe`, best-effort
  per mod (each half gated by `FabricLoader.isModLoaded` + try/catch):
  - Skyblocker: `SkyblockerConfigManager.update(cfg -> cfg.general.itemTooltip.<field> = true)` via
    reflection (mirrors `TabDesignManager`; `update()` persists on its own).
  - SBE: set static `SkyblockEnhancementsConfig.enablePriceTooltips = false` +
    `MidnightConfig.write("skyblock_enhancements")` (mirrors `StorageDesignManager`).

### Verification
- `./gradlew compileJava` passes.
- Reflection paths verified against the shipped jars, not the dev pins:
  - `skyblocker-6.5.3+26.1.2`: `SkyblockerConfig.general` → `GeneralConfig.itemTooltip` →
    public booleans `enableNPCPrice/enableAvgBIN/enableLowestBIN/enableBazaarPrice`; and
    `SkyblockerConfigManager.update(Consumer<SkyblockerConfig>)` is `public static`.
  - `skyblock_enhancements-1.1.7+26.1.2`: `SkyblockEnhancementsConfig extends MidnightConfig`, static
    `boolean enablePriceTooltips`.
- Human validation zone (in-game): (1) existing profile with old values + a prior
  `lastSeenModpackVersion` → the 4 Skyblocker fields become true and SBE's becomes false, once;
  (2) toggle one back, relaunch → not re-forced; (3) fresh profile (blank `lastSeenModpackVersion`) →
  configs untouched by the migration.

### Files
- `config/PackCoreConfig`: added hidden `appliedConfigMigrations`.
- `migration/ConfigMigration`, `migration/ConfigMigrationRunner`, `migration/PriceTooltipMigration`: new.
- `PackCore`: call `ConfigMigrationRunner.run()` at `CLIENT_STARTED`.
- `stonecutter.properties.toml`: `mod.version` 5.0.5 → 5.0.6. `CHANGELOG.md`: user-facing entry.

---

## Fix crash when clicking a wizard resolution — GLFW terminate at runtime (v5.0.5)

**Goal:** stop a hard native crash (SIGSEGV in `libgallium`) that happened whenever the welcome
wizard queried the screen resolution — e.g. opening/clicking a resolution in the config-switch
overlay. Regression introduced by the v5.0.4 Wayland fix.

### Root cause
The v5.0.4 fix added an unconditional `GLFW.glfwTerminate()` in a `finally` block inside
`ScreenResolution.detect()`. That is correct at the `preLaunch` entrypoint (we own GLFW there), but
`detect()` is *also* called at runtime from `ConfigSwitchOverlay` (wizard), `ExportPage`, and
`DiagnosticsCollector`. At runtime Minecraft owns GLFW with a live window and OpenGL context; the
second `glfwInit()` is a no-op, but the `finally` still ran `glfwTerminate()`, destroying the live
window/context out from under the running game. The next GL call dereferenced freed driver state →
SIGSEGV in Mesa (`libgallium`). Log signature: `detect()` logged on the Render thread immediately
before the crash.

### Changes
- `util/ScreenResolution`: split the single `detect()` into two context-specific methods sharing a
  private `queryPrimaryMonitor()`:
  - `detectAtPreLaunch()` — owns the GLFW lifecycle (`glfwInit()` + `glfwTerminate()`), preserving the
    v5.0.4 Wayland/X11 hint fix.
  - `detectFromRunningGame()` — queries only; never inits or terminates GLFW. Must run on the render
    (main) thread.
- Updated call sites: `PackCorePreLaunch` → `detectAtPreLaunch()`; `ConfigSwitchOverlay` (×2),
  `ExportPage`, `DiagnosticsCollector` → `detectFromRunningGame()`.
- `stonecutter.properties.toml`: `mod.version` 5.0.4 → 5.0.5.
- `CHANGELOG.md`: user-facing fix entry.

### Verification
- `./gradlew compileJava` passes; no remaining references to the old `detect()`.
- Human validation zone (runtime): open the welcome wizard and click a resolution in the config-switch
  overlay — no crash; resolution still detected correctly. Pre-launch Wayland fix path unchanged.

---

## Fix Wayland fractional-scale cursor offset — pre-launch GLFW init (v5.0.4)

**Goal:** stop PackCore from forcing the game onto native Wayland, which broke cursor↔framebuffer
mapping (cursor offset growing toward the bottom-right) in other mods' GUIs (SkyHanni, Odin) under
KDE fractional display scaling. Reported only with PackCore enabled, reproducible on a fresh install,
and the user themselves traced it to the KDE display-scale slider.

### Root cause
`ScreenResolution.detect()` (called from `PackCorePreLaunch.onPreLaunch()`) called `GLFW.glfwInit()`
during the `preLaunch` entrypoint — *before* Minecraft's `com.mojang.blaze3d.platform.GLX._initGlfw()`.
GLFW init hints only take effect at the *next* `glfwInit()`, and a second `glfwInit()` on an
already-initialized library is a silent no-op. MC's `GLX._initGlfw` does
`glfwInitHint(GLFW_PLATFORM, GLFW_PLATFORM_X11)` (constants `327683`/`393220`, confirmed via `javap`
on `minecraft-merged-...-26.1.2`) right before its `glfwInit()` when X11 is supported and
`SharedConstants.DEBUG_PREFER_WAYLAND` is false — deliberately forcing XWayland. PackCore's earlier
init (no platform hint) let GLFW auto-select native Wayland, so MC's hint + init were no-ops and the
game ran on native Wayland. Under fractional KDE scaling this desyncs cursor and framebuffer scale.
Also explains the generic "W" Wayland app icon and broken see-through farming.

### Changes
- `util/ScreenResolution.detect()`: wrap the post-`glfwInit()` query in a `try/finally` that always
  calls `GLFW.glfwTerminate()`, leaving GLFW uninitialized exactly as found so MC's `GLX._initGlfw`
  runs its full platform-hint path. Video-mode width/height copied into locals before terminate frees
  the native struct. Replaced the misleading "idempotent — safe" comment; expanded the class javadoc.
- `stonecutter.properties.toml`: `mod.version` 5.0.3 → 5.0.4.
- `CHANGELOG.md`: user-facing fix entry.

### Verification
- `./gradlew 26.1:build` passes.
- Human validation zone (KDE Wayland + fractional scaling): cursor aligns with highlighted buttons in
  SkyHanni/Odin, correct app icon returns, see-through farming works. Not reproducible without that setup.

---

## Re-add Dungeon Routes page — Skyblocker vs Stella (v5.0.0)

**Goal:** bring back the dungeon-routes wizard page, replacing Secret Routes Mod with Stella and marking Skyblocker as recommended.

### Changes
- `StellaConfigurator` (new) — reflection bridge to Stella's Kotlin DSL config (`ConfigKt.config`,
  `Config.valueCache`, `Config.elementMap`, `Config.save()`).
- `DungeonRoutesManager` — rewritten. Modes are now `SKYBLOCKER_WAYPOINTS` and `STELLA`.
  - Skyblocker mode enables `SecretWaypoints` and disables Stella `secretRoutes`/`secretWaypoints`.
  - Stella mode enables `secretRoutes`/`secretWaypoints` and disables Skyblocker `SecretWaypoints`.
- `DungeonRoutesPage` / `DungeonRoutesStep` — recreated.
  - Step requires both `skyblocker` and `stella` to be loaded.
  - Page now marks `skyblocker_waypoints` as recommended via the new card badge.
- `OptionCardGrid` — added optional `recommended` flag to `CardDescriptor` and a small "Recommended" badge.
- `WizardSteps.ALL` — re-added `DungeonRoutesStep` after `StorageDesignStep`.
- `PackCoreCommands` — re-added `/packcore dungeonroutes skyblocker|stella`.
- `en_us.json` — added `gui.packcore.recommended` and all dungeon-routes strings.

### Verification
- `./gradlew 26.1:build` passes.

### Notes
- Preview textures for the two options do not exist yet; the page references the standard
  `textures/gui/sprites/wizard/dungeon_routes_preview/<id>.png` paths.

---

## Config pack reapply prompt + overwrite-mode hints (v5.0.0)

**Goal:** add the pre-5.0 upgrade reapply prompt and clarify overwrite-mode hints in the config-pack status card.

### Changes
- `PackCoreConfig`:
  - Added hidden `lastAppliedOverwriteMode` field (`"full"` / `"preserved"`).
- `PackCorePreLaunch`:
  - Captured `previousModpackVersion` before overwriting `lastSeenModpackVersion`.
  - Set `lastAppliedOverwriteMode` in every extraction path (`extractIfNeeded` fresh/same-pack, `migrateFromV3`, `applyPendingConfig`).
- `gui/component/ConfigStatusCard`:
  - Replaced the v3-migration boolean with a `preserved` boolean.
  - Shows `"All preset files were applied."` or `"Existing settings were preserved during the update."` based on the mode.
  - Hoisted `setHeight(...)` out of the `if/else` branches.
- `gui/wizard/page/ConfigPackPage`:
  - Reads `lastAppliedOverwriteMode` and passes `preserved` into `ConfigStatusCard`.
- `gui/wizard/page/ConfigPackReapplyPage` (new):
  - Class-level Javadoc explaining the page's purpose.
  - Added an SLF4J `Logger` and logged a warning when `queueReapplyAndRestart` cannot proceed because `lastAppliedPackFile` is missing.
- `gui/wizard/page/ConfigPackStep`:
  - `createPage(...)` now returns `ConfigPackReapplyPage` for pre-5.0 upgrades and `ConfigPackPage` otherwise.
- `WizardState`, `PackCore`, `WelcomeWizardScreen`:
  - Removed the obsolete `migratedFromV3` flag and its wiring.
- `src/main/resources/assets/packcore/lang/en_us.json`:
  - Added reapply page title, body, buttons, footer, and full/preserved hint keys.
- `IMPLEMENTATION_LOG.md`:
  - Added this entry.

### Notes
- Translation keys for the preserved/full hints and the error hint already exist in `en_us.json`.
- `ConfigPackReapplyPage.onEnter()` now calls `this.clear()` instead of `this.clearComponents()` so window resize or page re-entry does not leak stale button widgets.
- The active Stonecutter target is now `26.1` only; `1.21.11` has been retired from the build.

### Verification
- `./gradlew build` passes for the `26.1` target.

---

## Welcome Wizard — support welcome page + config pack split (v5.0.0)

**Goal:** split the first wizard page into a dedicated support/welcome page and a separate config-pack selection page.

### Changes
- Added `SupportWelcomeStep` + `SupportWelcomePage`:
  - Two-column layout: scrollable `packcore/markdown/welcome.md` on the left, support buttons on the right.
  - Buttons: Donate on Ko-fi, Hytale Store (code KD1), Bisect Hosting (code SBE), Proton VPN, Get Help.
  - Uses the same red button sprites as the main menu (`menu/buttons/blank_red_button`).
  - Each button has a tooltip explaining what it does.
  - Hytale/Bisect buttons open their URLs and copy the referral code to the clipboard, then show a toast.
- Refactored the old `WelcomePage` into `ConfigPackStep` + `ConfigPackPage`:
  - Title changed from "Welcome" to "Config Packs".
  - Now reads `packcore/markdown/config_packs.md` instead of `welcome.md`.
  - Preserves all existing config-pack selection and migration behavior.
- Registered both steps at the top of `WizardSteps.ALL`: `support_welcome`, then `config_packs`.
- Updated `WelcomeWizardScreen` to remove the hardcoded welcome page; all pages now come from `WizardSteps.available()`.
- Added new translation keys for titles, button labels, button tooltips, and the "Code copied" toast.
- Updated runtime markdown files:
  - `run/packcore/markdown/welcome.md` — thank-you / support message.
  - `run/packcore/markdown/config_packs.md` — config pack / resolution explanation.

### Verification
- `./gradlew build` passes.
- `./gradlew 26.1:compileJava` passes.

---

## Remove Dungeon Routes wizard page (v5.0.0)

**Goal:** remove the dungeon-routes setup page and command because it is no longer needed.

### Changes
- Deleted `DungeonRoutesPage.java`, `DungeonRoutesStep.java`, and `DungeonRoutesManager.java`.
- Removed `DungeonRoutesStep` from `WizardSteps.ALL`.
- Removed `/packcore dungeonroutes` command and its handler from `PackCoreCommands`.

### Notes
- Removed dungeon-routes translation keys from `en_us.json`.
- No `dungeon_routes_preview` texture files were present in the repo.
- `WizardVersionStore` will simply ignore the old `dungeon_routes` entry on existing installs.

---

## Storage design integration — Firmament → Enhanced Storage (v5.0.0)

**Goal:** update the storage-design wizard step and command to configure Enhanced Storage instead of Firmament.

### Changes
- `StorageDesignManager` — rewritten to set `EnhancedStorageConfig.enableStorageOverlay` via reflection
  and save with `MidnightConfig.write("enhanced_storage")`.
- `StorageDesignStep.isAvailable()` — guard changed from `firmament` to `enhanced_storage`.
- `PackCoreCommands` — `/packcore storagedesign` failure message now says "Enhanced Storage" instead of
  "Firmament".

### Notes
- The page preview image is left to the user to update.
- Existing `BackupManager` / `ExportPage` references to `config/firmament/profiles` were not changed
  because the new Enhanced Storage config path is not confirmed yet.

---

## Wizard page availability guards (v5.0.0)

**Goal:** ensure wizard pages only appear when the mod they configure is actually loaded.

### Changes
- `ItemBackgroundStep` — now gated on `skyblocker` (it only edits Skyblocker's item-rarity config).
- `StorageDesignStep` — now gated on `enhanced_storage` (it only edits Enhanced Storage's storage-overlay config).
- `TabDesignStep` — now gated on `skyblocker || skyhanni`; `COMPACT` needs SkyHanni, `FANCY` needs Skyblocker.
- `PerformanceProfileService.applyAll` — added an explicit `isModLoaded("moreculling")` guard around the
  `MoreCullingConfigurator` call, matching the existing Sodium/Iris guards. The `PerformanceStep` page
  itself remains always-available because it always applies vanilla settings.

### Verification
- `./gradlew 26.1:compileJava` passes.
- `./gradlew 26.1:build` passes.

---

## Welcome Wizard — modular step/registry refactor (v5.0.0)

**Goal:** make adding/removing wizard pages trivial (a couple of files + one registry line),
co-locate each page's config-apply logic with the page, allow opening a single page by command,
and automatically re-show only *new or changed* pages to existing users after an update.

### New abstraction (`gui/wizard/`)
- `WizardStep` — descriptor interface pairing a render page with its config. Methods: `id()`,
  `version()`, `isAvailable()`, `requires()`, `createPage(...)`, `summaryRows(state)`, `apply(state)`.
  `requires()` declares coupled steps that must run together in a partial run (see Caxton coupling).
- `SummaryRow` — record for one Confirm & Apply row (`stepId`, `label`, `value`, `skipped`,
  `subRow`) + `of`/`sub`/`single` factories. `single()` centralises the "Skipped" / translated-name
  rendering used by single-select steps.
- `WizardSteps` — the single ordered registry (`ALL`). List order = both page order and apply
  order. Helpers: `all()`, `available()` (filters `isAvailable`), `byId(id)`, `outdated(store)`.
- `WizardVersionStore` — persists `{stepId: seenVersion}` to `packcore/wizard.json` (Gson, matches
  `ModpackMetadata` IO style). `isOutdated(step)` = stored version `<` `step.version()` (or absent);
  `markSeen(steps)` writes; `fileExists()` gates the one-time migration.

### Per-page steps (`gui/wizard/page/*Step.java`)
One step per content page, co-located with its `*Page`. Each step's `apply()` is the logic that
previously lived in `ConfirmApplyPage.applyXxx()`; each `summaryRows()` is its slice of the old
`SUMMARY_ENTRIES` / special-case row building:
- `MainMenuDesignStep`, `PerformanceStep`, `TabDesignStep`, `ItemBackgroundStep`,
  `StorageDesignStep` — always available, single-select.
- `DungeonRoutesStep` — available only when **both** `skyblocker` and `secretroutesmod` are loaded
  (unifies the previously inconsistent page-vs-startup conditions; the choice only makes sense with
  both providers present).
- `SwordBlockStep` (`scaleme`), `ScamScreenerStep` (`scamscreener`, multi-row: alert level + ping
  header + sub-rows), `CaxtonFontStep` (`caxton`).
- `ResourcePackStep` — registered **last** so `CaxtonFontStep` (just before it) can fold the chosen
  font pack into the resource-pack selection, applied together in one pass. Preserves the old
  Caxton→ResourcePack ordering/coupling.

**Caxton↔ResourcePack coupling in partial runs (bug found in review):** `CaxtonFontStep.apply` only
mutates `WizardState`; the actual push to the game is `ResourcePackStep.apply`, which also *excludes*
all Caxton pack ids (Caxton step re-adds the chosen one). So in a partial run (single-page command or
new-page flow) one without the other is wrong: `caxton_font` alone never applies the font;
`resource_packs` alone silently strips an active font. Fixed with `WizardStep.requires()` — both steps
declare each other; `WelcomeWizardScreen.forSteps` BFS-expands the requested ids over `requires()` then
re-orders by the registry, so they always run as a unit (and `isAvailable` still drops Caxton when the
mod is absent). Full wizard already includes both, so it was never affected.

### Rewrites
- `ConfirmApplyPage` — now page-agnostic. Takes the run's `List<WizardStep>`; renders
  `step.summaryRows(state)` for each and, on Apply, runs `step.apply(state)` per step, colouring
  rows by step result (status keyed on `stepId`). Deleted `miniWizardMode`, `MINI_*`,
  `SUMMARY_ENTRIES`, the `SummaryEntry` record, and all nine `applyXxx()` methods. Stonecutter
  `else`-branches stripped (26.1-only).
- `WelcomeWizardScreen` — `full(lastScreen)` (intro + all available steps + Confirm) and
  `forSteps(lastScreen, ids)` (those steps + Confirm) replace `forDungeonRoutes`/`miniWizard`.
  `registerPages()` loops the resolved steps. `forSteps` expands the requested ids over
  `WizardStep.requires()` (see coupling note above). `markWizardComplete()` always records
  `WizardVersionStore.markSeen(runSteps)` (on Finish and Skip) but only sets `successfulWelcomeWizard`
  in the full wizard, so `/packcore wizard <page>` can't make a brand-new user skip first-launch setup.
  Explicit imports (no wildcards).
- `PackCore.applyConfiguredTitleScreen` — new user → `full`; existing user with no `wizard.json` →
  one-time migration seeding all `available()` steps as seen (so only pages added/bumped *after*
  this release prompt); otherwise → `forSteps(outdated)` if any, else the configured title screen.
- `PackCoreCommands` — `/packcore wizard` (full) + `/packcore wizard <page>` with `available()` id
  tab-completion and an "unknown/unavailable page" error.
- `PackCoreConfig` — removed `seenDungeonRoutesWizard` (replaced by the per-page version store).

### How to add a page later
1. Create `XxxPage` (render) and `XxxStep` (id/version/condition/apply/summary) in
   `gui/wizard/page/`.
2. Add `new XxxStep()` to `WizardSteps.ALL` (position = page + apply order).
3. Add translation keys/textures as usual. Bump an existing step's `version()` to re-show it.
Remove a page = delete the two files + its `WizardSteps.ALL` line.

### Notes / verification
- No new translation keys: summary labels ("Main Menu Design", etc.) and "Skipped"/"None selected"
  were already hardcoded literals in the old `ConfirmApplyPage`; preserved as-is.
- Summary row order now follows registry order (page order). This intentionally differs cosmetically
  from the old `SUMMARY_ENTRIES` order (e.g. Dungeon Routes now before Sword Block; ScamScreener
  alert + pings grouped), making the summary consistent with the page sequence.
- `javac` ran clean for all 15 new/changed wizard files. The full `./gradlew 26.1:compileJava` cannot
  go green in the current tree because the in-progress 26.1 dependency migration removed the compile
  deps for `sodium`/`iris`/`moreculling`/`scamscreener` and set `modmenu` to `modRuntimeOnly`; the 5
  optional-mod integration files (untouched here) only build from Gradle's cache. Not a wizard issue.
- In-game validation still pending (owner: user): full wizard for a new user; version bump → only
  that page reopens; `/packcore wizard <id>`; Caxton→ResourcePack apply; summary status colours.

### Build unblock (incomplete 26.1 migration — not wizard code)
The game wouldn't launch because `:26.1:compileJava` then `:26.1:processResources` failed — both from
gaps in the (uncommitted) `build.gradle.kts` migration, surfaced once the wizard edits forced a real
recompile. Fixed minimally, touching only `build.gradle.kts`:
- Re-added the optional-mod **compile** deps the integration files still import — `sodium`, `iris`,
  `moreculling`, `scamscreener`, `modmenu` — as `modCompileOnly` (HEAD had them; the WIP rewrite
  dropped them). They are compile-only; the mod already degrades gracefully at runtime when absent.
- `processResources` registered the UI Lib template key as `ui_lib`, but `fabric.mod.json` expects
  `${uilib_version}`; registered it under `uilib_version` to match (committed manifest left intact).
After both, `./gradlew 26.1:build` is green (compile + processResources + jar + remap).
