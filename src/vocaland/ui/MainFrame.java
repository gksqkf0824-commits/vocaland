package vocaland.ui;

import vocaland.ui.lobby.LobbyPanel;
import vocaland.ui.login.LoginPanel;
import vocaland.ui.components.BackgroundPanel;

import java.awt.*;
import java.nio.file.Path;
import javax.swing.*;
import vocaland.service.LocalData;
import vocaland.ui.basketball.BasketballPanel;
import vocaland.ui.rain.AcidRainPanel;
import vocaland.ui.store.StorePanel;
import vocaland.ui.scoreboard.ScoreboardPanel;
import vocaland.ui.study.MatchingPanel;
import vocaland.ui.study.StudyPanel;

public class MainFrame extends JFrame {
    private final LocalData data;
    private AcidRainPanel rain;
    private BasketballPanel basketball;
    private LobbyPanel lobby;
    private StudyPanel study;
    private MatchingPanel matching;

    public MainFrame() {
        setTitle("보카랜드");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(800, 650));
        setSize(1000, 750);
        setLocationRelativeTo(null);
        data = new LocalData(Path.of("data"));
        showLogin();
        if (data.current() != null) safe(this::showApplication);
    }

    private void safe(Runnable action) {
        try { action.run(); }
        catch (RuntimeException error) {
            JOptionPane.showMessageDialog(this, error.getMessage(), "처리할 수 없어요", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showLogin() {
        BackgroundPanel.setBackgroundImage(null);
        setContentPane(new LoginPanel(name -> safe(() -> {
            data.login(name);
            showApplication();
        })));
        revalidate(); repaint();
    }

    private void showApplication() {
        var words = data.wordPairs();
        CardLayout layout = new CardLayout();
        JPanel screens = new JPanel(layout);
        rain = new AcidRainPanel(data.words().stream()
                .map(w -> new String[]{w.english(), w.meaning(), w.difficulty()}).toList());
        basketball = new BasketballPanel(words);
        basketball.stop();
        Runnable back = () -> safe(() -> {
            BackgroundPanel.setBackgroundImage(data.equippedImage("BACKGROUND"));
            lobby.refreshProfile();
            layout.show(screens, "main"); screens.repaint();
        });
        StorePanel store = new StorePanel(data, back);
        ScoreboardPanel ranking = new ScoreboardPanel(data, back);
        study = new StudyPanel(data.current().id(), back, () -> {
            matching.setWordPool(study.getStudyWordPairs());
            layout.show(screens, "pair");
        });
        matching = new MatchingPanel(() -> { study.refreshReviews(); layout.show(screens, "study"); }, words);
        matching.setOnAnswer((word, correct) -> safe(() -> data.recordAnswer(word, correct)));
        lobby = new LobbyPanel(data, () -> safe(() -> {
            data.logout(); rain.stop(); basketball.stop(); showLogin();
        }), () -> { study.showDifficultySelection(); layout.show(screens, "study"); },
        () -> safe(() -> {
            rain.setCharacterImage(data.equippedImage("CHARACTER"));
            layout.show(screens, "rain"); rain.start();
        }),
        () -> safe(() -> {
            basketball.setBallImage(data.equippedImage("BALL"));
            basketball.setHoopImage(data.equippedImage("HOOP"));
            layout.show(screens, "basketball"); basketball.start();
        }),
        () -> safe(() -> { store.refreshData(); layout.show(screens, "store"); }),
        () -> safe(() -> { ranking.refreshData(); layout.show(screens, "scoreboard"); }));
        rain.setOnBackToMain(back);
        basketball.setOnBackToMain(back);
        rain.setOnGameOver((score, level, missed) -> safe(() -> data.finishGame("RAIN", "NORMAL", score)));
        basketball.setOnGameOver((score, missed) -> safe(() -> data.finishGame("BASKETBALL", basketball.isHardMode() ? "HARD" : "EASY", score)));
        rain.setOnAnswer((word, correct) -> safe(() -> data.recordAnswer(word, correct)));
        basketball.setOnAnswer((word, correct) -> safe(() -> data.recordAnswer(word, correct)));
        screens.add(lobby, "main"); screens.add(study, "study"); screens.add(matching, "pair");
        screens.add(rain, "rain"); screens.add(basketball, "basketball");
        screens.add(store, "store"); screens.add(ranking, "scoreboard");
        BackgroundPanel.setBackgroundImage(data.equippedImage("BACKGROUND"));
        setContentPane(screens); layout.show(screens, "main"); revalidate(); repaint();
    }
}
