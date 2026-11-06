# v0.7.0 validation

JDK 17 / Gradle 9.6.0 / source target Java 11. latest.release resolved locally to RuneLite client/API 1.13.6. Command: gradlew.bat build preview --offline --no-daemon.

Passed invariant entry points: verifyBank, verifyGear, verifyReference, verifyPlanning, verifyCatalog and verifyCombat. These are JavaExec dependencies of check; the test task does not discover JUnit tests. previewComparison also runs during check, testing synthetic asynchronous UI completion/cancellation, style/account resets, apply-to-draft, optional invite handling, transparent logo alpha and clipping bounds. preview renders the original sidebar and magic panel. Additional outputs: build/combat-preview.png and build/welcome-preview.png.

Combat fixtures include 900 exhaustive small attack/defence dice pairs, successful-zero damage means, fixed melee/ranged/magic rolls/max hits/ticks, faster-versus-stronger weapons, Salve/Slayer non-stacking, full/incomplete Void, Void-mage rounding, crystal piece weights, powered charges/base damage, observed boosts and ownership/protection constraints. Tests use synthetic state and never connect to or control RuneScape.

The source ZIP and Git stage are checked for credentials, snapshots, caches and build output exclusion. Bundled JSON hashes and JAR resources/notices are verified. The repository upload is separately checked for correct owner, source paths, branch/head and author/committer attribution.

No v0.7.0 in-game or frame-time validation is claimed. Suggested manual check: capture bank, choose a real variant and assumptions, compare, inspect a draft, change style/character to verify invalidation, reopen Info and optionally choose the clan link. Bank timestamp should remain unchanged through withdrawals/searches until reopening.
