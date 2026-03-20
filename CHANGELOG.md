# Changelog

## v4.1.1

### Bug Fixes
- **Apply Selected Files** in the Configuration and Import tabs now correctly queues files for pre-launch application and closes the game, instead of applying immediately mid-session
- Fixed **Apply All** in the Import tab resolving the zip against the wrong directory (`configs/` instead of `imports/`), which caused the pending config to never be found on restart
- Pending config resolution now searches `configs/`, `imports/`, and `user_configs/` so all sources work correctly through the same pre-launch path
- Fixed keyboard events (typing, hotkeys, ESC) passing through the backup restore overlay and triggering navigation or closing the screen
- Fixed ESC in the Welcome Wizard incorrectly closing the screen instead of navigating back to the previous page and clicking any key sent one page back.

### Improvements
- **Backup restore overlay** now includes a file tree for selective restore — all files are pre-selected by default, deselect any files you want to keep untouched