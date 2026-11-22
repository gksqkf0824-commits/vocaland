package vocaland.ui.scoreboard;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import vocaland.ui.MainPanel;
import vocaland.ui.RoundedButton;
import vocaland.ui.Theme;

public class ScoreboardPanel extends JPanel {
    private static final Color BACKGROUND = new Color(250, 251, 248);
    private static final Color PANEL_BG = new Color(255, 255, 255, 235);
    private static final Color ROW_EVEN = new Color(248, 251, 243);
    private static final Color ROW_ODD = Color.WHITE;
    private static final Color MY_RANK_BG = new Color(224, 242, 188);
    private static final Color BORDER_COLOR = new Color(181, 222, 103);
    private static final int BACKGROUND_TILE_SIZE = 620;

    // 임시 랭킹 데이터 구조 (추후 scores.txt / users.txt 연동 시 교체)
    private static class RankEntry {
        int rank;
        String nickname;
        int score;
        String characterIcon;

        RankEntry(int rank, String nickname, int score, String characterIcon) {
            this.rank = rank;
            this.nickname = nickname;
            this.score = score;
            this.characterIcon = characterIcon;
        }
    }

    private final ImageIcon backgroundTile = loadImage("background.png", BACKGROUND_TILE_SIZE, BACKGROUND_TILE_SIZE, false);
    private final List<RankEntry> rainRanks = new ArrayList<>();
    private final List<RankEntry> basketballRanks = new ArrayList<>();

    private String currentGame = "산성비게임";
    private JButton rainTabBtn;
    private JButton basketballTabBtn;
    private JLabel boardTitleLabel;
    private JPanel listContainer;
    private JLabel myRankLabel;
    private JLabel myNameLabel;
    private JLabel myScoreLabel;

    public ScoreboardPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(18, 32, 24, 32));

        initMockRanks();

        add(createHeader(), BorderLayout.NORTH);
        add(createMainContent(), BorderLayout.CENTER);

        selectGame("산성비게임");
    }

    private void initMockRanks() {
        // 산성비게임 샘플 데이터
        rainRanks.add(new RankEntry(1, "기룡이", 15400, "giryong_basic.png"));
        rainRanks.add(new RankEntry(2, "아기거북", 14200, "turtle_green.png"));
        rainRanks.add(new RankEntry(3, "골드터틀", 12900, "turtle_gold.png"));
        rainRanks.add(new RankEntry(4, "단어왕", 11500, null));
        rainRanks.add(new RankEntry(5, "자바마스터", 9800, null));
        rainRanks.add(new RankEntry(6, "코딩거북", 8400, null));
        rainRanks.add(new RankEntry(7, "플레이어1", 7600, null));
        rainRanks.add(new RankEntry(8, "보카러버", 6900, null));

        // 농구게임 샘플 데이터
        basketballRanks.add(new RankEntry(1, "골든슈터", 8800, "turtle_gold.png"));
        basketballRanks.add(new RankEntry(2, "산타기룡", 7950, "giryong_santa.png"));
        basketballRanks.add(new RankEntry(3, "기룡이", 7200, "giryong_basic.png"));
        basketballRanks.add(new RankEntry(4, "덩크왕", 6500, null));
        basketballRanks.add(new RankEntry(5, "단어슈터", 5400, null));
        basketballRanks.add(new RankEntry(6, "용현", 4800, "turtle_green.png"));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (backgroundTile == null) return;

        int tileW = backgroundTile.getIconWidth();
        int tileH = backgroundTile.getIconHeight();
        for (int y = 0; y < getHeight(); y += tileH) {
            for (int x = 0; x < getWidth(); x += tileW) {
                backgroundTile.paintIcon(this, graphics, x, y);
            }
        }
    }

    private JPanel createHeader() {
        JPanel header = transparentPanel(new BorderLayout());

        // 좌측 뒤로가기 버튼
        JButton backBtn = new RoundedButton("← 뒤로", 14);
        backBtn.setFont(font(16));
        backBtn.setPreferredSize(new Dimension(110, 44));
        backBtn.addActionListener(e -> goBackToMain());

        JPanel leftWrap = transparentPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
        leftWrap.add(backBtn);
        header.add(leftWrap, BorderLayout.WEST);

        // 중앙 랭킹 로고
        JLabel titleLabel = new JLabel("랭킹", SwingConstants.CENTER);
        titleLabel.setFont(font(34));
        titleLabel.setForeground(Theme.TEXT);
        ImageIcon rankLogo = loadImage("rank.png", 220, 64, true);
        if (rankLogo != null) {
            titleLabel.setIcon(rankLogo);
            titleLabel.setText(null);
        }
        header.add(titleLabel, BorderLayout.CENTER);

        // 중앙 정렬 균형을 맞추기 위한 우측 빈 공간
        JPanel rightSpacer = transparentPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        rightSpacer.setPreferredSize(new Dimension(110, 44));
        header.add(rightSpacer, BorderLayout.EAST);

        return header;
    }

    private JPanel createMainContent() {
        JPanel splitContainer = transparentPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridy = 0;
        gbc.weighty = 1.0;

        // [좌측 28%] 게임 선택 패널
        JPanel leftCard = new JPanel(new BorderLayout(0, 14));
        leftCard.setBackground(PANEL_BG);
        leftCard.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COLOR, 3, true),
                new EmptyBorder(18, 16, 18, 16)
        ));

        JLabel selectTitle = new JLabel("게임 선택", SwingConstants.CENTER);
        selectTitle.setFont(font(20));
        selectTitle.setForeground(Theme.TEXT);
        leftCard.add(selectTitle, BorderLayout.NORTH);

        JPanel gameBtnGroup = transparentPanel(new GridLayout(2, 1, 0, 12));
        rainTabBtn = createGameSelectButton("산성비게임", "rain.png");
        rainTabBtn.addActionListener(e -> selectGame("산성비게임"));

        basketballTabBtn = createGameSelectButton("농구게임", "basketball.png");
        basketballTabBtn.addActionListener(e -> selectGame("농구게임"));

        gameBtnGroup.add(rainTabBtn);
        gameBtnGroup.add(basketballTabBtn);

        JPanel btnWrapper = transparentPanel(new BorderLayout());
        btnWrapper.add(gameBtnGroup, BorderLayout.NORTH);
        leftCard.add(btnWrapper, BorderLayout.CENTER);

        gbc.gridx = 0;
        gbc.weightx = 0.28;
        gbc.insets = new Insets(0, 0, 0, 16);
        splitContainer.add(leftCard, gbc);

        // [우측 72%] 랭킹 순위표 패널
        JPanel rightCard = new JPanel(new BorderLayout(0, 10));
        rightCard.setBackground(PANEL_BG);
        rightCard.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COLOR, 3, true),
                new EmptyBorder(16, 20, 16, 20)
        ));

        boardTitleLabel = new JLabel("산성비게임 랭킹", SwingConstants.CENTER);
        boardTitleLabel.setFont(font(22));
        boardTitleLabel.setForeground(Theme.TEXT);
        rightCard.add(boardTitleLabel, BorderLayout.NORTH);

        // 테이블 영역 (컬럼 헤더 + 스크롤 리스트 + 하단 내 순위)
        JPanel tableBox = new JPanel(new BorderLayout());
        tableBox.setBackground(Color.WHITE);
        tableBox.setBorder(new LineBorder(new Color(210, 228, 180), 2, true));

        tableBox.add(createTableHeader(), BorderLayout.NORTH);

        listContainer = new JPanel();
        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
        listContainer.setBackground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(listContainer);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        tableBox.add(scrollPane, BorderLayout.CENTER);

        tableBox.add(createMyRankFooter(), BorderLayout.SOUTH);

        rightCard.add(tableBox, BorderLayout.CENTER);

        gbc.gridx = 1;
        gbc.weightx = 0.72;
        gbc.insets = new Insets(0, 0, 0, 0);
        splitContainer.add(rightCard, gbc);

        return splitContainer;
    }

    private JButton createGameSelectButton(String text, String iconName) {
        JButton btn = new RoundedButton(text, 14);
        btn.setFont(font(17));
        btn.setPreferredSize(new Dimension(0, 56));
        ImageIcon icon = loadImage(iconName, 36, 24, true);
        if (icon != null) {
            btn.setIcon(icon);
            btn.setIconTextGap(8);
        }
        return btn;
    }

    private JPanel createTableHeader() {
        JPanel header = new JPanel(new GridLayout(1, 3));
        header.setBackground(Theme.PRIMARY_TINT);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, BORDER_COLOR),
                new EmptyBorder(10, 16, 10, 16)
        ));

        JLabel col1 = new JLabel("순위", SwingConstants.CENTER);
        JLabel col2 = new JLabel("닉네임", SwingConstants.CENTER);
        JLabel col3 = new JLabel("최고 점수", SwingConstants.CENTER);

        col1.setFont(font(16));
        col2.setFont(font(16));
        col3.setFont(font(16));
        col1.setForeground(Theme.TEXT);
        col2.setForeground(Theme.TEXT);
        col3.setForeground(Theme.TEXT);

        header.add(col1);
        header.add(col2);
        header.add(col3);
        return header;
    }

    private JPanel createMyRankFooter() {
        JPanel footer = new JPanel(new GridLayout(1, 3));
        footer.setBackground(MY_RANK_BG);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(2, 0, 0, 0, BORDER_COLOR),
                new EmptyBorder(12, 16, 12, 16)
        ));

        myRankLabel = new JLabel("내 순위 : 15위", SwingConstants.CENTER);
        myNameLabel = new JLabel("용현", SwingConstants.CENTER);
        myScoreLabel = new JLabel("4,250점", SwingConstants.CENTER);

        myRankLabel.setFont(font(16));
        myNameLabel.setFont(font(16));
        myScoreLabel.setFont(font(16));
        myRankLabel.setForeground(Theme.TEXT);
        myNameLabel.setForeground(Theme.TEXT);
        myScoreLabel.setForeground(Theme.TEXT);

        footer.add(myRankLabel);
        footer.add(myNameLabel);
        footer.add(myScoreLabel);
        return footer;
    }

    private void selectGame(String gameName) {
        this.currentGame = gameName;
        boardTitleLabel.setText(gameName + " 랭킹");

        boolean isRain = "산성비게임".equals(gameName);
        rainTabBtn.setFont(font(isRain ? 18 : 16));
        rainTabBtn.setForeground(isRain ? Theme.TEXT : new Color(95, 115, 75));

        basketballTabBtn.setFont(font(!isRain ? 18 : 16));
        basketballTabBtn.setForeground(!isRain ? Theme.TEXT : new Color(95, 115, 75));

        List<RankEntry> targetList = isRain ? rainRanks : basketballRanks;
        refreshRankList(targetList);

        if (isRain) {
            myRankLabel.setText("내 순위 : 15위");
            myScoreLabel.setText("4,250점");
        } else {
            myRankLabel.setText("내 순위 : 6위");
            myScoreLabel.setText("4,800점");
        }
    }

    private void refreshRankList(List<RankEntry> entries) {
        listContainer.removeAll();

        for (int i = 0; i < entries.size(); i++) {
            RankEntry entry = entries.get(i);
            JPanel row = new JPanel(new GridLayout(1, 3));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
            row.setPreferredSize(new Dimension(0, 50));
            row.setBackground(i % 2 == 0 ? ROW_ODD : ROW_EVEN);
            row.setBorder(new EmptyBorder(6, 16, 6, 16));

            // 1열: 순위 (1~3위 강조)
            String rankText = switch (entry.rank) {
                case 1 -> "🥇 1위";
                case 2 -> "🥈 2위";
                case 3 -> "🥉 3위";
                default -> entry.rank + "위";
            };
            JLabel rankLabel = new JLabel(rankText, SwingConstants.CENTER);
            rankLabel.setFont(font(entry.rank <= 3 ? 17 : 15));
            rankLabel.setForeground(Theme.TEXT);

            // 2열: 캐릭터 미니 아이콘 + 닉네임
            JLabel nameLabel = new JLabel(entry.nickname, SwingConstants.CENTER);
            nameLabel.setFont(font(16));
            nameLabel.setForeground(Theme.TEXT);
            if (entry.characterIcon != null) {
                ImageIcon miniChar = loadImage(entry.characterIcon, 26, 26, true);
                if (miniChar != null) {
                    nameLabel.setIcon(miniChar);
                    nameLabel.setIconTextGap(8);
                }
            }

            // 3열: 최고 점수
            JLabel scoreLabel = new JLabel(String.format("%,d점", entry.score), SwingConstants.CENTER);
            scoreLabel.setFont(font(16));
            scoreLabel.setForeground(Theme.TEXT);

            row.add(rankLabel);
            row.add(nameLabel);
            row.add(scoreLabel);
            listContainer.add(row);
        }

        listContainer.revalidate();
        listContainer.repaint();
    }

    private void goBackToMain() {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof JFrame frame) {
            frame.setContentPane(new MainPanel());
            frame.revalidate();
            frame.repaint();
        }
    }

    private ImageIcon loadImage(String imageName, int maxWidth, int maxHeight, boolean trim) {
        File file = new File("images", imageName);
        if (!file.isFile()) return null;

        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) return null;

            if (trim) {
                image = trimTransparentMargins(image);
            }

            double scale = Math.min((double) maxWidth / image.getWidth(), (double) maxHeight / image.getHeight());
            int width = Math.max(1, (int) Math.round(image.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(image.getHeight() * scale));

            return new ImageIcon(image.getScaledInstance(width, height, Image.SCALE_SMOOTH));
        } catch (IOException ignored) {
            return null;
        }
    }

    private BufferedImage trimTransparentMargins(BufferedImage image) {
        int left = image.getWidth(), top = image.getHeight(), right = -1, bottom = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) > 16) {
                    left = Math.min(left, x);
                    top = Math.min(top, y);
                    right = Math.max(right, x);
                    bottom = Math.max(bottom, y);
                }
            }
        }
        if (right < left || bottom < top) return image;
        return image.getSubimage(left, top, right - left + 1, bottom - top + 1);
    }

    private JPanel transparentPanel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    private Font font(int size) {
        return new Font(Font.DIALOG, Font.BOLD, size);
    }
}