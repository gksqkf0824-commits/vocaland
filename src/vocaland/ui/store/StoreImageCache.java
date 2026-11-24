package vocaland.ui.store;

import vocaland.ui.util.ImagePaths;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.swing.ImageIcon;
import vocaland.ui.util.ImageAssets;

/** Prepared by one worker, then read by the Swing thread after worker completion. */
public final class StoreImageCache {
    private record Key(String name, double width, double height, boolean trim) {}
    private final Map<Key, ImageIcon> icons = new HashMap<>();
    private final Map<String, BufferedImage> originals = new HashMap<>();
    private final Map<String, BufferedImage> trimmed = new HashMap<>();

    public ImageIcon get(String name, double width, double height, boolean trim) {
        return icons.get(new Key(name, width, height, trim));
    }

    public int size() {
        return icons.size();
    }

    void prepare(String name, double width, double height, boolean trim) {
        Key key = new Key(name, width, height, trim);
        if (icons.containsKey(key)) return;
        try {
            BufferedImage original = originals.get(name);
            if (original == null) {
                original = ImageAssets.read(ImagePaths.ROOT + name);
                if (original == null) return;
                originals.put(name, original);
            }
            if (width == 0 || height == 0) {
                icons.put(key, new ImageIcon(original));
                return;
            }
            BufferedImage image = original;
            if (trim) {
                image = trimmed.get(name);
                if (image == null) {
                    image = ImageAssets.trim(original, 64);
                    trimmed.put(name, image);
                }
            }
            icons.put(key, ImageAssets.scale(image, width, height, ImageAssets.characterScale(name)));
        } catch (IOException ignored) {
            // The store displays the item name when its image cannot be read.
        }
    }

    void releaseOriginals() {
        originals.clear();
        trimmed.clear();
    }
}
