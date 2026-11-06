package com.imth3keeza0.ultimateslayer;

import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/** Synthetic UI only: no client connection, browser opening or game interaction. */
public final class ComparisonPreview
{
    public static void main(String[] args) throws Exception
    {
        AtomicReference<UltimateSlayerPanel> ref = new AtomicReference<>(); AtomicInteger info = new AtomicInteger();
        SwingUtilities.invokeAndWait(() -> {
            UltimateSlayerPanel panel = new UltimateSlayerPanel(new UltimateSlayerConfig() {}, () -> {}); ref.set(panel);
            int[] equipment = new int[14], inventory = new int[28]; Arrays.fill(equipment,-1); Arrays.fill(inventory,-1);
            panel.update(new SidebarState("Abyssal demons",null,123,true,equipment,inventory,new int[28]),Map.of());
            panel.setAccountContext(CombatChecks.account(99,0)); panel.setInfoAction(info::incrementAndGet);
            BankSnapshot snapshot = CombatChecks.snapshot(); panel.setPlanningSnapshot(snapshot);
            panel.setOwnedGear(new OwnedGearCatalog(snapshot.capturedAt,List.of(
                CombatChecks.melee(4151,"Abyssal whip",false,82,82,0,82,0),
                CombatChecks.melee(7158,"Dragon 2h sword",true,100,120,0,100,0)),Map.of(),0));
            combo(panel,"Choose monster / variant").setSelectedIndex(1);
            button(panel,"Info").doClick(); require(info.get()==1,"Info opens only from the user action");
            button(panel,"Find best modeled setup").doClick();
        });
        try
        {
            long deadline = System.nanoTime()+20_000_000_000L; AtomicReference<Boolean> ready = new AtomicReference<>(false);
            while(!ready.get() && System.nanoTime()<deadline)
            { SwingUtilities.invokeAndWait(() -> ready.set(button(ref.get(),"Show in draft")!=null)); if(!ready.get()){Thread.sleep(50);} }
            require(ready.get(),"Background search returns a complete result to Swing");
            SwingUtilities.invokeAndWait(() -> {
                UltimateSlayerPanel panel=ref.get(); button(panel,"Show in draft").doClick();
                require(label(panel,"Best found modeled setup"),"Applying a result changes the sidebar draft only");
                render(panel,"build/combat-preview.png",225);
                button(panel,"Magic").doClick(); require(!label(panel,"Best found modeled setup"),"Changing style invalidates the displayed optimized draft");
                AtomicInteger close=new AtomicInteger(); JComponent welcome=WelcomeDialog.content(close::incrementAndGet);
                require(button(welcome,"Join our Discord")!=null && WelcomeDialog.DISCORD_URL.equals(button(welcome,"Join our Discord").getToolTipText()),"Exact AFK Timers invite retained");
                button(welcome,"Got it").doClick(); require(close.get()==1,"Welcome can be dismissed without joining");
                render(welcome,"build/welcome-preview.png",welcome.getPreferredSize().width);
                button(panel,"Find best modeled setup").doClick();
                panel.resetAccountContext(); panel.refreshAppearance();
            });
            Thread.sleep(150);
            SwingUtilities.invokeAndWait(() -> {
                require(button(ref.get(),"Show in draft")==null,"Late search completion cannot restore a prior account's result");
                require(!((javax.swing.JCheckBox)button(ref.get(),"On matching task (assume)")).isSelected(),"On-task assumption resets");
                require(!((javax.swing.JCheckBox)button(ref.get(),"Assume charged items usable")).isSelected(),"Charge assumption resets");
            });
            BufferedImage logo=ImageIO.read(WelcomeDialog.class.getResource("branding/logo.png"));
            require(logo!=null && logo.getColorModel().hasAlpha() && (logo.getRGB(0,0)>>>24)==0,"Packaged brand image has actual transparent alpha");
            System.out.println("Asynchronous comparison, apply-to-draft, account cancellation, optional clan page and transparent-logo previews passed.");
        }
        finally { SwingUtilities.invokeAndWait(() -> ref.get().disposePanel()); }
    }
    private static void render(JComponent component,String path,int width)
    {
        try
        {
            component.setSize(width,component.getPreferredSize().height); layout(component); checkBounds(component);
            BufferedImage image=new BufferedImage(component.getWidth(),component.getHeight(),BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics=image.createGraphics(); component.printAll(graphics); graphics.dispose();
            File file=new File(path); file.getParentFile().mkdirs(); ImageIO.write(image,"png",file);
        }
        catch(Exception e){throw new IllegalStateException(e);}
    }
    private static void layout(Container root){root.doLayout();for(java.awt.Component child:root.getComponents()){if(child instanceof Container){layout((Container)child);}}}
    private static void checkBounds(Container parent)
    {
        for(java.awt.Component child:parent.getComponents())
        {
            if(parent.getLayout() instanceof javax.swing.BoxLayout && (child instanceof JLabel||child instanceof JComboBox)
                && (child.getX()<0||child.getX()+child.getWidth()>parent.getWidth())){throw new AssertionError("Sidebar content clipped");}
            if(child instanceof Container){checkBounds((Container)child);}
        }
    }
    private static AbstractButton button(Container parent,String text)
    {
        for(java.awt.Component child:parent.getComponents())
        {
            if(child instanceof AbstractButton&&text.equals(((AbstractButton)child).getText())){return (AbstractButton)child;}
            if(child instanceof Container){AbstractButton found=button((Container)child,text);if(found!=null){return found;}}
        }return null;
    }
    private static JComboBox<?> combo(Container parent,Object first)
    {
        for(java.awt.Component child:parent.getComponents())
        {
            if(child instanceof JComboBox&&((JComboBox<?>)child).getItemCount()>0&&first.equals(((JComboBox<?>)child).getItemAt(0))){return (JComboBox<?>)child;}
            if(child instanceof Container){JComboBox<?> found=combo((Container)child,first);if(found!=null){return found;}}
        }return null;
    }
    private static boolean label(Container parent,String text)
    {for(java.awt.Component child:parent.getComponents()){if(child instanceof JLabel&&((JLabel)child).getText().contains(text)){return true;}if(child instanceof Container&&label((Container)child,text)){return true;}}return false;}
    private static void require(boolean value,String reason){if(!value){throw new AssertionError(reason);}}
}
