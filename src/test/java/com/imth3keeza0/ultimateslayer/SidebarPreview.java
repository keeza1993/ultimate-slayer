package com.imth3keeza0.ultimateslayer;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/** Standalone UI check; never connects to or controls the game. */
public final class SidebarPreview
{
    public static void main(String[] args) throws Exception
    {
        int[] gear = new int[14];
        int[] inventory = new int[28];
        SidebarState state = new SidebarState("Test", null, 1, true, gear, inventory, new int[28]);
        gear[0] = 99;
        if (state.equipmentAt(0) == 99) { throw new AssertionError("Snapshot must copy input"); }
        if (!"100K".equals(UltimateSlayerPanel.formatQuantity(100_000))) { throw new AssertionError("Quantity format"); }
        SwingUtilities.invokeAndWait(() -> {
            try
            {
                UltimateSlayerPanel panel = new UltimateSlayerPanel(new UltimateSlayerConfig() {}, () -> {});
                // Synthetic owned state exercises the comparison layout without a real account.
                panel.update(new SidebarState("Dust devils", null, 123, true, new int[14], new int[28], new int[28]), java.util.Map.of());
                panel.setOwnedGear(new OwnedGearCatalog(System.currentTimeMillis(), java.util.List.of(
                    new OwnedGearPlanner.Gear(net.runelite.api.gameval.ItemID.SLAYER_FACEMASK, net.runelite.api.gameval.ItemID.SLAYER_FACEMASK,
                        "Facemask", 0, false, 0, 0, 0, 0, 0, 0),
                    new OwnedGearPlanner.Gear(1, 1, "Synthetic weapon A", 3, false, 80, 0, 0, 82, 0, 0),
                    new OwnedGearPlanner.Gear(2, 2, "Synthetic weapon B", 3, false, 70, 0, 0, 75, 0, 0)), java.util.Map.of(), 0));
                javax.swing.JComboBox<?> taskSelector = findCombo(panel, "Current task");
                taskSelector.setSelectedItem("Hydras");
                if (!hasLabel(panel, "PREVIEW ONLY") || !hasLabel(panel, "Dust devils"))
                { throw new AssertionError("Preview must be separate from the real task display"); }
                findCombo(panel, "Current task").setSelectedItem("Current task");
                if (hasLabel(panel, "PREVIEW ONLY")) { throw new AssertionError("Return to current task failed"); }
                findCombo(panel, "Current task").setSelectedItem("Hydras");
                javax.swing.JComboBox<?> reward = findCombo(panel, TaskReferenceData.Exemption.UNKNOWN);
                reward.setSelectedItem(TaskReferenceData.Exemption.REWARDS_CLAIMED);
                panel.resetAccountContext();
                panel.refreshAppearance();
                findCombo(panel, "Current task").setSelectedItem("Hydras");
                if (findCombo(panel, TaskReferenceData.Exemption.UNKNOWN).getSelectedItem() != TaskReferenceData.Exemption.UNKNOWN)
                { throw new AssertionError("Account reset must clear diary assumptions"); }
                findCombo(panel, "Choose monster / variant").setSelectedIndex(1);
                if (!hasLabel(panel, "HP ")) { throw new AssertionError("Selecting a catalog variant must display its stats"); }
                panel.setSize(225, panel.getPreferredSize().height);
                layout(panel);
                BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g = image.createGraphics();
                panel.printAll(g);
                g.dispose();
                File output = new File("build/sidebar-preview.png");
                output.getParentFile().mkdirs();
                assertSidebarBounds(panel);
                ImageIO.write(image, "png", output);
                findStyle(panel, "Magic").doClick();
                panel.setPlanningSnapshot(new BankSnapshot(System.currentTimeMillis(), java.util.Map.of(
                    net.runelite.api.gameval.ItemID.AIRRUNE, 70L, net.runelite.api.gameval.ItemID.FIRERUNE, 100L, net.runelite.api.gameval.ItemID.WRATHRUNE, 5L), java.util.Map.of(), java.util.Map.of()));
                findCombo(panel, "Choose spell").setSelectedItem("Fire Surge");
                if (!hasLabel(panel, "loose runes: 5")) { throw new AssertionError("Spell plan must render cached rune count"); }
                panel.setSize(225, panel.getPreferredSize().height); layout(panel); assertSidebarBounds(panel);
                BufferedImage magicImage = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D magicGraphics = magicImage.createGraphics(); panel.printAll(magicGraphics); magicGraphics.dispose();
                ImageIO.write(magicImage, "png", new File("build/sidebar-magic-preview.png"));
                panel.resetAccountContext(); panel.refreshAppearance();
                if (findCombo(panel, "Choose spell").getSelectedIndex() != 0) { throw new AssertionError("Profile reset clears spell assumptions"); }
                UltimateSlayerPanel minimal = new UltimateSlayerPanel(new UltimateSlayerConfig() {
                    public boolean equipmentEnabled() { return false; } public boolean inventoryEnabled() { return false; }
                    public boolean monstersEnabled() { return false; } public boolean spellsEnabled() { return false; }
                }, () -> {});
                if (hasLabel(minimal, "MONSTER REFERENCE") || hasLabel(minimal, "SPELL PLAN")) { throw new AssertionError("Optional modules stay hidden"); }
                System.out.println("Snapshot checks passed; preview: " + output.getAbsolutePath());
            }
            catch (Exception e) { throw new IllegalStateException(e); }
        });
    }

    private static javax.swing.AbstractButton findStyle(java.awt.Container root, String text)
    {
        for (java.awt.Component child : root.getComponents())
        {
            if (child instanceof javax.swing.AbstractButton && ((javax.swing.AbstractButton) child).getText().equals(text)) { return (javax.swing.AbstractButton) child; }
            if (child instanceof java.awt.Container) { javax.swing.AbstractButton found = findStyle((java.awt.Container) child, text); if (found != null) { return found; } }
        }
        return null;
    }
    private static void assertSidebarBounds(java.awt.Container parent)
    {
        for (java.awt.Component child : parent.getComponents())
        {
            if (parent.getLayout() instanceof javax.swing.BoxLayout && (child instanceof javax.swing.JLabel || child instanceof javax.swing.JComboBox)
                && (child.getX() < 0 || child.getX() + child.getWidth() > parent.getWidth()))
            { throw new AssertionError("Sidebar content clipped: " + child.getClass().getSimpleName()); }
            if (child instanceof java.awt.Container) { assertSidebarBounds((java.awt.Container) child); }
        }
    }

    private static void layout(java.awt.Container container)
    {
        container.doLayout();
        for (java.awt.Component child : container.getComponents())
        {
            if (child instanceof java.awt.Container) { layout((java.awt.Container) child); }
        }
    }

    private static javax.swing.JComboBox<?> findCombo(java.awt.Container root, Object first)
    {
        for (java.awt.Component child : root.getComponents())
        {
            if (child instanceof javax.swing.JComboBox)
            {
                javax.swing.JComboBox<?> combo = (javax.swing.JComboBox<?>) child;
                if (combo.getItemCount() > 0 && first.equals(combo.getItemAt(0))) { return combo; }
            }
            if (child instanceof java.awt.Container)
            {
                javax.swing.JComboBox<?> nested = findCombo((java.awt.Container) child, first);
                if (nested != null) { return nested; }
            }
        }
        return null;
    }

    private static boolean hasLabel(java.awt.Container root, String text)
    {
        for (java.awt.Component child : root.getComponents())
        {
            if (child instanceof javax.swing.JLabel && ((javax.swing.JLabel) child).getText().contains(text)) { return true; }
            if (child instanceof java.awt.Container && hasLabel((java.awt.Container) child, text)) { return true; }
        }
        return false;
    }
}
