package vocaland.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JButton;
import javax.swing.border.EmptyBorder;

public class RoundedButton extends JButton {
    private static final int SHADOW_DEPTH = 4;
    private static final Color SHADOW = new Color(32, 50, 15, 65);

    private final int radius;

    public RoundedButton(String text) {
        this(text, 16);
    }

    public RoundedButton(String text, int radius) {
        super(text);
        this.radius = radius;

        setForeground(Theme.TEXT);
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setBorder(new EmptyBorder(8, 16, 8 + SHADOW_DEPTH, 16));
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    public void paint(Graphics graphics) {
        if (!isPressed()) {
            super.paint(graphics);
            return;
        }

        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.translate(getWidth() * 0.015, getHeight() * 0.015 + 2);
            g.scale(0.97, 0.97);
            super.paint(g);
        } finally {
            g.dispose();
        }
    }

    private boolean isPressed() {
        return getModel().isPressed() && getModel().isArmed();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                               RenderingHints.VALUE_ANTIALIAS_ON);

            boolean pressed = isPressed();
            int drop = pressed ? 2 : 0;
            int width = getWidth() - 2;
            int height = getHeight() - SHADOW_DEPTH - 2;
            int arc = radius * 2;

            g.setColor(SHADOW);
            g.fillRoundRect(1, 1 + SHADOW_DEPTH - drop, width, height, arc, arc);

            Color fill = Theme.PRIMARY;
            if (!isEnabled()) {
                fill = Theme.PRIMARY_TINT;
            } else if (pressed) {
                fill = Theme.PRIMARY_PRESSED;
            } else if (getModel().isRollover()) {
                fill = Theme.PRIMARY_HOVER;
            }

            g.setColor(fill);
            g.fillRoundRect(1, 1, width, height, arc, arc);
            super.paintComponent(g);
        } finally {
            g.dispose();
        }
    }
}