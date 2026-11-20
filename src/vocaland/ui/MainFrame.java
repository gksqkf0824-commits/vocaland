package vocaland.ui;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JPanel;
import vocaland.service.WordService;
import vocaland.ui.rain.AcidRainPanel;

public class MainFrame extends JFrame {
    private static final String MAIN = "main";
    private static final String RAIN = "rain";

    private final CardLayout cards = new CardLayout();
    private final JPanel screens = new JPanel(cards);

    public MainFrame() {
        setTitle("보카랜드");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        MainPanel mainPanel = new MainPanel();
        AcidRainPanel rainPanel = createRainPanel();

        // 메인 → 산성비
        mainPanel.setOnOpenRain(() -> {
            cards.show(screens, RAIN);
            rainPanel.start();
        });
        // 산성비 → 메인 (AcidRainPanel이 나가기 전에 stop()을 호출함)
        rainPanel.setOnBackToMain(() -> cards.show(screens, MAIN));

        screens.add(mainPanel, MAIN);
        screens.add(rainPanel, RAIN);
        setContentPane(screens);

        setMinimumSize(new Dimension(800, 650));
        setSize(1000, 750);
        setLocationRelativeTo(null);
    }

    // words.txt 단어로 산성비 생성, 파일이 비어 있으면 게임에 들어 있는 임시 단어 사용
    private AcidRainPanel createRainPanel() {
        List<String[]> words = new WordService().getGameWords();
        if (words.isEmpty()) {
            System.err.println("[words.txt] 단어가 없어 임시 단어로 산성비를 시작합니다.");
            return new AcidRainPanel();
        }
        return new AcidRainPanel(words);
    }
}
