# Ultimate Slayer 0.11.0 update

1. Close your development RuneLite client.
2. Back up your existing project folder if you have local edits.
3. Extract this ZIP into `C:\Users\User\IdeaProjects\UltimateSlayer` and accept Replace files. `build.gradle`, `gradlew.bat` and `src` belong directly inside that folder. There is no extra enclosing folder in the ZIP.
4. Open PowerShell in that folder and run `.\gradlew.bat run`.
   The launcher uses normal developer mode so GPU is available. Safe mode intentionally disables GPU and is reserved for troubleshooting.
5. Check that the sidebar says Version 0.11.0. Open your bank and select My saved for the task to see the saved equipment/inventory stars.
6. Open RuneLite Settings > Ultimate Slayer. Choose Star bank badges and the options under Drop markers. Drop markers default to your/group drops; disable that filter for all visible drops. Ground Items may remain enabled for its labels. Ultimate Slayer's optional labels start off.

This is a complete source update for your development project. It does not update an installed Plugin Hub release. Existing saved task presets stay in their normal RuneLite profile data; this archive contains no account credentials, bank snapshots or user presets.

Changes: vector-star bank badges; optional ground tile outlines with value-first gold/purple/blue and low-value rune/herb colours; separate toggles/colour pickers; optional labels; own/group ownership filter; quantity/despawn/hop cleanup. See DROP_MARKERS.md for exact thresholds.

Validation: all production/test sources compiled with JDK 17 targeting Java 11 against cached RuneLite 1.13.6. All twelve offline check/preview entry points passed, including new threshold, stack aggregation, quantity/despawn, ownership, noted-price, toggle and scene-seeding regressions. The Gradle build itself could not run in this environment because the sandbox blocked its local daemon connection; direct compilation and equivalent offline checks were run instead. No live in-game acceptance or FPS measurement is claimed. Public Wiki networking was not retested for this update.

Recommended in-game check: save/select a task preset with an inventory item; check its blue star in the bank; turn stars off/on; test a low-value rune/herb drop and a valuable stack; reduce/pick up the stack; test each category/master toggle; hop worlds and confirm old markers disappear. The new module shows current ground drops only, not a loot history or totals tracker.
