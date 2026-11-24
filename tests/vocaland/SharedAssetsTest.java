package vocaland;

import vocaland.ui.util.ImagePaths;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import vocaland.repository.WordRepository;
import vocaland.ui.util.ImageAssets;

/** Regression checks for shared readers and image processing. */
public final class SharedAssetsTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        var words = WordRepository.read(Path.of("data/words.txt"));
        check(words.size() == 600, "600 words");
        check(new HashSet<>(words.stream().map(vocaland.model.Word::id).toList()).size() == 600,
                "Unique word IDs");
        for (String difficulty : List.of("초급", "중급", "고급")) {
            check(words.stream().filter(word -> word.difficulty().equals(difficulty)).count() == 200,
                    "200 words per difficulty");
        }
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(2, 2, 0x20FFFFFF);
        image.setRGB(4, 4, 0xFFFFFFFF);
        var logo = ImageAssets.trim(image, 16);
        var character = ImageAssets.trim(image, 64);
        check(logo.getWidth() == 3 && character.getWidth() == 1, "Preserve per-screen alpha thresholds");
        var transparent = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        check(ImageAssets.trim(transparent, 16) == transparent, "Transparent fallback");
        var scaled = ImageAssets.scale(new BufferedImage(100, 50, BufferedImage.TYPE_INT_ARGB), 40, 40, 1);
        check(scaled.getIconWidth() == 40 && scaled.getIconHeight() == 20, "Preserve aspect ratio");
        check(ImageAssets.characterScale(ImagePaths.ROOT + ImagePaths.STORE_CHARACTER_GIRYONG_CHEF) == 1.10,
                "Preserve chef correction");
        check(ImageAssets.characterScale(ImagePaths.ROOT + ImagePaths.STORE_CHARACTER_GIRYONG_DEVIL) == 0.90,
                "Preserve devil correction");
        check(ImageAssets.load("images/missing-image-test.png", 40, 40, 16, false) == null,
                "Missing image fallback");
        System.out.println("PASS: shared word reader, difficulty counts, alpha thresholds, scaling and fallbacks");
    }
}
