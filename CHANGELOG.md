# Changelog

## v5.0.0

### Changes
- The first page of the setup wizard is now a dedicated welcome page with buttons to donate and show your support (Ko-fi, Hytale store, Bisect Hosting, Proton VPN, and Discord help).
- The setup wizard now remembers which setup pages you have already seen. When an update adds a brand-new setup page (or meaningfully changes an existing one), the wizard reopens on the next launch and shows you only those new pages — you no longer have to redo your whole setup just to see what's new.
- You can now jump straight to a single setup page with `/packcore wizard <page>` (the page name auto-completes as you type), instead of stepping through the entire wizard.
- Setup pages that configure a specific mod (item backgrounds, storage design, tab design, sword block, ScamScreener, Caxton fonts) are now hidden when that mod is not installed, so the wizard only shows options that actually work for your setup.
- The storage design option now integrates with Enhanced Storage instead of Firmament.
- Removed the dungeon routes setup page and `/packcore dungeonroutes` command.