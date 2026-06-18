# Implementation Log

Newest entries first. Keep under 500 lines; compact older entries when near the limit.

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
