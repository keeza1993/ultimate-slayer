# Third-party data and notices

Ultimate Slayer's original Java code remains under the BSD 2-Clause licence in LICENSE. This package also aggregates separately attributed third-party data. Do not remove these notices when copying or distributing the package or its data. No OSRS Wiki calculator implementation code has been ported into the Java plugin.

## OSRS Wiki / Weird Gloop reference data

Files: data/equipment.json, data/monsters.json and data/spells.json under src/main/resources/com/imth3keeza0/ultimateslayer/.

Source: https://github.com/weirdgloop/osrs-dps-calc/tree/498bd2ae666c83b0af3ac2265076ed237a27d1aa/cdn/json

Authors: OSRS Wiki contributors, Weird Gloop and the osrs-dps-calc contributors. The repository distributes its material under GNU GPL version 3; that licence is reproduced in third-party/GPL-3.0.txt. The underlying Wiki content is attributed to the Old School RuneScape Wiki contributors under CC BY-NC-SA 3.0, https://creativecommons.org/licenses/by-nc-sa/3.0/ . Additional terms: https://meta.weirdgloop.org/w/Licensing . Keep the underlying attribution, non-commercial and share-alike conditions with copied/derived Wiki content. This plugin is distributed free with the upstream data notices and licences retained.

The original v0.6/v0.7 package reformatted upstream arrays without changing records. In v0.8, runtime fields are projected and repeated records dictionary-normalized; equipment names/slots/bonuses still come from live RuneLite item metadata. All 5,096 equipment IDs, 2,921 monster variants and 2,135 advisory requirement entries remain. See docs/DATA_SCHEMA.md. Original packaged hashes and transformed hashes are both pinned in the manifest. SHA-256 hashes, retrieval date and record counts are in data/manifest.json. No upstream images, TypeScript calculator code or Python generator code are bundled. Upstream excludes some entries such as discontinued or special-mode content; this is not a guarantee that every entity in the game is covered.

## Item skill-reference table

File: data/requirements.json.

Source: https://github.com/RSPSApp/osrsreboxed-db/blob/961011bda6daac734208b12bcd58c0e8660da730/data/items/items-skill-requirements.json

Authors: osrsbox, osrsreboxed-db and contributing authors. Distributed with the upstream GPL-3.0 licence, reproduced in third-party/GPL-3.0.txt. Reformatted as compact JSON. These manually maintained reference entries can be outdated; the plugin explicitly treats them as advisory, not a complete eligibility proof. Curated current rules override them where implemented.

## RuneLite task names and aliases

File: data/tasks.json, derived from the task names and explicit monster aliases in https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/slayer/Task.java , retrieved 5 November 2026. The original copyright and BSD 2-Clause notice is reproduced in third-party/RuneLite-Task.txt. The transformation retains only task names and quoted target-name aliases; it does not access the package-private class or use reflection at runtime.

## Jagex

RuneScape and Old School RuneScape are trademarks of Jagex Limited. This plugin is not affiliated with or endorsed by Jagex, RuneLite, Weird Gloop or the OSRS Wiki.


## v0.7.0 additions

The separately attributed osrsreboxed-db factual weapon-stance table is included as data/stances.json, under the same pinned source and GPL-3.0 notice as its item skill table. It is used as data only. CombatCalculator.java and LoadoutOptimizer.java are original implementations, not copies/ports of Wiki calculator source.

The adapted clan logo derives from the user's existing AFK Timers artwork, with a stylised Slayer skull/sword motif based on the RuneLite/Jagex skill symbol. See docs/BRANDING.md. It does not imply endorsement by Jagex, RuneLite or the OSRS Wiki.
