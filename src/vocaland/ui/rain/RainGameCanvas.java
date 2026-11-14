package vocaland.ui.rain;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import vocaland.ui.Theme;

/*
 * RainGameCanvas - 단어가 떨어지는 영역 (그리기만 담당)
 *
 * getAreaWidth()   : 게임 영역 가로 길이 (테두리 제외)
 * getGroundY()     : 바닥의 y좌표
 * paintComponent() : 배경 → 단어 → 바닥 → 레벨 순서로 그리기
 * drawSky()        : 배경 그라데이션
 * drawWord()       : 빗줄기 + 단어 박스 (바닥에 가까우면 빨간색)
 * drawGround()     : 산성비가 고인 바닥
 * drawLevel()      : 바닥 위에 현재 레벨과 정답 점수 표시
 */
public class RainGameCanvas extends JPanel {
    public static final int GROUND_HEIGHT = 28;

    private static final Color SKY_TOP = Color.WHITE;
    private static final Color SKY_BOTTOM = Theme.PRIMARY_TINT;
    private static final Color RAIN_LINE = new Color(120, 140, 160);
    private static final Color DANGER_FILL = new Color(255, 228, 225);
    private static final Color DANGER_BORDER = new Color(210, 70, 60);
    private static final Color ACID = new Color(181, 222, 103, 200);
    private static final Font WORD_FONT = new Font(Font.DIALOG, Font.BOLD, 17);
    private static final Font LEVEL_FONT = new Font(Font.DIALOG, Font.BOLD, 15);

    private final RainGame game;

    public RainGameCanvas(RainGame game) {
        this.game = game;
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createLineBorder(Theme.TEXT, 2));
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
            for (FallingWord word : game.getFallingWords()) {
                drawWord(g, word, groundY);
            }
            drawGround(g, width, groundY);
            drawLevel(g, groundY);
        } finally {
            g.dispose();
        }
    }

    private void drawSky(Graphics2D g, int width, int groundY) {
        g.setPaint(new GradientPaint(0, 0, SKY_TOP, 0, groundY, SKY_BOTTOM));
        g.fillRect(0, 0, width, groundY);
    }

    private void drawWord(Graphics2D g, FallingWord word, int groundY) {
        String text = word.getDisplayText(game.getMode());
        g.setFont(WORD_FONT);
        FontMetrics fm = g.getFontMetrics();

        int boxW = fm.stringWidth(text) + 24;
        int boxH = FallingWord.BOX_HEIGHT;
        int left = (int) Math.round(word.getX() - boxW / 2.0);
        int top = (int) Math.round(word.getY());
        int cx = (int) Math.round(word.getX());

        g.setColor(RAIN_LINE);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(cx - 6, top - 26, cx - 6, top - 4);
        g.drawLine(cx + 2, top - 14, cx + 2, top - 6);

        boolean danger = top + boxH > groundY - 70;

        g.setColor(danger ? DANGER_FILL : Color.WHITE);
        g.fillRoundRect(left, top, boxW, boxH, 12, 12);
        g.setColor(danger ? DANGER_BORDER : Theme.TEXT);
        g.drawRoundRect(left, top, boxW, boxH, 12, 12);

        int textY = top + (boxH - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(text, left + 12, textY);
    }

    private void drawGround(Graphics2D g, int width, int groundY) {
        g.setColor(ACID);
        g.fillRect(0, groundY, width, GROUND_HEIGHT);
        g.setColor(Theme.PRIMARY_PRESSED);
        g.setStroke(new BasicStroke(3f));
        g.drawLine(0, groundY, width, groundY);
    }

    private void drawLevel(Graphics2D g, int groundY) {
        if (game.getState() == RainGame.State.READY) {
            return;
        }
        g.setFont(LEVEL_FONT);
        g.setColor(Theme.TEXT);
        g.drawString("Lv." + game.getLevel() + "  (정답 +" + game.getPointsPerWord() + "점)", 12, groundY + 20);
    }
}
