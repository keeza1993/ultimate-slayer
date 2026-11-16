# Task-specific setups — experimental

**Source version:** 0.10.2 (not submitted to Plugin Hub).

## In-game trial

1. Run the updated source locally: `gradlew.bat run` (JDK 17 recommended). Make sure another copy of Ultimate Slayer is not running.
2. Log in as your RuneLite account and make sure a real Slayer task is active.
3. Equip your preferred main weapon, armour and jewellery; pack potions, food, teleport items, rune pouch and other inventory supplies exactly as you want.
4. In Ultimate Slayer > **Gear**, click **Save current equipment + inventory for this task**.
5. Choose **My saved**. The gear and inventory grids should show your saved 14/28 slots.
6. Open the personal bank normally. Items from the saved setup should be bordered **blue** with a tick, whether or not a Wiki strategy is available.
7. Choose **Wiki / owned**. If a monster Wiki strategy and method are available, the gear grid shows recommended owned items and the bank uses the original green/yellow/red ranked highlights.
8. Switch away from the task and back; the preset should reload automatically. Restart the plugin or change RuneLite character profiles to test that presets are isolated.
9. **Delete my setup for this task** removes just that saved task preset.

## Scope and limits

- The plugin **does not open the bank interface**, move/withdraw items, modify bank search or filter/rearrange the bank into a separate item layout. Those would require a supported integration with Inventory Setups or a separately implemented UI.
- A selected preset paints highlights on visible items in the normal personal bank. The existing Wiki rank overlay is unchanged in the Wiki mode. Both overlays use the current bank widget view and do not enumerate a bank container.
- Blue means **personally selected**, not mathematically best. Existing Wiki rankings remain optional and distinct from your personal choices. Missing required protections are still warned about in the existing Requirements section.
- Save records exact item IDs and inventory quantities at the moment of clicking. A different potion dose or equipment charge state is not silently treated as identical. This is a starting version: no manual slot editing, wildcard substitutions or missing-quantity badges yet.
- Saves for an active Slayer task or a deliberately selected task preview; a preview does not change the in-game assignment. No automatic game input.
- Presets use a locally stored UTF-8 schema keyed by the SHA-256 of RuneLite's RSProfileKey. Changing in-game display name to KEEZA0 should not change that RuneLite profile key. Existing bank snapshots remain untouched.
- Storage is under the plugin's local data folder, not synced RuneLite config. The controller uses the same bounded serial IO queue as bank snapshots and never sends saved item lists to network services.
- This source update is **0.10.2**; merging it does not publish a Plugin Hub release.

## How to verify the code

Run `gradlew.bat --no-daemon clean check`. The `verifyTaskLoadouts` check validates serialization, normalization, immutable arrays and error handling. Real bank-widget overlays require in-game verification.

If saving fails or no highlights appear, check the in-plugin status text and confirm **Saved setup bank highlights** is enabled in Ultimate Slayer settings.
