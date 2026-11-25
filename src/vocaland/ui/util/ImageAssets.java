package vocaland.ui.util;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

/*
 * Shared image operations; callers choose trimming and character corrections.
 *
 * 성능 메모 (로그인 렉 수정)
 *  - 원본 이미지가 화면보다 10~20배 커서 읽기·자르기·줄이기에 장당 200~500ms 가 걸렸음
 *  - load() / icon() 결과를 (경로, 크기, 옵션) 별로 기억해 두고 재사용
 *  - trim() 은 한 줄씩 읽어서 계산, scale() 은 반씩 여러 번 줄이는 방식으로 변경 (SCALE_SMOOTH 보다 훨씬 빠름)
 */
public final class ImageAssets {
    private ImageAssets() {}

    private static final Map<String, ImageIcon> CACHE = new ConcurrentHashMap<>();
    private static final ImageIcon MISSING = new ImageIcon();

    public static BufferedImage read(String path) throws IOException {
        File file = new File(path);
        return file.isFile() ? ImageIO.read(file) : null;
    }

    // ---- 미리 읽어 둔 원본 (화면 표시용으로 충분한 크기까지만 줄여서 보관) ----
    private static final int SOURCE_MAX_SIDE = 900;
    private static final Map<String, java.util.Optional<BufferedImage>> SOURCES = new ConcurrentHashMap<>();

    /** load() 가 쓰는 원본. 같은 파일은 한 번만 읽고, 다른 스레드가 읽는 중이면 끝날 때까지 기다림. */
    private static BufferedImage source(String path) throws IOException {
        try {
            return SOURCES.computeIfAbsent(path, p -> {
                try {
                    BufferedImage image = read(p);
                    if (image == null) return java.util.Optional.empty();
                    int longest = Math.max(image.getWidth(), image.getHeight());
                    if (longest > SOURCE_MAX_SIDE) {
                        double s = (double) SOURCE_MAX_SIDE / longest;
                        image = resize(image, Math.max(1, (int) Math.round(image.getWidth() * s)),
                                Math.max(1, (int) Math.round(image.getHeight() * s)));
                    }
                    return java.util.Optional.of(image);
                } catch (IOException e) {
                    throw new java.io.UncheckedIOException(e);
                }
            }).orElse(null);
        } catch (java.io.UncheckedIOException e) {
            throw e.getCause();
        }
    }

    /**
     * 백그라운드 스레드에서 이미지들을 미리 읽어 둠 (로그인 화면에서 이름을 입력하는 동안).
     * 앞쪽 폴더일수록 먼저 읽음. 실패해도 나중에 화면에서 다시 읽으므로 무시.
     */
    public static void preloadInBackground(String root, String... folders) {
        Thread thread = new Thread(() -> {
            for (String folder : folders) {
                File[] files = new File(root, folder).listFiles((dir, name) -> name.endsWith(".png"));
                if (files == null) continue;
                java.util.Arrays.sort(files);
                java.util.Arrays.stream(files).parallel().forEach(file -> {
                    try {
                        source(root + folder + "/" + file.getName());
                    } catch (IOException | RuntimeException ignored) {
                        // 화면에서 필요할 때 다시 시도
                    }
                });
            }
        }, "image-preload");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    /** 자르기 없이 최대 크기 안에 맞춰 줄인 아이콘 (없으면 null). 결과는 캐시됨. */
    public static ImageIcon icon(String path, double maxWidth, double maxHeight) {
        return load(path, maxWidth, maxHeight, -1, false);
    }

    public static ImageIcon load(String path, double width, double height, int alphaThreshold,
            boolean correctCharacterSize) {
        String key = path + '|' + width + '|' + height + '|' + alphaThreshold + '|' + correctCharacterSize;
        ImageIcon cached = CACHE.get(key);
        if (cached != null) return cached == MISSING ? null : cached;

        ImageIcon result;
        try {
            BufferedImage image = source(path);
            if (image == null) {
                result = null;
            } else {
                if (alphaThreshold >= 0) image = trim(image, alphaThreshold);
                result = scale(image, width, height, correctCharacterSize ? characterScale(path) : 1.0);
            }
        } catch (IOException error) {
            result = null;
        }
        CACHE.put(key, result == null ? MISSING : result);
        return result;
    }

    public static double characterScale(String path) {
        if (path.endsWith(ImagePaths.STORE_CHARACTER_GIRYONG_CHEF)) return 1.10;
        if (path.endsWith(ImagePaths.STORE_CHARACTER_GIRYONG_DEVIL)) return 0.90;
        return 1.0;
    }

    public static ImageIcon scale(BufferedImage image, double maxWidth, double maxHeight, double correction) {
        double scale = Math.min(maxWidth / image.getWidth(), maxHeight / image.getHeight()) * correction;
        int width = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(image.getHeight() * scale));
        return new ImageIcon(resize(image, width, height));
    }

    /** 반씩 여러 번 줄인 뒤 마지막에 목표 크기로 맞춤 (품질은 SCALE_SMOOTH 와 비슷, 속도는 훨씬 빠름). */
    public static BufferedImage resize(BufferedImage source, int width, int height) {
        BufferedImage current = source;
        int w = source.getWidth();
        int h = source.getHeight();
        while (w / 2 >= width && h / 2 >= height) {
            w /= 2;
            h /= 2;
            current = draw(current, w, h);
        }
        if (w != width || h != height || current == source) {
            current = draw(current, width, height);
        }
        return current;
    }

    private static BufferedImage draw(BufferedImage source, int width, int height) {
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(source, 0, 0, width, height, null);
        } finally {
            g.dispose();
        }
        return out;
    }

    public static BufferedImage trim(BufferedImage image, int alphaThreshold) {
        int width = image.getWidth();
        int height = image.getHeight();
        int left = width, top = height, right = -1, bottom = -1;
        int[] row = new int[width];
        for (int y = 0; y < height; y++) {
            image.getRGB(0, y, width, 1, row, 0, width);
            for (int x = 0; x < width; x++) {
                if ((row[x] >>> 24) > alphaThreshold) {
                    if (x < left) left = x;
                    if (x > right) right = x;
                    if (y < top) top = y;
                    bottom = y;
                }
            }
        }
        return right < left || bottom < top ? image
                : image.getSubimage(left, top, right - left + 1, bottom - top + 1);
    }
}
