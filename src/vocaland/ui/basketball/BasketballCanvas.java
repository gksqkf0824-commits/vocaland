package vocaland.ui.basketball;

import vocaland.ui.util.ImagePaths;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/*
 * BasketballCanvas - 농구 게임 코트 영역 (그리기만 담당)
 *
 * paintComponent()       : 배경 → 제시 단어 → 예상 포물선 → 골대 3개 → 게이지 → 농구공 → 그물 앞레이어 → 판정 디버그 순서로 그리기
 * drawQuestionCard()     : 좌상단 제시 단어 박스 그리기
 * drawTrajectoryGuide()  : 난이도별 예상 포물선 점선 그리기 (이지: 바닥까지 전체 / 하드: 중간까지만 / 익스트림: 보라색 통일)
 * drawHoopsAndChoices()  : 오른쪽 골대 3개와 선택지 박스 그리기
 * drawNetFrontOverlay()  : 익스트림 모드 골인 시 공이 그물 안으로 들어가는 입체감을 위한 그물 앞부분 오버레이 그리기
 * drawPowerInfoBox()     : 하단 각도 및 힘 게이지 정보창 그리기
 * drawBall()             : 농구공 스프라이트 그리기
 * drawHitboxDebug()      : 골대(백보드, 림, 통과선) 및 농구공 충돌 판정 영역 시각화 그리기
 */
public class BasketballCanvas extends vocaland.ui.components.RoundedPanel {

    private static final Color GRADIENT_TOP = Color.WHITE;
    private static final Color GRADIENT_BOTTOM = new Color(236, 247, 226);
    private static final Color BORDER_BLACK = Color.BLACK;
    private static final boolean SHOW_HITBOX_DEBUG = false;

    private final BasketballGame game;
    private final Image courtBackground = new ImageIcon(
            ImagePaths.ROOT + ImagePaths.GAME_BASKETBALL_BACKGROUND).getImage();
    // 원본(1200px 이상)을 매 프레임 축소하면 느려서, 그릴 크기의 2배로 미리 한 번 줄여 둠
    private static final int SPRITE_QUALITY = 2;
    private Image ballImg = sprite(ImagePaths.ROOT + ImagePaths.STORE_BASKETBALL_BALL_BASIC,
            (int) BasketballGame.BALL_SIZE, (int) BasketballGame.BALL_SIZE);
    private Image hoopImg = sprite(ImagePaths.ROOT + ImagePaths.STORE_BASKETBALL_HOOP_BASIC,
            BasketballGame.HOOP_W, BasketballGame.HOOP_H);
    private BufferedImage backgroundCache;

    public void setHoopImage(String path) {
        Image img = sprite(path == null ? ImagePaths.ROOT + ImagePaths.STORE_BASKETBALL_HOOP_BASIC : path,
                BasketballGame.HOOP_W, BasketballGame.HOOP_H);
        if (img != null) hoopImg = img;
        repaint();
    }

    public void setBallImage(String path) {
        Image img = sprite(path == null ? ImagePaths.ROOT + ImagePaths.STORE_BASKETBALL_BALL_BASIC : path,
                (int) BasketballGame.BALL_SIZE, (int) BasketballGame.BALL_SIZE);
        if (img != null) ballImg = img;
        repaint();
    }

    private static Image sprite(String path, int w, int h) {
        ImageIcon icon = vocaland.ui.util.ImageAssets.icon(path, w * SPRITE_QUALITY, h * SPRITE_QUALITY);
        return icon == null ? null : icon.getImage();
    }

    private BufferedImage createBackground(int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = out.createGraphics();
        g2.setPaint(new GradientPaint(0, 0, GRADIENT_TOP, 0, h, GRADIENT_BOTTOM));
        g2.fillRect(0, 0, w, h);
        if (courtBackground.getWidth(null) > 0 && courtBackground.getHeight(null) > 0) {
            double backgroundScale = Math.max((double) w / courtBackground.getWidth(null),
                    (double) h / courtBackground.getHeight(null));
            int backgroundWidth = (int) Math.ceil(courtBackground.getWidth(null) * backgroundScale);
            int backgroundHeight = (int) Math.ceil(courtBackground.getHeight(null) * backgroundScale);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(courtBackground, (w - backgroundWidth) / 2, (h - backgroundHeight) / 2,
                    backgroundWidth, backgroundHeight, null);
            g2.setColor(new Color(255, 255, 255, 145));
            g2.fillRect(0, 0, w, h);
        }
        g2.dispose();
        return out;
    }

    public BasketballCanvas(BasketballGame game) {
        this.game = game;
        setLayout(new GridBagLayout());
        setOpaque(false);
        setBorder(new vocaland.ui.components.RoundedBorder(new Color(181, 205, 157), 2));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int curW = getWidth();
        int curH = getHeight();

        if (curW <= 0 || curH <= 0) {
            g2.dispose();
            return;
        }

        // 배경(그라데이션 + 코트 사진 + 흰 막)은 크기가 바뀔 때만 새로 만듦
        if (backgroundCache == null || backgroundCache.getWidth() != curW || backgroundCache.getHeight() != curH) {
            backgroundCache = createBackground(curW, curH);
        }
        g2.drawImage(backgroundCache, 0, 0, null);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        double scale = Math.min(curW / BasketballGame.BASE_W, curH / BasketballGame.BASE_H);
        double logicalW = curW / scale;
        double logicalH = curH / scale;

        game.setFloorY(logicalH);

        if (game.getState() == BasketballGame.State.READY) {
            g2.dispose();
            return;
        }

        g2.scale(scale, scale);

        double centerOffsetX = Math.max(0.0, (logicalW - BasketballGame.BASE_W) / 2.0);
        double bottomShiftY = Math.max(0.0, logicalH - BasketballGame.BASE_H);

        drawQuestionCard(g2, centerOffsetX);

        if (!game.isShooting() && game.getState() == BasketballGame.State.PLAYING) {
            drawTrajectoryGuide(g2, centerOffsetX, bottomShiftY, logicalW, logicalH);
        }

        drawHoopsAndChoices(g2, centerOffsetX, bottomShiftY);
        drawPowerInfoBox(g2, centerOffsetX, bottomShiftY);
        drawBall(g2, centerOffsetX, bottomShiftY);

        if (game.isExtremeMode() && game.isScoredThisShot()) {
            drawNetFrontOverlay(g2, centerOffsetX, bottomShiftY);
        }

        if (SHOW_HITBOX_DEBUG) {
            drawHitboxDebug(g2, centerOffsetX, bottomShiftY);
        }

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

    private void drawTrajectoryGuide(Graphics2D g2, double centerOffsetX, double bottomShiftY, double logicalW, double logicalH) {
        double rad = Math.toRadians(game.getAngleDeg());
        double simVx = game.getPower() * Math.cos(rad);
        double simVy = -game.getPower() * Math.sin(rad);
        double simX = BasketballGame.BALL_START_X + BasketballGame.BALL_DRAW_RADIUS;
        double simY = BasketballGame.BALL_START_Y + BasketballGame.BALL_DRAW_RADIUS;

        boolean isShort = game.isShortTrajectory();
        boolean isExtreme = game.isExtremeMode();
        double hardCutoffX = (BasketballGame.BALL_START_X + BasketballGame.HOOP_X) / 2.0 + 25.0;

        int maxSteps = isShort ? 35 : 220;

        for (int step = 1; step <= maxSteps; step++) {
            simX += simVx;
            simY += simVy;
            simVy += BasketballGame.GRAVITY;

            double drawX = centerOffsetX + simX;
            double drawY = bottomShiftY + simY;

            if (isShort && simX > hardCutoffX) {
                break;
            }
            if (!isShort && (drawY + BasketballGame.BALL_DRAW_RADIUS >= logicalH || drawX > logicalW)) {
                break;
            }

            int alpha;
            int dotSize;

            if (isShort) {
                double progress = (simX - (BasketballGame.BALL_START_X + BasketballGame.BALL_DRAW_RADIUS))
                        / Math.max(1.0, (hardCutoffX - (BasketballGame.BALL_START_X + BasketballGame.BALL_DRAW_RADIUS)));
                alpha = (int) Math.max(150, 255 - progress * 95);
                dotSize = (int) Math.max(6, 10 - progress * 3);
            } else {
                alpha = Math.max(45, 240 - step * 2);
                dotSize = Math.max(4, 9 - step / 18);
            }

            Color dotColor;
            if (isExtreme) {
                dotColor = new Color(156, 39, 176, alpha);
            } else if (isShort) {
                dotColor = new Color(220, 53, 69, alpha);
            } else {
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
                g2.fillRect(drawHoopX + 18, drawHoopY + 36, 56, 10);
            }

            int choiceX = drawHoopX + BasketballGame.HOOP_W + 6;
            int choiceY = drawHoopY + 26;
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(choiceX, choiceY, 128, 42, 12, 12);
            g2.setColor(BORDER_BLACK);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(choiceX, choiceY, 128, 42, 12, 12);

            g2.setColor(Color.BLACK);
            g2.setFont(new Font("맑은 고딕", Font.BOLD, 16));
            FontMetrics fm = g2.getFontMetrics();
            int textX = choiceX + Math.max(10, (128 - fm.stringWidth(choices[i])) / 2);
            g2.drawString(choices[i], textX, choiceY + 27);
        }
    }

    private void drawNetFrontOverlay(Graphics2D g2, double centerOffsetX, double bottomShiftY) {
        double ballCenterY = game.getBallY() + BasketballGame.BALL_DRAW_RADIUS;
        for (int i = 0; i < 3; i++) {
            double rimY = BasketballGame.HOOP_Y[i] + 41.0;
            if (ballCenterY >= rimY - 5 && ballCenterY <= rimY + 56) {
                int drawHoopX = (int) (centerOffsetX + BasketballGame.HOOP_X);
                int drawHoopY = (int) (bottomShiftY + BasketballGame.HOOP_Y[i]);

                Shape oldClip = g2.getClip();
                g2.clipRect(drawHoopX + 16, drawHoopY + 38, 62, 52);
                Composite oldComp = g2.getComposite();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
                if (hoopImg != null && hoopImg.getWidth(null) > 0) {
                    g2.drawImage(hoopImg, drawHoopX, drawHoopY, BasketballGame.HOOP_W, BasketballGame.HOOP_H, this);
                }
                g2.setComposite(oldComp);
                g2.setClip(oldClip);
            }
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

        Color barColor;
        if (game.isExtremeMode()) {
            barColor = new Color(156, 39, 176);
        } else if (game.getDifficulty() == BasketballGame.Difficulty.HARD) {
            barColor = new Color(229, 57, 53);
        } else {
            barColor = new Color(66, 165, 245);
        }
        g2.setColor(barColor);

        int barW = (int) (((game.getPower() - 12.0) / 16.0) * 232);
        g2.fillRoundRect(infoX + 14, infoY + 29, barW, 10, 6, 6);
        g2.setColor(BORDER_BLACK);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(infoX + 14, infoY + 29, 232, 10, 6, 6);
    }

    private void drawBall(Graphics2D g2, double centerOffsetX, double bottomShiftY) {
        int drawBallX = (int) (centerOffsetX + game.getBallX());
        int drawBallY = (int) (bottomShiftY + game.getBallY());
        int size = (int) BasketballGame.BALL_SIZE;

        if (ballImg != null && ballImg.getWidth(null) > 0) {
            g2.drawImage(ballImg, drawBallX, drawBallY, size, size, this);
        } else {
            g2.setColor(new Color(255, 111, 0));
            g2.fillOval(drawBallX, drawBallY, size, size);
        }
    }

    private void drawHitboxDebug(Graphics2D g2, double centerOffsetX, double bottomShiftY) {
        Stroke oldStroke = g2.getStroke();
        g2.setStroke(new BasicStroke(1.5f));

        double ballCx = centerOffsetX + game.getBallX() + BasketballGame.BALL_DRAW_RADIUS;
        double ballCy = bottomShiftY + game.getBallY() + BasketballGame.BALL_DRAW_RADIUS;
        double hitR = BasketballGame.BALL_HIT_RADIUS;
        g2.setColor(new Color(0, 200, 83, 210));
        g2.drawOval((int) (ballCx - hitR), (int) (ballCy - hitR), (int) (hitR * 2), (int) (hitR * 2));
        g2.fillOval((int) (ballCx - 2), (int) (ballCy - 2), 4, 4);

        for (int i = 0; i < 3; i++) {
            double hx = centerOffsetX + BasketballGame.HOOP_X;
            double hy = bottomShiftY + BasketballGame.HOOP_Y[i];

            if (game.isExtremeMode()) {
                g2.setColor(new Color(33, 150, 243, 190));
                g2.drawRect((int) (hx + 79), (int) (hy + 10), 20, 54);

                double rimY = hy + 52.0;
                double frontRimX = hx + 24.0;
                double backRimX = hx + 74.0;
                double rimR = 2.7;

                g2.setColor(new Color(244, 67, 54, 220));
                g2.fillOval((int) (frontRimX - rimR), (int) (rimY - rimR), (int) (rimR * 2), (int) (rimR * 2));
                g2.fillOval((int) (backRimX - rimR), (int) (rimY - rimR), (int) (rimR * 2), (int) (rimR * 2));

                g2.setColor(new Color(255, 193, 7, 220));
                g2.drawLine((int) (frontRimX + 2), (int) rimY, (int) (backRimX - 2), (int) rimY);
            } else {
                g2.setColor(new Color(255, 152, 0, 190));
                g2.drawRect((int) (hx + 20), (int) (hy + 24), 65, 36);
            }
        }

        g2.setStroke(oldStroke);
    }
}
