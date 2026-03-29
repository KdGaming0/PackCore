# Changelog

## v4.2.0

### Fix
- Freeze when lunching the game on macOS (again)
- Changelog overlay showing "PackCore could not load changelog text for this version." when modpack is up to date

### New
- Automatic backup created when updating the modpack to a new version
- Automatic backup created when applying a different config pack
- Backups now show what triggered them (e.g. "Modpack update: v1.2.3 → v1.4.0 · 2025-03-28 14:32:11")
- Backup list now shows a color-coded accent bar per backup type (blue = modpack update, purple = config switch, green = auto, grey = manual)
- Old backups are now pruned automatically per type to avoid buildup