package vocaland.ui.util;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/*
 * ImageScaler - 큰 이미지를 그릴 크기로 미리 한 번 줄여 두기 (매 프레임 축소 방지)
 *
 * scale(image, width, height) : 원하는 크기의 BufferedImage로 변환 (크게 줄일 때는 절반씩 나눠서 줄임)
 */
public final class ImageScaler {
    private ImageScaler() {
    }

    public static BufferedImage scale(Image image, int width, int height) {
        int w = image.getWidth(null);
        int h = image.getHeight(null);
        if (w <= 0 || h <= 0 || width <= 0 || height <= 0) {
            return null;
        }
        BufferedImage current = toBuffered(image, w, h);
        while (w / 2 >= width && h / 2 >= height) {
            w /= 2;
            h /= 2;
            current = draw(current, w, h);
        }
        return draw(current, width, height);
    }

    private static BufferedImage toBuffered(Image image, int w, int h) {
        if (image instanceof BufferedImage buffered && buffered.getType() == BufferedImage.TYPE_INT_ARGB) {
            return buffered;
        }
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return result;
    }

    private static BufferedImage draw(Image image, int width, int height) {
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(image, 0, 0, width, height, null);
        g.dispose();
        return result;
    }
}
