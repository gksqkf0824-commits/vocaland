package vocaland.ui.study;

import vocaland.ui.util.ImagePaths;

import vocaland.ui.components.RoundedButton;
import vocaland.ui.theme.Theme;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.HierarchyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

public class MatchingPanel extends JPanel {
    private List<String[]> wordPool;
    private String[][] pairs;
    private final JPanel grid = new JPanel(new GridLayout(0, 4, 12, 12));
    private Timer flipTimer;
    private java.util.function.BiConsumer<String, Boolean> onAnswer;
    public void setOnAnswer(java.util.function.BiConsumer<String, Boolean> listener) { onAnswer = listener; }

    private static final int PREVIEW_SECONDS = 7;
    private static final int MISMATCH_DELAY_MS = 850;

    private static final Color BACKGROUND = new Color(0xFAFBF8);
    private static final Color CARD_BACK = new Color(0xF1F5E9);
    private static final Color MATCHED = new Color(0xD4EFB5);
    private static final Color MISMATCHED = new Color(0xFBE0E0);
    private static final Color WRONG_TEXT = new Color(0xA52C2C);

    private static final String INSTRUCTIONS =
            "영어 단어와 뜻이 같은 카드를 찾아보세요.";


    private final List<CardButton> cards = new ArrayList<>();
    private final JLabel count = new JLabel();
    private final JLabel status = new JLabel("", SwingConstants.CENTER);
    private final Timer previewTimer = new Timer(1000, event -> tickPreview());

    private CardButton firstCard;
    private boolean waitingToFlip;
    private int matchedPairs;
    private int previewSeconds = PREVIEW_SECONDS;

    public MatchingPanel(Runnable goMain, List<String[]> wordPairs) {
        wordPool = List.copyOf(wordPairs);
        setLayout(new BorderLayout(0, 18));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(16, 24, 22, 30));

        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);
        top.add(createHeader(goMain), BorderLayout.NORTH);
        top.add(createCounter(), BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        grid.setOpaque(false);
        add(grid, BorderLayout.CENTER);

        status.setOpaque(true);
        status.setBackground(Color.WHITE);
        status.setBorder(new LineBorder(new Color(0xDCE5CF), 1, true));
        status.setPreferredSize(new Dimension(0, 50));
        status.setFont(status.getFont().deriveFont(Font.BOLD, 16f));
        add(status, BorderLayout.SOUTH);

        startRound();

        // CardLayout에서 짝 맞추기 화면이 보일 때만 시간을 센다.
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0) {
                return;
            }

            if (isShowing()) {
                startRound();
            } else {
                previewTimer.stop();
            }
        });
    }

    public void setWordPool(List<String[]> wordPairs) {
        wordPool = List.copyOf(wordPairs);
        if (isShowing()) startRound();
    }

    private void startRound() {
        previewTimer.stop();
        if (flipTimer != null) flipTimer.stop();
        firstCard = null;
        waitingToFlip = false;
        matchedPairs = 0;
        previewSeconds = PREVIEW_SECONDS;
        List<String[]> shuffled = new ArrayList<>(wordPool);
        Collections.shuffle(shuffled);
        pairs = shuffled.stream().limit(6).toArray(String[][]::new);
        count.setText("0/" + pairs.length);
        cards.clear();
        grid.removeAll();
        for (int i = 0; i < pairs.length; i++) {
            cards.add(new CardButton(i, pairs[i][0]));
            cards.add(new CardButton(i, pairs[i][1]));
        }
        Collections.shuffle(cards);
        for (CardButton card : cards) {
            card.reveal(Color.WHITE);
            card.addActionListener(event -> selectCard(card));
            grid.add(card);
        }
        showPreviewCountdown();
        grid.revalidate();
        grid.repaint();
        if (isShowing()) previewTimer.start();
    }

    private JPanel createHeader(Runnable goMain) {
        JLabel title = new JLabel("카드 짝 맞추기");
        title.setForeground(Theme.TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));
        applyImage(title, loadIcon(ImagePaths.ROOT + ImagePaths.LOGO_PAIR, 230, 88));
        title.setBorder(new EmptyBorder(0, 0, 0, 0));
        title.setVerticalAlignment(SwingConstants.TOP);

        RoundedButton back = new RoundedButton("← 메인으로");
        back.setPreferredSize(new Dimension(132, 42));
        back.setBorder(new EmptyBorder(8, 12, 12, 18));

        ImageIcon backIcon = loadIcon(ImagePaths.ROOT + ImagePaths.TEXT_GO_MAIN, 102, 24);
        if (backIcon != null) {
            back.setIcon(backIcon);
            back.setText("");
            back.getAccessibleContext().setAccessibleName("메인으로");
        }
        back.addActionListener(event -> goMain.run());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.setOpaque(false);
        actions.add(back);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(title, BorderLayout.WEST);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private JPanel createCounter() {
        JLabel label = new JLabel("맞춘 쌍");
        label.setForeground(Theme.TEXT);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 18f));
        applyImage(label, loadIcon(ImagePaths.ROOT + ImagePaths.TEXT_CORRECT_MATCH, 115, 35));

        count.setForeground(Theme.TEXT);
        count.setFont(count.getFont().deriveFont(Font.BOLD, 22f));

        JPanel counter = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        counter.setOpaque(false);
        counter.add(label);
        counter.add(count);
        return counter;
    }

    private void tickPreview() {
        previewSeconds--;
        if (previewSeconds > 0) {
            showPreviewCountdown();
            return;
        }

        previewTimer.stop();
        for (CardButton card : cards) {
            card.hideFace();
        }
        setStatus(INSTRUCTIONS, Theme.TEXT);
    }

    private void showPreviewCountdown() {
        setStatus("카드 위치를 기억하세요 · " + previewSeconds + "초", Theme.TEXT);
    }

    private void selectCard(CardButton card) {
        if (previewSeconds > 0 || waitingToFlip || card.matched || card == firstCard) {
            return;
        }

        card.reveal(Color.WHITE);
        if (firstCard == null) {
            firstCard = card;
            setStatus("짝이 되는 카드를 선택하세요.", Theme.TEXT);
            return;
        }

        CardButton first = firstCard;
        firstCard = null;

        if (onAnswer != null) onAnswer.accept(pairs[first.pairId][0], first.pairId == card.pairId);
        if (first.pairId == card.pairId) {
            first.markMatched();
            card.markMatched();
            matchedPairs++;
            count.setText(matchedPairs + "/" + pairs.length);

            if (matchedPairs == pairs.length) {
                setStatus("모든 짝을 맞췄어요!", Theme.TEXT);
            } else {
                setStatus("짝을 맞췄어요!", Theme.TEXT);
            }
            return;
        }

        waitingToFlip = true;
        first.reveal(MISMATCHED);
        card.reveal(MISMATCHED);
        setStatus("짝이 달라요. 잠시 후 다시 뒤집힙니다.", WRONG_TEXT);

        flipTimer = new Timer(MISMATCH_DELAY_MS, event -> {
            first.hideFace();
            card.hideFace();
            waitingToFlip = false;
            setStatus("다시 카드를 선택하세요.", Theme.TEXT);
        });
        flipTimer.setRepeats(false);
        flipTimer.start();
    }

    private void setStatus(String message, Color color) {
        status.setText(message);
        status.setForeground(color);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        vocaland.ui.components.BackgroundPanel.paintBackground(this, graphics);
    }

    private static void applyImage(JLabel label, ImageIcon icon) {
        if (icon != null) {
            label.getAccessibleContext().setAccessibleName(label.getText());
            label.setIcon(icon);
            label.setText("");
        }
    }

    private static ImageIcon loadIcon(String path, int width, int height) {
        return vocaland.ui.util.ImageAssets.load(path, width, height, -1, false);
    }

    private static class CardButton extends JButton {
        private static final int CORNER_RADIUS = 16;
        private static final int SHADOW_DEPTH = 4;
        private static final Color SHADOW = new Color(32, 50, 15, 65);
        private static final Color OUTLINE = new Color(0xAAB99B);

        private final int pairId;
        private final String face;
        private boolean matched;

        CardButton(int pairId, String face) {
            this.pairId = pairId;
            this.face = face;

            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setBorder(new EmptyBorder(8, 12, 8 + SHADOW_DEPTH, 12));
            setForeground(Theme.TEXT);
            setFont(getFont().deriveFont(Font.BOLD, 26f));
            hideFace();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                int width = getWidth() - 3;
                int height = getHeight() - SHADOW_DEPTH - 3;
                int arc = CORNER_RADIUS * 2;

                g.setColor(SHADOW);
                g.fillRoundRect(1, 1 + SHADOW_DEPTH, width, height, arc, arc);

                g.setColor(getBackground());
                g.fillRoundRect(1, 1, width, height, arc, arc);

                g.setColor(OUTLINE);
                g.drawRoundRect(1, 1, width, height, arc, arc);
            } finally {
                g.dispose();
            }

            super.paintComponent(graphics);
        }

        void hideFace() {
            setText("?");
            setBackground(CARD_BACK);
            getAccessibleContext().setAccessibleName("뒤집힌 카드");
        }

        void reveal(Color color) {
            setText(face);
            setBackground(color);
            getAccessibleContext().setAccessibleName(face);
        }

        void markMatched() {
            matched = true;
            reveal(MATCHED);
            getAccessibleContext().setAccessibleName(face + ", 짝 맞춤");
        }
    }
}
