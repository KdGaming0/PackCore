# Changelog

## v4.0.0

### Core Features
- First-launch **Welcome Wizard** for guided setup
- Custom **main menu styles** (Modern, Modern Minimal, Minimal)
- Built-in **performance profile selector**
- Visual customization pages for:
    - Tab design
    - Item background style
    - Storage design
- Optional wizard pages for supported mods:
    - **ScaleMe** sword block toggle
    - **ScamScreener** alert + ping setup
- **Resource pack selection** during setup
- Final **review + apply** page with per-setting status

### Config Pack System
- Automatic config pack detection and loading
- Resolution-based best-match config selection
- Safer update behavior (tracks applied pack + version)
- Restart-safe pending apply flow for full preset changes

### Config Manager UI
- New in-game **modpack config screen** with tabs:
    - Configuration
    - Export
    - Import
    - Backups
- Browse config pack contents before applying
- Apply **selected files** or apply **entire preset**

### Export / Import / Backups
- Export selected files as reusable config pack `.zip`
- Include metadata in exports (name, version, author, description, target resolution, GUI scale)
- Import external config packs from imports folder
- Create and restore backups from in-game UI

### Commands & Utilities
- Command to reopen wizard: `/packcore wizard`
- Command to open config manager: `/packcore modpack_config`
- Update check commands:
    - `/packcore update check`
    - `/packcore update reset`
- Performance/design quick commands for advanced users
