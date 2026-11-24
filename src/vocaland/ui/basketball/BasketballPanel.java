package vocaland.ui.basketball;

import vocaland.ui.util.ImagePaths;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import vocaland.ui.components.RoundedBorder;
import vocaland.ui.components.RoundedPanel;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import vocaland.ui.components.RoundedButton;
import vocaland.ui.util.LogoIcon;
import vocaland.ui.theme.Theme;
import vocaland.ui.components.BackgroundPanel;

/*
 * BasketballPanel - 농구 게임 화면 (화면 조립, 타이머, 키보드 입력 처리)
 *
 * start() / stop()        : 화면에 들어올 때 모드 선택 대기 상태로 초기화, 나갈 때 타이머 정지
 * setOnBackToMain()       : ← 메인으로 버튼을 눌렀을 때 실행할 코드 등록 (화면 전환용)
 * setOnGameOver()         : 게임이 끝났을 때 점수·놓친 단어를 받을 코드 등록 (백엔드용)
 * startGamePlay()         : 선택한 모드와 난이도로 실제 게임 시작
 * showGameOverOverlay()   : 게임 오버 결과 카드 표시 및 콜백 호출
 * createTopBar()          : 상단 (제목, 남은 기회, 점수, 메인으로)
 * createWrongReviewBox()  : 우측 (오답 복습 목록)
 * createBottomHelpBar()   : 하단 (키보드 조작법 안내 문구)
 * createStartOverlay()    : 시작 대기 안내창 (모드 선택, 이지/하드 난이도 선택)
 * createGameOverOverlay() : 게임 오버 안내창 (최종 점수, 다시하기, 모드 변경, 메인으로)
 * main()                  : 혼자 테스트용 실행
 */
public class BasketballPanel extends BackgroundPanel {

    private static final Color BG_WHITE = Color.WHITE;
    private static final Color BLUE_BTN = new Color(100, 181, 246);
    private static final Color BLUE_BTN_SHADOW = new Color(66, 145, 210);
    private static final Color RED_BTN = new Color(239, 83, 80);
    private static final Color RED_BTN_SHADOW = new Color(198, 40, 40);
    private static final Color MINT_CARD = new Color(241, 248, 233);
    private static final Color BORDER_BLACK = Color.BLACK;

    private ImageIcon activeHeartIcon;
    private ImageIcon grayHeartIcon;

    private final BasketballGame game;
    private final BasketballCanvas canvas;

    private Runnable onBackToMain;
    private BiConsumer<Integer, List<String>> onGameOver;
    private BiConsumer<String, Boolean> onAnswer;
    public void setOnAnswer(BiConsumer<String, Boolean> listener) { onAnswer = listener; }
    public boolean isHardMode() { return game.isHardMode(); }
    public void setHoopImage(String path) { canvas.setHoopImage(path); }
    public void setBallImage(String path) { canvas.setBallImage(path); }

    private JLabel titleLabel;
    private JLabel lifeTextLabel;
    private JLabel scoreLabel;
    private JButton backBtn;
    private JPanel livesPanel;
    private JPanel wrongReviewBox;
    private JLabel wrongHeaderLabel;
    private JPanel wrongListPanel;
    private final List<JLabel> helpLabels = new ArrayList<>();

    private JPanel startOverlay;
    private JPanel gameOverOverlay;
    private JLabel finalScoreLabel;
    private JLabel missedSummaryLabel;

    private Timer gameTimer;

    public BasketballPanel() {
        this(createDummyWords());
    }

    public BasketballPanel(List<String[]> wordPairs) {
        List<String[]> validWords = (wordPairs != null && !wordPairs.isEmpty()) ? wordPairs : createDummyWords();
        this.game = new BasketballGame(validWords);
        this.canvas = new BasketballCanvas(game);

        initHeartIcons();

        setLayout(new BorderLayout(0, 12));
        setBackground(BG_WHITE);
        setBorder(new EmptyBorder(16, 22, 16, 22));
        setFocusable(true);

        add(createTopBar(), BorderLayout.NORTH);

        JPanel centerWrapper = new JPanel(new BorderLayout(14, 0));
        centerWrapper.setOpaque(false);

        JPanel gameBoxWrapper = new JPanel(new BorderLayout());
        gameBoxWrapper.setOpaque(false);

        startOverlay = createStartOverlay();
        gameOverOverlay = createGameOverOverlay();
        canvas.add(startOverlay);
        canvas.add(gameOverOverlay);
        gameOverOverlay.setVisible(false);

        gameBoxWrapper.add(canvas, BorderLayout.CENTER);
        wrongReviewBox = createWrongReviewBox();

        centerWrapper.add(gameBoxWrapper, BorderLayout.CENTER);
        centerWrapper.add(wrongReviewBox, BorderLayout.EAST);

        add(centerWrapper, BorderLayout.CENTER);
        add(createBottomHelpBar(), BorderLayout.SOUTH);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                syncResponsiveLayout();
            }
        });

        gameTimer = new Timer(20, e -> {
            String questionEnglish = game.getCurrentEnglish();
            int result = game.updatePhysics();
            if (result != 0 && onAnswer != null) onAnswer.accept(questionEnglish, result == 1);
            if (result == 1) {
                updateTopStatus();
            } else if (result == 2) {
                updateTopStatus();
                refreshWrongList();
            } else if (result == 3) {
                updateTopStatus();
                refreshWrongList();
                showGameOverOverlay();
            }
            canvas.repaint();
        });
        gameTimer.start();

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (game.getState() != BasketballGame.State.PLAYING) {
                    return;
                }

                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP -> game.adjustAngle(2.0);
                    case KeyEvent.VK_DOWN -> game.adjustAngle(-2.0);
                    case KeyEvent.VK_RIGHT -> game.adjustPower(0.5);
                    case KeyEvent.VK_LEFT -> game.adjustPower(-0.5);
                    case KeyEvent.VK_SPACE -> game.shoot();
                }
                canvas.repaint();
            }
        });

        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
            }
        });

        updateTopStatus();
    }

    private void initHeartIcons() {
        Image raw = new ImageIcon(ImagePaths.ROOT + ImagePaths.GAME_HEART).getImage();
        if (raw != null && raw.getWidth(null) > 0) {
            Image scaled = raw.getScaledInstance(22, 22, Image.SCALE_SMOOTH);
            activeHeartIcon = new ImageIcon(scaled);

            BufferedImage grayBuf = new BufferedImage(22, 22, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = grayBuf.createGraphics();
            g2.drawImage(scaled, 0, 0, null);
            g2.dispose();

            for (int y = 0; y < 22; y++) {
                for (int x = 0; x < 22; x++) {
                    int rgba = grayBuf.getRGB(x, y);
                    int alpha = (rgba >> 24) & 0xff;
                    if (alpha > 0) {
                        int r = (rgba >> 16) & 0xff;
                        int g = (rgba >> 8) & 0xff;
                        int b = rgba & 0xff;
                        int gray = (int) (0.3 * r + 0.59 * g + 0.11 * b);
                        gray = Math.min(255, gray + 70);
                        grayBuf.setRGB(x, y, (alpha << 24) | (gray << 16) | (gray << 8) | gray);
                    }
                }
            }
            grayHeartIcon = new ImageIcon(grayBuf);
        }
    }

    private static class PillButton extends JButton {
        private Color mainColor;
        private Color shadowColor;
        private boolean isPressed = false;

        public PillButton(String text, Color mainColor, Color shadowColor) {
            super(text);
            this.mainColor = mainColor;
            this.shadowColor = shadowColor;
            setFont(new Font("맑은 고딕", Font.BOLD, 15));
            setForeground(Color.BLACK);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(9, 22, 12, 22));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    isPressed = true;
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    isPressed = false;
                    repaint();
                }
            });
        }


        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int shadowOffset = 4;
            int yOffset = isPressed ? 2 : 0;

            g2.setColor(shadowColor);
            g2.fillRoundRect(0, shadowOffset, w, h - shadowOffset, 22, 22);

            Color fill = isSelected() ? mainColor : new Color(
                    (mainColor.getRed() + 3 * 255) / 4,
                    (mainColor.getGreen() + 3 * 255) / 4,
                    (mainColor.getBlue() + 3 * 255) / 4);
            g2.setColor(fill);
            g2.fillRoundRect(0, yOffset, w, h - shadowOffset, 22, 22);
            if (isSelected()) {
                g2.setColor(shadowColor);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, yOffset + 1, w - 3, h - shadowOffset - 3, 20, 20);
            }

            FontMetrics fm = g2.getFontMetrics();
            String text = (isSelected() ? "✓ " : "") + getText();
            int textX = (w - fm.stringWidth(text)) / 2;
            int textY = yOffset + ((h - shadowOffset - fm.getHeight()) / 2) + fm.getAscent();
            g2.setColor(isSelected() ? getForeground() : Theme.TEXT);
            g2.drawString(text, textX, textY);

            g2.dispose();
        }
    }

    private JPanel createStartOverlay() {
        JPanel card = new vocaland.ui.components.GamePopupCard();

        JLabel title = new JLabel("알맞은 골대로 공을 던지세요");
        title.setFont(new Font(Font.DIALOG, Font.BOLD, 20));
        title.setForeground(Theme.TEXT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel modeBtnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        modeBtnRow.setOpaque(false);

        JButton meaningBtn = new RoundedButton("● 영어 보고 뜻 맞히기", 14);
        meaningBtn.setFont(new Font(Font.DIALOG, Font.BOLD, 15));
        JButton spellingBtn = new RoundedButton("○ 뜻 보고 영어 맞히기", 14);
        spellingBtn.setFont(new Font(Font.DIALOG, Font.BOLD, 15));

        meaningBtn.addActionListener(e -> {
            game.setMode(BasketballMode.MEANING);
            meaningBtn.setText("● 영어 보고 뜻 맞히기");
            spellingBtn.setText("○ 뜻 보고 영어 맞히기");
        });

        spellingBtn.addActionListener(e -> {
            game.setMode(BasketballMode.SPELLING);
            spellingBtn.setText("● 뜻 보고 영어 맞히기");
            meaningBtn.setText("○ 영어 보고 뜻 맞히기");
        });

        modeBtnRow.add(meaningBtn);
        modeBtnRow.add(spellingBtn);

        JLabel diffTitle = new JLabel("난이도 선택");
        diffTitle.setFont(new Font(Font.DIALOG, Font.BOLD, 14));
        diffTitle.setForeground(Theme.TEXT);
        diffTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel diffBtnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        diffBtnRow.setOpaque(false);

        PillButton easyBtn = new PillButton("이지 모드", BLUE_BTN, BLUE_BTN_SHADOW);
        easyBtn.setForeground(Color.WHITE);

        PillButton hardBtn = new PillButton("하드 모드", RED_BTN, RED_BTN_SHADOW);
        hardBtn.setForeground(Color.WHITE);
        easyBtn.setSelected(!game.isHardMode());
        hardBtn.setSelected(game.isHardMode());
        easyBtn.addActionListener(e -> {
            game.setHardMode(false);
            easyBtn.setSelected(true);
            hardBtn.setSelected(false);
            diffBtnRow.repaint();
        });
        hardBtn.addActionListener(e -> {
            game.setHardMode(true);
            easyBtn.setSelected(false);
            hardBtn.setSelected(true);
            diffBtnRow.repaint();
        });

        diffBtnRow.add(easyBtn);
        diffBtnRow.add(hardBtn);

        card.add(title);
        card.add(Box.createVerticalStrut(20));
        card.add(modeBtnRow);
        card.add(Box.createVerticalStrut(18));
        card.add(diffTitle);
        card.add(Box.createVerticalStrut(8));
        card.add(diffBtnRow);
        card.add(Box.createVerticalStrut(18));

        JButton startButton = new RoundedButton("시작하기", 16);
        startButton.setFont(new Font(Font.DIALOG, Font.BOLD, 20));
        ImageIcon startIcon = new ImageIcon(ImagePaths.ROOT + ImagePaths.TEXT_START);
        if (startIcon.getIconWidth() > 0 && startIcon.getIconHeight() > 0) {
            double scale = Math.min(100.0 / startIcon.getIconWidth(),
                    30.0 / startIcon.getIconHeight());
            startButton.setIcon(new ImageIcon(startIcon.getImage().getScaledInstance(
                    Math.max(1, (int) Math.round(startIcon.getIconWidth() * scale)),
                    Math.max(1, (int) Math.round(startIcon.getIconHeight() * scale)),
                    Image.SCALE_SMOOTH)));
            startButton.setText("");
        }
        startButton.getAccessibleContext().setAccessibleName("시작하기");
        startButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        startButton.addActionListener(e -> startGamePlay());
        card.add(startButton);

        return card;
    }

    private JPanel createGameOverOverlay() {
        JPanel card = new vocaland.ui.components.GamePopupCard();

        JLabel overTitle = new JLabel("게임 오버");
        overTitle.setFont(new Font(Font.DIALOG, Font.BOLD, 30));
        overTitle.setForeground(Theme.TEXT);
        overTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        ImageIcon gameOverIcon = new ImageIcon(ImagePaths.ROOT + ImagePaths.GAME_GAME_OVER);
        if (gameOverIcon.getIconWidth() > 0 && gameOverIcon.getIconHeight() > 0) {
            double scale = Math.min(240.0 / gameOverIcon.getIconWidth(),
                    70.0 / gameOverIcon.getIconHeight());
            overTitle.setIcon(new ImageIcon(gameOverIcon.getImage().getScaledInstance(
                    Math.max(1, (int) Math.round(gameOverIcon.getIconWidth() * scale)),
                    Math.max(1, (int) Math.round(gameOverIcon.getIconHeight() * scale)),
                    Image.SCALE_SMOOTH)));
            overTitle.setText("");
        }
        overTitle.getAccessibleContext().setAccessibleName("게임 오버");

        finalScoreLabel = new JLabel("최종 점수: 0점");
        finalScoreLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 20));
        finalScoreLabel.setForeground(Theme.TEXT);
        finalScoreLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel topBtnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        topBtnRow.setOpaque(false);

        JButton retryBtn = new RoundedButton("다시하기", 14);
        retryBtn.setFont(new Font(Font.DIALOG, Font.BOLD, 17));
        retryBtn.addActionListener(e -> startGamePlay());

        JButton changeModeBtn = new RoundedButton("모드 변경", 14);
        changeModeBtn.setFont(new Font(Font.DIALOG, Font.BOLD, 17));
        changeModeBtn.addActionListener(e -> {
            game.toReadyState();
            gameOverOverlay.setVisible(false);
            startOverlay.setVisible(true);
            canvas.repaint();
        });

        topBtnRow.add(retryBtn);
        topBtnRow.add(changeModeBtn);

        card.add(overTitle);
        card.add(Box.createVerticalStrut(10));
        card.add(finalScoreLabel);
        card.add(Box.createVerticalStrut(6));
        missedSummaryLabel = new JLabel("오답은 오른쪽에서 확인하세요.");
        missedSummaryLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 14));
        missedSummaryLabel.setForeground(Theme.TEXT);
        missedSummaryLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(missedSummaryLabel);
        card.add(Box.createVerticalStrut(18));
        card.add(topBtnRow);

        return card;
    }

    private void startGamePlay() {
        startOverlay.setVisible(false);
        gameOverOverlay.setVisible(false);
        game.startNewGame();
        updateTopStatus();
        refreshWrongList();
        canvas.repaint();
        requestFocusInWindow();
    }

    private void showGameOverOverlay() {
        if (onGameOver != null) {
            onGameOver.accept(game.getScore(), game.getMissedEnglishWords());
        }
        finalScoreLabel.setText("점수 " + String.format("%,d", game.getScore()));
        gameOverOverlay.setVisible(true);
    }

    private JPanel createTopBar() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        titleLabel = new JLabel("농구 게임");
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 26));
        titleLabel.setForeground(Color.BLACK);
        ImageIcon titleIcon = LogoIcon.load(ImagePaths.LOGO_BASKETBALL, 180, 58);
        if (titleIcon != null) {
            titleLabel.setIcon(titleIcon);
            titleLabel.setText("");
        }
        titleLabel.getAccessibleContext().setAccessibleName("농구 게임");
        titleLabel.setVerticalAlignment(JLabel.TOP);
        titleLabel.setBorder(new EmptyBorder(4, 0, 0, 0));
        topBar.add(titleLabel, BorderLayout.WEST);

        JPanel rightInfo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightInfo.setOpaque(false);

        JPanel lifeContent = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        lifeContent.setOpaque(false);

        lifeTextLabel = new JLabel("남은 기회");
        lifeTextLabel.setFont(new Font("맑은 고딕", Font.BOLD, 15));
        lifeTextLabel.setForeground(Color.BLACK);
        ImageIcon healthIcon = new ImageIcon(ImagePaths.ROOT + ImagePaths.TEXT_HEALTH);
        if (healthIcon.getIconWidth() > 0 && healthIcon.getIconHeight() > 0) {
            double scale = Math.min(84.0 / healthIcon.getIconWidth(), 24.0 / healthIcon.getIconHeight());
            lifeTextLabel.setIcon(new ImageIcon(healthIcon.getImage().getScaledInstance(
                    Math.max(1, (int) Math.round(healthIcon.getIconWidth() * scale)),
                    Math.max(1, (int) Math.round(healthIcon.getIconHeight() * scale)), Image.SCALE_SMOOTH)));
            lifeTextLabel.setText("");
        }
        lifeTextLabel.getAccessibleContext().setAccessibleName("남은 기회");

        livesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        livesPanel.setOpaque(false);

        lifeContent.add(lifeTextLabel);
        lifeContent.add(livesPanel);
        JPanel lifeBox = statusBox(lifeContent, new EmptyBorder(7, 12, 7, 12));

        scoreLabel = new JLabel("점수 0");
        scoreLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 17));
        scoreLabel.setForeground(Theme.TEXT);
        ImageIcon scoreIcon = new ImageIcon(ImagePaths.ROOT + ImagePaths.TEXT_SCORE);
        if (scoreIcon.getIconWidth() > 0 && scoreIcon.getIconHeight() > 0) {
            double scale = Math.min(46.0 / scoreIcon.getIconWidth(), 25.0 / scoreIcon.getIconHeight());
            int width = Math.max(1, (int) Math.round(scoreIcon.getIconWidth() * scale));
            int height = Math.max(1, (int) Math.round(scoreIcon.getIconHeight() * scale));
            BufferedImage shiftedScore = new BufferedImage(width, height + 4, BufferedImage.TYPE_INT_ARGB);
            ImageIcon scaledScore = new ImageIcon(
                    scoreIcon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH));
            Graphics2D g = shiftedScore.createGraphics();
            scaledScore.paintIcon(null, g, 0, 3);
            g.dispose();
            scoreLabel.setIcon(new ImageIcon(shiftedScore));
        }
        scoreLabel.setIconTextGap(8);
        JPanel scoreBox = statusBox(scoreLabel, new EmptyBorder(5, 22, 9, 22));

        backBtn = new RoundedButton("← 메인으로", 18);
        backBtn.setPreferredSize(new Dimension(132, 42));
        backBtn.setBorder(new EmptyBorder(8, 12, 12, 18));
        ImageIcon backIcon = new ImageIcon(ImagePaths.ROOT + ImagePaths.TEXT_GO_MAIN);
        if (backIcon.getIconWidth() > 0 && backIcon.getIconHeight() > 0) {
            double scale = Math.min(102.0 / backIcon.getIconWidth(), 24.0 / backIcon.getIconHeight());
            backBtn.setIcon(new ImageIcon(backIcon.getImage().getScaledInstance(
                    Math.max(1, (int) Math.round(backIcon.getIconWidth() * scale)),
                    Math.max(1, (int) Math.round(backIcon.getIconHeight() * scale)), Image.SCALE_SMOOTH)));
            backBtn.setText("");
            backBtn.setBorder(new EmptyBorder(13, 12, 12, 18));
        }
        backBtn.getAccessibleContext().setAccessibleName("메인으로");
        backBtn.addActionListener(e -> {
            stop();
            if (onBackToMain != null) {
                onBackToMain.run();
            }
        });

        rightInfo.add(lifeBox);
        rightInfo.add(scoreBox);
        rightInfo.add(backBtn);

        topBar.add(rightInfo, BorderLayout.EAST);
        return topBar;
    }

    private static JPanel statusBox(JComponent content, EmptyBorder padding) {
        JPanel box = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(super.getPreferredSize().width, 42);
            }

            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g = (Graphics2D) graphics.create();
                try {
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setColor(new Color(32, 50, 15, 65));
                    g.fillRoundRect(1, 5, getWidth() - 2, getHeight() - 6, 36, 36);
                    g.setColor(Theme.PRIMARY);
                    g.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 6, 36, 36);
                } finally {
                    g.dispose();
                }
            }
        };
        box.setOpaque(false);
        box.setBorder(padding);
        content.setOpaque(false);
        box.add(content);
        return box;
    }

    private JPanel createWrongReviewBox() {
        JPanel box = new RoundedPanel(new BorderLayout(0, 8));
        box.setPreferredSize(new Dimension(250, 0));
        box.setBackground(BG_WHITE);
        box.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(new Color(181, 205, 157), 2),
                new EmptyBorder(12, 12, 12, 12)
        ));

        wrongHeaderLabel = new JLabel("오답 복습 (0)");
        wrongHeaderLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 17));
        wrongHeaderLabel.setForeground(Color.BLACK);
        wrongHeaderLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_BLACK),
                new EmptyBorder(0, 2, 8, 2)
        ));
        box.add(wrongHeaderLabel, BorderLayout.NORTH);

        wrongListPanel = new JPanel();
        wrongListPanel.setLayout(new BoxLayout(wrongListPanel, BoxLayout.Y_AXIS));
        wrongListPanel.setBackground(BG_WHITE);

        JScrollPane scroll = new JScrollPane(wrongListPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_WHITE);
        box.add(scroll, BorderLayout.CENTER);

        return box;
    }

    private JPanel createBottomHelpBar() {
        JPanel bottomBar = new RoundedPanel(new FlowLayout(FlowLayout.CENTER, 40, 8));
        bottomBar.setOpaque(false);
        bottomBar.setBorder(new RoundedBorder(new Color(181, 205, 157), 2));

        for (String text : new String[] {"[ ↑ / ↓ ] 각도 조절", "[ ← / → ] 힘 조절", "[ SPACE ] 슛"}) {
            JLabel label = new JLabel(text);
            label.setFont(new Font("맑은 고딕", Font.BOLD, 15));
            label.setForeground(Color.BLACK);
            helpLabels.add(label);
            bottomBar.add(label);
        }

        return bottomBar;
    }

    private void refreshWrongList() {
        List<BasketballGame.WrongRecord> records = game.getWrongRecords();
        wrongHeaderLabel.setText("오답 복습 (" + records.size() + ")");
        wrongListPanel.removeAll();

        for (BasketballGame.WrongRecord rec : records) {
            JPanel card = new RoundedPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBackground(MINT_CARD);
            card.setBorder(BorderFactory.createCompoundBorder(
                    new RoundedBorder(new Color(181, 205, 157), 1),
                    new EmptyBorder(8, 10, 8, 10)
            ));
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                    card.getInsets().top + card.getInsets().bottom
                            + 3 * card.getFontMetrics(Theme.REVIEW_FONT).getHeight()));

            JLabel wordLbl = new JLabel("문제: " + rec.question);
            wordLbl.setFont(Theme.REVIEW_FONT);
            wordLbl.setForeground(Color.BLACK);

            JLabel ansLbl = new JLabel("정답: " + rec.answer);
            ansLbl.setFont(Theme.REVIEW_FONT);
            ansLbl.setForeground(new Color(46, 125, 50));

            JLabel myLbl = new JLabel("선택: " + rec.selected);
            myLbl.setFont(Theme.REVIEW_FONT);
            myLbl.setForeground(new Color(220, 53, 69));

            card.add(wordLbl);
            card.add(ansLbl);
            card.add(myLbl);

            wrongListPanel.add(card);
            wrongListPanel.add(Box.createVerticalStrut(8));
        }

        wrongListPanel.revalidate();
        wrongListPanel.repaint();
    }

    private void updateTopStatus() {
        scoreLabel.setText((scoreLabel.getIcon() == null ? "점수 " : "")
                + String.format("%,d", game.getScore()));
        scoreLabel.getAccessibleContext().setAccessibleName("점수 " + game.getScore());
        livesPanel.removeAll();
        int lives = game.getLives();

        for (int i = 0; i < 3; i++) {
            JLabel h = new JLabel();
            if (activeHeartIcon != null && grayHeartIcon != null) {
                h.setIcon(i < lives ? activeHeartIcon : grayHeartIcon);
            } else {
                h.setText("♥");
                h.setForeground(i < lives ? new Color(229, 57, 53) : Color.LIGHT_GRAY);
                h.setFont(new Font("맑은 고딕", Font.BOLD, 18));
            }
            livesPanel.add(h);
        }
        livesPanel.revalidate();
        livesPanel.repaint();
    }

    private void syncResponsiveLayout() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        double uiScale = Math.min(1.0, Math.min(w / 1020.0, h / 620.0));
        uiScale = Math.max(0.6, uiScale);

        wrongReviewBox.setPreferredSize(new Dimension((int) (250 * uiScale), 0));
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, (int) (26 * uiScale)));
        lifeTextLabel.setFont(new Font("맑은 고딕", Font.BOLD, (int) (15 * uiScale)));
        scoreLabel.setFont(new Font("맑은 고딕", Font.BOLD, (int) (16 * uiScale)));
        wrongHeaderLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 17));
        for (JLabel label : helpLabels) {
            label.setFont(new Font("맑은 고딕", Font.BOLD, (int) (15 * uiScale)));
        }
        backBtn.setFont(new Font("맑은 고딕", Font.BOLD, (int) (15 * uiScale)));

        revalidate();
    }

    public void setOnBackToMain(Runnable onBackToMain) {
        this.onBackToMain = onBackToMain;
    }

    public void setOnGameOver(BiConsumer<Integer, List<String>> onGameOver) {
        this.onGameOver = onGameOver;
    }

    public void start() {
        game.toReadyState();
        startOverlay.setVisible(true);
        gameOverOverlay.setVisible(false);
        updateTopStatus();
        refreshWrongList();
        if (!gameTimer.isRunning()) {
            gameTimer.start();
        }
        requestFocusInWindow();
    }

    public void stop() {
        if (gameTimer != null && gameTimer.isRunning()) {
            gameTimer.stop();
        }
    }

    private static List<String[]> createDummyWords() {
        List<String[]> list = new ArrayList<>();
        list.add(new String[]{"apple", "사과"});
        list.add(new String[]{"banana", "바나나"});
        list.add(new String[]{"computer", "컴퓨터"});
        list.add(new String[]{"library", "도서관"});
        list.add(new String[]{"umbrella", "우산"});
        list.add(new String[]{"elephant", "코끼리"});
        return list;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("보카랜드 - 농구 게임 테스트");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1020, 620);
            frame.setLocationRelativeTo(null);
            frame.add(new BasketballPanel());
            frame.setVisible(true);
        });
    }
}
