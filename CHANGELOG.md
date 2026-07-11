## v5.0.10

### Changes
- Fixed: the Hypixel server resource pack's panorama was overriding PackCore's custom title-screen panorama. Only the Hypixel (server-sent) pack's title-background textures are now filtered — other resource packs can still override the panorama as before.
- Fixed: the "This server requires a resource pack" prompt appeared on every Hypixel quick-join, even after clicking Proceed. The quick-join buttons now use a persisted `servers.dat` entry instead of a throwaway one, so your Proceed/Disconnect choice is remembered like a normal saved server.