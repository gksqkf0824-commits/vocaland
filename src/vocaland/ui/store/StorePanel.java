package vocaland.ui.store;

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

public class StorePanel extends JPanel {
    private static final Color BACKGROUND = new Color(250, 251, 248);
    private static final Color PANEL_BG = new Color(255, 255, 255, 235);
    private static final Color CARD_BG = new Color(248, 251, 243);
    private static final Color BORDER_COLOR = new Color(181, 222, 103);
    private static final Color SELECTED_BORDER = new Color(92, 145, 34);
    private static final int BACKGROUND_TILE_SIZE = 620;

    // 임시 아이템 데이터 구조 (추후 model.Item / repository 연동 시 교체)
    private static class StoreItem {
        String id;
        String name;
        String category; // "캐릭터", "농구공", "배경"
        int price;
        String imageName;
        boolean owned;
        boolean equipped;

        StoreItem(String id, String name, String category, int price, String imageName, boolean owned, boolean equipped) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.price = price;
            this.imageName = imageName;
            this.owned = owned;
            this.equipped = equipped;
        }
    }

    private final ImageIcon backgroundTile = loadImage("background.png", BACKGROUND_TILE_SIZE, BACKGROUND_TILE_SIZE, false);
    private final List<StoreItem> allItems = new ArrayList<>();
    private int userCoins = 1500; // 테스트용 초기 코인 (추후 User 데이터 연동)
    private String currentCategory = "캐릭터";
    private StoreItem selectedItem;

    private JButton coinButton;
    private JPanel gridPanel;
    private JLabel previewImageLabel;
    private JLabel previewNameLabel;
    private JButton actionButton;
    private final List<JButton> categoryButtons = new ArrayList<>();

    public StorePanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(18, 32, 24, 32));

        initMockItems();

        add(createHeader(), BorderLayout.NORTH);
        add(createMainContent(), BorderLayout.CENTER);

        selectCategory("캐릭터");
    }

    private void initMockItems() {
        // 캐릭터
        allItems.add(new StoreItem("C01", "기본 아기거북", "캐릭터", 0, "turtle_green.png", true, true));
        allItems.add(new StoreItem("C02", "황금 아기거북", "캐릭터", 500, "turtle_gold.png", false, false));
        allItems.add(new StoreItem("C03", "기룡이", "캐릭터", 1000, "giryong_basic.png", false, false));
        allItems.add(new StoreItem("C04", "산타 기룡이", "캐릭터", 1200, "giryong_santa.png", false, false));

        // 농구공
        allItems.add(new StoreItem("B01", "기본 농구공", "농구공", 0, "basketball_basic.png", true, true));
        allItems.add(new StoreItem("B02", "빈티지 농구공", "농구공", 400, "basketball_worn.png", false, false));
        allItems.add(new StoreItem("B03", "황금 농구공", "농구공", 1000, "basketball_gold.png", false, false));

        // 배경
        allItems.add(new StoreItem("BG01", "기본 잔디 배경", "배경", 0, "background.png", true, true));
        allItems.add(new StoreItem("BG02", "밤하늘 테마", "배경", 800, "bg_night.png", false, false));
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

        // 중앙 상점 로고
        JLabel titleLabel = new JLabel("상점", SwingConstants.CENTER);
        titleLabel.setFont(font(34));
        titleLabel.setForeground(Theme.TEXT);
        ImageIcon storeLogo = loadImage("store.png", 220, 64, true);
        if (storeLogo != null) {
            titleLabel.setIcon(storeLogo);
            titleLabel.setText(null);
        }
        header.add(titleLabel, BorderLayout.CENTER);

        // 우측 보유 코인 표시
        coinButton = new RoundedButton(String.format("%,d", userCoins), 14);
        coinButton.setFont(font(16));
        coinButton.setPreferredSize(new Dimension(130, 44));
        ImageIcon coinIcon = loadImage("coin.png", 20, 20, true);
        if (coinIcon != null) {
            coinButton.setIcon(coinIcon);
            coinButton.setIconTextGap(8);
        }

        JPanel rightWrap = transparentPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        rightWrap.add(coinButton);
        header.add(rightWrap, BorderLayout.EAST);

        return header;
    }

    private JPanel createMainContent() {
        JPanel splitContainer = transparentPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridy = 0;
        gbc.weighty = 1.0;

        // [좌측 70%] 카테고리 탭 + 아이템 그리드
        JPanel leftCard = new JPanel(new BorderLayout(0, 12));
        leftCard.setBackground(PANEL_BG);
        leftCard.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COLOR, 3, true),
                new EmptyBorder(16, 18, 16, 18)
        ));

        JPanel tabBar = transparentPanel(new GridLayout(1, 3, 12, 0));
        String[] categories = {"캐릭터", "농구공", "배경"};
        for (String cat : categories) {
            JButton tabBtn = new RoundedButton(cat, 12);
            tabBtn.setFont(font(17));
            tabBtn.setPreferredSize(new Dimension(0, 44));
            tabBtn.addActionListener(e -> selectCategory(cat));
            categoryButtons.add(tabBtn);
            tabBar.add(tabBtn);
        }
        leftCard.add(tabBar, BorderLayout.NORTH);

        gridPanel = new JPanel(new GridLayout(0, 4, 12, 12));
        gridPanel.setBackground(Color.WHITE);
        gridPanel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JScrollPane scrollPane = new JScrollPane(gridPanel);
        scrollPane.setBorder(new LineBorder(new Color(220, 235, 195), 2, true));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        leftCard.add(scrollPane, BorderLayout.CENTER);

        gbc.gridx = 0;
        gbc.weightx = 0.7;
        gbc.insets = new Insets(0, 0, 0, 16);
        splitContainer.add(leftCard, gbc);

        // [우측 30%] 선택 아이템 미리보기 + 장착/구매 패널
        JPanel rightCard = new JPanel(new BorderLayout(0, 12));
        rightCard.setBackground(PANEL_BG);
        rightCard.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COLOR, 3, true),
                new EmptyBorder(18, 18, 18, 18)
        ));

        previewNameLabel = new JLabel("아이템 선택", SwingConstants.CENTER);
        previewNameLabel.setFont(font(20));
        previewNameLabel.setForeground(Theme.TEXT);
        rightCard.add(previewNameLabel, BorderLayout.NORTH);

        JPanel stagePanel = new JPanel(new BorderLayout());
        stagePanel.setBackground(CARD_BG);
        stagePanel.setBorder(new LineBorder(new Color(220, 235, 195), 2, true));

        previewImageLabel = new JLabel("", SwingConstants.CENTER);
        previewImageLabel.setFont(font(16));
        previewImageLabel.setForeground(Theme.TEXT);
        stagePanel.add(previewImageLabel, BorderLayout.CENTER);
        rightCard.add(stagePanel, BorderLayout.CENTER);

        actionButton = new RoundedButton("장착중", 14);
        actionButton.setFont(font(18));
        actionButton.setPreferredSize(new Dimension(0, 52));
        actionButton.addActionListener(e -> handleActionClick());
        rightCard.add(actionButton, BorderLayout.SOUTH);

        gbc.gridx = 1;
        gbc.weightx = 0.3;
        gbc.insets = new Insets(0, 0, 0, 0);
        splitContainer.add(rightCard, gbc);

        return splitContainer;
    }

    private void selectCategory(String category) {
        this.currentCategory = category;

        for (JButton btn : categoryButtons) {
            if (btn.getText().equals(category)) {
                btn.setForeground(Theme.TEXT);
                btn.setFont(font(18));
            } else {
                btn.setForeground(new Color(90, 110, 70));
                btn.setFont(font(16));
            }
        }

        List<StoreItem> filtered = new ArrayList<>();
        for (StoreItem item : allItems) {
            if (item.category.equals(category)) {
                filtered.add(item);
            }
        }

        if (!filtered.isEmpty()) {
            selectedItem = filtered.get(0);
        }

        refreshGrid(filtered);
        updatePreviewPanel();
    }

    private void refreshGrid(List<StoreItem> items) {
        gridPanel.removeAll();

        for (StoreItem item : items) {
            JPanel card = new JPanel(new BorderLayout(0, 6));
            card.setPreferredSize(new Dimension(120, 155));
            card.setBackground(item == selectedItem ? Theme.PRIMARY_TINT : CARD_BG);
            card.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(item == selectedItem ? SELECTED_BORDER : BORDER_COLOR, item == selectedItem ? 3 : 2, true),
                    new EmptyBorder(8, 8, 8, 8)
            ));
            card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JLabel imgLabel = new JLabel("", SwingConstants.CENTER);
            ImageIcon icon = loadImage(item.imageName, 85, 85, true);
            if (icon != null) {
                imgLabel.setIcon(icon);
            } else {
                imgLabel.setText(item.name);
                imgLabel.setFont(font(13));
            }
            card.add(imgLabel, BorderLayout.CENTER);

            String statusText = item.equipped ? "장착중" : (item.owned ? "보유중" : String.format("%,d", item.price));
            JLabel priceLabel = new JLabel(statusText, SwingConstants.CENTER);
            priceLabel.setFont(font(14));
            priceLabel.setForeground(Theme.TEXT);
            if (!item.owned) {
                ImageIcon miniCoin = loadImage("coin.png", 14, 14, true);
                if (miniCoin != null) {
                    priceLabel.setIcon(miniCoin);
                }
            }
            card.add(priceLabel, BorderLayout.SOUTH);

            card.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    selectedItem = item;
                    refreshGrid(items);
                    updatePreviewPanel();
                }
            });

            gridPanel.add(card);
        }

        // 8칸 최소 그리드 유지용 빈 슬롯 채우기
        int minSlots = 8;
        for (int i = items.size(); i < minSlots; i++) {
            JPanel emptySlot = new JPanel();
            emptySlot.setPreferredSize(new Dimension(120, 155));
            emptySlot.setBackground(new Color(245, 247, 242));
            emptySlot.setBorder(new LineBorder(new Color(225, 232, 215), 2, true));
            gridPanel.add(emptySlot);
        }

        gridPanel.revalidate();
        gridPanel.repaint();
    }

    private void updatePreviewPanel() {
        if (selectedItem == null) return;

        previewNameLabel.setText(selectedItem.name);
        ImageIcon bigIcon = loadImage(selectedItem.imageName, 175, 175, true);
        if (bigIcon != null) {
            previewImageLabel.setIcon(bigIcon);
            previewImageLabel.setText(null);
        } else {
            previewImageLabel.setIcon(null);
            previewImageLabel.setText(selectedItem.name);
        }

        if (selectedItem.equipped) {
            actionButton.setText("장착중");
            actionButton.setEnabled(false);
        } else if (selectedItem.owned) {
            actionButton.setText("장착하기");
            actionButton.setEnabled(true);
        } else {
            actionButton.setText("구매하기 (" + String.format("%,d", selectedItem.price) + ")");
            actionButton.setEnabled(true);
        }
    }

    private void handleActionClick() {
        if (selectedItem == null) return;

        if (!selectedItem.owned) {
            if (userCoins < selectedItem.price) {
                JOptionPane.showMessageDialog(this, "코인이 부족합니다!", "상점", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    selectedItem.name + "을(를) " + selectedItem.price + " 코인에 구매하시겠습니까?",
                    "아이템 구매",
                    JOptionPane.YES_NO_OPTION
            );
            if (confirm == JOptionPane.YES_OPTION) {
                userCoins -= selectedItem.price;
                selectedItem.owned = true;
                equipSelectedItem();
                coinButton.setText(String.format("%,d", userCoins));
            }
        } else if (!selectedItem.equipped) {
            equipSelectedItem();
        }

        selectCategory(currentCategory);
    }

    private void equipSelectedItem() {
        for (StoreItem item : allItems) {
            if (item.category.equals(selectedItem.category)) {
                item.equipped = false;
            }
        }
        selectedItem.equipped = true;
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