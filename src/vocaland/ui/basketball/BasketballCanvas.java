package vocaland.ui.basketball;

import javax.swing.*;
import java.awt.*;

/*
 * BasketballCanvas - 농구 게임 코트 영역
 *
 * paintComponent()       : 배경 → 제시 단어 → 예상 포물선 → 골대 3개 → 게이지 → 농구공 순서로 그리기
 * drawQuestionCard()     : 좌상단 제시 단어 박스 그리기
 * drawTrajectoryGuide()  : 난이도별 예상 포물선 점선 그리기 (이지: 바닥까지 전체 / 하드: 중간까지만)
 * drawHoopsAndChoices()  : 오른쪽 골대 3개와 선택지 박스 그리기
 * drawPowerInfoBox()     : 하단 각도 및 힘 게이지 정보창 그리기
 * drawBall()             : 농구공 스프라이트 그리기
 */
public class BasketballCanvas extends JPanel {

    private static final Color GRADIENT_TOP = Color.WHITE;
    private static final Color GRADIENT_BOTTOM = new Color(236, 247, 226);
    private static final Color BORDER_BLACK = Color.BLACK;

    private final BasketballGame game;
    private final Image ballImg = new ImageIcon("images/basketball-ball.png").getImage();
    private final Image hoopImg = new ImageIcon("images/basketball-hoop.png").getImage();

    public BasketballCanvas(BasketballGame game) {
        this.game = game;
        setLayout(new GridBagLayout());
        setOpaque(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int curW = getWidth();
        int curH = getHeight();

        g2.setPaint(new GradientPaint(0, 0, GRADIENT_TOP, 0, curH, GRADIENT_BOTTOM));
        g2.fillRect(0, 0, curW, curH);

        if (game.getState() == BasketballGame.State.READY) {
            g2.dispose();
            return;
        }

        double scale = Math.min(1.0, Math.min(curW / BasketballGame.BASE_W, curH / BasketballGame.BASE_H));
        double logicalW = curW / scale;
        double logicalH = curH / scale;

        g2.scale(scale, scale);

        double centerOffsetX = Math.max(0.0, (logicalW - BasketballGame.BASE_W) / 2.0);
        double bottomShiftY = Math.max(0.0, logicalH - BasketballGame.BASE_H);

        drawQuestionCard(g2, centerOffsetX);

        if (!game.isShooting() && game.getState() == BasketballGame.State.PLAYING) {
            drawTrajectoryGuide(g2, centerOffsetX, bottomShiftY, logicalH);
        }

        drawHoopsAndChoices(g2, centerOffsetX, bottomShiftY);
        drawPowerInfoBox(g2, centerOffsetX, bottomShiftY);
        drawBall(g2, centerOffsetX, bottomShiftY);

        g2.dispose();
    }

    private void drawQuestionCard(Graphics2D g2, double centerOffsetX) {
        int wordBoxX = (int) (centerOffsetX + 35);
        int wordBoxY = 22;

        g2.setColor(Color.WHITE);
        g2.fillRoundRect(wordBoxX, wordBoxY, 260, 54, 14, 14);
        g2.setColor(BORDER_BLACK);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(wordBoxX, wordBoxY, 260, 54, 14, 14);

        g2.setColor(Color.BLACK);
        g2.setFont(new Font("맑은 고딕", Font.BOLD, 24));
        FontMetrics fm = g2.getFontMetrics();
        String qText = game.getQuestionText();
        int textX = wordBoxX + (260 - fm.stringWidth(qText)) / 2;
        g2.drawString(qText, textX, wordBoxY + 36);
    }

    private void drawTrajectoryGuide(Graphics2D g2, double centerOffsetX, double bottomShiftY, double logicalH) {
        double rad = Math.toRadians(game.getAngleDeg());
        double simVx = game.getPower() * Math.cos(rad);
        double simVy = -game.getPower() * Math.sin(rad);
        double simX = BasketballGame.BALL_START_X + 22;
        double simY = BasketballGame.BALL_START_Y + 22;

        boolean isHard = game.isHardMode();
        double maxGuideX = isHard
                ? (BasketballGame.BALL_START_X + BasketballGame.HOOP_X) / 2.0 + 30
                : BasketballGame.BASE_W + centerOffsetX;

        int maxSteps = isHard ? 25 : 120;

        for (int step = 1; step <= maxSteps; step++) {
            simX += simVx;
            simY += simVy;
            simVy += BasketballGame.GRAVITY;

            double drawX = centerOffsetX + simX;
            double drawY = bottomShiftY + simY;

            if (isHard && simX > maxGuideX) {
                break;
            }
            if (!isHard && (drawY >= logicalH - 6 || simX > maxGuideX)) {
                break;
            }

            int alpha;
            int dotSize;
            Color dotColor;

            if (isHard) {
                double progress = (simX - (BasketballGame.BALL_START_X + 22)) / (maxGuideX - (BasketballGame.BALL_START_X + 22));
                alpha = (int) Math.max(160, 255 - progress * 80);
                dotSize = (int) Math.max(6, 10 - progress * 3);
                dotColor = new Color(220, 53, 69, alpha);
            } else {
                alpha = Math.max(35, 240 - step * 3);
                dotSize = Math.max(4, 9 - step / 12);
                dotColor = new Color(33, 150, 243, alpha);
            }

            g2.setColor(dotColor);
            g2.fillOval((int) (drawX - dotSize / 2.0), (int) (drawY - dotSize / 2.0), dotSize, dotSize);
        }
    }

    private void drawHoopsAndChoices(Graphics2D g2, double centerOffsetX, double bottomShiftY) {
        String[] choices = game.getChoices();
        for (int i = 0; i < 3; i++) {
            int drawHoopX = (int) (centerOffsetX + BasketballGame.HOOP_X);
            int drawHoopY = (int) (bottomShiftY + BasketballGame.HOOP_Y[i]);

            if (hoopImg != null && hoopImg.getWidth(null) > 0) {
                g2.drawImage(hoopImg, drawHoopX, drawHoopY, BasketballGame.HOOP_W, BasketballGame.HOOP_H, this);
            } else {
                g2.setColor(Color.ORANGE);
                g2.fillRect(drawHoopX, drawHoopY + 20, BasketballGame.HOOP_W, 12);
            }

            int choiceX = drawHoopX + BasketballGame.HOOP_W + 10;
            int choiceY = drawHoopY + 10;
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(choiceX, choiceY, 130, 42, 12, 12);
            g2.setColor(BORDER_BLACK);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(choiceX, choiceY, 130, 42, 12, 12);

            g2.setColor(Color.BLACK);
            g2.setFont(new Font("맑은 고딕", Font.BOLD, 16));
            g2.drawString(choices[i], choiceX + 14, choiceY + 27);
        }
    }

    private void drawPowerInfoBox(Graphics2D g2, double centerOffsetX, double bottomShiftY) {
        int infoX = (int) (centerOffsetX + 35);
        int infoY = (int) (bottomShiftY + 402);

        g2.setColor(Color.WHITE);
        g2.fillRoundRect(infoX, infoY, 260, 48, 12, 12);
        g2.setColor(BORDER_BLACK);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(infoX, infoY, 260, 48, 12, 12);

        g2.setColor(Color.BLACK);
        g2.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        g2.drawString(String.format("각도: %.0f° (↑↓)", game.getAngleDeg()), infoX + 14, infoY + 20);
        double powerPct = ((game.getPower() - 12.0) / 16.0) * 100;
        g2.drawString(String.format("힘: %.0f%% (←→)", powerPct), infoX + 135, infoY + 20);

        g2.setColor(new Color(230, 230, 230));
        g2.fillRoundRect(infoX + 14, infoY + 29, 232, 10, 6, 6);
        g2.setColor(game.isHardMode() ? new Color(229, 57, 53) : new Color(66, 165, 245));
        int barW = (int) (((game.getPower() - 12.0) / 16.0) * 232);
        g2.fillRoundRect(infoX + 14, infoY + 29, barW, 10, 6, 6);
        g2.setColor(BORDER_BLACK);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(infoX + 14, infoY + 29, 232, 10, 6, 6);
    }

    private void drawBall(Graphics2D g2, double centerOffsetX, double bottomShiftY) {
        int drawBallX = (int) (centerOffsetX + game.getBallX());
        int drawBallY = (int) (bottomShiftY + game.getBallY());

        if (ballImg != null && ballImg.getWidth(null) > 0) {
            g2.drawImage(ballImg, drawBallX, drawBallY, 45, 45, this);
        } else {
            g2.setColor(new Color(255, 111, 0));
            g2.fillOval(drawBallX, drawBallY, 45, 45);
        }
    }
}