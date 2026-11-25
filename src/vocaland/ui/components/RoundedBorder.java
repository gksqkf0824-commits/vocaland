package vocaland.ui.components;

import java.awt.*;
import javax.swing.border.AbstractBorder;

/** Antialiased border with generous corners and stable layout insets. */
public final class RoundedBorder extends AbstractBorder {
    private final Color color;
    private final int thickness;
    public RoundedBorder(Color color, int thickness) { this.color = color; this.thickness = thickness; }
    @Override public Insets getBorderInsets(Component c) { return new Insets(thickness, thickness, thickness, thickness); }
    @Override public Insets getBorderInsets(Component c, Insets insets) {
        insets.set(thickness, thickness, thickness, thickness); return insets;
    }
    @Override public void paintBorder(Component c, Graphics graphics, int x, int y, int width, int height) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(color); g.setStroke(new BasicStroke(thickness));
            double inset = thickness / 2.0;
            g.draw(new java.awt.geom.RoundRectangle2D.Double(x + inset, y + inset,
                    width - thickness, height - thickness, 24, 24));
        } finally { g.dispose(); }
    }
}
