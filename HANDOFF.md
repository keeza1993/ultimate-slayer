# Ultimate Slayer v0.8.0 handoff — 6 November 2026

Started from code matching GitHub main merge 3593873120feb46d12d921a19db045634b22faea: local baseline tree c91fe1060f903e8d63ef22f50bb60c90d319ef13 was identical. The original source working copy was clean and remains untouched; work is isolated in the 6 November chat folder. The intended IdeaProjects installation is untouched.

## Changes

Expanded ordinary target allowlist and supported ordinary melee weapons. Added Warped sceptre, dragon hunter lance draconic bonus, leaf-bladed battleaxe leafy bonus and granite hammer golembane bonus. Leafy target immunity also applies when browsing directly without a matching task; compatible broad ranged ammunition now works on leafy tasks. Hydra boss selection remains fixed, but phase-aware boss DPS is still unsupported.

Readable dictionary-normalized offline resources preserve all runtime fields and record counts; unused equipment reference fields are omitted because live metadata already supplies the item's name, slot and bonuses. Every monster runtime field, equipment speed/category/inactive-state and requirement pair was independently compared against v0.7 data. Original and derived hashes and all licences remain. Schema documented in docs/DATA_SCHEMA.md.

## Validation

JDK 17 targets Java 11; RuneLite latest stable Maven metadata was checked as 1.13.6 on 6 November. Full Gradle 9.6.0 offline build, all six invariant suites and comparison UI checks passed using a temporary local init script selecting the external javac executable. Default Gradle compiler workers cannot connect through this environment's restricted loopback sockets; the init workaround does not alter submitted build.gradle. Direct javac compilation and all suites, comparison and sidebar previews also passed. See VALIDATION.md for latest scope. No live in-game acceptance or FPS validation claimed.

## Submission

GitHub identity verified as keeza1993, ID 301443897. Source repository remains private. Preparing a source commit is not Plugin Hub publication. Public source is required by RuneLite's submission README; obtain the user's explicit visibility decision before changing it. No Plugin Hub PR created by this batch yet.

A local o200k_base source-size proxy is recorded in docs/REVIEW_SIZE.md; it is not a RuneLite bot verdict or development-token usage. Do not claim exact official review cost or automatic approval before the actual check runs.

## Contracts

Information only; no gameplay input/automation, runtime HTTP, telemetry, bank upload, PIN/login/credential inspection. One capture per legitimate bank opening; immutable local character-specific snapshots. Live worn/carried gear remains separate. Unknown items remain CHECK; hidden requirements constrain recommendations. Bounded best-found search remains cancellable and stale results are invalidated; applying changes only the sidebar draft. Modular UI, attribution and pinned data hashes retained.

Fang, Scythe, Twisted bow, Shadow, blowpipe, enchanted bolts and unmodeled boss/phase effects remain unavailable numerically. One-cast rune checking is not trip endurance. Read COMBAT_MODEL.md before expanding support.
