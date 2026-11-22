package vocaland.ui.rain;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import vocaland.ui.Theme;

/*
 * RainGameCanvas - 단어가 떨어지는 영역 (그리기와 영역 안 배치 담당)
 *
 * setOverlay()     : 가운데에 띄울 안내창 등록 (시작, 게임 오버, 오답 노트)
 * setInput()       : 바닥 가운데에 놓을 입력칸 등록
 * addPopup()       : 단어 위치에 잠깐 떴다 사라지는 글자 추가 (정답 점수, 놓침 등)
 * updatePopups()   : 떠 있는 글자의 남은 시간 줄이기
 * clearPopups()    : 떠 있는 글자 모두 지우기
 * getAreaWidth()   : 게임 영역 가로 길이 (테두리 제외)
 * getGroundY()     : 바닥의 y좌표
 * doLayout()       : 안내창은 가운데, 입력칸은 바닥 가운데에 배치
 * paintComponent() : 배경 → 단어(게임 중에만) → 바닥 → 떠 있는 글자 → 레벨 순서로 그리기
 * drawWord()       : 빗줄기 + 같은 크기의 단어 박스 (하트 단어는 빨간 네모 + 흰 글씨)
 * fitFont()        : 박스에 안 들어가는 긴 단어는 글자 크기를 줄여서 맞추기
 * drawGround()     : 산성비가 고인 바닥
 * drawPopups()     : 위로 올라가며 흐려지는 글자
 * drawLevel()      : 왼쪽 위에 현재 레벨 (반투명 배경)
 */
public class RainGameCanvas extends JPanel {
    public static final int GROUND_HEIGHT = 64;

    private static final Color SKY_TOP = Color.WHITE;
    private static final Color SKY_BOTTOM = Theme.PRIMARY_TINT;
    private static final Color RAIN_LINE = new Color(120, 140, 160);
    private static final Color HEART_FILL = new Color(220, 50, 60);
    private static final Color ACID = new Color(181, 222, 103, 200);
    private static final Color LEVEL_BACK = new Color(255, 255, 255, 170);
    private static final Font WORD_FONT = new Font(Font.DIALOG, Font.BOLD, 34);
    private static final int MIN_WORD_SIZE = 16;
    private static final int TEXT_PADDING = 12;
    private static final Font LEVEL_FONT = new Font(Font.DIALOG, Font.BOLD, 30);
    private static final Font POPUP_FONT = new Font(Font.DIALOG, Font.BOLD, 24);
    private static final int POPUP_MS = 900;
    private static final int POPUP_RISE = 40;
    private static final Dimension INPUT_SIZE = new Dimension(460, 50);

    private final RainGame game;
    private final List<Popup> popups = new ArrayList<>();
    private JComponent overlay;
    private JComponent input;

    public RainGameCanvas(RainGame game) {
        this.game = game;
        setLayout(null);
        setBorder(BorderFactory.createLineBorder(Theme.TEXT, 2));
    }

    public void setOverlay(JComponent overlay) {
        this.overlay = overlay;
        add(overlay);
    }

    public void setInput(JComponent input) {
        this.input = input;
        add(input);
    }

    public void addPopup(String text, double x, double y, Color color) {
        popups.add(new Popup(text, x, y, color));
    }

    public void updatePopups(int elapsedMs) {
        Iterator<Popup> it = popups.iterator();
        while (it.hasNext()) {
            Popup popup = it.next();
            popup.leftMs -= elapsedMs;
            if (popup.leftMs <= 0) {
                it.remove();
            }
        }
    }

    public void clearPopups() {
        popups.clear();
    }

    public int getAreaWidth() {
        Insets in = getInsets();
        return getWidth() - in.left - in.right;
    }

    public int getGroundY() {
        Insets in = getInsets();
        return getHeight() - in.top - in.bottom - GROUND_HEIGHT;
    }

    @Override
    public void doLayout() {
        Insets in = getInsets();
        int width = getAreaWidth();

        if (overlay != null) {
            Dimension size = overlay.getPreferredSize();
            int w = Math.min(size.width, width - 20);
            int h = Math.min(size.height, getGroundY() - 20);
            overlay.setBounds(in.left + (width - w) / 2, in.top + (getGroundY() - h) / 2, w, h);
        }
        if (input != null) {
            int w = Math.min(INPUT_SIZE.width, width - 20);
            int h = INPUT_SIZE.height;
            input.setBounds(in.left + (width - w) / 2, in.top + getGroundY() + (GROUND_HEIGHT - h) / 2, w, h);
        }
    }

    @Override
    public boolean isOptimizedDrawingEnabled() {
        return false;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Insets in = getInsets();
            g.translate(in.left, in.top);
            int width = getAreaWidth();
            int groundY = getGroundY();
            g.clipRect(0, 0, width, groundY + GROUND_HEIGHT);

            drawSky(g, width, groundY);
            if (game.getState() == RainGame.State.PLAYING) {
                for (FallingWord word : game.getFallingWords()) {
                    drawWord(g, word);
                }
            }
            drawGround(g, width, groundY);
            drawPopups(g);
            drawLevel(g);
        } finally {
            g.dispose();
        }
    }

    private void drawSky(Graphics2D g, int width, int groundY) {
        g.setPaint(new GradientPaint(0, 0, SKY_TOP, 0, groundY, SKY_BOTTOM));
        g.fillRect(0, 0, width, groundY);
    }

    private void drawWord(Graphics2D g, FallingWord word) {
        String text = word.getDisplayText(game.getMode());
        int boxW = FallingWord.BOX_WIDTH;
        int boxH = FallingWord.BOX_HEIGHT;
        int left = (int) Math.round(word.getX() - boxW / 2.0);
        int top = (int) Math.round(word.getY());
        int cx = (int) Math.round(word.getX());

        g.setColor(RAIN_LINE);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(cx - 10, top - 38, cx - 10, top - 8);
        g.drawLine(cx + 5, top - 22, cx + 5, top - 10);

        if (word.isHeart()) {
            g.setColor(HEART_FILL);
            g.fillRoundRect(left, top, boxW, boxH, 16, 16);
            g.setColor(Color.WHITE);
        } else {
            g.setColor(Color.WHITE);
            g.fillRoundRect(left, top, boxW, boxH, 16, 16);
            g.setColor(Theme.TEXT);
            g.drawRoundRect(left, top, boxW, boxH, 16, 16);
        }

        g.setFont(fitFont(g, text, boxW - TEXT_PADDING * 2));
        FontMetrics fm = g.getFontMetrics();
        int textX = left + (boxW - fm.stringWidth(text)) / 2;
        int textY = top + (boxH - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(text, textX, textY);
    }

    private Font fitFont(Graphics2D g, String text, int maxWidth) {
        Font font = WORD_FONT;
        while (font.getSize() > MIN_WORD_SIZE && g.getFontMetrics(font).stringWidth(text) > maxWidth) {
            font = font.deriveFont((float) (font.getSize() - 1));
        }
        return font;
    }

    private void drawGround(Graphics2D g, int width, int groundY) {
        g.setColor(ACID);
        g.fillRect(0, groundY, width, GROUND_HEIGHT);
        g.setColor(Theme.PRIMARY_PRESSED);
        g.setStroke(new BasicStroke(3f));
        g.drawLine(0, groundY, width, groundY);
    }

    private void drawPopups(Graphics2D g) {
        g.setFont(POPUP_FONT);
        FontMetrics fm = g.getFontMetrics();
        for (Popup popup : popups) {
            float progress = 1f - (float) popup.leftMs / POPUP_MS;
            int x = (int) Math.round(popup.x - fm.stringWidth(popup.text) / 2.0);
            int y = (int) Math.round(popup.y - POPUP_RISE * progress);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f - progress));
            g.setColor(popup.color);
            g.drawString(popup.text, x, y);
        }
        g.setComposite(AlphaComposite.SrcOver);
    }

    private void drawLevel(Graphics2D g) {
        if (game.getState() != RainGame.State.PLAYING) {
            return;
        }
        String text = "Lv." + game.getLevel();
        g.setFont(LEVEL_FONT);
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(text) + 28;
        int h = fm.getHeight() + 10;

        g.setColor(LEVEL_BACK);
        g.fillRoundRect(12, 12, w, h, 16, 16);
        g.setColor(Theme.TEXT);
        g.drawString(text, 26, 12 + 5 + fm.getAscent());
    }

    private static class Popup {
        private final String text;
        private final double x;
        private final double y;
        private final Color color;
        private int leftMs = POPUP_MS;

        Popup(String text, double x, double y, Color color) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.color = color;
        }
    }
}
