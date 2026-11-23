package vocaland.ui.basketball;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import vocaland.ui.RoundedButton;

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
public class BasketballPanel extends JPanel {

    private static final Color BG_WHITE = Color.WHITE;
    private static final Color GREEN_BTN = new Color(174, 213, 129);
    private static final Color GREEN_BTN_SHADOW = new Color(139, 179, 94);
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

    private JLabel titleLabel;
    private JLabel lifeTextLabel;
    private JLabel scoreLabel;
    private JButton backBtn;
    private JPanel livesPanel;
    private JPanel wrongReviewBox;
    private JLabel wrongHeaderLabel;
    private JPanel wrongListPanel;
    private JLabel helpLabel;

    private JPanel startOverlay;
    private JPanel gameOverOverlay;
    private JLabel finalScoreLabel;

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
        centerWrapper.setBackground(BG_WHITE);

        JPanel gameBoxWrapper = new JPanel(new BorderLayout());
        gameBoxWrapper.setBackground(BG_WHITE);
        gameBoxWrapper.setBorder(new LineBorder(BORDER_BLACK, 2));

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
            int result = game.updatePhysics();
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
        Image raw = new ImageIcon("images/heart.png").getImage();
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

        public void setButtonColors(Color main, Color shadow) {
            this.mainColor = main;
            this.shadowColor = shadow;
            repaint();
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

            g2.setColor(mainColor);
            g2.fillRoundRect(0, yOffset, w, h - shadowOffset, 22, 22);

            FontMetrics fm = g2.getFontMetrics();
            int textX = (w - fm.stringWidth(getText())) / 2;
            int textY = yOffset + ((h - shadowOffset - fm.getHeight()) / 2) + fm.getAscent();
            g2.setColor(getForeground());
            g2.drawString(getText(), textX, textY);

            g2.dispose();
        }
    }

    private JPanel createStartOverlay() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(BG_WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_BLACK, 1, true),
                new EmptyBorder(26, 36, 26, 36)
        ));

        JLabel title = new JLabel("농구 게임");
        title.setFont(new Font("맑은 고딕", Font.BOLD, 28));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subDesc = new JLabel(game.getMode().getGuide());
        subDesc.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        subDesc.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel modeTitle = new JLabel("모드 선택");
        modeTitle.setFont(new Font("맑은 고딕", Font.BOLD, 15));
        modeTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel modeBtnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        modeBtnRow.setBackground(BG_WHITE);

        PillButton meaningBtn = new PillButton("● 영어 보고 뜻 맞히기", GREEN_BTN, GREEN_BTN_SHADOW);
        PillButton spellingBtn = new PillButton("○ 뜻 보고 영어 맞히기", new Color(235, 235, 235), new Color(200, 200, 200));

        meaningBtn.addActionListener(e -> {
            game.setMode(BasketballMode.MEANING);
            meaningBtn.setText("● 영어 보고 뜻 맞히기");
            meaningBtn.setButtonColors(GREEN_BTN, GREEN_BTN_SHADOW);
            spellingBtn.setText("○ 뜻 보고 영어 맞히기");
            spellingBtn.setButtonColors(new Color(235, 235, 235), new Color(200, 200, 200));
            subDesc.setText(game.getMode().getGuide());
            helpLabel.setText("조작법 : [ ↑ / ↓ ] 각도 조절  |  [ ← / → ] 힘 조절  |  [ SPACE ] 슛   (" + game.getMode().getGuide() + ")");
        });

        spellingBtn.addActionListener(e -> {
            game.setMode(BasketballMode.SPELLING);
            spellingBtn.setText("● 뜻 보고 영어 맞히기");
            spellingBtn.setButtonColors(GREEN_BTN, GREEN_BTN_SHADOW);
            meaningBtn.setText("○ 영어 보고 뜻 맞히기");
            meaningBtn.setButtonColors(new Color(235, 235, 235), new Color(200, 200, 200));
            subDesc.setText(game.getMode().getGuide());
            helpLabel.setText("조작법 : [ ↑ / ↓ ] 각도 조절  |  [ ← / → ] 힘 조절  |  [ SPACE ] 슛   (" + game.getMode().getGuide() + ")");
        });

        modeBtnRow.add(meaningBtn);
        modeBtnRow.add(spellingBtn);

        JLabel diffTitle = new JLabel("난이도 선택 (클릭 시 바로 시작)");
        diffTitle.setFont(new Font("맑은 고딕", Font.BOLD, 15));
        diffTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel diffBtnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        diffBtnRow.setBackground(BG_WHITE);

        PillButton easyBtn = new PillButton("이지 모드", BLUE_BTN, BLUE_BTN_SHADOW);
        easyBtn.setForeground(Color.WHITE);
        easyBtn.addActionListener(e -> {
            game.setHardMode(false);
            startGamePlay();
        });

        PillButton hardBtn = new PillButton("하드 모드", RED_BTN, RED_BTN_SHADOW);
        hardBtn.setForeground(Color.WHITE);
        hardBtn.addActionListener(e -> {
            game.setHardMode(true);
            startGamePlay();
        });

        diffBtnRow.add(easyBtn);
        diffBtnRow.add(hardBtn);

        card.add(title);
        card.add(Box.createVerticalStrut(8));
        card.add(subDesc);
        card.add(Box.createVerticalStrut(18));
        card.add(modeTitle);
        card.add(Box.createVerticalStrut(8));
        card.add(modeBtnRow);
        card.add(Box.createVerticalStrut(18));
        card.add(diffTitle);
        card.add(Box.createVerticalStrut(8));
        card.add(diffBtnRow);

        return card;
    }

    private JPanel createGameOverOverlay() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(BG_WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_BLACK, 2, true),
                new EmptyBorder(26, 42, 26, 42)
        ));

        JLabel overTitle = new JLabel("게임 오버!");
        overTitle.setFont(new Font("맑은 고딕", Font.BOLD, 28));
        overTitle.setForeground(new Color(220, 53, 69));
        overTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        finalScoreLabel = new JLabel("최종 점수: 0점");
        finalScoreLabel.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        finalScoreLabel.setForeground(Color.BLACK);
        finalScoreLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel topBtnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        topBtnRow.setBackground(BG_WHITE);

        PillButton retryBtn = new PillButton("다시하기", GREEN_BTN, GREEN_BTN_SHADOW);
        retryBtn.addActionListener(e -> startGamePlay());

        PillButton changeModeBtn = new PillButton("모드 변경", GREEN_BTN, GREEN_BTN_SHADOW);
        changeModeBtn.addActionListener(e -> {
            game.toReadyState();
            gameOverOverlay.setVisible(false);
            startOverlay.setVisible(true);
            canvas.repaint();
        });

        topBtnRow.add(retryBtn);
        topBtnRow.add(changeModeBtn);

        JPanel bottomBtnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        bottomBtnRow.setBackground(BG_WHITE);

        PillButton mainBtn = new PillButton("← 메인으로", new Color(230, 230, 230), new Color(195, 195, 195));
        mainBtn.addActionListener(e -> {
            stop();
            if (onBackToMain != null) {
                onBackToMain.run();
            }
        });
        bottomBtnRow.add(mainBtn);

        card.add(overTitle);
        card.add(Box.createVerticalStrut(10));
        card.add(finalScoreLabel);
        card.add(Box.createVerticalStrut(22));
        card.add(topBtnRow);
        card.add(Box.createVerticalStrut(12));
        card.add(bottomBtnRow);

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
        finalScoreLabel.setText("최종 점수: " + game.getScore() + "점");
        gameOverOverlay.setVisible(true);
    }

    private JPanel createTopBar() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(BG_WHITE);

        titleLabel = new JLabel("농구 게임");
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 26));
        titleLabel.setForeground(Color.BLACK);
        topBar.add(titleLabel, BorderLayout.WEST);

        JPanel rightInfo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightInfo.setBackground(BG_WHITE);

        JPanel lifeBox = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
        lifeBox.setBackground(BG_WHITE);
        lifeBox.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_BLACK, 2),
                new EmptyBorder(2, 10, 2, 10)
        ));

        lifeTextLabel = new JLabel("남은 기회");
        lifeTextLabel.setFont(new Font("맑은 고딕", Font.BOLD, 15));
        lifeTextLabel.setForeground(Color.BLACK);
        ImageIcon healthIcon = new ImageIcon("images/health.png");
        if (healthIcon.getIconWidth() > 0 && healthIcon.getIconHeight() > 0) {
            double scale = Math.min(84.0 / healthIcon.getIconWidth(), 24.0 / healthIcon.getIconHeight());
            lifeTextLabel.setIcon(new ImageIcon(healthIcon.getImage().getScaledInstance(
                    Math.max(1, (int) Math.round(healthIcon.getIconWidth() * scale)),
                    Math.max(1, (int) Math.round(healthIcon.getIconHeight() * scale)), Image.SCALE_SMOOTH)));
            lifeTextLabel.setText("");
        }
        lifeTextLabel.getAccessibleContext().setAccessibleName("남은 기회");

        livesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        livesPanel.setBackground(BG_WHITE);

        lifeBox.add(lifeTextLabel);
        lifeBox.add(livesPanel);

        JPanel scoreBox = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
        scoreBox.setBackground(BG_WHITE);
        scoreBox.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_BLACK, 2),
                new EmptyBorder(2, 12, 2, 12)
        ));

        scoreLabel = new JLabel("점수 0");
        scoreLabel.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        scoreLabel.setForeground(Color.BLACK);
        scoreBox.add(scoreLabel);

        backBtn = new RoundedButton("← 메인으로", 18);
        backBtn.setPreferredSize(new Dimension(132, 42));
        backBtn.setBorder(new EmptyBorder(8, 12, 12, 18));
        ImageIcon backIcon = new ImageIcon("images/text/go_main.png");
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

    private JPanel createWrongReviewBox() {
        JPanel box = new JPanel(new BorderLayout(0, 8));
        box.setPreferredSize(new Dimension(250, 0));
        box.setBackground(BG_WHITE);
        box.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_BLACK, 2),
                new EmptyBorder(12, 12, 12, 12)
        ));

        wrongHeaderLabel = new JLabel("오답 복습 (0)");
        wrongHeaderLabel.setFont(new Font("맑은 고딕", Font.BOLD, 16));
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
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 8));
        bottomBar.setBackground(BG_WHITE);
        bottomBar.setBorder(new LineBorder(BORDER_BLACK, 2));

        helpLabel = new JLabel("조작법 : [ ↑ / ↓ ] 각도 조절  |  [ ← / → ] 힘 조절  |  [ SPACE ] 슛   (" + game.getMode().getGuide() + ")");
        helpLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        helpLabel.setForeground(Color.BLACK);
        bottomBar.add(helpLabel);

        return bottomBar;
    }

    private void refreshWrongList() {
        List<BasketballGame.WrongRecord> records = game.getWrongRecords();
        wrongHeaderLabel.setText("오답 복습 (" + records.size() + ")");
        wrongListPanel.removeAll();

        for (BasketballGame.WrongRecord rec : records) {
            JPanel card = new JPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBackground(MINT_CARD);
            card.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(BORDER_BLACK, 1),
                    new EmptyBorder(8, 10, 8, 10)
            ));
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 76));

            JLabel wordLbl = new JLabel("문제: " + rec.question);
            wordLbl.setFont(new Font("맑은 고딕", Font.BOLD, 14));
            wordLbl.setForeground(Color.BLACK);

            JLabel ansLbl = new JLabel("정답: " + rec.answer);
            ansLbl.setFont(new Font("맑은 고딕", Font.BOLD, 13));
            ansLbl.setForeground(new Color(46, 125, 50));

            JLabel myLbl = new JLabel("선택: " + rec.selected);
            myLbl.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
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
        scoreLabel.setText("점수 " + game.getScore());
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
        wrongHeaderLabel.setFont(new Font("맑은 고딕", Font.BOLD, (int) (16 * uiScale)));
        helpLabel.setFont(new Font("맑은 고딕", Font.BOLD, (int) (14 * uiScale)));
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
