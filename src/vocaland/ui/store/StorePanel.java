package vocaland.ui.store;

import vocaland.ui.util.ImagePaths;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import vocaland.ui.components.RoundedBorder;
import vocaland.ui.components.RoundedPanel;
import vocaland.ui.components.RoundedButton;
import vocaland.ui.theme.Theme;

public class StorePanel extends JPanel {
    private final Runnable onBackToMain;
    private final vocaland.service.LocalData data;
    private static final Color BACKGROUND = new Color(250, 251, 248);
    private static final Color PANEL_BG = new Color(255, 255, 255, 235);
    private static final Color CARD_BG = new Color(248, 251, 243);
    private static final Color BORDER_COLOR = new Color(181, 222, 103);
    private static final Color SELECTED_BORDER = new Color(92, 145, 34);

    // Display data for each store card.
    private static class StoreItem {
        String id;
        String name;
        String category; // "캐릭터", "농구", "배경"
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

    private final List<StoreItem> allItems = new ArrayList<>();
    private final java.util.Map<String, JPanel> itemCards = new java.util.HashMap<>();
    private final StoreImageCache imageCache = new StoreImageCache();
    private boolean imagesReady;

    private int userCoins;
    private String currentCategory = "캐릭터";
    private StoreItem selectedItem;

    private JButton coinButton;
    private JPanel gridPanel;
    private JLabel previewImageLabel;
    private JLabel previewNameLabel;
    private JButton actionButton;
    private final List<JButton> categoryButtons = new ArrayList<>();

    public StorePanel(vocaland.service.LocalData data, Runnable onBackToMain) {
        this.data = data;
        userCoins = data.current().coins();
        this.onBackToMain = onBackToMain;
        setLayout(new BorderLayout(0, 16));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(18, 32, 24, 32));

        loadItems();

        add(createHeader(), BorderLayout.NORTH);
        add(createMainContent(), BorderLayout.CENTER);

        selectCategory("캐릭터");
        prepareImages();
    }

    private void loadItems() {
        allItems.clear();
        var inventory = data.inventoryStates();
        for (vocaland.service.LocalData.Item item : data.items()) {
            String category = switch (item.type()) { case "CHARACTER" -> "캐릭터"; case "BALL", "HOOP" -> "농구"; default -> "배경"; };
            allItems.add(new StoreItem(item.id(), item.name(), category, item.price(),
                    item.image().replaceFirst("^images/", ""), inventory.containsKey(item.id()),
                    inventory.getOrDefault(item.id(), false)));
        }
    }

    public void refreshData() {
        userCoins = data.current().coins();
        coinButton.setText(String.format("%,d", userCoins));
        loadItems();
        selectCategory(currentCategory);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        vocaland.ui.components.BackgroundPanel.paintBackground(this, graphics);
    }

    private JPanel createHeader() {
        JPanel header = transparentPanel(new BorderLayout());

        // 다른 학습 화면과 같은 메인 복귀 버튼
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

        // 좌측 상단 상점 로고
        JLabel titleLabel = new JLabel("상점", SwingConstants.LEFT);
        titleLabel.setVerticalAlignment(SwingConstants.TOP);
        titleLabel.getAccessibleContext().setAccessibleName("상점");
        titleLabel.setFont(font(34));
        titleLabel.setForeground(Theme.TEXT);
        ImageIcon storeLogo = loadImage(ImagePaths.LOGO_STORE, 187, 54, true);
        if (storeLogo != null) {
            titleLabel.setIcon(storeLogo);
            titleLabel.setText(null);
        }
        header.add(titleLabel, BorderLayout.WEST);

        // 우측 보유 코인 표시
        coinButton = new RoundedButton(String.format("%,d", userCoins), 14);
        coinButton.setFont(font(16));
        coinButton.setPreferredSize(new Dimension(130, 44));
        ImageIcon coinIcon = loadImage(ImagePaths.STORE_COIN, 20, 20, true);
        if (coinIcon != null) {
            coinButton.setIcon(coinIcon);
            coinButton.setIconTextGap(8);
        }

        JPanel rightWrap = transparentPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        rightWrap.add(coinButton);
        rightWrap.add(backBtn);
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
        JPanel leftCard = new RoundedPanel(new BorderLayout(0, 12));
        leftCard.setPreferredSize(new Dimension(0, 0));
        leftCard.setMinimumSize(new Dimension(0, 0));
        leftCard.setBackground(PANEL_BG);
        leftCard.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(BORDER_COLOR, 3),
                new EmptyBorder(16, 18, 16, 18)
        ));

        JPanel tabBar = transparentPanel(new GridLayout(1, 3, 12, 0));
        String[] categories = {"캐릭터", "농구", "배경"};
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
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        JPanel gridFrame = new RoundedPanel(new BorderLayout());
        gridFrame.setBackground(Color.WHITE);
        gridFrame.setBorder(new RoundedBorder(new Color(220, 235, 195), 2));
        gridFrame.add(scrollPane, BorderLayout.CENTER);
        leftCard.add(gridFrame, BorderLayout.CENTER);

        gbc.gridx = 0;
        gbc.weightx = 0.7;
        gbc.insets = new Insets(0, 0, 0, 16);
        splitContainer.add(leftCard, gbc);

        // [우측 30%] 선택 아이템 미리보기 + 장착/구매 패널
        JPanel rightCard = new RoundedPanel(new BorderLayout(0, 12));
        rightCard.setPreferredSize(new Dimension(0, 0));
        rightCard.setMinimumSize(new Dimension(0, 0));
        rightCard.setBackground(PANEL_BG);
        rightCard.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(BORDER_COLOR, 3),
                new EmptyBorder(18, 18, 18, 18)
        ));

        previewNameLabel = new JLabel("아이템 선택", SwingConstants.CENTER);
        previewNameLabel.setFont(font(20));
        previewNameLabel.setForeground(Theme.TEXT);
        rightCard.add(previewNameLabel, BorderLayout.NORTH);

        JPanel stagePanel = new RoundedPanel(new BorderLayout());
        stagePanel.setBackground(CARD_BG);
        stagePanel.setBorder(new RoundedBorder(new Color(220, 235, 195), 2));

        previewImageLabel = new JLabel("", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics graphics) {
                if ("배경".equals(currentCategory) && getIcon() instanceof ImageIcon icon) {
                    Graphics2D g = (Graphics2D) graphics.create();
                    try {
                        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        double scale = Math.max((double) getWidth() / icon.getIconWidth(),
                                (double) getHeight() / icon.getIconHeight());
                        int width = (int) Math.ceil(icon.getIconWidth() * scale);
                        int height = (int) Math.ceil(icon.getIconHeight() * scale);
                        g.drawImage(icon.getImage(), (getWidth() - width) / 2,
                                (getHeight() - height) / 2, width, height, this);
                    } finally {
                        g.dispose();
                    }
                    return;
                }
                super.paintComponent(graphics);
            }
        };
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
                btn.setFont(font(17));
            } else {
                btn.setForeground(new Color(90, 110, 70));
                btn.setFont(font(17));
            }
        }

        List<StoreItem> filtered = new ArrayList<>();
        for (StoreItem item : allItems) {
            if (item.category.equals(category)) {
                filtered.add(item);
            }
        }

        String selectedId = selectedItem == null ? null : selectedItem.id;
        selectedItem = filtered.stream().filter(item -> item.id.equals(selectedId)).findFirst()
                .orElse(filtered.isEmpty() ? null : filtered.get(0));

        refreshGrid(filtered);
        updatePreviewPanel();
    }

    private void refreshGrid(List<StoreItem> items) {
        gridPanel.removeAll();
        itemCards.clear();
        boolean character = currentCategory.equals("캐릭터");
        gridPanel.setLayout(new GridLayout(0, character ? 4 : 3, 12, 12));

        for (StoreItem item : items) {
            boolean background = currentCategory.equals("배경");
            ImageIcon backgroundIcon = background
                    ? loadImage(item.imageName, 0, 0, false) : null;
            JPanel card = new RoundedPanel(new BorderLayout(0, 6)) {
                @Override
                protected void paintComponent(Graphics graphics) {
                    super.paintComponent(graphics);
                    if (backgroundIcon == null || backgroundIcon.getIconWidth() <= 0
                            || backgroundIcon.getIconHeight() <= 0) return;
                    Graphics2D g = (Graphics2D) graphics.create();
                    try {
                        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        double scale = Math.max((double) getWidth() / backgroundIcon.getIconWidth(),
                                (double) getHeight() / backgroundIcon.getIconHeight());
                        int width = (int) Math.ceil(backgroundIcon.getIconWidth() * scale);
                        int height = (int) Math.ceil(backgroundIcon.getIconHeight() * scale);
                        g.drawImage(backgroundIcon.getImage(), (getWidth() - width) / 2,
                                (getHeight() - height) / 2, width, height, this);
                    } finally {
                        g.dispose();
                    }
                }
            };
            card.setPreferredSize(new Dimension(120, 155));
            card.setBackground(item == selectedItem ? Theme.PRIMARY_TINT : CARD_BG);
            card.setBorder(BorderFactory.createCompoundBorder(
                    new RoundedBorder(item == selectedItem ? SELECTED_BORDER : BORDER_COLOR, 3),
                    new EmptyBorder(8, 8, 8, 8)
            ));
            card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JLabel imgLabel = new JLabel("", SwingConstants.CENTER);
            ImageIcon icon = background ? null : loadImage(item.imageName,
                    character ? 85 * 1.10 * 1.10 : 140,
                    character ? 85 * 1.10 * 1.10 : 110, !currentCategory.equals("배경"));
            if (background && backgroundIcon != null) {
                // The background fills the card behind its status label.
            } else if (icon != null) {
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
                ImageIcon miniCoin = loadImage(ImagePaths.STORE_COIN, 14, 14, true);
                if (miniCoin != null) {
                    priceLabel.setIcon(miniCoin);
                }
            }
            card.add(priceLabel, BorderLayout.SOUTH);

            card.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    StoreItem previous = selectedItem;
                    selectedItem = item;
                    if (previous != null) updateCardSelection(previous);
                    updateCardSelection(item);
                    updatePreviewPanel();
                }
            });

            itemCards.put(item.id, card);
            gridPanel.add(card);
        }

        gridPanel.revalidate();
        gridPanel.repaint();
    }

    private void updatePreviewPanel() {
        if (selectedItem == null) return;

        previewNameLabel.setText(selectedItem.name);
        ImageIcon bigIcon;
        if (currentCategory.equals("배경")) {
            bigIcon = loadImage(selectedItem.imageName, 0, 0, false);
        } else {
            bigIcon = loadImage(selectedItem.imageName, 175, 175, true);
        }
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
            actionButton.setText("구매하기");
            actionButton.setEnabled(true);
        }
    }

    private void handleActionClick() {
        if (selectedItem == null || selectedItem.equipped) return;
        if (!selectedItem.owned && JOptionPane.showConfirmDialog(this,
                selectedItem.name + "을(를) " + selectedItem.price + " 코인에 구매하시겠습니까?",
                "아이템 구매", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            data.buyOrEquip(selectedItem.id);
            vocaland.ui.components.BackgroundPanel.setBackgroundImage(data.equippedImage("BACKGROUND"));
            refreshData();
            repaint();
        } catch (RuntimeException error) {
            JOptionPane.showMessageDialog(this, error.getMessage(), "상점", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void goBackToMain() {
        onBackToMain.run();
    }

    private void updateCardSelection(StoreItem item) {
        JPanel card = itemCards.get(item.id);
        if (card == null) return;
        card.setBackground(item == selectedItem ? Theme.PRIMARY_TINT : CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(item == selectedItem ? SELECTED_BORDER : BORDER_COLOR, 3),
                new EmptyBorder(8, 8, 8, 8)));
        card.repaint();
    }

    private ImageIcon loadImage(String name, double width, double height, boolean trim) {
        return imagesReady ? imageCache.get(name, width, height, trim) : null;
    }

    private void prepareImages() {
        List<StoreItem> snapshot = List.copyOf(allItems);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                imageCache.prepare(ImagePaths.TEXT_GO_MAIN, 102, 24, false);
                imageCache.prepare(ImagePaths.LOGO_STORE, 187, 54, true);
                imageCache.prepare(ImagePaths.STORE_COIN, 20, 20, true);
                imageCache.prepare(ImagePaths.STORE_COIN, 14, 14, true);
                for (StoreItem item : snapshot) {
                    if (item.category.equals("배경")) {
                        imageCache.prepare(item.imageName, 0, 0, false);
                    } else {
                        boolean character = item.category.equals("캐릭터");
                        imageCache.prepare(item.imageName, character ? 85 * 1.10 * 1.10 : 140,
                                character ? 85 * 1.10 * 1.10 : 110, true);
                        imageCache.prepare(item.imageName, 175, 175, true);
                    }
                }
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    imagesReady = true;
                    remove(((BorderLayout) getLayout()).getLayoutComponent(BorderLayout.NORTH));
                    add(createHeader(), BorderLayout.NORTH);
                    selectCategory(currentCategory);
                    revalidate();
                    repaint();
                } catch (Exception error) {
                    previewImageLabel.setText("이미지를 불러오지 못했어요.");
                } finally {
                    imageCache.releaseOriginals();
                }
            }
        }.execute();
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
