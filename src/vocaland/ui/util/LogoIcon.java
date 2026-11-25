package vocaland.ui.util;

import javax.swing.ImageIcon;

public final class LogoIcon {
    private LogoIcon() {}

    public static ImageIcon load(String name, int width, int height) {
        return ImageAssets.load(ImagePaths.ROOT + name, width, height, 16, false);
    }
}
