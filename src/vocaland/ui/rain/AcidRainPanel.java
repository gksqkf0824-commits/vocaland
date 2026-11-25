package vocaland.ui.rain;

import vocaland.ui.util.ImagePaths;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import vocaland.ui.components.BackgroundPanel;
import vocaland.ui.components.GamePopupCard;
import vocaland.ui.components.RoundedBorder;
import vocaland.ui.components.RoundedButton;
import vocaland.ui.components.RoundedPanel;
import vocaland.ui.theme.Theme;
import vocaland.ui.util.LogoIcon;

/*
 * AcidRainPanel - 산성비 게임 화면 (화면 조립, 타이머, 입력 처리)
 *
 * [develop + main 통합]
 *  - 기능(develop) : 하트 단어, 단어 위치 팝업, 코인, 오답 노트, 게임 영역 바닥 입력칸
 *  - 디자인(main)  : 배경 패널, 로고/텍스트 이미지, 둥근 상태 박스, 공용 팝업 카드
 *  - 연결(main)    : setOnAnswer() 로 정답/오답 기록 (LocalData.recordAnswer)
 *
 * start() / stop()    : 화면에 들어올 때 시작 대기 상태로, 나갈 때 타이머 정지
 * setOnBackToMain()   : ← 메인으로 버튼을 눌렀을 때 실행할 코드 등록 (화면 전환용)
 * setOnGameOver()     : 게임이 끝났을 때 점수·레벨·놓친 단어를 받을 코드 등록 (백엔드용)
 * setOnAnswer()       : 단어를 맞히거나 놓칠 때마다 (영어단어, 정답여부) 를 받을 코드 등록 (학습 기록용)
 * setOnCoinsEarned()  : 게임이 끝났을 때 얻은 코인을 받을 코드 등록 (상점 연결용)
 * startGame()         : 고른 모드로 게임 시작
 * tick()              : 타이머가 0.03초마다 호출, 게임 진행 후 화면 갱신
 * submitAnswer()      : 입력값 판정, 정답 점수·하트·레벨업을 단어 위치에 표시
 * endGame()           : 게임 오버 처리, 점수와 코인 전달
 * showReady()         : 시작 안내창 (모드 선택, 시작하기)
 * showGameOver()      : 결과 안내창 (점수, 레벨, 코인, 놓친 단어 수, 오답 보기/다시하기/메인으로)
 * showWrongNote()     : 오답 노트 (놓친 단어만, 입력해야 했던 쪽은 노란 네모로 가리고 클릭할 때마다 공개/가리기)
 * createHeader()      : 상단 (제목 로고, 남은 기회, 점수, 메인으로)
 * createCenter()      : 가운데 (게임 영역, 오른쪽 오답 복습 목록)
 * createInputField()  : 게임 영역 바닥에 놓이는 반투명 입력칸
 * sampleWords()       : 화면 단독 실행용 예제 단어 (실제 앱은 words.txt 사용)
 * main()              : 혼자 테스트용 실행
 */
public class AcidRainPanel extends BackgroundPanel {

    public interface GameOverListener {
        void onGameOver(int score, int level, List<FallingWord> missedWords);
    }

    private static final int TICK_MS = 30;
    private static final Color BACKGROUND = new Color(250, 251, 248);
    private static final Color CORRECT = new Color(46, 140, 70);
    private static final Color WRONG = new Color(210, 70, 60);
    private static final Color HEART = new Color(220, 50, 60);
    private static final Color NOTE_BACK = new Color(225, 242, 252);
    private static final Color COVER = new Color(255, 214, 79);
    private static final Color GREEN_LINE = new Color(181, 222, 103);

    private final RainGame game;
    private final RainGameCanvas canvas;
    private final Timer gameTimer = new Timer(TICK_MS, e -> tick());

    private final HeartsView hearts = new HeartsView();
    private final JLabel scoreLabel = new JLabel();
    private final JTextField inputField = createInputField();
    private final JPanel overlay = new GamePopupCard();
    private final DefaultListModel<String> reviewModel = new DefaultListModel<>();
    private final JLabel reviewTitle = new JLabel();
    private InputMode selectedMode = InputMode.SPELLING;

    private Runnable onBackToMain;
    private GameOverListener onGameOver;
    private BiConsumer<String, Boolean> onAnswer;
    private IntConsumer onCoinsEarned;

    public AcidRainPanel() {
        this(sampleWords());
    }

    public AcidRainPanel(List<String[]> words) {
        game = new RainGame(words);
        canvas = new RainGameCanvas(game);
        canvas.setOverlay(overlay);
        canvas.setInput(inputField);

        setLayout(new BorderLayout(0, 14));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(12, 28, 20, 28));

        add(createHeader(), BorderLayout.NORTH);
        add(createCenter(), BorderLayout.CENTER);

        showReady();
    }

    public void start() {
        showReady();
    }

    public void stop() {
        gameTimer.stop();
    }

    public void setOnBackToMain(Runnable onBackToMain) {
        this.onBackToMain = onBackToMain;
    }

    public void setOnGameOver(GameOverListener onGameOver) {
        this.onGameOver = onGameOver;
    }

    public void setOnAnswer(BiConsumer<String, Boolean> listener) {
        this.onAnswer = listener;
    }

    public void setOnCoinsEarned(IntConsumer onCoinsEarned) {
        this.onCoinsEarned = onCoinsEarned;
    }

    private void startGame() {
        game.start(selectedMode);
        reviewModel.clear();
        canvas.clearPopups();
        overlay.setVisible(false);

        inputField.setText("");
        inputField.setVisible(true);
        inputField.setEnabled(true);
        refreshStatus();

        gameTimer.start();
        inputField.requestFocusInWindow();
    }

    private void tick() {
        List<FallingWord> landed = game.update(TICK_MS, canvas.getAreaWidth(), canvas.getGroundY());

        for (FallingWord word : landed) {
            if (onAnswer != null) {
                onAnswer.accept(word.getEnglish(), false);
            }
            reviewModel.addElement(word.getReviewText());
            canvas.addPopup(word.isHeart() ? "♥ -1" : "놓침 -1", word.getX(), canvas.getGroundY() - 12, WRONG);
        }
        if (!landed.isEmpty()) {
            refreshStatus();
        }

        canvas.updatePopups(TICK_MS);
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
        FallingWord hit = game.submit(input);
        if (hit == null) {
            return;
        }
        if (onAnswer != null) {
            onAnswer.accept(hit.getEnglish(), true);
        }

        double y = hit.getY() + FallingWord.BOX_HEIGHT / 2.0;
        canvas.addPopup("+" + game.getLastPoints(), hit.getX(), y, CORRECT);
        if (hit.isHeart()) {
            canvas.addPopup("♥ +1", hit.getX(), y - 30, HEART);
        }
        if (game.getLevel() > levelBefore) {
            canvas.addPopup("Lv." + game.getLevel() + "!", canvas.getAreaWidth() / 2.0,
                    canvas.getGroundY() / 2.0, Theme.TEXT);
        }
        refreshStatus();
        canvas.repaint();
    }

    private void endGame() {
        gameTimer.stop();
        canvas.clearPopups();
        inputField.setEnabled(false);
        inputField.setVisible(false);
        showGameOver();

        if (onGameOver != null) {
            onGameOver.onGameOver(game.getScore(), game.getLevel(), new ArrayList<>(game.getMissedWords()));
        }
        if (onCoinsEarned != null && game.getCoins() > 0) {
            onCoinsEarned.accept(game.getCoins());
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
        scoreLabel.setText((scoreLabel.getIcon() == null ? "점수 " : "")
                + String.format("%,d", game.getScore()));
        scoreLabel.getAccessibleContext().setAccessibleName("점수 " + game.getScore());
        reviewTitle.setText("오답 복습 (" + reviewModel.size() + ")");
    }

    private void showReady() {
        stop();
        game.reset();
        reviewModel.clear();
        canvas.clearPopups();
        inputField.setEnabled(false);
        inputField.setVisible(false);
        refreshStatus();

        overlay.removeAll();
        overlay.add(centered(label("단어가 바닥에 닿기 전에 정답을 입력하세요", 20)));
        overlay.add(Box.createVerticalStrut(4));
        overlay.add(centered(label("빨간 하트 단어를 맞히면 목숨 +1", 16, HEART)));
        overlay.add(Box.createVerticalStrut(20));

        JPanel modes = transparent(new FlowLayout(FlowLayout.CENTER, 10, 0));
        for (InputMode mode : InputMode.values()) {
            JButton button = button((mode == selectedMode ? "● " : "○ ") + mode.getLabel(), 15);
            button.addActionListener(e -> {
                selectedMode = mode;
                showReady();
            });
            modes.add(button);
        }
        overlay.add(centered(modes));
        overlay.add(Box.createVerticalStrut(8));
        overlay.add(centered(label(selectedMode.getGuide(), 14)));
        overlay.add(Box.createVerticalStrut(18));

        JButton startButton = button("시작하기", 20);
        startButton.setFont(font(20));
        ImageIcon startIcon = loadLabelIcon(ImagePaths.TEXT_START, 100, 30);
        if (startIcon != null) {
            startButton.setIcon(startIcon);
            startButton.setText("");
        }
        startButton.getAccessibleContext().setAccessibleName("시작하기");
        startButton.addActionListener(e -> startGame());
        overlay.add(centered(startButton));

        refreshOverlay();
    }

    private void showGameOver() {
        overlay.removeAll();
        JLabel gameOverTitle = label("게임 오버", 30);
        ImageIcon gameOverIcon = loadLabelIcon(ImagePaths.GAME_GAME_OVER, 240, 70);
        if (gameOverIcon != null) {
            gameOverTitle.setIcon(gameOverIcon);
            gameOverTitle.setText("");
        }
        gameOverTitle.getAccessibleContext().setAccessibleName("게임 오버");
        overlay.add(centered(gameOverTitle));
        overlay.add(Box.createVerticalStrut(10));
        overlay.add(centered(label("점수 " + String.format("%,d", game.getScore())
                + "   ·   Lv." + game.getLevel(), 20)));
        overlay.add(Box.createVerticalStrut(6));
        overlay.add(centered(label("코인 +" + game.getCoins()
                + "   ·   놓친 단어 " + game.getMissedWords().size() + "개", 16)));
        overlay.add(Box.createVerticalStrut(18));

        JPanel buttons = transparent(new FlowLayout(FlowLayout.CENTER, 10, 0));
        JButton wrong = button("오답 보기", 17);
        wrong.setEnabled(!game.getMissedWords().isEmpty());
        wrong.addActionListener(e -> showWrongNote());
        JButton retry = button("다시하기", 17);
        retry.addActionListener(e -> {
            List<String> keep = new ArrayList<>();
            for (int i = 0; i < reviewModel.size(); i++) {
                keep.add(reviewModel.get(i));
            }
            showReady();
            keep.forEach(reviewModel::addElement);
            refreshStatus();
        });
        JButton main = button("메인으로", 17);
        main.addActionListener(e -> goToMain());
        buttons.add(wrong);
        buttons.add(retry);
        buttons.add(main);
        overlay.add(centered(buttons));

        refreshOverlay();
    }

    private void showWrongNote() {
        List<FallingWord> missed = game.getMissedWords();
        InputMode mode = game.getMode();

        overlay.removeAll();
        overlay.add(centered(label("오답 노트 (" + missed.size() + ")", 24)));
        overlay.add(Box.createVerticalStrut(4));
        overlay.add(centered(label("노란 칸을 누르면 정답이 보이고, 다시 누르면 가려져요.", 14)));
        overlay.add(Box.createVerticalStrut(12));

        Font wordFont = font(18);
        FontMetrics fm = getFontMetrics(wordFont);
        int coverWidth = 0;
        for (FallingWord word : missed) {
            coverWidth = Math.max(coverWidth, fm.stringWidth(word.getAnswerText(mode)));
        }
        coverWidth += 28;

        JPanel rows = new JPanel(new GridBagLayout());
        rows.setBackground(NOTE_BACK);
        rows.setBorder(new EmptyBorder(10, 16, 10, 16));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        for (int i = 0; i < missed.size(); i++) {
            FallingWord word = missed.get(i);
            c.gridy = i;

            c.gridx = 0;
            c.anchor = GridBagConstraints.EAST;
            rows.add(label(word.getDisplayText(mode), 18), c);

            c.gridx = 1;
            c.anchor = GridBagConstraints.CENTER;
            rows.add(label("→", 18), c);

            c.gridx = 2;
            c.anchor = GridBagConstraints.WEST;
            rows.add(new AnswerCell(word.getAnswerText(mode), wordFont, coverWidth), c);
        }
        c.gridy = missed.size();
        c.gridx = 0;
        c.gridwidth = 3;
        c.weighty = 1;
        rows.add(Box.createVerticalGlue(), c);

        JScrollPane scroll = new JScrollPane(rows);
        scroll.setBorder(new RoundedBorder(GREEN_LINE, 2));
        scroll.getViewport().setBackground(NOTE_BACK);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        int listHeight = Math.min(300, rows.getPreferredSize().height + 4);
        scroll.setPreferredSize(new Dimension(Math.max(360, rows.getPreferredSize().width + 24), listHeight));
        scroll.setMaximumSize(scroll.getPreferredSize());
        overlay.add(centered(scroll));
        overlay.add(Box.createVerticalStrut(16));

        JButton back = button("← 결과로", 16);
        back.addActionListener(e -> showGameOver());
        overlay.add(centered(back));

        refreshOverlay();
    }

    private void refreshOverlay() {
        overlay.setVisible(true);
        canvas.revalidate();
        canvas.doLayout();
        overlay.revalidate();
        canvas.repaint();
    }

    private JComponent createHeader() {
        JPanel header = transparent(new BorderLayout());

        JLabel title = label("산성비 게임", 30);
        ImageIcon titleIcon = LogoIcon.load(ImagePaths.LOGO_RAIN, 190, 61);
        if (titleIcon != null) {
            title.setIcon(titleIcon);
            title.setText("");
        }
        title.getAccessibleContext().setAccessibleName("산성비 게임");
        title.setVerticalAlignment(JLabel.TOP);
        header.add(title, BorderLayout.WEST);

        JPanel right = transparent(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setBorder(new EmptyBorder(6, 0, 0, 0));

        JPanel livesBox = transparent(new FlowLayout(FlowLayout.CENTER, 8, 0));
        JLabel lifeTextLabel = label("남은 기회", 15);
        ImageIcon healthIcon = loadLabelIcon(ImagePaths.TEXT_HEALTH, 84, 24);
        if (healthIcon != null) {
            lifeTextLabel.setIcon(healthIcon);
            lifeTextLabel.setText("");
        }
        lifeTextLabel.getAccessibleContext().setAccessibleName("남은 기회");
        livesBox.add(lifeTextLabel);
        livesBox.add(hearts);
        JComponent livesStatus = boxed(livesBox);
        livesStatus.setBorder(new EmptyBorder(7, 12, 7, 12));
        right.add(livesStatus);

        scoreLabel.setFont(font(17));
        scoreLabel.setForeground(Theme.TEXT);
        ImageIcon scoreIcon = loadLabelIcon(ImagePaths.TEXT_SCORE, 46, 25);
        if (scoreIcon != null) {
            BufferedImage shiftedScore = new BufferedImage(
                    scoreIcon.getIconWidth(), scoreIcon.getIconHeight() + 4, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = shiftedScore.createGraphics();
            scoreIcon.paintIcon(null, g, 0, 3);
            g.dispose();
            scoreLabel.setIcon(new ImageIcon(shiftedScore));
        }
        scoreLabel.setIconTextGap(8);
        JComponent scoreBox = boxed(scoreLabel);
        scoreBox.setBorder(new EmptyBorder(5, 22, 9, 22));
        right.add(scoreBox);

        JButton back = new RoundedButton("← 메인으로", 18);
        back.setFont(font(15));
        back.setPreferredSize(new Dimension(132, 42));
        back.setBorder(new EmptyBorder(8, 12, 12, 18));
        ImageIcon backIcon = loadLabelIcon(ImagePaths.TEXT_GO_MAIN, 102, 24);
        if (backIcon != null) {
            back.setIcon(backIcon);
            back.setText("");
            back.setBorder(new EmptyBorder(13, 12, 12, 18));
        }
        back.getAccessibleContext().setAccessibleName("메인으로");
        back.addActionListener(e -> goToMain());
        right.add(back);

        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JComponent createCenter() {
        JPanel center = transparent(new BorderLayout(14, 0));
        center.add(canvas, BorderLayout.CENTER);

        JPanel review = new RoundedPanel(new BorderLayout());
        review.setBackground(Color.WHITE);
        review.setBorder(new RoundedBorder(GREEN_LINE, 2));
        review.setPreferredSize(new Dimension(210, 0));

        reviewTitle.setFont(font(17));
        reviewTitle.setForeground(Theme.TEXT);
        reviewTitle.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, Theme.TEXT),
                new EmptyBorder(10, 12, 8, 12)));
        review.add(reviewTitle, BorderLayout.NORTH);

        JList<String> list = new JList<>(reviewModel);
        list.setFont(Theme.REVIEW_FONT);
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

    private JTextField createInputField() {
        JTextField field = new JTextField() {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(new Color(255, 255, 255, 170));
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                g.setColor(GREEN_LINE);
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        field.setOpaque(false);
        field.setFont(new Font(Font.DIALOG, Font.PLAIN, 26));
        field.setForeground(Theme.TEXT);
        field.setHorizontalAlignment(SwingConstants.CENTER);
        field.setBorder(new EmptyBorder(4, 14, 4, 14));
        field.addActionListener(e -> submitAnswer());
        return field;
    }

    private static ImageIcon loadLabelIcon(String name, int maxWidth, int maxHeight) {
        return vocaland.ui.util.ImageAssets.load(ImagePaths.ROOT + name, maxWidth, maxHeight, -1, false);
    }

    private static Font font(int size) {
        return new Font(Font.DIALOG, Font.BOLD, size);
    }

    private static JLabel label(String text, int size) {
        return label(text, size, Theme.TEXT);
    }

    private static JLabel label(String text, int size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font(size));
        label.setForeground(color);
        return label;
    }

    private static JButton button(String text, int size) {
        JButton button = new RoundedButton(text, 14);
        button.setFont(font(size));
        return button;
    }

    private static JPanel transparent(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    private static JComponent centered(JComponent component) {
        component.setAlignmentX(Component.CENTER_ALIGNMENT);
        return component;
    }

    private static JComponent boxed(JComponent content) {
        JPanel box = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                return new Dimension(size.width, 42);
            }

            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g = (Graphics2D) graphics.create();
                try {
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    int width = getWidth() - 2;
                    int height = getHeight() - 6;
                    g.setColor(new Color(32, 50, 15, 65));
                    g.fillRoundRect(1, 5, width, height, 36, 36);
                    g.setColor(Theme.PRIMARY);
                    g.fillRoundRect(1, 1, width, height, 36, 36);
                } finally {
                    g.dispose();
                }
            }
        };
        box.setOpaque(false);
        box.setBorder(new EmptyBorder(5, 12, 9, 12));
        content.setOpaque(false);
        box.add(content);
        return box;
    }

    private static class AnswerCell extends JComponent {
        private final String text;
        private boolean revealed;

        AnswerCell(String text, Font font, int width) {
            this.text = text;
            setFont(font);
            setPreferredSize(new Dimension(width, 32));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    revealed = !revealed;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if (revealed) {
                FontMetrics fm = g.getFontMetrics(getFont());
                g.setFont(getFont());
                g.setColor(Theme.TEXT);
                g.drawString(text, 14, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            } else {
                g.setColor(COVER);
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            }
            g.dispose();
        }
    }

    private static class HeartsView extends JComponent {
        private static final int SIZE = 22;
        private static final int GAP = 4;
        private final Image heart = loadHeart();
        private int lives = RainGame.START_LIVES;

        HeartsView() {
            int count = RainGame.MAX_LIVES;
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
            for (int i = 0; i < RainGame.MAX_LIVES; i++) {
                int x = i * (SIZE + GAP);
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, i < lives ? 1f : 0.2f));
                if (heart != null) {
                    g.drawImage(heart, x, 0, SIZE, SIZE, null);
                } else {
                    g.setColor(HEART);
                    g.setFont(new Font(Font.DIALOG, Font.BOLD, SIZE));
                    g.drawString("♥", x, SIZE - 3);
                }
            }
            g.dispose();
        }

        private static Image loadHeart() {
            ImageIcon icon = loadLabelIcon(ImagePaths.GAME_HEART, SIZE, SIZE);
            return icon == null ? null : icon.getImage();
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
            panel.setOnBackToMain(() -> System.out.println("메인으로 이동"));
            panel.setOnGameOver((score, level, missed) ->
                    System.out.println("게임 오버! 점수=" + score + ", 레벨=" + level + ", 놓친 단어=" + missed.size()));
            panel.setOnAnswer((word, correct) -> System.out.println((correct ? "정답 " : "놓침 ") + word));
            panel.setOnCoinsEarned(coins -> System.out.println("코인 +" + coins));

            frame.setContentPane(panel);
            frame.setSize(1000, 750);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
