# v0.8.0 validation — 6 November 2026

Full Gradle 9.6.0 offline build passed against RuneLite client/API 1.13.6 with JDK 17 / Java 11 target. A local-only init script configures the external javac executable to avoid restricted loopback compiler workers. No init script or environment workaround is required by the submitted source. Direct javac compilation also passed.

All six invariant suites pass: bank capture/snapshots, owned gear, required equipment/reference rules, progression/planning, catalogue and combat. Synthetic comparison/sidebar/welcome checks pass, including account cancellation and apply-to-draft. New fixtures cover zombie axe speed/damage, leaf axe bonus and target immunity, broad-ammo kurask task, lance draconic rolls/damage, granite hammer golem rolls/damage, Warped sceptre damage/speed, charge rejection, inactive reference rejection and optimizer treatment of built-in attacks.

Offline transformation equivalence was independently checked for all 2,921 monsters, 5,096 equipment runtime records and 2,135 requirements. Original and derived SHA-256 hashes retained in the manifest. RuneLite Maven metadata reported 1.13.6 as the latest stable version at verification.

No in-game acceptance, account-switch live client test or FPS validation is claimed. Synthetic tests do not establish live performance. Alchemical Hydra phases remain outside numerical combat support.

## Previous v0.7 validation record

# v0.7.0 validation

JDK 17 / Gradle 9.6.0 / source target Java 11. latest.release resolved locally to RuneLite client/API 1.13.6. Command: gradlew.bat build preview --offline --no-daemon.

Passed invariant entry points: verifyBank, verifyGear, verifyReference, verifyPlanning, verifyCatalog and verifyCombat. These are JavaExec dependencies of check; the test task does not discover JUnit tests. previewComparison also runs during check, testing synthetic asynchronous UI completion/cancellation, style/account resets, apply-to-draft, optional invite handling, transparent logo alpha and clipping bounds. preview renders the original sidebar and magic panel. Additional outputs: build/combat-preview.png and build/welcome-preview.png.

Combat fixtures include 900 exhaustive small attack/defence dice pairs, successful-zero damage means, fixed melee/ranged/magic rolls/max hits/ticks, faster-versus-stronger weapons, Salve/Slayer non-stacking, full/incomplete Void, Void-mage rounding, crystal piece weights, powered charges/base damage, observed boosts and ownership/protection constraints. Tests use synthetic state and never connect to or control RuneScape.

The source ZIP and Git stage are checked for credentials, snapshots, caches and build output exclusion. Bundled JSON hashes and JAR resources/notices are verified. The repository upload is separately checked for correct owner, source paths, branch/head and author/committer attribution.

No v0.7.0 in-game or frame-time validation is claimed. Suggested manual check: capture bank, choose a real variant and assumptions, compare, inspect a draft, change style/character to verify invalidation, reopen Info and optionally choose the clan link. Bank timestamp should remain unchanged through withdrawals/searches until reopening.
