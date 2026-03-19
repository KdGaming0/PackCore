# Changelog

## v4.1.0

### Modern UI Integration
- New wizard page for configuring Modern UI features
- Toggle **Custom Font** (Inter) — disabling reverts to the vanilla Minecraft font and requires a restart
- Toggle **Fancy Tooltips** — rounded, styled tooltips with rarity-adaptive border colors
- Toggle **Startup Ding** — sound effect played when the game finishes loading
- Font values are backed up before clearing, and restored if the custom font is re-enabled
- Confirm page now shows a restart warning when the font toggle differs from the current live state
- Custom Font row in the summary is annotated with "(restart required)"
- New `/packcore modernui` command with subcommands: `font vanilla`, `font custom`, `tooltip on/off`, `ding on/off`

### Diagnostics
- Full diagnostics report logged on every startup (modpack info, config pack, settings, runtime, system)
- Crash reports now include a **PackCore Diagnostics** section with the same data as the startup log
- New `/packcore diagnose` command — shows a compact report in chat with a **click-to-copy** button for easy sharing when reporting issues
- New `/packcore crashtest` command — triggers a test crash to verify the crash report enrichment is working