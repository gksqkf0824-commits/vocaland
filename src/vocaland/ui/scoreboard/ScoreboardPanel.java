package vocaland.ui.scoreboard;

import vocaland.ui.util.ImagePaths;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import vocaland.ui.components.RoundedBorder;
import vocaland.ui.components.RoundedPanel;
import vocaland.ui.components.RoundedButton;
import vocaland.ui.theme.Theme;

public class ScoreboardPanel extends JPanel {
    private final Runnable onBackToMain;
    private final vocaland.service.LocalData data;
    private static final Color BACKGROUND = new Color(250, 251, 248);
    private static final Color PANEL_BG = new Color(255, 255, 255, 235);
    private static final Color ROW_EVEN = new Color(248, 251, 243);
    private static final Color ROW_ODD = Color.WHITE;
    private static final Color MY_RANK_BG = new Color(224, 242, 188);
    private static final Color BORDER_COLOR = new Color(181, 222, 103);

    // 저장된 사용자별 최고 점수를 화면에 표시한다.
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


    private String currentGame = "산성비게임";
    private JButton rainTabBtn;
    private JButton basketballTabBtn;
    private JPanel listContainer;
    private JLabel myRankLabel;
    private JLabel myNameLabel;
    private JLabel myScoreLabel;

    public ScoreboardPanel(vocaland.service.LocalData data, Runnable onBackToMain) {
        this.data = data;
        this.onBackToMain = onBackToMain;
        setLayout(new BorderLayout(0, 16));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(18, 32, 24, 32));


        add(createHeader(), BorderLayout.NORTH);
        add(createMainContent(), BorderLayout.CENTER);

        selectGame("산성비게임");
    }

    public void refreshData() { selectGame(currentGame); }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        vocaland.ui.components.BackgroundPanel.paintBackground(this, graphics);
    }

    private JPanel createHeader() {
        JPanel header = transparentPanel(new BorderLayout());

        // 상점과 같은 메인 복귀 버튼
        JButton backBtn = new RoundedButton("← 메인으로", 18);
        backBtn.setFont(font(16));
        backBtn.setPreferredSize(new Dimension(132, 42));
        backBtn.setBorder(new EmptyBorder(8, 12, 12, 18));
        ImageIcon backIcon = loadImage(ImagePaths.TEXT_GO_MAIN, 102, 24, false);
        if (backIcon != null) {
            backBtn.setIcon(backIcon);
            backBtn.setText("");
            backBtn.setBorder(new EmptyBorder(13, 12, 12, 18));
        }
        backBtn.getAccessibleContext().setAccessibleName("메인으로");
        backBtn.addActionListener(e -> goBackToMain());

        // 좌측 상단 랭킹 로고
        JLabel titleLabel = new JLabel("랭킹", SwingConstants.LEFT);
        titleLabel.setVerticalAlignment(SwingConstants.TOP);
        titleLabel.getAccessibleContext().setAccessibleName("랭킹");
        titleLabel.setFont(font(34));
        titleLabel.setForeground(Theme.TEXT);
        ImageIcon rankLogo = loadImage(ImagePaths.LOGO_RANK, 187, 54, true);
        if (rankLogo != null) {
            titleLabel.setIcon(rankLogo);
            titleLabel.setText(null);
        }
        header.add(titleLabel, BorderLayout.WEST);

        JPanel rightWrap = transparentPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        rightWrap.add(backBtn);
        header.add(rightWrap, BorderLayout.EAST);

        return header;
    }

    private JPanel createMainContent() {
        JPanel splitContainer = transparentPanel(new BorderLayout(0, 16));
        JPanel gameBtnGroup = transparentPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        rainTabBtn = createGameSelectButton("산성비게임", ImagePaths.TEXT_RAIN_TEXT);
        rainTabBtn.addActionListener(e -> selectGame("산성비게임"));

        basketballTabBtn = createGameSelectButton("농구게임", ImagePaths.TEXT_BASKETBALL_TEXT);
        basketballTabBtn.addActionListener(e -> selectGame("농구게임"));

        gameBtnGroup.add(rainTabBtn);
        gameBtnGroup.add(basketballTabBtn);

        splitContainer.add(gameBtnGroup, BorderLayout.NORTH);

        // [우측 72%] 랭킹 순위표 패널
        JPanel rightCard = new RoundedPanel(new BorderLayout(0, 10));
        rightCard.setBackground(PANEL_BG);
        rightCard.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(BORDER_COLOR, 3),
                new EmptyBorder(16, 20, 16, 20)
        ));

        // 테이블 영역 (컬럼 헤더 + 스크롤 리스트 + 하단 내 순위)
        JPanel tableBox = new RoundedPanel(new BorderLayout());
        tableBox.setBackground(Color.WHITE);
        tableBox.setBorder(new RoundedBorder(new Color(210, 228, 180), 2));

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

        splitContainer.add(rightCard, BorderLayout.CENTER);

        return splitContainer;
    }

    private JButton createGameSelectButton(String text, String iconName) {
        RoundedButton btn = new RoundedButton("", 14);
        btn.setSelectionIndicatorVisible(true);
        btn.getAccessibleContext().setAccessibleName(text);
        btn.setToolTipText(text);
        btn.setFont(font(17));
        btn.setPreferredSize(new Dimension(190, 60));
        ImageIcon icon = loadImage(iconName, 130, 26, true);
        if (icon != null) {
            btn.setIcon(icon);
            btn.setIconTextGap(0);
            btn.addComponentListener(new java.awt.event.ComponentAdapter() {
                @Override
                public void componentResized(java.awt.event.ComponentEvent event) {
                    Insets insets = btn.getInsets();
                    int availableWidth = Math.max(1, btn.getWidth() - insets.left - insets.right - 12);
                    int availableHeight = Math.max(1, btn.getHeight() - insets.top - insets.bottom - 12);
                    double scale = Math.min(1.0, Math.min(
                            (double) availableWidth / icon.getIconWidth(),
                            (double) availableHeight / icon.getIconHeight()));
                    btn.setIcon(new ImageIcon(icon.getImage().getScaledInstance(
                            Math.max(1, (int) Math.round(icon.getIconWidth() * scale)),
                            Math.max(1, (int) Math.round(icon.getIconHeight() * scale)), Image.SCALE_SMOOTH)));
                }
            });
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

        myRankLabel = new JLabel("내 순위 : 기록 없음", SwingConstants.CENTER);
        myNameLabel = new JLabel("", SwingConstants.CENTER);
        myScoreLabel = new JLabel("—", SwingConstants.CENTER);

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

        boolean isRain = "산성비게임".equals(gameName);
        rainTabBtn.setSelected(isRain);
        basketballTabBtn.setSelected(!isRain);


        List<vocaland.service.LocalData.Rank> ranks = data.ranks(isRain ? "RAIN" : "BASKETBALL");
        List<RankEntry> entries = ranks.stream().map(r -> new RankEntry(r.position(), r.name(), r.score(),
                data.equippedImage(r.userId(), "CHARACTER"))).toList();
        refreshRankList(entries);
        myNameLabel.setText(data.current().name());
        var mine = ranks.stream().filter(r -> r.userId().equals(data.current().id())).findFirst();
        myRankLabel.setText(mine.map(r -> "내 순위 : " + r.position() + "위").orElse("내 순위 : 기록 없음"));
        myScoreLabel.setText(mine.map(r -> String.format("%,d점", r.score())).orElse("—"));
    }

    private void refreshRankList(List<RankEntry> entries) {
        listContainer.removeAll();
        if (entries.isEmpty()) {
            JLabel empty = new JLabel("아직 게임 기록이 없어요. 게임을 완료해 보세요.");
            empty.setFont(font(16));
            empty.setBorder(new EmptyBorder(20, 16, 20, 16));
            listContainer.add(empty);
        }

        for (int i = 0; i < entries.size(); i++) {
            RankEntry entry = entries.get(i);
            JPanel row = new JPanel(new GridLayout(1, 3));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
            row.setPreferredSize(new Dimension(0, 64));
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
            rankLabel.setFont(font(entry.rank <= 3 ? 19 : 17));
            rankLabel.setForeground(Theme.TEXT);

            // 2열: 캐릭터 미니 아이콘 + 닉네임
            JLabel nameLabel = new JLabel(entry.nickname, SwingConstants.CENTER);
            nameLabel.setFont(font(17));
            nameLabel.setForeground(Theme.TEXT);
            if (entry.characterIcon != null) {
                ImageIcon miniChar = loadImage(entry.characterIcon.replaceFirst("^images/", ""), 34, 34, true);
                if (miniChar != null) {
                    nameLabel.setIcon(miniChar);
                    nameLabel.setIconTextGap(8);
                }
            }

            // 3열: 최고 점수
            JLabel scoreLabel = new JLabel(String.format("%,d점", entry.score), SwingConstants.CENTER);
            scoreLabel.setFont(font(17));
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
        onBackToMain.run();
    }

    private ImageIcon loadImage(String name, int width, int height, boolean trim) {
        return vocaland.ui.util.ImageAssets.load(ImagePaths.ROOT + name, width, height, trim ? 64 : -1, true);
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
