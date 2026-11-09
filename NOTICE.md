# Boss Checklist for NeoForge

Boss Checklist is by **Yori3o**: https://github.com/yori3o/BossChecklist
(CurseForge: https://www.curseforge.com/minecraft/mc-mods/boss-checklist,
Modrinth: https://modrinth.com/mod/boss-checklist).

This is a NeoForge port of it, made by **Oculifelis**. It is not an official release by Yori3o.
Report problems with the port here, not to the original project.

The mod's code, art, sounds and translations are Yori3o's and are used under the MIT licence
(`LICENSE.txt`, "Copyright (c) 2026 yori3o"). The port's changes are under the same licence.

What the port changes: the Fabric entry points and the `impl/Platform*` classes are replaced by NeoForge ones
(`neoforge/` and `impl/`). The common code, the mixins and the data formats are unchanged:

- `assets/boss_checklist/bosses.json` (client: the boss list, read from every mod and resource pack)
- `data/boss_checklist/server_bosses_ids.json` (server: which entities count as bosses, read from every mod and data pack)

So other mods' Boss Checklist files work the same on NeoForge as on Fabric.
