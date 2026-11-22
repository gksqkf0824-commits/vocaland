package vocaland.ui;

import java.awt.CardLayout;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JPanel;
import vocaland.ui.basketball.BasketballPanel;
import vocaland.ui.rain.AcidRainPanel;
import vocaland.ui.store.StorePanel;
import vocaland.ui.study.PairPanel;
import vocaland.ui.study.StudyPanel;

public class MainFrame extends JFrame {
    public MainFrame() {
        setTitle("보카랜드");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        CardLayout layout = new CardLayout();
        JPanel screens = new JPanel(layout);

        AcidRainPanel rainPanel = new AcidRainPanel();
        BasketballPanel basketballPanel = new BasketballPanel();
        basketballPanel.stop();
        StorePanel storePanel = new StorePanel(() -> layout.show(screens, "main"));
        MainPanel mainPanel = new MainPanel(
                () -> layout.show(screens, "study"),
                () -> {
                    layout.show(screens, "rain");
                    rainPanel.start();
                },
                () -> {
                    layout.show(screens, "basketball");
                    basketballPanel.start();
                },
                () -> layout.show(screens, "store")
        );
        rainPanel.setOnBackToMain(() -> {
            rainPanel.stop();
            layout.show(screens, "main");
        });
        basketballPanel.setOnBackToMain(() -> {
            basketballPanel.stop();
            layout.show(screens, "main");
        });
        StudyPanel studyPanel = new StudyPanel(
                () -> layout.show(screens, "main"),
                () -> layout.show(screens, "pair")
        );
        PairPanel pairPanel = new PairPanel(() -> layout.show(screens, "study"));

        screens.add(mainPanel, "main");
        screens.add(studyPanel, "study");
        screens.add(pairPanel, "pair");
        screens.add(rainPanel, "rain");
        screens.add(basketballPanel, "basketball");
        screens.add(storePanel, "store");

        setContentPane(screens);
        layout.show(screens, "main");

        setMinimumSize(new Dimension(800, 650));
        setSize(1000, 750);
        setLocationRelativeTo(null);
    }
}
