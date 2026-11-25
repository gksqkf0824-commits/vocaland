package vocaland.ui.basketball;

import vocaland.ui.util.ImagePaths;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/*
 * BasketballCanvas - 농구 게임 코트 영역 (그리기만 담당)
 *
 * setBallImage() / setHoopImage() : 상점에서 구매·장착한 공과 골대 스킨 이미지 동적 교체
 * createBackground()       : 창 크기에 맞춘 배경 이미지 리사이징 및 캐싱(BufferedImage) 생성
 * paintComponent()         : 배경 → 제시 단어 → 예상 포물선 → 골대 3개 → 게이지 → 농구공 → 그물 앞레이어 → 연출 → 판정 디버그 순서로 그리기
 * drawQuestionCard()       : 좌상단 제시 단어 박스 그리기
 * drawTrajectoryGuide()    : 난이도별 예상 포물선 점선 그리기 (이지: 바닥까지 전체 / 하드: 중간까지만 / 익스퍼트: 보라색 통일)
 * drawHoopsAndChoices()    : 오른쪽 골대 3개와 선택지 박스 그리기
 * drawNetFrontOverlay()    : 익스퍼트 모드 골인 시 공이 그물 안으로 들어가는 입체감을 위한 그물 앞부분 오버레이 그리기
 * drawPowerInfoBox()       : 하단 각도 및 힘 게이지 정보창 그리기
 * drawBall()               : 농구공 스프라이트 그리기
 * drawFireworksEffect()    : 정답 골인 시 골대 주변 폭죽 및 NICE SHOT! 연출 그리기
 * drawScreenOverlayEffect(): 실패 시 화면 전체 오버레이 연출 분기 ('아닙니다!', '저런!', 'HOME RUN')
 * drawSlantedAlertText()   : 대각선 거대 빨간색 '아닙니다!' / '저런!' 텍스트 팝업 그리기
 * drawHomeRunEffect()      : 골대 넘어감 시 가로 그라데이션 띠 + 3단계 가감속 HOME RUN 연출 그리기
 * drawHitboxDebug()        : 골대(백보드, 림, 통과선) 및 농구공 충돌 판정 영역 시각화 그리기
 * drawComboFireEffect()    : 황금 세트 콤보 활성화 시 하단 콤보 배지 주변에 일렁이는 불꽃 파티클 렌더링
 */
public class BasketballCanvas extends vocaland.ui.components.RoundedPanel {

    private static final Color GRADIENT_TOP = Color.WHITE;
    private static final Color GRADIENT_BOTTOM = new Color(236, 247, 226);
    private static final Color BORDER_BLACK = Color.BLACK;
    private static final boolean SHOW_HITBOX_DEBUG = false;

    private static final Color[] FIREWORK_COLORS = {
            new Color(255, 82, 82),
            new Color(255, 193, 7),
            new Color(76, 175, 80),
            new Color(33, 150, 243),
            new Color(171, 71, 188),
            new Color(255, 112, 67)
    };

    private static final Color[] GOLD_FIREWORK_COLORS = {
            new Color(255, 245, 157),
            new Color(255, 193, 7),
            new Color(255, 143, 0)
    };

    private static final Font FONT_QUESTION = new Font("맑은 고딕", Font.BOLD, 24);
    private static final Font FONT_CHOICE = new Font("맑은 고딕", Font.BOLD, 16);
    private static final Font FONT_INFO = new Font("맑은 고딕", Font.BOLD, 13);
    private static final Font FONT_COMBO = new Font("맑은 고딕", Font.BOLD, 15);
    private static final Font FONT_FIREWORK_NORMAL = new Font("맑은 고딕", Font.BOLD, 24);
    private static final Font FONT_FIREWORK_GOLD = new Font("맑은 고딕", Font.BOLD, 27);
    private static final Font FONT_ALERT_BASE = new Font("맑은 고딕", Font.BOLD, 48);
    private static final Font FONT_HOMERUN_BASE = new Font("Arial Black", Font.BOLD, 48);

    private final BasketballGame game;
    private final Image courtBackground = new ImageIcon(
            ImagePaths.ROOT + ImagePaths.GAME_BASKETBALL_BACKGROUND).getImage();
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
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int curW = getWidth();
        int curH = getHeight();

        if (curW <= 0 || curH <= 0) {
            g2.dispose();
            return;
        }

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

        Graphics2D worldG = (Graphics2D) g2.create();
        worldG.scale(scale, scale);

        double centerOffsetX = Math.max(0.0, (logicalW - BasketballGame.BASE_W) / 2.0);
        double bottomShiftY = Math.max(0.0, logicalH - BasketballGame.BASE_H);

        drawQuestionCard(worldG, centerOffsetX);

        if (!game.isShooting() && !game.isEffectPlaying() && game.getState() == BasketballGame.State.PLAYING) {
            drawTrajectoryGuide(worldG, centerOffsetX, bottomShiftY, logicalW, logicalH);
        }

        drawHoopsAndChoices(worldG, centerOffsetX, bottomShiftY);
        drawPowerInfoBox(worldG, centerOffsetX, bottomShiftY);

        if (!game.isBallHidden()) {
            drawBall(worldG, centerOffsetX, bottomShiftY);
        }

        if (game.isExpertMode() && game.isScoredThisShot() && !game.isBallHidden()) {
            drawNetFrontOverlay(worldG, centerOffsetX, bottomShiftY);
        }

        if (game.getActiveEffect() == BasketballGame.EffectType.FIREWORKS) {
            drawFireworksEffect(worldG, centerOffsetX, bottomShiftY);
        }

        if (SHOW_HITBOX_DEBUG) {
            drawHitboxDebug(worldG, centerOffsetX, bottomShiftY);
        }

        if (game.getState() == BasketballGame.State.PLAYING) {
            boolean showEasyNotice = game.isExpertMode() && !game.isExpertHardTrajectory();
            if (showEasyNotice) {
                worldG.setFont(FONT_INFO);
                worldG.setColor(Color.BLACK);
                String easyNotice = "이지 버전은 정답 시 120점을 획득합니다.";
                FontMetrics efm = worldG.getFontMetrics();
                int nx = (int) (centerOffsetX + BasketballGame.BASE_W - efm.stringWidth(easyNotice) - 22);
                int ny = (int) (bottomShiftY + BasketballGame.BASE_H - 18);
                worldG.drawString(easyNotice, nx, ny);
            }

            double comboFadeAlpha = 0.0;
            double displayComboLevel = 0.0;
            if (game.isGoldComboActive()) {
                comboFadeAlpha = 1.0;
                displayComboLevel = game.getGoldComboCount();
            } else if (game.getLastBrokenGoldCombo() > 0 && game.getComboFadeOutStartMs() > 0L) {
                long elapsed = System.currentTimeMillis() - game.getComboFadeOutStartMs();
                long fadeDurationMs = 550L;
                if (elapsed < fadeDurationMs) {
                    double remain = 1.0 - ((double) elapsed / fadeDurationMs);
                    comboFadeAlpha = remain;
                    displayComboLevel = game.getLastBrokenGoldCombo() * remain;
                }
            }

            if (comboFadeAlpha > 0.01) {
                worldG.setFont(FONT_COMBO);
                FontMetrics cfm = worldG.getFontMetrics();
                String comboTxt = "콤보 활성화";
                int padX = 16;
                int boxW = cfm.stringWidth(comboTxt) + padX * 2;
                int boxH = 34;
                int bx = (int) (centerOffsetX + BasketballGame.BASE_W - boxW - 22);
                int by = (int) (bottomShiftY + BasketballGame.BASE_H - boxH - (showEasyNotice ? 42 : 18));

                Composite oldComp = worldG.getComposite();
                worldG.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) comboFadeAlpha));

                drawComboFireEffect(worldG, bx, by, boxW, boxH, displayComboLevel);

                worldG.setColor(new Color(120, 75, 0, 95));
                worldG.fillRoundRect(bx + 1, by + 3, boxW, boxH, 16, 16);

                float[] dist = {0.0f, 0.5f, 1.0f};
                Color[] cols = {
                        new Color(255, 224, 102),
                        new Color(235, 165, 15),
                        new Color(185, 115, 0)
                };
                worldG.setPaint(new LinearGradientPaint(bx, by, bx + boxW, by + boxH, dist, cols));
                worldG.fillRoundRect(bx, by, boxW, boxH, 16, 16);

                worldG.setColor(new Color(145, 90, 0));
                worldG.setStroke(new BasicStroke(1.8f));
                worldG.drawRoundRect(bx, by, boxW, boxH, 16, 16);

                int tx = bx + (boxW - cfm.stringWidth(comboTxt)) / 2;
                int ty = by + ((boxH - cfm.getHeight()) / 2) + cfm.getAscent();
                worldG.setColor(new Color(110, 65, 0, 170));
                worldG.drawString(comboTxt, tx + 1, ty + 1);
                worldG.setColor(Color.WHITE);
                worldG.drawString(comboTxt, tx, ty);

                worldG.setComposite(oldComp);
            }
        }

        worldG.dispose();

        if (game.isEffectPlaying() && game.getActiveEffect() != BasketballGame.EffectType.FIREWORKS) {
            drawScreenOverlayEffect(g2, curW, curH);
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
        g2.setFont(FONT_QUESTION);
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
        boolean isExpert = game.isExpertMode();
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
            if (isExpert) {
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
                g2.drawImage(hoopImg, drawHoopX, drawHoopY, BasketballGame.HOOP_W, BasketballGame.HOOP_H, null);
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
            g2.setFont(FONT_CHOICE);
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
                    g2.drawImage(hoopImg, drawHoopX, drawHoopY, BasketballGame.HOOP_W, BasketballGame.HOOP_H, null);
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
        g2.setFont(FONT_INFO);
        g2.drawString(String.format("각도: %.0f° (↑↓)", game.getAngleDeg()), infoX + 14, infoY + 20);
        double powerRatio = (game.getPower() - BasketballGame.MIN_POWER) / (BasketballGame.MAX_POWER - BasketballGame.MIN_POWER);
        double powerPct = powerRatio * 100;
        g2.drawString(String.format("힘: %.0f%% (←→)", powerPct), infoX + 135, infoY + 20);

        g2.setColor(new Color(230, 230, 230));
        g2.fillRoundRect(infoX + 14, infoY + 29, 232, 10, 6, 6);

        Color barColor;
        if (game.isExpertMode()) {
            barColor = new Color(156, 39, 176);
        } else if (game.getDifficulty() == BasketballGame.Difficulty.HARD) {
            barColor = new Color(229, 57, 53);
        } else {
            barColor = new Color(66, 165, 245);
        }
        g2.setColor(barColor);

        int barW = (int) (powerRatio * 232);
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
            g2.drawImage(ballImg, drawBallX, drawBallY, size, size, null);
        } else {
            g2.setColor(new Color(255, 111, 0));
            g2.fillOval(drawBallX, drawBallY, size, size);
        }
    }

    private void drawFireworksEffect(Graphics2D g2, double centerOffsetX, double bottomShiftY) {
        double t = game.getEffectProgress();
        double baseX = centerOffsetX + game.getFireworkX();
        double baseY = bottomShiftY + game.getFireworkY();
        boolean isGold = (game.getSetEffect() == BasketballGame.SetEffect.GOLD);

        drawSingleBurst(g2, baseX, baseY, t, isGold ? 34 : 24, isGold ? 140.0 : 115.0, 0, isGold);
        if (t > 0.14) {
            double t2 = (t - 0.14) / 0.86;
            drawSingleBurst(g2, baseX - 42, baseY - 26, t2, isGold ? 22 : 14, isGold ? 95.0 : 80.0, 2, isGold);
            drawSingleBurst(g2, baseX + 44, baseY - 18, t2, isGold ? 22 : 14, isGold ? 95.0 : 80.0, 4, isGold);
        }

        int textAlpha = (int) (255 * Math.min(1.0, (1.0 - t) * 1.6));
        int liftY = (int) (baseY - 38 - t * 34);
        g2.setFont(isGold ? FONT_FIREWORK_GOLD : FONT_FIREWORK_NORMAL);
        FontMetrics fm = g2.getFontMetrics();
        String msg = "+ " + game.getLastEarnedPoints();
        int tx = (int) (baseX - fm.stringWidth(msg) / 2.0);

        g2.setColor(new Color(255, 255, 255, textAlpha));
        g2.drawString(msg, tx + 2, liftY + 2);
        g2.setColor(isGold ? new Color(218, 145, 0, textAlpha) : new Color(46, 125, 50, textAlpha));
        g2.drawString(msg, tx, liftY);
    }

    private void drawSingleBurst(Graphics2D g2, double cx, double cy, double t, int count, double maxRadius, int colorOffset, boolean isGold) {
        double eased = 1.0 - Math.pow(1.0 - t, 2.3);
        double radius = maxRadius * eased;
        double gravityDrop = 26.0 * t * t;
        float alphaFloat = (float) Math.max(0.0, Math.min(1.0, 1.0 - Math.pow(t, 1.8)));
        if (alphaFloat <= 0.01f) return;

        Composite oldComp = g2.getComposite();
        AlphaComposite lineComp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alphaFloat * 0.5f);
        AlphaComposite dotComp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alphaFloat);

        g2.setStroke(new BasicStroke(2.0f));
        int dotSize = (int) Math.max(3, (isGold ? 9 : 7) * (1.0 - t * 0.5));

        for (int i = 0; i < count; i++) {
            double angle = (2.0 * Math.PI * i) / count + (colorOffset * 0.25);
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double px = cx + cos * radius;
            double py = cy + sin * radius + gravityDrop;

            double tailRadius = Math.max(0, radius - 14.0 * (1.0 - t));
            double tx = cx + cos * tailRadius;
            double ty = cy + sin * tailRadius + gravityDrop * 0.85;

            Color c = isGold
                    ? GOLD_FIREWORK_COLORS[i % 3]
                    : FIREWORK_COLORS[(i + colorOffset) % FIREWORK_COLORS.length];

            g2.setColor(c);
            g2.setComposite(lineComp);
            g2.drawLine((int) tx, (int) ty, (int) px, (int) py);

            g2.setComposite(dotComp);
            g2.fillOval((int) (px - dotSize / 2.0), (int) (py - dotSize / 2.0), dotSize, dotSize);
        }
        g2.setComposite(oldComp);
    }

    private void drawScreenOverlayEffect(Graphics2D g2, int curW, int curH) {
        BasketballGame.EffectType type = game.getActiveEffect();
        if (type == BasketballGame.EffectType.WRONG_HOOP) {
            drawSlantedAlertText(g2, curW, curH, "아닙니다!", -12.0, 0.20, false);
        } else if (type == BasketballGame.EffectType.MISS_DROP) {
            drawSlantedAlertText(g2, curW, curH, "저런!", -10.0, 0.16, false);
        } else if (type == BasketballGame.EffectType.HOME_RUN) {
            drawHomeRunEffect(g2, curW, curH);
        }

        if (game.isLastMissBlockedByNeon()) {
            drawSlantedAlertText(g2, curW, curH, "방어 성공!", 11.0, 0.20, true);
        }
    }

    private void drawSlantedAlertText(Graphics2D g2, int curW, int curH, String message, double angleDeg, double widthRatio, boolean isNeonShieldOverlay) {
        double t = game.getEffectProgress();
        if (isNeonShieldOverlay && t < 0.24) {
            return;
        }

        double localT = isNeonShieldOverlay ? ((t - 0.24) / 0.76) : t;
        double popScale = (localT < 0.22) ? (0.35 + (localT / 0.22) * 0.78)
                : (localT < 0.35 ? 1.13 - ((localT - 0.22) / 0.13) * 0.13 : 1.0);
        int alpha = (localT > 0.72) ? (int) (255 * ((1.0 - localT) / 0.28)) : 255;
        alpha = Math.max(0, Math.min(255, alpha));

        int baseFontSize = Math.max(48, (int) (curW * widthRatio * popScale));

        AffineTransform oldTx = g2.getTransform();
        g2.translate(curW / 2.0, curH / 2.0);
        g2.rotate(Math.toRadians(angleDeg));

        g2.setFont(FONT_ALERT_BASE.deriveFont((float) baseFontSize));
        FontMetrics fm = g2.getFontMetrics();
        int textX = -fm.stringWidth(message) / 2;
        int textY = (fm.getAscent() - fm.getDescent()) / 2;

        g2.setColor(new Color(0, 0, 0, (int) (alpha * 0.42)));
        g2.drawString(message, textX + 5, textY + 5);

        g2.setColor(new Color(255, 255, 255, alpha));
        g2.drawString(message, textX - 2, textY - 2);

        if (isNeonShieldOverlay) {
            float[] dist = {0.0f, 0.52f, 1.0f};
            Color[] cols = {
                    new Color(0, 229, 255, alpha),
                    new Color(26, 35, 126, alpha),
                    new Color(49, 27, 146, alpha)
            };
            g2.setPaint(new LinearGradientPaint(textX, textY - fm.getAscent(),
                    textX + fm.stringWidth(message), textY + 6, dist, cols));
        } else {
            g2.setColor(new Color(229, 28, 35, alpha));
        }
        g2.drawString(message, textX, textY);

        g2.setTransform(oldTx);
    }

    private void drawHomeRunEffect(Graphics2D g2, int curW, int curH) {
        double t = game.getEffectProgress();
        int ribbonHeight = Math.max(62, (int) (curH * 0.15));
        int ribbonY = (curH - ribbonHeight) / 2;

        double slideT = Math.min(1.0, t / 0.20);
        int ribbonX = (int) (-curW + curW * (1.0 - Math.pow(1.0 - slideT, 2.5)));
        int ribbonAlpha = (t > 0.72) ? (int) (255 * ((1.0 - t) / 0.28)) : 255;
        ribbonAlpha = Math.max(0, Math.min(255, ribbonAlpha));

        if (ribbonAlpha > 0) {
            g2.setColor(new Color(255, 232, 140, ribbonAlpha));
            g2.fillRect(ribbonX, ribbonY, curW, ribbonHeight);
            g2.setColor(new Color(186, 104, 200, ribbonAlpha));
            g2.setStroke(new BasicStroke(3f));
            g2.drawLine(ribbonX, ribbonY, ribbonX + curW, ribbonY);
            g2.drawLine(ribbonX, ribbonY + ribbonHeight, ribbonX + curW, ribbonY + ribbonHeight);
        }

        if (t < 0.20) return;

        double textPhase = (t - 0.20) / 0.80;
        double holdSize = ribbonHeight * 1.15;
        double maxZoomSize = Math.min(curW * 0.85, ribbonHeight * 3.4);

        double fontSize;
        int textAlpha;
        if (textPhase < 0.20) {
            double p = textPhase / 0.20;
            fontSize = 16.0 + (holdSize - 16.0) * p;
            textAlpha = (int) (255 * p);
        } else if (textPhase < 0.65) {
            fontSize = holdSize * (1.0 + 0.18 * ((textPhase - 0.20) / 0.45));
            textAlpha = 255;
        } else {
            double p = (textPhase - 0.65) / 0.35;
            fontSize = holdSize * 1.18 + (maxZoomSize - holdSize * 1.18) * (p * p);
            textAlpha = (int) (255 * (1.0 - p));
        }

        textAlpha = Math.max(0, Math.min(255, textAlpha));
        g2.setFont(FONT_HOMERUN_BASE.deriveFont((float) Math.max(12, (int) fontSize)));
        FontMetrics fm = g2.getFontMetrics();
        String hrText = "HOME RUN";
        int tx = (curW - fm.stringWidth(hrText)) / 2;
        int ty = (curH + fm.getAscent() - fm.getDescent()) / 2;

        g2.setColor(new Color(0, 0, 0, textAlpha));
        g2.drawString(hrText, tx, ty);
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

            if (game.isExpertMode()) {
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
    private void drawComboFireEffect(Graphics2D g2, int bx, int by, int boxW, int boxH, double comboCount) {
        double level = Math.min(10.0, Math.max(0.0, comboCount));
        double growRatio = 0.35 + (level - 1.0) * 0.14;
        double t = System.currentTimeMillis() / 130.0;

        int flameCount = 9;
        double stepX = (double) (boxW - 14) / (flameCount - 1);

        for (int i = 0; i < flameCount; i++) {
            double normPos = (i - (flameCount - 1) / 2.0) / ((flameCount - 1) / 2.0);
            double archFactor = 1.0 - 0.32 * (normPos * normPos);

            double wave1 = Math.sin(t + i * 1.15);
            double wave2 = Math.cos(t * 1.45 - i * 0.85);

            double baseHeight = (22.0 + 36.0 * (growRatio - 0.35) / 1.26) * archFactor;
            double flameH = baseHeight + wave1 * (5.0 + level * 0.9);
            double flameW = 18.0 + level * 1.1 + wave2 * 2.5;

            double cx = bx + 7 + i * stepX + wave2 * 2.2;
            double baseY = by + boxH * 0.55;
            double tipX = cx + wave1 * (3.5 + level * 0.45);
            double tipY = by - flameH;

            java.awt.geom.Path2D outerFlame = new java.awt.geom.Path2D.Double();
            outerFlame.moveTo(cx - flameW * 0.5, baseY);
            outerFlame.curveTo(
                    cx - flameW * 0.55, by - flameH * 0.28,
                    tipX - flameW * 0.22, by - flameH * 0.72,
                    tipX, tipY
            );
            outerFlame.curveTo(
                    tipX + flameW * 0.22, by - flameH * 0.72,
                    cx + flameW * 0.55, by - flameH * 0.28,
                    cx + flameW * 0.5, baseY
            );
            outerFlame.closePath();

            int outerAlpha = (int) Math.min(135, 68 + level * 6);
            g2.setPaint(new GradientPaint(
                    (float) cx, (float) baseY, new Color(255, 111, 0, outerAlpha),
                    (float) tipX, (float) tipY, new Color(255, 213, 79, 10)
            ));
            g2.fill(outerFlame);

            double innerH = flameH * 0.62;
            double innerW = flameW * 0.54;
            double innerTipY = by - innerH;

            java.awt.geom.Path2D innerFlame = new java.awt.geom.Path2D.Double();
            innerFlame.moveTo(cx - innerW * 0.5, baseY);
            innerFlame.curveTo(
                    cx - innerW * 0.45, by - innerH * 0.25,
                    tipX - innerW * 0.18, by - innerH * 0.68,
                    tipX, innerTipY
            );
            innerFlame.curveTo(
                    tipX + innerW * 0.18, by - innerH * 0.68,
                    cx + innerW * 0.45, by - innerH * 0.25,
                    cx + innerW * 0.5, baseY
            );
            innerFlame.closePath();

            int innerAlpha = (int) Math.min(155, 80 + level * 7);
            g2.setPaint(new GradientPaint(
                    (float) cx, (float) baseY, new Color(255, 245, 157, innerAlpha),
                    (float) tipX, (float) innerTipY, new Color(255, 193, 7, 15)
            ));
            g2.fill(innerFlame);
        }
    }
}