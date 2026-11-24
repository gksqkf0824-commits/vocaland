package vocaland.ui.util;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

/** Shared image operations; callers choose trimming and character corrections. */
public final class ImageAssets {
    private ImageAssets() {}

    public static BufferedImage read(String path) throws IOException {
        File file = new File(path);
        return file.isFile() ? ImageIO.read(file) : null;
    }

    public static ImageIcon load(String path, double width, double height, int alphaThreshold,
            boolean correctCharacterSize) {
        try {
            BufferedImage image = read(path);
            if (image == null) return null;
            if (alphaThreshold >= 0) image = trim(image, alphaThreshold);
            return scale(image, width, height, correctCharacterSize ? characterScale(path) : 1.0);
        } catch (IOException error) {
            return null;
        }
    }

    public static double characterScale(String path) {
        if (path.endsWith(ImagePaths.STORE_CHARACTER_GIRYONG_CHEF)) return 1.10;
        if (path.endsWith(ImagePaths.STORE_CHARACTER_GIRYONG_DEVIL)) return 0.90;
        return 1.0;
    }

    public static ImageIcon scale(BufferedImage image, double maxWidth, double maxHeight, double correction) {
        double scale = Math.min(maxWidth / image.getWidth(), maxHeight / image.getHeight()) * correction;
        return new ImageIcon(image.getScaledInstance(
                Math.max(1, (int) Math.round(image.getWidth() * scale)),
                Math.max(1, (int) Math.round(image.getHeight() * scale)), Image.SCALE_SMOOTH));
    }

    public static BufferedImage trim(BufferedImage image, int alphaThreshold) {
        int left = image.getWidth(), top = image.getHeight(), right = -1, bottom = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) > alphaThreshold) {
                    left = Math.min(left, x); top = Math.min(top, y);
                    right = Math.max(right, x); bottom = Math.max(bottom, y);
                }
            }
        }
        return right < left || bottom < top ? image
                : image.getSubimage(left, top, right - left + 1, bottom - top + 1);
    }
}
