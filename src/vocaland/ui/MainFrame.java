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
    private StorePanel store;
    private ScoreboardPanel ranking;
    private java.util.List<String[]> words;
    private CardLayout layout;
    private JPanel screens;
    private Runnable back;

    public MainFrame() {
        setTitle("보카랜드");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(800, 650));
        setSize(1000, 750);
        setLocationRelativeTo(null);
        // 로그인 화면에서 이름을 입력하는 동안 로비 → 나머지 화면 순서로 이미지를 미리 읽어 둠
        vocaland.ui.util.ImageAssets.preloadInBackground(vocaland.ui.util.ImagePaths.ROOT,
                "logo", "store/character", "store", "text", "game", "store/basketball", "store/background");
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

    /*
     * 로그인 직후에는 로비만 만들고, 나머지 화면은 처음 들어갈 때 만든다.
     * (예전엔 모든 화면을 한꺼번에 만들어서 로그인 [확인] 후 몇 초씩 멈췄음)
     */
    private void showApplication() {
        words = data.wordPairs();
        layout = new CardLayout();
        screens = new JPanel(layout);
        rain = null; basketball = null; study = null; matching = null; store = null; ranking = null;
        back = () -> safe(() -> {
            BackgroundPanel.setBackgroundImage(data.equippedImage("BACKGROUND"));
            lobby.refreshProfile();
            layout.show(screens, "main"); screens.repaint();
        });
        lobby = new LobbyPanel(data, () -> safe(() -> {
            data.logout();
            if (rain != null) rain.stop();
            if (basketball != null) basketball.stop();
            showLogin();
        }), () -> { ensureStudy(); study.showDifficultySelection(); layout.show(screens, "study"); },
        () -> safe(() -> {
            ensureRain();
            rain.setCharacterImage(data.equippedImage("CHARACTER"));
            layout.show(screens, "rain"); rain.start();
        }),
        () -> safe(() -> {
            ensureBasketball();
            basketball.setBallImage(data.equippedImage("BALL"));
            basketball.setHoopImage(data.equippedImage("HOOP"));
            layout.show(screens, "basketball"); basketball.start();
        }),
        () -> safe(() -> { ensureStore(); store.refreshData(); layout.show(screens, "store"); }),
        () -> safe(() -> { ensureRanking(); ranking.refreshData(); layout.show(screens, "scoreboard"); }));
        screens.add(lobby, "main");
        BackgroundPanel.setBackgroundImage(data.equippedImage("BACKGROUND"));
        setContentPane(screens); layout.show(screens, "main"); revalidate(); repaint();
    }

    private void ensureRain() {
        if (rain != null) return;
        rain = new AcidRainPanel(data.words().stream()
                .map(w -> new String[]{w.english(), w.meaning(), w.difficulty()}).toList());
        rain.setOnBackToMain(back);
        rain.setOnGameOver((score, level, missed) -> safe(() -> data.finishGame("RAIN", "NORMAL", score)));
        rain.setOnAnswer((word, correct) -> safe(() -> data.recordAnswer(word, correct)));
        screens.add(rain, "rain");
    }

    private void ensureBasketball() {
        if (basketball != null) return;
        basketball = new BasketballPanel(words);
        basketball.stop();
        basketball.setOnBackToMain(back);
        basketball.setOnGameOver((score, missed) -> safe(() -> data.finishGame("BASKETBALL", basketball.getDifficulty().name(), score)));
        basketball.setOnAnswer((word, correct) -> safe(() -> data.recordAnswer(word, correct)));
        screens.add(basketball, "basketball");
    }

    private void ensureStudy() {
        if (study != null) return;
        study = new StudyPanel(data.current().id(), back, () -> {
            ensureMatching();
            matching.setWordPool(study.getStudyWordPairs());
            layout.show(screens, "pair");
        });
        screens.add(study, "study");
    }

    private void ensureMatching() {
        if (matching != null) return;
        matching = new MatchingPanel(() -> { study.refreshReviews(); layout.show(screens, "study"); }, words);
        matching.setOnAnswer((word, correct) -> safe(() -> data.recordAnswer(word, correct)));
        screens.add(matching, "pair");
    }

    private void ensureStore() {
        if (store != null) return;
        store = new StorePanel(data, back);
        screens.add(store, "store");
    }

    private void ensureRanking() {
        if (ranking != null) return;
        ranking = new ScoreboardPanel(data, back);
        screens.add(ranking, "scoreboard");
    }
}
