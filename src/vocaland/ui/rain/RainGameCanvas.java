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
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import vocaland.ui.components.RoundedBorder;
import vocaland.ui.components.RoundedPanel;
import vocaland.ui.theme.Theme;
import vocaland.ui.util.ImageAssets;
import vocaland.ui.util.ImagePaths;
import vocaland.ui.util.ImageScaler;

/*
 * RainGameCanvas - 단어가 떨어지는 영역 (그리기와 영역 안 배치 담당)
 *
 * setOverlay()     : 가운데에 띄울 안내창 등록 (시작, 게임 오버, 오답 노트)
 * setInput()       : 바닥 가운데에 놓을 입력칸 등록
 * addPopup()       : 단어 위치에 잠깐 떴다 사라지는 글자 추가 (정답 점수, 놓침 등)
 * updatePopups()   : 떠 있는 글자의 남은 시간 줄이기
 * addSplash()      : 맞힌 단어 자리에서 물방울이 튀고 물결이 퍼지는 효과
 * showLevelUp()    : 레벨업 효과 (가운데 LEVEL UP 글자가 커졌다 자리 잡기 + 노란 반짝이)
 * renderBanner()   : LEVEL UP 글자를 크기별 그림으로 미리 만들어 둠 (매 프레임 글꼴 생성·확대 방지)
 * dotSprite()      : 물방울·반짝이 동그라미를 색·크기별 그림으로 한 번만 만들어 재사용
 * warmUp()         : 효과 그리기를 화면 밖에서 한 번 미리 실행 (첫 레벨업 끊김 방지)
 * setCharacter()   : 바닥 오른쪽에 서 있을 캐릭터 이미지 설정 (여백 자르고 원본을 적당히 줄여 보관, null이면 기본 기룡이)
 * reactCorrect() / reactLevelUp() / reactMiss() : 캐릭터가 깡충 / 높이 점프 / 부르르 떨기
 * clearPopups()    : 떠 있는 글자와 효과 모두 지우기
 * getAreaWidth()   : 게임 영역 가로 길이 (테두리 제외)
 * getGroundY()     : 바닥의 y좌표
 * doLayout()       : 안내창은 가운데, 입력칸은 바닥 가운데에 배치
 * paintComponent() : 배경 → 단어(게임 중에만) → 바닥 → 떠 있는 글자 → 레벨 순서로 그리기
 * drawSky()        : 게임 배경 이미지 (영역 크기가 바뀔 때만 다시 줄여 둠, 없으면 그라데이션)
 * drawWord()       : 빗줄기 + 같은 크기의 단어 박스 (하트 단어는 빨간 네모 + 흰 글씨)
 * fitFont()        : 박스에 안 들어가는 긴 단어는 글자 크기를 줄여서 맞추기
 * drawGround()     : 산성비가 고인 바닥
 * drawPopups()     : 위로 올라가며 흐려지는 글자
 * drawEffects()    : 물방울, 물결, 레벨업 효과 그리기
 * drawCharacter()  : 캐릭터 그리기 (게임 영역 높이에 맞춘 크기, 크기가 바뀔 때만 다시 줄임, 단어보다 뒤)
 * drawLevel()      : 왼쪽 위에 현재 레벨 (반투명 배경)
 */
public class RainGameCanvas extends RoundedPanel {
    public static final int GROUND_HEIGHT = 64;

    private static final Color SKY_TOP = Color.WHITE;
    private static final Color SKY_BOTTOM = Theme.PRIMARY_TINT;
    private static final Color RAIN_LINE = new Color(120, 140, 160);
    private static final Color HEART_FILL = new Color(220, 50, 60);
    private static final Color ACID = new Color(181, 222, 103, 200);
    private static final Color LEVEL_BACK = new Color(255, 255, 255, 235);
    private static final Font WORD_FONT = new Font(Font.DIALOG, Font.BOLD, 34);
    private static final int MIN_WORD_SIZE = 16;
    private static final int TEXT_PADDING = 12;
    private static final Font LEVEL_FONT = new Font(Font.DIALOG, Font.BOLD, 30);
    private static final Font POPUP_FONT = new Font(Font.DIALOG, Font.BOLD, 24);
    private static final int POPUP_MS = 900;
    private static final int POPUP_RISE = 40;
    private static final Dimension INPUT_SIZE = new Dimension(460, 50);
    private static final double GRAVITY = 0.0016;
    private static final int SPLASH_DROPS = 24;
    private static final int SPLASH_MS = 850;
    private static final int RIPPLE_MS = 600;
    private static final int RIPPLE_SIZE = 120;
    private static final int LEVEL_UP_MS = 1400;
    private static final Font LEVEL_UP_FONT = new Font(Font.DIALOG, Font.BOLD, 56);
    private static final Color LEVEL_UP_COLOR = new Color(255, 196, 0);
    private static final float[] BANNER_SCALES = {0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f, 1.1f, 1.2f};
    private static final java.util.Map<Long, BufferedImage> DOT_SPRITES = new java.util.HashMap<>();
    private static final double CHARACTER_RATIO = 0.2;
    private static final int CHARACTER_MIN = 70;
    private static final int CHARACTER_MAX = 160;
    private static final int CHARACTER_SOURCE_HEIGHT = 400;
    private static final int CHARACTER_RIGHT = 18;
    private static final int CHARACTER_SINK = 6;
    private static final int HOP_MS = 350;
    private static final int HOP_HEIGHT = 18;
    private static final int JUMP_MS = 650;
    private static final int JUMP_HEIGHT = 46;
    private static final int SHAKE_MS = 400;

    private final RainGame game;
    private final ImageIcon rainBackground = new ImageIcon(ImagePaths.ROOT + ImagePaths.GAME_RAIN_BACKGROUND);
    private BufferedImage skyCache;
    private final List<Popup> popups = new ArrayList<>();
    private final List<Drop> drops = new ArrayList<>();
    private final List<Ripple> ripples = new ArrayList<>();
    private final Random random = new Random();
    private int levelUpLeftMs;
    private BufferedImage[] levelUpBanners;
    private BufferedImage characterSource;
    private double characterCorrection = 1.0;
    private BufferedImage character;
    private String characterPath;
    private int hopLeftMs;
    private int hopTotalMs = HOP_MS;
    private int hopHeight = HOP_HEIGHT;
    private int shakeLeftMs;
    private JComponent overlay;
    private JComponent input;

    public RainGameCanvas(RainGame game) {
        this.game = game;
        setLayout(null);
        setBorder(new RoundedBorder(new Color(181, 222, 103), 2));
        warmUp();
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

    public void addSplash(double x, double y, Color color) {
        for (int i = 0; i < SPLASH_DROPS; i++) {
            double angle = Math.PI + random.nextDouble() * Math.PI;
            double speed = 0.25 + random.nextDouble() * 0.45;
            drops.add(new Drop(x, y, Math.cos(angle) * speed, Math.sin(angle) * speed,
                    8 + random.nextInt(9), color, SPLASH_MS));
        }
        ripples.add(new Ripple(x, y, color, RIPPLE_MS, RIPPLE_SIZE));
    }

    public void setCharacter(String path) {
        String target = path == null ? ImagePaths.ROOT + ImagePaths.STORE_CHARACTER_GIRYONG_BASIC : path;
        if (target.equals(characterPath) && characterSource != null) {
            return;
        }
        try {
            BufferedImage image = ImageAssets.read(target);
            if (image == null && path != null) {
                setCharacter(null);
                return;
            }
            if (image != null) {
                image = ImageAssets.trim(image, 64);
                int h = Math.min(CHARACTER_SOURCE_HEIGHT, image.getHeight());
                int w = (int) Math.round((double) image.getWidth() * h / image.getHeight());
                characterSource = ImageScaler.scale(image, w, h);
                characterCorrection = ImageAssets.characterScale(target);
                character = null;
                characterPath = target;
            }
        } catch (java.io.IOException e) {
            characterSource = null;
            character = null;
        }
        repaint();
    }

    public void reactCorrect() {
        if (hopLeftMs <= 0 || hopTotalMs != JUMP_MS) {
            hopTotalMs = HOP_MS;
            hopHeight = HOP_HEIGHT;
            hopLeftMs = HOP_MS;
        }
    }

    public void reactLevelUp() {
        hopTotalMs = JUMP_MS;
        hopHeight = JUMP_HEIGHT;
        hopLeftMs = JUMP_MS;
    }

    public void reactMiss() {
        shakeLeftMs = SHAKE_MS;
    }

    public void showLevelUp(int level) {
        levelUpBanners = renderBanners("LEVEL UP!  Lv." + level);
        levelUpLeftMs = LEVEL_UP_MS;
        double cx = getAreaWidth() / 2.0;
        double cy = getGroundY() / 2.0;
        for (int i = 0; i < 20; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 0.15 + random.nextDouble() * 0.35;
            drops.add(new Drop(cx, cy, Math.cos(angle) * speed, Math.sin(angle) * speed,
                    5 + random.nextInt(6), LEVEL_UP_COLOR, LEVEL_UP_MS - 300));
        }
    }

    public void updatePopups(int elapsedMs) {
        Iterator<Drop> d = drops.iterator();
        while (d.hasNext()) {
            Drop drop = d.next();
            drop.leftMs -= elapsedMs;
            drop.vy += GRAVITY * elapsedMs;
            drop.x += drop.vx * elapsedMs;
            drop.y += drop.vy * elapsedMs;
            if (drop.leftMs <= 0) {
                d.remove();
            }
        }
        ripples.removeIf(r -> (r.leftMs -= elapsedMs) <= 0);
        levelUpLeftMs = Math.max(0, levelUpLeftMs - elapsedMs);
        hopLeftMs = Math.max(0, hopLeftMs - elapsedMs);
        shakeLeftMs = Math.max(0, shakeLeftMs - elapsedMs);

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
        drops.clear();
        ripples.clear();
        levelUpLeftMs = 0;
        hopLeftMs = 0;
        shakeLeftMs = 0;
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
            drawCharacter(g, width, groundY);
            if (game.getState() == RainGame.State.PLAYING) {
                for (FallingWord word : game.getFallingWords()) {
                    drawWord(g, word);
                }
            }
            drawGround(g, width, groundY);
            drawEffects(g, width, groundY);
            drawPopups(g);
            drawLevel(g);
        } finally {
            g.dispose();
        }
    }

    private void drawSky(Graphics2D g, int width, int groundY) {
        int height = groundY + GROUND_HEIGHT;
        if (width <= 0 || height <= 0) {
            return;
        }
        if (rainBackground.getIconWidth() > 0 && rainBackground.getIconHeight() > 0) {
            double scale = Math.max((double) width / rainBackground.getIconWidth(),
                    (double) height / rainBackground.getIconHeight());
            int imageWidth = (int) Math.ceil(rainBackground.getIconWidth() * scale);
            int imageHeight = (int) Math.ceil(rainBackground.getIconHeight() * scale);
            if (skyCache == null || skyCache.getWidth() != imageWidth || skyCache.getHeight() != imageHeight) {
                skyCache = ImageScaler.scale(rainBackground.getImage(), imageWidth, imageHeight);
            }
            if (skyCache != null) {
                g.drawImage(skyCache, (width - imageWidth) / 2, (height - imageHeight) / 2, null);
                return;
            }
        }
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

    private void drawCharacter(Graphics2D g, int width, int groundY) {
        if (characterSource == null) {
            return;
        }
        int h = (int) Math.round(groundY * CHARACTER_RATIO * characterCorrection);
        h = Math.max(CHARACTER_MIN, Math.min(CHARACTER_MAX, h));
        if (character == null || character.getHeight() != h) {
            int w = (int) Math.round((double) characterSource.getWidth() * h / characterSource.getHeight());
            character = ImageScaler.scale(characterSource, w, h);
            if (character == null) {
                return;
            }
        }
        int x = width - CHARACTER_RIGHT - character.getWidth();
        int y = groundY + CHARACTER_SINK - character.getHeight();
        if (hopLeftMs > 0) {
            double t = 1.0 - (double) hopLeftMs / hopTotalMs;
            y -= (int) Math.round(Math.sin(Math.PI * t) * hopHeight);
        }
        if (shakeLeftMs > 0) {
            double t = 1.0 - (double) shakeLeftMs / SHAKE_MS;
            x += (int) Math.round(Math.sin(t * Math.PI * 8) * 7 * (1.0 - t));
        }
        g.drawImage(character, x, y, null);
    }

    private void drawEffects(Graphics2D g, int width, int groundY) {
        for (Ripple r : ripples) {
            float progress = 1f - (float) r.leftMs / r.totalMs;
            int radius = (int) Math.round(r.maxRadius * progress);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f * (1f - progress)));
            g.setColor(r.color);
            g.setStroke(new BasicStroke(4f * (1f - progress) + 1f));
            g.drawOval((int) r.x - radius, (int) r.y - radius / 2, radius * 2, radius);
        }
        for (Drop drop : drops) {
            float alpha = Math.max(0f, Math.min(1f, (float) drop.leftMs / drop.totalMs));
            BufferedImage dot = dotSprite(drop.color, drop.size);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g.drawImage(dot, (int) drop.x - dot.getWidth() / 2, (int) drop.y - dot.getHeight() / 2, null);
        }
        if (levelUpLeftMs > 0 && levelUpBanners != null) {
            float progress = 1f - (float) levelUpLeftMs / LEVEL_UP_MS;
            float scale = progress < 0.15f ? 0.5f + progress / 0.15f * 0.7f
                    : progress < 0.25f ? 1.2f - (progress - 0.15f) / 0.1f * 0.2f : 1f;
            float alpha = progress < 0.75f ? 1f : 1f - (progress - 0.75f) / 0.25f;
            BufferedImage banner = levelUpBanners[nearestScale(scale)];
            int bx = (width - banner.getWidth()) / 2;
            int by = (groundY - banner.getHeight()) / 2 - (int) (20 * progress);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g.drawImage(banner, bx, by, null);
        }
        g.setComposite(AlphaComposite.SrcOver);
    }

    private static void warmUp() {
        renderBanners("LEVEL UP!  Lv.0123456789");
    }

    private static int nearestScale(float scale) {
        int best = 0;
        for (int i = 1; i < BANNER_SCALES.length; i++) {
            if (Math.abs(BANNER_SCALES[i] - scale) < Math.abs(BANNER_SCALES[best] - scale)) {
                best = i;
            }
        }
        return best;
    }

    private static BufferedImage[] renderBanners(String text) {
        BufferedImage[] banners = new BufferedImage[BANNER_SCALES.length];
        for (int i = 0; i < BANNER_SCALES.length; i++) {
            banners[i] = renderBanner(text, LEVEL_UP_FONT.deriveFont(LEVEL_UP_FONT.getSize2D() * BANNER_SCALES[i]));
        }
        return banners;
    }

    private static BufferedImage renderBanner(String text, Font font) {
        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D pg = probe.createGraphics();
        FontMetrics fm = pg.getFontMetrics(font);
        int w = fm.stringWidth(text) + 8;
        int h = fm.getHeight() + 8;
        pg.dispose();

        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(font);
        g.setColor(Theme.TEXT);
        g.drawString(text, 4, 4 + fm.getAscent());
        g.setColor(LEVEL_UP_COLOR);
        g.drawString(text, 0, fm.getAscent());
        g.dispose();
        return image;
    }

    private static BufferedImage dotSprite(Color color, int size) {
        long key = ((long) color.getRGB() << 8) | size;
        BufferedImage sprite = DOT_SPRITES.get(key);
        if (sprite == null) {
            int h = (int) (size * 1.3);
            sprite = new BufferedImage(size + 2, h + 2, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = sprite.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(color);
            g.fillOval(1, 1, size, h);
            g.dispose();
            DOT_SPRITES.put(key, sprite);
        }
        return sprite;
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

    private static class Drop {
        private double x;
        private double y;
        private final double vx;
        private double vy;
        private final int size;
        private final Color color;
        private final int totalMs;
        private int leftMs;

        Drop(double x, double y, double vx, double vy, int size, Color color, int ms) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.color = color;
            this.totalMs = ms;
            this.leftMs = ms;
        }
    }

    private static class Ripple {
        private final double x;
        private final double y;
        private final Color color;
        private final int totalMs;
        private final double maxRadius;
        private int leftMs;

        Ripple(double x, double y, Color color, int ms, double maxRadius) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.totalMs = ms;
            this.maxRadius = maxRadius;
            this.leftMs = ms;
        }
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
