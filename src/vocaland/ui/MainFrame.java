package vocaland.ui;

import java.awt.Dimension;
import javax.swing.JFrame;

public class MainFrame extends JFrame {
    public MainFrame() {
        setTitle("보카랜드");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setContentPane(new MainPanel());
        setMinimumSize(new Dimension(800, 650));
        setSize(1000, 750);
        setLocationRelativeTo(null);
    }
}