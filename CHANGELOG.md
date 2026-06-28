## v5.0.4

### Fixes
- Fixed a misaligned mouse cursor in other mods' menus (e.g. SkyHanni, Odin) for Linux players on Wayland who use a fractional display-scale setting. The cursor highlighted the wrong button, with the gap growing toward the bottom-right of the screen. PackCore was unintentionally changing how the game window handled display scaling at startup.