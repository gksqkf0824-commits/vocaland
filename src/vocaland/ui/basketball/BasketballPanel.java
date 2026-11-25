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
 * setOnOpenStore()          : 세트 효과 배지 클릭 시 상점 화면으로 이동할 콜백 등록
 * setOnAnswer()             : 문제 정답/오답 판정 시 학습 통계 기록용 콜백 등록
 * setEarnedCoins()          : 게임 종료 시 획득한 코인 수량 표시 갱신
 * syncEquippedSet()         : 상점 장착 아이템에 따른 세트 효과(네온/황금/없음) UI 및 게임 로직 동기화
 * createStartOverlay()      : 시작 대기 안내창 (모드 선택, 이지/하드/익스퍼트 난이도 선택)
 * positionStartOverlays()   : 창 크기 변경에 따른 오버레이 팝업 줌 배율 및 UI 크기 반응형 조절 1
 * createGameOverOverlay()   : 게임 오버 안내창 (최종 점수, 획득 코인, 다시하기, 모드 변경, 메인으로)
 * createExitConfirmOverlay(): 게임 도중 '메인으로' 클릭 시 점수 정산 및 나가기 확인 팝업 생성
 * startGamePlay()           : 선택한 모드와 난이도로 실제 게임 시작
 * showGameOverOverlay()     : 게임 오버 결과 카드 표시 및 콜백 호출
 * syncExpertSwitchUI()      : 익스퍼트 모드 여부에 따른 상단 바 입체 스위치 표시 및 색상 동기화
 * createTopBar()            : 상단 (제목, 익스퍼트 전용 포물선 스위치 버튼, 남은 기회, 점수, 메인으로)
 * createWrongReviewBox()    : 우측 (오답 복습 목록)
 * createBottomHelpBar()     : 하단 (키보드 조작법 안내 문구)
 * syncResponsiveLayout()    : 창 크기 변경에 따른 오버레이 팝업 줌 배율 및 UI 크기 반응형 조절 2
 * setOnBackToMain()         : ← 메인으로 버튼을 눌렀을 때 실행할 코드 등록 (화면 전환용)
 * setOnGameOver()           : 게임이 끝났을 때 점수·놓친 단어를 받을 코드 등록 (백엔드용)
 * start() / stop()          : 화면에 들어올 때 모드 선택 대기 상태로 초기화, 나갈 때 타이머 정지
 * main()                    : 혼자 테스트용 실행
 */
public class BasketballPanel extends BackgroundPanel {

    private static final Color BG_WHITE = Color.WHITE;
    private static final Color BLUE_BTN = new Color(100, 181, 246);
    private static final Color BLUE_BTN_SHADOW = new Color(66, 145, 210);
    private static final Color RED_BTN = new Color(239, 83, 80);
    private static final Color RED_BTN_SHADOW = new Color(198, 40, 40);
    private static final Color PURPLE_BTN = new Color(156, 39, 176);
    private static final Color PURPLE_BTN_END = new Color(94, 53, 177);
    private static final Color PURPLE_BTN_SHADOW = new Color(69, 39, 160);
    private static final Color MINT_CARD = new Color(241, 248, 233);
    private static final Color BORDER_BLACK = Color.BLACK;

    private ImageIcon activeHeartIcon;
    private ImageIcon grayHeartIcon;

    private final BasketballGame game;
    private final BasketballCanvas canvas;

    private Runnable onBackToMain;
    private Runnable onOpenStore;
    private BiConsumer<Integer, List<String>> onGameOver;
    private BiConsumer<String, Boolean> onAnswer;
    private Integer customEarnedCoins = null;

    public void setOnOpenStore(Runnable onOpenStore) { this.onOpenStore = onOpenStore; }
    public void setOnAnswer(BiConsumer<String, Boolean> listener) { onAnswer = listener; }
    public BasketballGame.Difficulty getDifficulty() { return game.getDifficulty(); }
    public void setHoopImage(String path) { canvas.setHoopImage(path); }
    public void setBallImage(String path) { canvas.setBallImage(path); }

    public void setEarnedCoins(int coins) {
        this.customEarnedCoins = coins;
        if (earnedCoinLabel != null) {
            earnedCoinLabel.setText(String.format("획득 코인: %,d", coins));
        }
    }

    public void syncEquippedSet(boolean isNeonEquipped, boolean isGoldEquipped) {
        if (isNeonEquipped) {
            game.setSetEffect(BasketballGame.SetEffect.NEON);
            if (setHeaderLabel != null) {
                setHeaderLabel.setText("세트 효과 발동!");
                setHeaderLabel.setForeground(new Color(26, 35, 126));
            }
            if (activeSetMainBtn != null) {
                activeSetMainBtn.setText("네온");
                activeSetMainBtn.setButtonGradient(
                        new Color(0, 229, 255),
                        new Color(26, 35, 126),
                        new Color(49, 27, 146),
                        new Color(20, 12, 75),
                        new Color(0, 229, 255)
                );
            }
            if (activeSetDescLabel != null) {
                activeSetDescLabel.setText("오답 시 일정 확률로 체력이 방어됩니다.");
            }
            if (setDescFloatingBox != null) {
                setDescFloatingBox.setVisible(startZoomWrapper != null && startZoomWrapper.isVisible());
            }
        } else if (isGoldEquipped) {
            game.setSetEffect(BasketballGame.SetEffect.GOLD);
            if (setHeaderLabel != null) {
                setHeaderLabel.setText("세트 효과 발동!");
                setHeaderLabel.setForeground(new Color(180, 110, 0));
            }
            if (activeSetMainBtn != null) {
                activeSetMainBtn.setText("황금");
                activeSetMainBtn.setButtonGradient(
                        new Color(255, 235, 120),
                        new Color(230, 165, 0),
                        new Color(170, 105, 0),
                        new Color(130, 75, 0),
                        new Color(255, 215, 64)
                );
            }
            if (activeSetDescLabel != null) {
                activeSetDescLabel.setText("정답 시 보너스 점수를 부여합니다.");
            }
            if (setDescFloatingBox != null) {
                setDescFloatingBox.setVisible(startZoomWrapper != null && startZoomWrapper.isVisible());
            }
        } else {
            game.setSetEffect(BasketballGame.SetEffect.CLASSIC);
            if (setHeaderLabel != null) {
                setHeaderLabel.setText("세트 효과");
                setHeaderLabel.setForeground(Theme.TEXT);
            }
            if (activeSetMainBtn != null) {
                activeSetMainBtn.setText("없음");
                activeSetMainBtn.setButtonColors(new Color(165, 165, 165), new Color(125, 125, 125));
            }
            if (activeSetDescLabel != null) {
                activeSetDescLabel.setText("");
            }
            if (setDescFloatingBox != null) {
                setDescFloatingBox.setVisible(false);
            }
        }
        positionStartOverlays();
    }

    private JLabel titleLabel;
    private PillButton expertSwitchBtn;
    private JLabel lifeTextLabel;
    private JLabel scoreLabel;
    private JButton backBtn;
    private JLabel setHeaderLabel;
    private PillButton activeSetMainBtn;
    private JLabel activeSetDescLabel;
    private JPanel setDescFloatingBox;
    private int accumulatedPendingScore = 0;
    private JPanel livesPanel;
    private JPanel wrongReviewBox;
    private JLabel wrongHeaderLabel;
    private JPanel wrongListPanel;
    private final List<JLabel> helpLabels = new ArrayList<>();
    private final java.util.Set<Integer> pressedKeys = new java.util.HashSet<>();

    private JPanel startOverlay;
    private JPanel gameOverOverlay;
    private JPanel exitConfirmOverlay;
    private ZoomOverlayWrapper startZoomWrapper;
    private ZoomOverlayWrapper gameOverZoomWrapper;
    private ZoomOverlayWrapper exitConfirmZoomWrapper;
    private JLabel finalScoreLabel;
    private JLabel earnedCoinLabel;
    private JLabel exitCurrentScoreLabel;
    private JLabel exitCurrentCoinLabel;
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
        setDescFloatingBox = createSetDescFloatingBox();
        gameOverOverlay = createGameOverOverlay();
        exitConfirmOverlay = createExitConfirmOverlay();

        JPanel startStage = buildStartStagePanel();
        startZoomWrapper = new ZoomOverlayWrapper(startStage);
        gameOverZoomWrapper = new ZoomOverlayWrapper(gameOverOverlay);
        exitConfirmZoomWrapper = new ZoomOverlayWrapper(exitConfirmOverlay);

        canvas.setLayout(null);
        canvas.add(startZoomWrapper);
        canvas.add(gameOverZoomWrapper);
        canvas.add(exitConfirmZoomWrapper);
        setDescFloatingBox.setVisible(false);
        gameOverZoomWrapper.setVisible(false);
        exitConfirmZoomWrapper.setVisible(false);

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
            if (game.getState() == BasketballGame.State.PLAYING && !game.isEffectPlaying()) {
                if (pressedKeys.contains(KeyEvent.VK_UP)) game.adjustAngle(1.2);
                if (pressedKeys.contains(KeyEvent.VK_DOWN)) game.adjustAngle(-1.2);
                if (pressedKeys.contains(KeyEvent.VK_RIGHT)) game.adjustPower(0.35);
                if (pressedKeys.contains(KeyEvent.VK_LEFT)) game.adjustPower(-0.35);
            }

            String questionEnglish = game.getCurrentEnglish();
            int result = game.updatePhysics();
            if ((result == 1 || result == 2) && onAnswer != null) {
                onAnswer.accept(questionEnglish, result == 1);
            }
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
                int code = e.getKeyCode();
                pressedKeys.add(code);
                if (code == KeyEvent.VK_SPACE
                        && game.getState() == BasketballGame.State.PLAYING
                        && !game.isEffectPlaying()) {
                    game.shoot();
                    canvas.repaint();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                pressedKeys.remove(e.getKeyCode());
            }
        });

        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
            }
        });

        updateTopStatus();
        syncExpertSwitchUI();
    }

    private void initHeartIcons() {
        ImageIcon heartIcon = vocaland.ui.util.ImageAssets.icon(ImagePaths.ROOT + ImagePaths.GAME_HEART, 22, 22);
        if (heartIcon != null) {
            Image scaled = heartIcon.getImage();
            activeHeartIcon = heartIcon;

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
        private Color midColor;
        private Color gradientEndColor;
        private Color shadowColor;
        private Color glowBorderColor;
        private boolean isPressed = false;

        public PillButton(String text, Color mainColor, Color shadowColor) {
            this(text, mainColor, null, null, shadowColor);
        }

        public PillButton(String text, Color mainColor, Color gradientEndColor, Color shadowColor) {
            this(text, mainColor, null, gradientEndColor, shadowColor);
        }

        public PillButton(String text, Color mainColor, Color midColor, Color gradientEndColor, Color shadowColor) {
            super(text);
            this.mainColor = mainColor;
            this.midColor = midColor;
            this.gradientEndColor = gradientEndColor;
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
                    if (isEnabled()) {
                        isPressed = true;
                        repaint();
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    isPressed = false;
                    repaint();
                }
            });
        }

        public void setButtonColors(Color main, Color shadow) {
            setButtonGradient(main, null, null, shadow, null);
        }

        public void setButtonGradient(Color main, Color mid, Color end, Color shadow, Color glowBorder) {
            this.mainColor = main;
            this.midColor = mid;
            this.gradientEndColor = end;
            this.shadowColor = shadow;
            this.glowBorderColor = glowBorder;
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

            g2.setColor(isEnabled() ? shadowColor : new Color(155, 155, 155));
            g2.fillRoundRect(0, shadowOffset, w, h - shadowOffset, 22, 22);

            if (!isEnabled()) {
                g2.setColor(new Color(205, 205, 205));
            } else if (!isSelected()) {
                g2.setColor(pale(mainColor));
            } else if (midColor != null && gradientEndColor != null) {
                float[] dist = {0.0f, 0.55f, 1.0f};
                Color[] cols = {mainColor, midColor, gradientEndColor};
                g2.setPaint(new LinearGradientPaint(0, yOffset, w, h - shadowOffset, dist, cols));
            } else if (gradientEndColor != null) {
                g2.setPaint(new GradientPaint(0, yOffset, mainColor, w, h - shadowOffset, gradientEndColor));
            } else {
                g2.setColor(mainColor);
            }
            g2.fillRoundRect(0, yOffset, w, h - shadowOffset, 22, 22);

            if (isEnabled() && isSelected()) {
                g2.setColor(glowBorderColor != null ? glowBorderColor : shadowColor);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, yOffset + 1, w - 3, h - shadowOffset - 3, 20, 20);
            }

            FontMetrics fm = g2.getFontMetrics();
            String text = getText();
            int textX = (w - fm.stringWidth(text)) / 2;
            int textY = yOffset + ((h - shadowOffset - fm.getHeight()) / 2) + fm.getAscent();
            g2.setColor(!isEnabled() ? new Color(120, 120, 120) : (isSelected() ? getForeground() : Theme.TEXT));
            g2.drawString(text, textX, textY);

            if (isEnabled() && getModel().isRollover()) {
                g2.setColor(new Color(0, 0, 0, 36));
                g2.fillRoundRect(0, yOffset, w, h - shadowOffset, 22, 22);
            }

            g2.dispose();
        }

        private static Color pale(Color c) {
            return new Color((c.getRed() + 3 * 255) / 4, (c.getGreen() + 3 * 255) / 4, (c.getBlue() + 3 * 255) / 4);
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

        JPanel expertBtnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        expertBtnRow.setOpaque(false);

        PillButton expertBtn = new PillButton("익스퍼트 모드", PURPLE_BTN, PURPLE_BTN_END, PURPLE_BTN_SHADOW);
        expertBtn.setForeground(Color.WHITE);

        JLabel expertDesc = new JLabel(" ");
        expertDesc.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        expertDesc.setForeground(new Color(106, 27, 154));
        expertDesc.setAlignmentX(Component.CENTER_ALIGNMENT);

        Runnable syncDifficulty = () -> {
            BasketballGame.Difficulty d = game.getDifficulty();
            easyBtn.setSelected(d == BasketballGame.Difficulty.EASY);
            hardBtn.setSelected(d == BasketballGame.Difficulty.HARD);
            expertBtn.setSelected(d == BasketballGame.Difficulty.EXPERT);
            expertDesc.setText(d == BasketballGame.Difficulty.EXPERT
                    ? "※ 익스퍼트 모드: 공이 링과 백보드에 튕깁니다! 신중하게 넣으세요"
                    : " ");
            diffBtnRow.repaint();
            expertBtnRow.repaint();
        };
        easyBtn.addActionListener(e -> {
            game.setDifficulty(BasketballGame.Difficulty.EASY);
            syncDifficulty.run();
        });
        hardBtn.addActionListener(e -> {
            game.setDifficulty(BasketballGame.Difficulty.HARD);
            syncDifficulty.run();
        });
        expertBtn.addActionListener(e -> {
            game.setDifficulty(BasketballGame.Difficulty.EXPERT);
            syncDifficulty.run();
        });
        syncDifficulty.run();

        diffBtnRow.add(easyBtn);
        diffBtnRow.add(hardBtn);
        expertBtnRow.add(expertBtn);

        card.add(title);
        card.add(Box.createVerticalStrut(18));
        card.add(modeBtnRow);
        card.add(Box.createVerticalStrut(16));
        card.add(diffTitle);
        card.add(Box.createVerticalStrut(8));
        card.add(diffBtnRow);
        card.add(Box.createVerticalStrut(10));
        card.add(expertBtnRow);
        card.add(Box.createVerticalStrut(4));
        card.add(expertDesc);
        card.add(Box.createVerticalStrut(4));

        JPanel bottomLayer = new JPanel();
        bottomLayer.setLayout(new OverlayLayout(bottomLayer));
        bottomLayer.setOpaque(false);
        bottomLayer.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel centerStartLayer = new JPanel(new GridBagLayout());
        centerStartLayer.setOpaque(false);
        centerStartLayer.setAlignmentX(0.5f);
        centerStartLayer.setAlignmentY(0.5f);

        JButton startButton = new RoundedButton("시작하기", 16);
        startButton.setFont(new Font(Font.DIALOG, Font.BOLD, 20));
        ImageIcon startIcon = vocaland.ui.util.ImageAssets.icon(ImagePaths.ROOT + ImagePaths.TEXT_START, 108, 32);
        if (startIcon != null) {
            startButton.setIcon(startIcon);
            startButton.setText("");
        }
        startButton.getAccessibleContext().setAccessibleName("시작하기");
        startButton.addActionListener(e -> startGamePlay());
        centerStartLayer.add(startButton);

        JPanel rightSetLayer = new JPanel(new GridBagLayout());
        rightSetLayer.setOpaque(false);
        rightSetLayer.setAlignmentX(0.5f);
        rightSetLayer.setAlignmentY(0.5f);

        JPanel topRightBadge = new JPanel();
        topRightBadge.setLayout(new BoxLayout(topRightBadge, BoxLayout.Y_AXIS));
        topRightBadge.setOpaque(false);

        setHeaderLabel = new JLabel("세트 효과", SwingConstants.CENTER);
        setHeaderLabel.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        setHeaderLabel.setForeground(Theme.TEXT);
        setHeaderLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        activeSetMainBtn = new PillButton("없음", new Color(165, 165, 165), new Color(125, 125, 125));
        activeSetMainBtn.setForeground(Color.WHITE);
        activeSetMainBtn.setSelected(true);
        activeSetMainBtn.setPreferredSize(new Dimension(88, 34));
        activeSetMainBtn.setMaximumSize(new Dimension(88, 34));
        activeSetMainBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        activeSetMainBtn.addActionListener(e -> {
            if (onOpenStore != null) {
                stop();
                onOpenStore.run();
            }
        });

        topRightBadge.add(setHeaderLabel);
        topRightBadge.add(Box.createVerticalStrut(3));
        topRightBadge.add(activeSetMainBtn);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(0, 0, 0, 6);
        rightSetLayer.add(topRightBadge, gbc);

        bottomLayer.add(rightSetLayer);
        bottomLayer.add(centerStartLayer);

        card.add(bottomLayer);
        return card;
    }

    private JPanel createSetDescFloatingBox() {
        JPanel box = new RoundedPanel(new BorderLayout());
        box.setBackground(Color.WHITE);
        box.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(new Color(181, 205, 157), 2),
                new EmptyBorder(6, 14, 6, 14)
        ));

        activeSetDescLabel = new JLabel("", SwingConstants.CENTER);
        activeSetDescLabel.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        activeSetDescLabel.setForeground(new Color(50, 50, 50));
        box.add(activeSetDescLabel, BorderLayout.CENTER);
        return box;
    }

    private JPanel buildStartStagePanel() {
        JPanel stage = new JPanel(null);
        stage.setOpaque(false);

        int cardW = 540;
        int cardH = 355;
        int boxW = 250;
        int boxH = 34;

        startOverlay.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(new Color(181, 205, 157), 2),
                new EmptyBorder(22, 34, 22, 34)
        ));
        startOverlay.setBounds(0, 0, cardW, cardH);

        setDescFloatingBox.setBounds(cardW - boxW + 14, cardH - (boxH / 2), boxW, boxH);

        stage.add(setDescFloatingBox);
        stage.add(startOverlay);
        stage.setComponentZOrder(setDescFloatingBox, 0);

        int totalW = cardW + 16;
        int totalH = cardH + (boxH / 2) + 4;
        stage.setPreferredSize(new Dimension(totalW, totalH));
        stage.setSize(totalW, totalH);
        return stage;
    }

    private static class ZoomOverlayWrapper extends JPanel {
        private final JComponent target;
        private double zoom = 1.0;
        private Component hoveredComp = null;

        public ZoomOverlayWrapper(JComponent target) {
            super(null);
            this.target = target;
            setOpaque(false);
            add(target);
            Dimension pref = target.getPreferredSize();
            target.setBounds(0, 0, Math.max(pref.width, 380), Math.max(pref.height, 220));
            enableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
        }

        public void setZoom(double newZoom) {
            this.zoom = Math.max(1.0, newZoom);
            Dimension pref = target.getPreferredSize();
            int baseW = Math.max(pref.width, 380);
            int baseH = Math.max(pref.height, 220);
            target.setBounds(0, 0, baseW, baseH);
            int zw = (int) Math.round(baseW * zoom);
            int zh = (int) Math.round(baseH * zoom);
            setPreferredSize(new Dimension(zw, zh));
            setSize(zw, zh);
            revalidate();
            repaint();
        }

        @Override
        public boolean contains(int x, int y) {
            return x >= 0 && y >= 0 && x < getWidth() && y < getHeight();
        }

        @Override
        protected void paintChildren(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.scale(zoom, zoom);
            super.paintChildren(g2);
            g2.dispose();
        }

        @Override
        protected void processMouseEvent(MouseEvent e) {
            forwardScaledMouseEvent(e);
        }

        @Override
        protected void processMouseMotionEvent(MouseEvent e) {
            forwardScaledMouseEvent(e);
        }

        @Override
        public Component getComponentAt(int x, int y) {
            return contains(x, y) ? this : null;
        }

        private void forwardScaledMouseEvent(MouseEvent e) {
            int sx = (int) Math.round(e.getX() / zoom);
            int sy = (int) Math.round(e.getY() / zoom);
            Component deep = (e.getID() == MouseEvent.MOUSE_EXITED)
                    ? null
                    : findDeepestEnabledComponent(target, sx, sy);

            if (deep != hoveredComp) {
                if (hoveredComp != null) {
                    Point exitPt = SwingUtilities.convertPoint(target, sx, sy, hoveredComp);
                    hoveredComp.dispatchEvent(new MouseEvent(hoveredComp, MouseEvent.MOUSE_EXITED,
                            e.getWhen(), e.getModifiersEx(), exitPt.x, exitPt.y,
                            e.getXOnScreen(), e.getYOnScreen(), 0, false, MouseEvent.NOBUTTON));
                }
                hoveredComp = deep;
                if (hoveredComp != null) {
                    Point enterPt = SwingUtilities.convertPoint(target, sx, sy, hoveredComp);
                    hoveredComp.dispatchEvent(new MouseEvent(hoveredComp, MouseEvent.MOUSE_ENTERED,
                            e.getWhen(), e.getModifiersEx(), enterPt.x, enterPt.y,
                            e.getXOnScreen(), e.getYOnScreen(), 0, false, MouseEvent.NOBUTTON));
                }
            }

            if (deep != null && deep != this) {
                Point pt = SwingUtilities.convertPoint(target, sx, sy, deep);
                MouseEvent mapped = new MouseEvent(deep, e.getID(), e.getWhen(), e.getModifiersEx(),
                        pt.x, pt.y, e.getXOnScreen(), e.getYOnScreen(), e.getClickCount(),
                        e.isPopupTrigger(), e.getButton());
                deep.dispatchEvent(mapped);
            }
            repaint();
        }

        private static Component findDeepestEnabledComponent(Component parent, int x, int y) {
            if (!parent.isVisible() || !parent.contains(x, y)) {
                return null;
            }
            if (parent instanceof Container container) {
                for (int i = 0; i < container.getComponentCount(); i++) {
                    Component child = container.getComponent(i);
                    if (child.isVisible()) {
                        int cx = x - child.getX();
                        int cy = y - child.getY();
                        if (cx >= 0 && cy >= 0 && cx < child.getWidth() && cy < child.getHeight()) {
                            Component found = findDeepestEnabledComponent(child, cx, cy);
                            if (found instanceof AbstractButton) {
                                return found;
                            }
                        }
                    }
                }
            }
            return (parent instanceof AbstractButton) ? parent : null;
        }
    }

    private void positionStartOverlays() {
        if (canvas == null || startZoomWrapper == null) {
            return;
        }
        int cw = canvas.getWidth();
        int ch = canvas.getHeight();
        if (cw <= 0 || ch <= 0) {
            return;
        }

        double scale = Math.max(1.0, Math.min(cw / 700.0, ch / 470.0));

        for (ZoomOverlayWrapper wrapper : new ZoomOverlayWrapper[]{startZoomWrapper, gameOverZoomWrapper, exitConfirmZoomWrapper}) {
            if (wrapper != null) {
                wrapper.setZoom(scale);
                int wx = (cw - wrapper.getWidth()) / 2;
                int wy = (ch - wrapper.getHeight()) / 2;
                wrapper.setLocation(wx, wy);
            }
        }
    }

    private JPanel createGameOverOverlay() {
        JPanel card = new vocaland.ui.components.GamePopupCard();

        JLabel overTitle = new JLabel("게임 오버");
        overTitle.setFont(new Font(Font.DIALOG, Font.BOLD, 30));
        overTitle.setForeground(Theme.TEXT);
        overTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        ImageIcon gameOverIcon = vocaland.ui.util.ImageAssets.icon(ImagePaths.ROOT + ImagePaths.GAME_GAME_OVER, 240, 70);
        if (gameOverIcon != null) {
            overTitle.setIcon(gameOverIcon);
            overTitle.setText("");
        }
        overTitle.getAccessibleContext().setAccessibleName("게임 오버");

        finalScoreLabel = new JLabel("최종 점수: 0점");
        finalScoreLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 20));
        finalScoreLabel.setForeground(Theme.TEXT);
        finalScoreLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        earnedCoinLabel = new JLabel("획득 코인: 0");
        earnedCoinLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 18));
        earnedCoinLabel.setForeground(new Color(210, 130, 0));
        ImageIcon coinIcon = vocaland.ui.util.ImageAssets.icon(ImagePaths.ROOT + "coin.png", 20, 20);
        if (coinIcon == null) {
            coinIcon = vocaland.ui.util.ImageAssets.icon(ImagePaths.ROOT + "store/coin.png", 20, 20);
        }
        if (coinIcon != null) {
            earnedCoinLabel.setIcon(coinIcon);
            earnedCoinLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
            earnedCoinLabel.setIconTextGap(6);
        }
        earnedCoinLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel topBtnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        topBtnRow.setOpaque(false);

        JButton retryBtn = new RoundedButton("다시하기", 14);
        retryBtn.setFont(new Font(Font.DIALOG, Font.BOLD, 17));
        retryBtn.addActionListener(e -> startGamePlay());

        JButton changeModeBtn = new RoundedButton("모드 변경", 14);
        changeModeBtn.setFont(new Font(Font.DIALOG, Font.BOLD, 17));
        changeModeBtn.addActionListener(e -> {
            game.toReadyState();
            gameOverZoomWrapper.setVisible(false);
            startZoomWrapper.setVisible(true);
            if (setDescFloatingBox != null) {
                setDescFloatingBox.setVisible(game.getSetEffect() != BasketballGame.SetEffect.CLASSIC);
            }
            positionStartOverlays();
            syncExpertSwitchUI();
            canvas.repaint();
        });

        topBtnRow.add(retryBtn);
        topBtnRow.add(changeModeBtn);

        JButton goMainOverBtn = new RoundedButton("메인으로", 14);
        goMainOverBtn.setFont(new Font(Font.DIALOG, Font.BOLD, 17));
        goMainOverBtn.addActionListener(e -> {
            stop();
            if (onBackToMain != null) onBackToMain.run();
        });
        topBtnRow.add(goMainOverBtn);

        card.add(overTitle);
        card.add(Box.createVerticalStrut(10));
        card.add(finalScoreLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(earnedCoinLabel);
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

    private JPanel createExitConfirmOverlay() {
        JPanel card = new vocaland.ui.components.GamePopupCard();

        JLabel warnTitle = new JLabel("메인으로 나가시겠습니까?");
        warnTitle.setFont(new Font("맑은 고딕", Font.BOLD, 22));
        warnTitle.setForeground(Theme.TEXT);
        warnTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel midInfoRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 4));
        midInfoRow.setOpaque(false);

        exitCurrentScoreLabel = new JLabel("현재 점수: 0점");
        exitCurrentScoreLabel.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        exitCurrentScoreLabel.setForeground(Color.BLACK);

        exitCurrentCoinLabel = new JLabel("획득한 코인: 0");
        exitCurrentCoinLabel.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        exitCurrentCoinLabel.setForeground(new Color(210, 130, 0));
        ImageIcon cIcon = vocaland.ui.util.ImageAssets.icon(ImagePaths.ROOT + "coin.png", 18, 18);
        if (cIcon == null) cIcon = vocaland.ui.util.ImageAssets.icon(ImagePaths.ROOT + "store/coin.png", 18, 18);
        if (cIcon != null) exitCurrentCoinLabel.setIcon(cIcon);

        midInfoRow.add(exitCurrentScoreLabel);
        midInfoRow.add(exitCurrentCoinLabel);

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        row1.setOpaque(false);
        JButton resumeBtn = new RoundedButton("계속하기", 14);
        resumeBtn.setFont(new Font("맑은 고딕", Font.BOLD, 15));
        resumeBtn.addActionListener(e -> {
            exitConfirmZoomWrapper.setVisible(false);
            requestFocusInWindow();
        });

        JButton switchModeBtn = new RoundedButton("모드 변경", 14);
        switchModeBtn.setFont(new Font("맑은 고딕", Font.BOLD, 15));
        switchModeBtn.addActionListener(e -> {
            accumulatedPendingScore += game.getScore();
            game.resetGoldCombo();
            game.toReadyStateKeepProgress();
            exitConfirmZoomWrapper.setVisible(false);
            startZoomWrapper.setVisible(true);
            if (setDescFloatingBox != null) {
                setDescFloatingBox.setVisible(game.getSetEffect() != BasketballGame.SetEffect.CLASSIC);
            }
            positionStartOverlays();
            syncExpertSwitchUI();
            updateTopStatus();
            canvas.repaint();
        });
        row1.add(resumeBtn);
        row1.add(switchModeBtn);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        row2.setOpaque(false);
        JButton cashOutExitBtn = new RoundedButton("메인으로 나가기", 14);
        cashOutExitBtn.setFont(new Font("맑은 고딕", Font.BOLD, 17));
        cashOutExitBtn.setPreferredSize(new Dimension(220, 44));
        cashOutExitBtn.addActionListener(e -> {
            exitConfirmZoomWrapper.setVisible(false);
            int totalScoreForReward = accumulatedPendingScore + game.getScore();
            accumulatedPendingScore = 0;
            if (onGameOver != null && totalScoreForReward > 0) {
                onGameOver.accept(totalScoreForReward, game.getMissedEnglishWords());
            }
            stop();
            if (onBackToMain != null) {
                onBackToMain.run();
            }
        });
        row2.add(cashOutExitBtn);

        card.add(warnTitle);
        card.add(Box.createVerticalStrut(14));
        card.add(midInfoRow);
        card.add(Box.createVerticalStrut(18));
        JLabel coinKeepNotice = new JLabel("메인으로 나가도 얻은 코인은 정상적으로 획득됩니다.");
        coinKeepNotice.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        coinKeepNotice.setForeground(new Color(90, 90, 90));
        coinKeepNotice.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(row1);
        card.add(Box.createVerticalStrut(10));
        card.add(row2);
        card.add(Box.createVerticalStrut(8));
        card.add(coinKeepNotice);

        return card;
    }

    private void startGamePlay() {
        startZoomWrapper.setVisible(false);
        if (setDescFloatingBox != null) setDescFloatingBox.setVisible(false);
        gameOverZoomWrapper.setVisible(false);
        customEarnedCoins = null;
        game.startNewGame();
        syncExpertSwitchUI();
        updateTopStatus();
        refreshWrongList();
        canvas.repaint();
        requestFocusInWindow();
    }

    private void showGameOverOverlay() {
        int totalScoreForReward = accumulatedPendingScore + game.getScore();
        accumulatedPendingScore = 0;
        if (onGameOver != null) {
            onGameOver.accept(totalScoreForReward, game.getMissedEnglishWords());
        }
        finalScoreLabel.setText("점수 " + String.format("%,d", game.getScore()));
        int coins = (customEarnedCoins != null) ? customEarnedCoins : (totalScoreForReward / 10);
        earnedCoinLabel.setText(String.format("획득 코인: %,d", coins));
        gameOverZoomWrapper.setVisible(true);
        positionStartOverlays();
    }

    private void syncExpertSwitchUI() {
        if (expertSwitchBtn == null) {
            return;
        }
        boolean showSwitch = game.isExpertMode() && (game.getState() != BasketballGame.State.READY);
        expertSwitchBtn.setVisible(showSwitch);

        if (game.isExpertHardTrajectory()) {
            expertSwitchBtn.setText("포물선: 하드");
            expertSwitchBtn.setButtonColors(RED_BTN, RED_BTN_SHADOW);
            expertSwitchBtn.setForeground(Color.WHITE);
        } else {
            expertSwitchBtn.setText("포물선: 이지");
            expertSwitchBtn.setButtonColors(BLUE_BTN, BLUE_BTN_SHADOW);
            expertSwitchBtn.setForeground(Color.WHITE);
        }
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

        expertSwitchBtn = new PillButton("포물선: 하드", RED_BTN, RED_BTN_SHADOW);
        expertSwitchBtn.setForeground(Color.WHITE);
        expertSwitchBtn.setVisible(false);
        expertSwitchBtn.addActionListener(e -> {
            game.setExpertHardTrajectory(!game.isExpertHardTrajectory());
            syncExpertSwitchUI();
            canvas.repaint();
            requestFocusInWindow();
        });
        expertSwitchBtn.setSelected(true);

        JPanel lifeContent = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        lifeContent.setOpaque(false);

        lifeTextLabel = new JLabel("남은 기회");
        lifeTextLabel.setFont(new Font("맑은 고딕", Font.BOLD, 15));
        lifeTextLabel.setForeground(Color.BLACK);
        ImageIcon healthIcon = vocaland.ui.util.ImageAssets.icon(ImagePaths.ROOT + ImagePaths.TEXT_HEALTH, 84, 24);
        if (healthIcon != null) {
            lifeTextLabel.setIcon(healthIcon);
            lifeTextLabel.setText("");
        }
        lifeTextLabel.getAccessibleContext().setAccessibleName("남은 기회");

        livesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        livesPanel.setOpaque(false);

        lifeContent.add(lifeTextLabel);
        lifeContent.add(livesPanel);
        JPanel lifeBox = statusBox(lifeContent, new EmptyBorder(7, 12, 7, 12));

        JPanel scoreContent = new JPanel();
        scoreContent.setLayout(new BoxLayout(scoreContent, BoxLayout.Y_AXIS));
        scoreContent.setOpaque(false);

        scoreLabel = new JLabel("점수 0", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                if (game.isGoldComboActive()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setFont(getFont());
                    FontMetrics fm = g2.getFontMetrics();
                    String txt = getText();
                    int iconW = (getIcon() != null) ? getIcon().getIconWidth() + getIconTextGap() : 0;
                    if (getIcon() != null) {
                        getIcon().paintIcon(this, g2, (getWidth() - (iconW + fm.stringWidth(txt))) / 2,
                                (getHeight() - getIcon().getIconHeight()) / 2);
                    }
                    int tx = (getWidth() - (iconW + fm.stringWidth(txt))) / 2 + iconW;
                    int ty = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                    g2.setPaint(new GradientPaint(tx, ty - fm.getAscent(), new Color(184, 134, 11),
                            tx, ty, new Color(120, 80, 0)));
                    g2.drawString(txt, tx, ty);
                    g2.dispose();
                } else {
                    setForeground(Color.BLACK);
                    super.paintComponent(g);
                }
            }
        };
        scoreLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 17));
        scoreLabel.setForeground(Color.BLACK);
        scoreLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        ImageIcon scaledScore = vocaland.ui.util.ImageAssets.icon(ImagePaths.ROOT + ImagePaths.TEXT_SCORE, 46, 25);
        if (scaledScore != null) {
            int width = scaledScore.getIconWidth();
            int height = scaledScore.getIconHeight();
            BufferedImage shiftedScore = new BufferedImage(width, height + 4, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = shiftedScore.createGraphics();
            scaledScore.paintIcon(null, g, 0, 3);
            g.dispose();
            scoreLabel.setIcon(new ImageIcon(shiftedScore));
        }
        scoreLabel.setIconTextGap(8);

        scoreContent.add(scoreLabel);

        JPanel scoreBox = statusBox(scoreContent, new EmptyBorder(6, 14, 6, 14));
        scoreBox.setPreferredSize(new Dimension(165, 42));
        scoreBox.setMinimumSize(new Dimension(165, 42));
        scoreBox.setMaximumSize(new Dimension(165, 42));

        backBtn = new RoundedButton("← 메인으로", 18);
        backBtn.setPreferredSize(new Dimension(132, 42));
        backBtn.setBorder(new EmptyBorder(8, 12, 12, 18));
        ImageIcon backIcon = vocaland.ui.util.ImageAssets.icon(ImagePaths.ROOT + ImagePaths.TEXT_GO_MAIN, 102, 24);
        if (backIcon != null) {
            backBtn.setIcon(backIcon);
            backBtn.setText("");
            backBtn.setBorder(new EmptyBorder(13, 12, 12, 18));
        }
        backBtn.getAccessibleContext().setAccessibleName("메인으로");
        backBtn.addActionListener(e -> {
            if (game.getState() == BasketballGame.State.PLAYING
                    && (game.getScore() > 0 || !game.getWrongRecords().isEmpty())) {
                int totalScoreSoFar = accumulatedPendingScore + game.getScore();
                int estCoins = totalScoreSoFar / 10;
                exitCurrentScoreLabel.setText(String.format("현재 점수: %,d점", game.getScore()));
                exitCurrentCoinLabel.setText(String.format("획득한 코인: %,d", estCoins));
                exitConfirmZoomWrapper.setVisible(true);
                positionStartOverlays();
                return;
            }
            if (accumulatedPendingScore > 0 && onGameOver != null) {
                onGameOver.accept(accumulatedPendingScore, game.getMissedEnglishWords());
                accumulatedPendingScore = 0;
            }
            stop();
            if (onBackToMain != null) {
                onBackToMain.run();
            }
        });

        rightInfo.add(expertSwitchBtn);
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
        scoreLabel.repaint();

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
        uiScale = Math.max(0.65, uiScale);

        wrongReviewBox.setPreferredSize(new Dimension((int) (250 * uiScale), 0));
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, (int) (26 * uiScale)));
        expertSwitchBtn.setFont(new Font("맑은 고딕", Font.BOLD, (int) (14 * uiScale)));
        lifeTextLabel.setFont(new Font("맑은 고딕", Font.BOLD, (int) (15 * uiScale)));
        scoreLabel.setFont(new Font("맑은 고딕", Font.BOLD, (int) (16 * uiScale)));
        wrongHeaderLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 17));
        for (JLabel label : helpLabels) {
            label.setFont(new Font("맑은 고딕", Font.BOLD, (int) (15 * uiScale)));
        }
        backBtn.setFont(new Font("맑은 고딕", Font.BOLD, (int) (15 * uiScale)));
        positionStartOverlays();
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
        canvas.remove(startZoomWrapper);
        startOverlay = createStartOverlay();
        setDescFloatingBox = createSetDescFloatingBox();
        startZoomWrapper = new ZoomOverlayWrapper(buildStartStagePanel());
        canvas.add(startZoomWrapper);
        startZoomWrapper.setVisible(true);
        gameOverZoomWrapper.setVisible(false);
        exitConfirmZoomWrapper.setVisible(false);
        accumulatedPendingScore = 0;
        syncExpertSwitchUI();
        positionStartOverlays();
        syncResponsiveLayout();
        updateTopStatus();
        refreshWrongList();
        if (!gameTimer.isRunning()) {
            gameTimer.start();
        }
        requestFocusInWindow();
    }

    public void stop() {
        pressedKeys.clear();
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