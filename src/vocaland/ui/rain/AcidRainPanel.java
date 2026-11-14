package vocaland.ui.rain;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import vocaland.ui.RoundedButton;
import vocaland.ui.Theme;

/*
 * AcidRainPanel - 산성비 게임 화면 (화면 조립, 타이머, 입력 처리)
 *
 * start() / stop()  : 화면에 들어올 때 시작 대기 상태로, 나갈 때 타이머 정지
 * setOnBackToMain() : ← 메인으로 버튼을 눌렀을 때 실행할 코드 등록 (화면 전환용)
 * setOnGameOver()   : 게임이 끝났을 때 점수·레벨·놓친 단어를 받을 코드 등록 (백엔드용)
 * startGame()       : 고른 모드로 게임 시작
 * tick()            : 타이머가 0.03초마다 호출, 게임 진행 후 화면 갱신
 * submitAnswer()    : 입력값을 판정하고 정답/오답 표시
 * endGame()         : 게임 오버 처리
 * showReady()       : 시작 대기 안내창 (모드 선택, 시작하기)
 * showGameOver()    : 게임 오버 안내창 (점수, 다시하기, 메인으로)
 * createHeader()    : 상단 (제목, 남은 기회, 점수, 메인으로)
 * createCenter()    : 가운데 (게임 영역, 오답 복습 목록)
 * createInputBar()  : 하단 (안내 문구, 입력창, 입력 버튼, 정답 표시)
 * sampleWords()     : 임시 단어 목록 (백엔드 단어 불러오기가 생기면 교체)
 * main()            : 혼자 테스트용 실행
 */
public class AcidRainPanel extends JPanel {

    public interface GameOverListener {
        void onGameOver(int score, int level, List<FallingWord> missedWords);
    }

    private static final int TICK_MS = 30;
    private static final Color BACKGROUND = new Color(250, 251, 248);
    private static final Color CORRECT = new Color(46, 140, 70);
    private static final Color WRONG = new Color(210, 70, 60);

    private final RainGame game;
    private final RainGameCanvas canvas;
    private final Timer gameTimer = new Timer(TICK_MS, e -> tick());
    private final Timer feedbackTimer;

    private final HeartsView hearts = new HeartsView();
    private final JLabel scoreLabel = new JLabel();

    private final DefaultListModel<String> reviewModel = new DefaultListModel<>();
    private final JLabel reviewTitle = new JLabel();

    private final JLabel guideLabel = new JLabel();
    private final JTextField inputField = new JTextField();
    private final JButton submitButton = new RoundedButton("입력", 14);
    private final JLabel feedbackLabel = new JLabel(" ");

    private final JPanel overlay = new RoundedCard();
    private InputMode selectedMode = InputMode.SPELLING;

    private Runnable onBackToMain;
    private GameOverListener onGameOver;

    public AcidRainPanel() {
        this(sampleWords());
    }

    public AcidRainPanel(List<String[]> words) {
        game = new RainGame(words);
        canvas = new RainGameCanvas(game);

        feedbackTimer = new Timer(1200, e -> feedbackLabel.setText(" "));
        feedbackTimer.setRepeats(false);

        setLayout(new BorderLayout(0, 14));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(18, 28, 20, 28));

        add(createHeader(), BorderLayout.NORTH);
        add(createCenter(), BorderLayout.CENTER);
        add(createInputBar(), BorderLayout.SOUTH);

        canvas.add(overlay);
        showReady();
    }

    public void start() {
        showReady();
    }

    public void stop() {
        gameTimer.stop();
        feedbackTimer.stop();
    }

    public void setOnBackToMain(Runnable onBackToMain) {
        this.onBackToMain = onBackToMain;
    }

    public void setOnGameOver(GameOverListener onGameOver) {
        this.onGameOver = onGameOver;
    }

    private void startGame() {
        game.start(selectedMode);
        reviewModel.clear();
        overlay.setVisible(false);

        guideLabel.setText(selectedMode.getGuide());
        inputField.setText("");
        setInputEnabled(true);
        refreshStatus();

        gameTimer.start();
        inputField.requestFocusInWindow();
    }

    private void tick() {
        List<FallingWord> landed = game.update(TICK_MS, canvas.getAreaWidth(), canvas.getGroundY());

        for (FallingWord word : landed) {
            reviewModel.addElement(word.getReviewText());
            showFeedback("놓쳤어요! 기회 -1", WRONG);
        }
        if (!landed.isEmpty()) {
            refreshStatus();
        }

        if (game.getState() == RainGame.State.GAME_OVER) {
            endGame();
        }
        canvas.repaint();
    }

    private void submitAnswer() {
        String input = inputField.getText();
        inputField.setText("");
        if (input.isBlank() || game.getState() != RainGame.State.PLAYING) {
            return;
        }

        int levelBefore = game.getLevel();
        int points = game.submit(input);

        if (points > 0) {
            String text = "정답 +" + points + "점";
            if (game.getLevel() > levelBefore) {
                text += "  Lv." + game.getLevel() + "!";
            }
            showFeedback(text, CORRECT);
        } else {
            showFeedback("오답!", WRONG);
        }
        refreshStatus();
        canvas.repaint();
    }

    private void endGame() {
        gameTimer.stop();
        setInputEnabled(false);
        showGameOver();

        if (onGameOver != null) {
            onGameOver.onGameOver(game.getScore(), game.getLevel(), new ArrayList<>(game.getMissedWords()));
        }
    }

    private void goToMain() {
        stop();
        if (onBackToMain != null) {
            onBackToMain.run();
        }
    }

    private void refreshStatus() {
        hearts.setLives(game.getLives());
        scoreLabel.setText("점수 " + String.format("%,d", game.getScore()));
        reviewTitle.setText("오답 복습 (" + reviewModel.size() + ")");
    }

    private void showFeedback(String text, Color color) {
        feedbackLabel.setText(text);
        feedbackLabel.setForeground(color);
        feedbackTimer.restart();
    }

    private void setInputEnabled(boolean enabled) {
        inputField.setEnabled(enabled);
        submitButton.setEnabled(enabled);
    }

    private void showReady() {
        stop();
        game.reset();
        reviewModel.clear();
        setInputEnabled(false);
        guideLabel.setText(selectedMode.getGuide());
        feedbackLabel.setText(" ");
        refreshStatus();

        overlay.removeAll();
        overlay.add(centered(label("산성비 게임", 28)));
        overlay.add(Box.createVerticalStrut(6));
        overlay.add(centered(label("단어가 바닥에 닿기 전에 정답을 입력하세요!", 15)));
        overlay.add(Box.createVerticalStrut(18));
        overlay.add(centered(label("모드 선택", 16)));
        overlay.add(Box.createVerticalStrut(8));

        JPanel modes = transparent(new FlowLayout(FlowLayout.CENTER, 10, 0));
        for (InputMode mode : InputMode.values()) {
            JButton button = new RoundedButton(modeText(mode), 14);
            button.setFont(font(15));
            button.addActionListener(e -> {
                selectedMode = mode;
                showReady();
            });
            modes.add(button);
        }
        overlay.add(modes);
        overlay.add(Box.createVerticalStrut(8));
        overlay.add(centered(label(selectedMode.getGuide(), 14)));
        overlay.add(Box.createVerticalStrut(18));

        JButton startButton = new RoundedButton("시작하기", 16);
        startButton.setFont(font(20));
        startButton.addActionListener(e -> startGame());
        overlay.add(centered(startButton));

        overlay.setVisible(true);
        overlay.revalidate();
        canvas.repaint();
    }

    private void showGameOver() {
        overlay.removeAll();
        overlay.add(centered(label("게임 오버", 30)));
        overlay.add(Box.createVerticalStrut(10));
        overlay.add(centered(label("점수 " + String.format("%,d", game.getScore())
                + "   ·   Lv." + game.getLevel(), 20)));
        overlay.add(Box.createVerticalStrut(6));
        overlay.add(centered(label("놓친 단어 " + game.getMissedWords().size()
                + "개는 오른쪽 오답 복습에서 확인하세요.", 14)));
        overlay.add(Box.createVerticalStrut(18));

        JPanel buttons = transparent(new FlowLayout(FlowLayout.CENTER, 10, 0));
        JButton retry = new RoundedButton("다시하기", 14);
        retry.setFont(font(17));
        retry.addActionListener(e -> {
            List<String> keep = new ArrayList<>();
            for (int i = 0; i < reviewModel.size(); i++) {
                keep.add(reviewModel.get(i));
            }
            showReady();
            keep.forEach(reviewModel::addElement);
            refreshStatus();
        });
        JButton main = new RoundedButton("메인으로", 14);
        main.setFont(font(17));
        main.addActionListener(e -> goToMain());
        buttons.add(retry);
        buttons.add(main);
        overlay.add(buttons);

        overlay.setVisible(true);
        overlay.revalidate();
        canvas.repaint();
    }

    private String modeText(InputMode mode) {
        return (mode == selectedMode ? "● " : "○ ") + mode.getLabel();
    }

    private JComponent createHeader() {
        JPanel header = transparent(new BorderLayout());

        JLabel title = label("산성비 게임", 30);
        header.add(title, BorderLayout.WEST);

        JPanel right = transparent(new FlowLayout(FlowLayout.RIGHT, 10, 0));

        JPanel livesBox = transparent(new FlowLayout(FlowLayout.CENTER, 8, 0));
        livesBox.add(label("남은 기회", 15));
        livesBox.add(hearts);
        right.add(boxed(livesBox));

        scoreLabel.setFont(font(17));
        scoreLabel.setForeground(Theme.TEXT);
        right.add(boxed(scoreLabel));

        JButton back = new RoundedButton("← 메인으로", 14);
        back.setFont(font(15));
        back.addActionListener(e -> goToMain());
        right.add(back);

        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JComponent createCenter() {
        JPanel center = transparent(new BorderLayout(14, 0));
        center.add(canvas, BorderLayout.CENTER);

        JPanel review = new JPanel(new BorderLayout());
        review.setBackground(Color.WHITE);
        review.setBorder(BorderFactory.createLineBorder(Theme.TEXT, 2));
        review.setPreferredSize(new Dimension(210, 0));

        reviewTitle.setFont(font(17));
        reviewTitle.setForeground(Theme.TEXT);
        reviewTitle.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, Theme.TEXT),
                new EmptyBorder(10, 12, 8, 12)));
        review.add(reviewTitle, BorderLayout.NORTH);

        JList<String> list = new JList<>(reviewModel);
        list.setFont(new Font(Font.DIALOG, Font.PLAIN, 15));
        list.setForeground(Theme.TEXT);
        list.setFixedCellHeight(28);
        list.setBorder(new EmptyBorder(6, 12, 6, 12));
        list.setFocusable(false);
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        review.add(scroll, BorderLayout.CENTER);

        center.add(review, BorderLayout.EAST);
        return center;
    }

    private JComponent createInputBar() {
        JPanel bar = transparent(new BorderLayout(0, 8));
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(2, 0, 0, 0, Theme.TEXT),
                new EmptyBorder(10, 0, 0, 0)));

        guideLabel.setFont(font(16));
        guideLabel.setForeground(Theme.TEXT);
        bar.add(guideLabel, BorderLayout.NORTH);

        JPanel row = transparent(new BorderLayout(12, 0));

        inputField.setFont(new Font(Font.DIALOG, Font.PLAIN, 19));
        inputField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.TEXT, 2, true),
                new EmptyBorder(6, 12, 6, 12)));
        inputField.addActionListener(e -> submitAnswer());
        row.add(inputField, BorderLayout.CENTER);

        JPanel right = transparent(new FlowLayout(FlowLayout.LEFT, 12, 0));
        submitButton.setFont(font(16));
        submitButton.addActionListener(e -> {
            submitAnswer();
            inputField.requestFocusInWindow();
        });
        right.add(submitButton);

        feedbackLabel.setFont(font(20));
        feedbackLabel.setPreferredSize(new Dimension(220, 36));
        right.add(feedbackLabel);
        row.add(right, BorderLayout.EAST);

        bar.add(row, BorderLayout.CENTER);
        return bar;
    }

    private static Font font(int size) {
        return new Font(Font.DIALOG, Font.BOLD, size);
    }

    private static JLabel label(String text, int size) {
        JLabel label = new JLabel(text);
        label.setFont(font(size));
        label.setForeground(Theme.TEXT);
        return label;
    }

    private static JPanel transparent(java.awt.LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    private static JComponent centered(JComponent component) {
        component.setAlignmentX(Component.CENTER_ALIGNMENT);
        return component;
    }

    private static JComponent boxed(JComponent content) {
        JPanel box = new JPanel(new BorderLayout());
        box.setBackground(Color.WHITE);
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.TEXT, 2, true),
                new EmptyBorder(5, 12, 5, 12)));
        box.add(content);
        return box;
    }

    private static class RoundedCard extends JPanel {
        RoundedCard() {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(new EmptyBorder(26, 40, 26, 40));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(255, 255, 255, 240));
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 28, 28);
            g.setColor(Theme.TEXT);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 28, 28);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static class HeartsView extends JComponent {
        private static final int SIZE = 22;
        private static final int GAP = 4;
        private final Image heart = loadHeart();
        private int lives = RainGame.START_LIVES;

        HeartsView() {
            int count = RainGame.START_LIVES;
            setPreferredSize(new Dimension(count * SIZE + (count - 1) * GAP, SIZE));
        }

        void setLives(int lives) {
            this.lives = lives;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            for (int i = 0; i < RainGame.START_LIVES; i++) {
                int x = i * (SIZE + GAP);
                boolean alive = i < lives;
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alive ? 1f : 0.2f));
                if (heart != null) {
                    g.drawImage(heart, x, 0, SIZE, SIZE, null);
                } else {
                    g.setColor(new Color(220, 50, 60));
                    g.setFont(new Font(Font.DIALOG, Font.BOLD, SIZE));
                    g.drawString("♥", x, SIZE - 3);
                }
            }
            g.dispose();
        }

        private static Image loadHeart() {
            File file = new File("images", "heart.png");
            if (!file.isFile()) {
                return null;
            }
            try {
                BufferedImage image = ImageIO.read(file);
                return image == null ? null : image.getScaledInstance(SIZE, SIZE, Image.SCALE_SMOOTH);
            } catch (IOException e) {
                return null;
            }
        }
    }

    private static List<String[]> sampleWords() {
        String[][] words = {
                {"apple", "사과"}, {"school", "학교"}, {"friend", "친구"}, {"happiness", "행복"},
                {"water", "물"}, {"book", "책"}, {"tree", "나무"}, {"house", "집"},
                {"music", "음악"}, {"river", "강"}, {"flower", "꽃"}, {"window", "창문"},
                {"teacher", "선생님"}, {"dream", "꿈"}, {"rain", "비"}, {"cloud", "구름"},
                {"star", "별"}, {"family", "가족"}, {"bread", "빵"}, {"chair", "의자"},
        };
        return new ArrayList<>(List.of(words));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("산성비 테스트");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            AcidRainPanel panel = new AcidRainPanel();
            panel.setOnBackToMain(() -> System.out.println("메인으로 이동 (팀 화면 전환에 연결 예정)"));
            panel.setOnGameOver((score, level, missed) ->
                    System.out.println("게임 오버! 점수=" + score + ", 레벨=" + level + ", 놓친 단어=" + missed.size()));

            frame.setContentPane(panel);
            frame.setSize(1000, 750);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
