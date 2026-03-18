# Changelog

## v4.1.0

### Diagnostics
- Full diagnostics report logged on every startup (modpack info, config pack, settings, runtime, system)
- Crash reports now include a **PackCore Diagnostics** section with the same data as the startup log
- New `/packcore diagnose` command — shows a compact report in chat with a **click-to-copy** button for easy sharing when reporting issues
- New `/packcore crashtest` command — triggers a test crash to verify the crash report enrichment is working