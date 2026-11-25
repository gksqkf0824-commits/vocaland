package vocaland.ui.components;

import vocaland.ui.util.ImagePaths;

import java.awt.*;
import javax.swing.*;

public class BackgroundPanel extends JPanel {
    private static ImageIcon background;
    static { setBackgroundImage(null); }

    public static void setBackgroundImage(String path) {
        // 결과가 캐시되어, 같은 배경으로 돌아올 때 다시 읽지 않음
        ImageIcon icon = vocaland.ui.util.ImageAssets.icon(path == null ? ImagePaths.ROOT + ImagePaths.STORE_BACKGROUND_BG_BASIC : path, 620, 620);
        if (icon == null) icon = vocaland.ui.util.ImageAssets.icon(ImagePaths.ROOT + ImagePaths.STORE_BACKGROUND_BG_BASIC, 620, 620);
        background = icon;
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
