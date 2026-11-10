package vocaland.ui;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class MainPanel extends JPanel {
    private static final Color BACKGROUND = new Color(250, 251, 248);
    private static final Dimension MENU_SIZE = new Dimension(250, 120);
    private static final Dimension STORE_SIZE = new Dimension(156, 56);
    private static final int BACKGROUND_TILE_SIZE = 620;
    private static final int HEADER_ICON_GAP = 10;

    private final ImageIcon backgroundTile = loadImage(
            "background.png", BACKGROUND_TILE_SIZE, BACKGROUND_TILE_SIZE, false
    );

    public MainPanel() {
        setLayout(new BorderLayout(0, 12));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(18, 32, 22, 32));

        add(createHeader(), BorderLayout.NORTH);
        add(createContent(), BorderLayout.CENTER);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        if (backgroundTile == null) {
            return;
        }

        int tileWidth = backgroundTile.getIconWidth();
        int tileHeight = backgroundTile.getIconHeight();

        for (int y = 0; y < getHeight(); y += tileHeight) {
            for (int x = 0; x < getWidth(); x += tileWidth) {
                backgroundTile.paintIcon(this, graphics, x, y);
            }
        }
    }

    private JPanel createHeader() {
        JPanel header = transparentPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));

        JButton profile = createButton("용현", 16);
        profile.setIcon(loadImage("profile.png", 18, 18, true));
        profile.setIconTextGap(HEADER_ICON_GAP);
        profile.addActionListener(e -> showPreparing("프로필"));

        JButton coins = createButton("350", 16);
        ImageIcon coinIcon = loadImage("coin.png", 20, 20, true);
        if (coinIcon != null) {
            coins.setIcon(coinIcon);
            coins.setIconTextGap(HEADER_ICON_GAP);
        } else {
            coins.setText("코인 350");
        }

        coins.getAccessibleContext().setAccessibleName("코인 350, 상점 열기");
        coins.addActionListener(e -> openStore());

        header.add(profile);
        header.add(coins);
        return header;
    }

    private JPanel createContent() {
        JPanel content = transparentPanel(new GridBagLayout());
        JPanel column = transparentPanel(new BorderLayout(0, 64));

        JLabel title = new JLabel("보카랜드", SwingConstants.CENTER);
        title.setFont(font(42));
        title.setForeground(Theme.TEXT);

        ImageIcon logo = loadImage("logo.png", 440, 125, true);
        if (logo != null) {
            title.getAccessibleContext().setAccessibleName("보카랜드");
            title.setIcon(logo);
            title.setText(null);
        }

        JPanel menu = transparentPanel(new GridLayout(2, 2, 14, 14));

        // 로고별 최대 가로, 최대 세로, 좌우 이동, 상하 이동
        menu.add(createMenuButton("학습", "study.png", 188, 72, 0, 4));
        menu.add(createMenuButton("랭킹", "rank.png", 198, 76, 4, 0));
        menu.add(createMenuButton("산성비", "rain.png", 208, 80, 0, -5));
        menu.add(createMenuButton("농구", "basketball.png", 193, 70, 0, 3));

        JPanel menuAndStore = transparentPanel(new BorderLayout(0, 16));
        menuAndStore.add(menu, BorderLayout.CENTER);
        menuAndStore.add(createStoreRow(), BorderLayout.SOUTH);

        column.add(title, BorderLayout.NORTH);
        column.add(menuAndStore, BorderLayout.CENTER);
        content.add(column);
        return content;
    }

    private JPanel createStoreRow() {
        JPanel row = transparentPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));

        JButton shop = createButton("상점", 10);
        shop.setPreferredSize(STORE_SIZE);
        shop.setBorder(new EmptyBorder(8, 12, 12, 12));
        applyButtonLogo(shop, "store.png", 120, 36);
        shop.addActionListener(e -> openStore());

        row.add(shop);
        return row;
    }

    private JButton createMenuButton(
            String name, String imageName,
            int maxWidth, int maxHeight, int offsetX, int offsetY
    ) {
        JButton button = createButton(name, 16);
        button.setPreferredSize(MENU_SIZE);
        button.setBorder(new EmptyBorder(
                18 + offsetY, 24 + offsetX,
                22 - offsetY, 24 - offsetX
        ));
        button.setFont(font(26));
        applyButtonLogo(button, imageName, maxWidth, maxHeight);
        button.addActionListener(e -> showPreparing(name));
        return button;
    }

    private void applyButtonLogo(
            JButton button, String imageName, int maxWidth, int maxHeight
    ) {
        ImageIcon icon = loadImage(imageName, maxWidth, maxHeight, true);
        if (icon != null) {
            button.getAccessibleContext().setAccessibleName(button.getText());
            button.setIcon(icon);
            button.setText(null);
        }
    }

    private ImageIcon loadImage(
            String imageName, int maxWidth, int maxHeight, boolean trim
    ) {
        File file = new File("images", imageName);
        if (!file.isFile()) {
            return null;
        }

        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                return null;
            }

            if (trim) {
                image = trimTransparentMargins(image);
            }

            double scale = Math.min(
                    (double) maxWidth / image.getWidth(),
                    (double) maxHeight / image.getHeight()
            );
            int width = Math.max(1, (int) Math.round(image.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(image.getHeight() * scale));

            return new ImageIcon(
                    image.getScaledInstance(width, height, Image.SCALE_SMOOTH)
            );
        } catch (IOException ignored) {
            return null;
        }
    }

    private BufferedImage trimTransparentMargins(BufferedImage image) {
        int left = image.getWidth();
        int top = image.getHeight();
        int right = -1;
        int bottom = -1;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int alpha = image.getRGB(x, y) >>> 24;
                if (alpha > 16) {
                    left = Math.min(left, x);
                    top = Math.min(top, y);
                    right = Math.max(right, x);
                    bottom = Math.max(bottom, y);
                }
            }
        }

        if (right < left || bottom < top) {
            return image;
        }

        return image.getSubimage(left, top, right - left + 1, bottom - top + 1);
    }

    private JButton createButton(String text, int radius) {
        JButton button = new RoundedButton(text, radius);
        button.setFont(font(16));
        return button;
    }

    private JPanel transparentPanel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    private Font font(int size) {
        return new Font(Font.DIALOG, Font.BOLD, size);
    }

    private void openStore() {
        showPreparing("상점");
    }

    private void showPreparing(String screen) {
        JOptionPane.showMessageDialog(
                this,
                screen + " 화면은 준비 중입니다.",
                "보카랜드",
                JOptionPane.INFORMATION_MESSAGE
        );
    }
}