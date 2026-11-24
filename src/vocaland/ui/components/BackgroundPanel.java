package vocaland.ui.components;

import vocaland.ui.util.ImagePaths;

import java.awt.*;
import javax.swing.*;

public class BackgroundPanel extends JPanel {
    private static ImageIcon background;
    static { setBackgroundImage(null); }

    public static void setBackgroundImage(String path) {
        ImageIcon source = new ImageIcon(path == null ? ImagePaths.ROOT + ImagePaths.STORE_BACKGROUND_BG_BASIC : path);
        if (source.getIconWidth() <= 0) source = new ImageIcon(ImagePaths.ROOT + ImagePaths.STORE_BACKGROUND_BG_BASIC);
        background = source.getIconWidth() > 0
                ? new ImageIcon(source.getImage().getScaledInstance(620, 620, Image.SCALE_SMOOTH)) : null;
    }

    public static void paintBackground(Component component, Graphics graphics) {
        if (background == null) return;
        for (int y = 0; y < component.getHeight(); y += 620)
            for (int x = 0; x < component.getWidth(); x += 620)
                background.paintIcon(component, graphics, x, y);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        paintBackground(this, graphics);
    }
}
