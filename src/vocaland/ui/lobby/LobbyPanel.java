package vocaland.ui.lobby;

import vocaland.ui.util.ImagePaths;

import vocaland.ui.components.RoundedButton;
import vocaland.ui.theme.Theme;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class LobbyPanel extends JPanel {
    private final Runnable openStore;
    private vocaland.service.LocalData data;
    private Runnable logout;
    private JButton profileButton, coinsButton;
    private JLabel portraitLabel;
    private static final Color BACKGROUND = new Color(250, 251, 248);
    private static final Dimension MENU_SIZE = new Dimension(250, 120);
    private static final Dimension STORE_SIZE = new Dimension(156, 56);
    private static final int HEADER_ICON_GAP = 10;


    public LobbyPanel(vocaland.service.LocalData data, Runnable logout, Runnable openStudy, Runnable openRain, Runnable openBasketball, Runnable openStore, Runnable openScoreboard) {
        this(openStudy, openRain, openBasketball, openStore, openScoreboard);
        this.data = data;
        this.logout = logout;
        refreshProfile();
    }

    public void refreshProfile() {
        if (data == null || data.current() == null) return;
        profileButton.setText(data.current().name());
        String character = data.equippedImage("CHARACTER");
        ImageIcon portrait = character == null ? null
                : loadImage(character.replaceFirst("^images/", ""), 101, 101, true);
        portraitLabel.setIcon(portrait != null ? portrait : loadImage(ImagePaths.STORE_CHARACTER_GIRYONG_BASIC, 88, 88, true));
        portraitLabel.getAccessibleContext().setAccessibleName(data.current().name() + " 캐릭터");
        profileButton.setToolTipText(data.current().name());
        coinsButton.setText(String.format("%,d", data.current().coins()));
        coinsButton.getAccessibleContext().setAccessibleName("코인 " + data.current().coins() + ", 상점 열기");
    }

    public LobbyPanel(Runnable openStudy, Runnable openRain, Runnable openBasketball, Runnable openStore, Runnable openScoreboard) {
        this.openStore = openStore;
        setLayout(new BorderLayout(0, 12));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(18, 32, 22, 32));

        add(createHeader(), BorderLayout.EAST);
        add(createContent(openStudy, openRain, openBasketball, openScoreboard), BorderLayout.CENTER);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        vocaland.ui.components.BackgroundPanel.paintBackground(this, graphics);
    }

    private JPanel createHeader() {
        JPanel header = transparentPanel(new BorderLayout());

        JButton profile = new JButton("프로필");
        profileButton = profile;
        profile.setFont(font(16));
        profile.setForeground(Theme.TEXT);
        profile.setContentAreaFilled(false);
        profile.setBorder(new EmptyBorder(2, 0, 2, 0));
        profile.setFocusPainted(false);
        profile.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JPanel portraitCard = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g = (Graphics2D) graphics.create();
                try {
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int w = getWidth() - 2, h = getHeight() - 6;
                    g.setColor(new Color(32, 50, 15, 30));
                    g.fillRoundRect(1, 5, w, h, 28, 28);
                    Shape frame = new java.awt.geom.RoundRectangle2D.Double(1, 1, w, h, 28, 28);
                    g.clip(frame);
                    g.setPaint(new GradientPaint(0, 0, new Color(226, 246, 246), 0, h, new Color(246, 250, 219)));
                    g.fillRect(0, 0, getWidth(), getHeight());
                    g.setColor(new Color(255, 255, 255, 180));
                    g.fillOval(16, 17, 35, 18);
                    g.fillOval(30, 12, 27, 25);
                    g.setColor(new Color(207, 231, 173));
                    g.fillOval(-25, h - 29, w + 50, 65);
                    g.setClip(null);
                    g.setColor(new Color(190, 215, 172));
                    g.draw(frame);
                } finally {
                    g.dispose();
                }
            }
        };
        portraitCard.setOpaque(false);
        portraitCard.setPreferredSize(new Dimension(128, 144));
        portraitCard.setBorder(new EmptyBorder(18, 8, 12, 8));
        portraitLabel = new JLabel(loadImage(ImagePaths.STORE_CHARACTER_GIRYONG_BASIC, 88, 88, true), SwingConstants.CENTER);
        portraitCard.add(portraitLabel);
        profile.addActionListener(e -> {
            if (data != null) JOptionPane.showMessageDialog(this, data.current().name() + "\n경험치 " + data.current().xp() + " / 코인 " + data.current().coins());
        });

        JButton coins = createButton("0", 16);
        coinsButton = coins;
        ImageIcon coinIcon = loadImage(ImagePaths.STORE_COIN, 20, 20, true);
        if (coinIcon != null) {
            coins.setIcon(coinIcon);
            coins.setIconTextGap(HEADER_ICON_GAP);
        } else {
            coins.setText("코인 0");
        }

        coins.getAccessibleContext().setAccessibleName("코인 0, 상점 열기");
        coins.addActionListener(e -> openStore());

        JPanel account = transparentPanel(new GridBagLayout());
        GridBagConstraints position = new GridBagConstraints();
        position.gridx = 0;
        position.gridy = 0;
        position.fill = GridBagConstraints.HORIZONTAL;
        account.add(portraitCard, position);
        position.gridy++;
        position.insets = new Insets(6, 0, 0, 0);
        account.add(profile, position);
        position.gridy++;
        position.insets = new Insets(10, 0, 0, 0);
        account.add(coins, position);
        JButton signOut = createButton("로그아웃", 16);
        signOut.addActionListener(e -> { if (logout != null) logout.run(); });
        position.gridy++;
        account.add(signOut, position);
        header.add(account, BorderLayout.NORTH);
        return header;
    }

    private JPanel createContent(Runnable openStudy, Runnable openRain, Runnable openBasketball, Runnable openScoreboard) {
        JPanel content = transparentPanel(new GridBagLayout());
        JPanel column = transparentPanel(new BorderLayout(0, 64));

        JLabel title = new JLabel("보카랜드", SwingConstants.CENTER);
        title.setFont(font(42));
        title.setForeground(Theme.TEXT);

        ImageIcon logo = loadImage(ImagePaths.LOGO_LOGO, 440, 125, true);
        if (logo != null) {
            title.getAccessibleContext().setAccessibleName("보카랜드");
            title.setIcon(logo);
            title.setText(null);
        }

        JPanel menu = transparentPanel(new GridLayout(2, 2, 14, 14));

        // 로고별 최대 가로, 최대 세로, 좌우 이동, 상하 이동, 클릭 동작
        menu.add(createMenuButton("학습", ImagePaths.LOGO_STUDY, 188, 72, 0, 4,
                openStudy));
        menu.add(createMenuButton("랭킹", ImagePaths.LOGO_RANK, 198, 76, 4, 0,
                openScoreboard));
        menu.add(createMenuButton("산성비", ImagePaths.LOGO_RAIN, 208, 80, 0, -5,
                openRain));
        menu.add(createMenuButton("농구", ImagePaths.LOGO_BASKETBALL, 203, 74, 0, 3,
                openBasketball));

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
        applyButtonLogo(shop, ImagePaths.LOGO_STORE, 120, 36);
        shop.addActionListener(e -> openStore());

        row.add(shop);
        return row;
    }

    private JButton createMenuButton(
            String name, String imageName,
            int maxWidth, int maxHeight, int offsetX, int offsetY,
            Runnable onClick
    ) {
        JButton button = createButton(name, 16);
        button.setPreferredSize(MENU_SIZE);
        button.setBorder(new EmptyBorder(
                18 + offsetY, 24 + offsetX,
                22 - offsetY, 24 - offsetX
        ));
        button.setFont(font(26));
        applyButtonLogo(button, imageName,
                (int) Math.round(maxWidth * 0.95), (int) Math.round(maxHeight * 0.95));
        button.addActionListener(e -> onClick.run());
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

    private ImageIcon loadImage(String name, int width, int height, boolean trim) {
        return vocaland.ui.util.ImageAssets.load(ImagePaths.ROOT + name, width, height, trim ? 64 : -1, true);
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
        openStore.run();
    }


}

