package vocaland.ui.components;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JPanel;

/** Clips backgrounds and child components to the same rounded outline. */
public class RoundedPanel extends JPanel {
    public RoundedPanel() { super(); setOpaque(false); }
    public RoundedPanel(LayoutManager layout) { super(layout); setOpaque(false); }
    @Override public void paint(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.clip(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 24, 24));
            super.paint(g);
        } finally { g.dispose(); }
        // Swing normally paints children after the border. Draw the border last
        // so opaque scroll panes and lists cannot cover its curved corners.
        super.paintBorder(graphics);
    }
    @Override protected void paintBorder(Graphics graphics) {
        // Deferred until after the children in paint().
    }
    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(getBackground());
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
        } finally { g.dispose(); }
        super.paintComponent(graphics);
    }
}
