# Review scope proxy — 6 November 2026

Local measurement uses js-tiktoken 1.0.21 with o200k_base. This tokenizer and counting scope are a local conservative proxy, not the Plugin Hub bot's documented algorithm or an official check result. No RuneLite bot cost/approval verdict is claimed.

Main Java sources including comments: 44,309 tokens. All 55 measured source/resource/document/licence text files: 193,879 tokens before this report (roughly 194k total including it). Tests, comments, licences, source JSON and documentation were included in the broad count; no review content was hidden. Filenames and bot/system prompt overhead are not included. Binary images, Gradle wrapper JAR, caches, build outputs and Git history are excluded. Development tokens, ZIP bytes and credits are different measures.

Original equipment and monster JSON alone measured 1,136,028 tokens. Schema 2 projects unused fields and normalizes repeated data without removing runtime catalogue coverage. See DATA_SCHEMA.md. A full initial review should use the pinned source commit, not only the v0.8 change diff.

RuneLite's public submission README requires public source and a plugins/ultimate-slayer manifest pinned to a 40-character source commit. The public review wiki describes security/game-rule review and automated reviewers, but the exact tokenizer/counting algorithm was not available in the public rules/tooling inspected. The official bot still needs to run after submission.

Sources: https://github.com/runelite/plugin-hub/blob/master/README.md and https://github.com/runelite/runelite/wiki/Plugin-Hub-Review .
