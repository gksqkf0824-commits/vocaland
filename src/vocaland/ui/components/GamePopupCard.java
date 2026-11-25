package vocaland.ui.components;

import vocaland.ui.theme.Theme;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Shared presentation for the two games' ready and result overlays. */
public final class GamePopupCard extends JPanel {
    public GamePopupCard() {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(26, 40, 26, 40));
    }
    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(255, 255, 255, 240));
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 28, 28);
            g.setColor(Theme.TEXT);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 28, 28);
        } finally { g.dispose(); }
    }
}
