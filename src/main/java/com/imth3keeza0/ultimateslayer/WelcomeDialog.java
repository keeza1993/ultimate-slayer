package com.imth3keeza0.ultimateslayer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import net.runelite.client.util.LinkBrowser;
import net.runelite.client.util.ImageUtil;

/** Same optional introduction/clan-page style as AFK Timers. Browsing requires a click. */
final class WelcomeDialog
{
    static final String DISCORD_URL = "https://discord.gg/UAAe6QEGD";
    static JDialog create(Window owner)
    {
        JDialog dialog = new JDialog(owner, "Welcome to Ultimate Slayer");
        dialog.setModal(false); dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setContentPane(content(dialog::dispose));
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.pack(); dialog.setResizable(false); dialog.setLocationRelativeTo(owner); return dialog;
    }
    static JPanel content(Runnable close)
    {
        Color background = new Color(35, 39, 46); JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(background); panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        JPanel header = new JPanel(new BorderLayout(18, 0)); header.setOpaque(false);
        BufferedImage logo = ImageUtil.loadImageResource(WelcomeDialog.class, "branding/logo.png");
        if (logo != null)
        {
            Image image = logo.getScaledInstance(150, 150, Image.SCALE_SMOOTH);
            header.add(new JLabel(new ImageIcon(image)), BorderLayout.WEST);
        }
        header.add(new JLabel("<html><body style='color:#eeeeee;font-family:sans-serif'><h1 style='color:#57d6dc'>Ultimate Slayer</h1><p><b>Created by Th3keeza0</b></p><p>OSRS geezers · v0.8.0</p></body></html>"), BorderLayout.CENTER);
        panel.add(header, BorderLayout.NORTH);
        JLabel text = new JLabel("<html><body style='width:370px;color:#eeeeee;font-family:sans-serif'>"
            + "<h3 style='color:#57d6dc'>Get started</h3><p><b>Open your bank once</b> to capture the equipment you own. Withdrawals and searches do not scan it again; close and reopen to refresh.</p><br>"
            + "<p><b>Choose the actual monster variant</b> in the sidebar. Your real assignment stays separate from any preview.</p><br>"
            + "<p><b>Compare owned setups:</b> choose a spell if needed, check the on-task, charge and prayer assumptions, then press <b>Find best modeled setup</b>. Use <b>Show in draft</b> to view a result. This never equips or moves anything in game.</p><br>"
            + "<h3>Know what is being compared</h3><p>Estimates cover the supported normal attacks and set effects. The search is bounded, and some weapons/boss mechanics are not modeled. <b>CHECK</b> means requirements or item state still need confirming. Your snapshot is local to this character; it is never uploaded.</p><br>"
            + "<h3 style='color:#efc370'>Join OSRS geezers</h3><p>Want to join the clan? Add <b>Th3keeza0</b> in game, or join our Discord below.</p><br>"
            + "<p style='color:#b8bec8'>This welcome appears once. Reopen it with <b>Info</b> in the sidebar or <b>Show welcome</b> in plugin settings.</p></body></html>");
        panel.add(text, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); buttons.setOpaque(false);
        JButton discord = new JButton("Join our Discord"); discord.setToolTipText(DISCORD_URL);
        discord.addActionListener(e -> LinkBrowser.browse(DISCORD_URL));
        JButton done = new JButton("Got it"); done.addActionListener(e -> close.run()); buttons.add(discord); buttons.add(done);
        panel.add(buttons, BorderLayout.SOUTH); return panel;
    }
    private WelcomeDialog() {}
}
