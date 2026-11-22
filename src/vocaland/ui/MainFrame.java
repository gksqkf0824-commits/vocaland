package vocaland.ui;

import java.awt.CardLayout;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class MainFrame extends JFrame {
    public MainFrame() {
        setTitle("보카랜드");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        CardLayout layout = new CardLayout();
        JPanel screens = new JPanel(layout);

        MainPanel mainPanel = new MainPanel(() -> layout.show(screens, "study"));
        StudyPanel studyPanel = new StudyPanel(
                () -> layout.show(screens, "main"),
                () -> layout.show(screens, "pair")
        );
        PairPanel pairPanel = new PairPanel(() -> layout.show(screens, "study"));

        screens.add(mainPanel, "main");
        screens.add(studyPanel, "study");
        screens.add(pairPanel, "pair");

        setContentPane(screens);
        layout.show(screens, "main");

        setMinimumSize(new Dimension(800, 650));
        setSize(1000, 750);
        setLocationRelativeTo(null);
    }
}