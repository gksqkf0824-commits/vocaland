package vocaland;

import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import vocaland.model.Word;
import vocaland.repository.ReviewRepository;
import vocaland.ui.study.MatchingPanel;
import vocaland.ui.study.StudyPanel;

public final class LearningFlowTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static JButton button(Container parent, String name) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JButton button && name.equals(button.getAccessibleContext().getAccessibleName())) {
                return button;
            }
            if (child instanceof Container container) {
                JButton found = button(container, name);
                if (found != null) return found;
            }
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("review-repository-test-");
        try {
            ReviewRepository reviews = new ReviewRepository(directory.resolve("reviews.txt"));
            check(reviews.read().isEmpty(), "Missing file starts empty");
            reviews.save(Set.of("U001|W001", "U002|W002"));
            check(reviews.read().equals(Set.of("U001|W001", "U002|W002")), "Profile marks round trip");
            reviews.save(Set.of("U002|W002"));
            check(reviews.read().equals(Set.of("U002|W002")), "Unmark preserves other profile");
            Path export = directory.resolve("export.txt");
            reviews.export(export, List.of(new Word("W001", "apple", "사과", "초급")));
            check(Files.readAllLines(export).equals(List.of("apple\t사과")), "Export format unchanged");
            try (var files = Files.list(directory)) {
                check(files.noneMatch(path -> path.toString().endsWith(".tmp")), "No temporary file leaks");
            }
        } finally {
            try (var files = Files.walk(directory)) {
                for (Path path : files.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
        SwingUtilities.invokeAndWait(() -> {
            try {
                StudyPanel study = new StudyPanel("read-only-test", () -> {}, () -> {});
                Field selected = MatchingPanel.class.getDeclaredField("pairs");
                selected.setAccessible(true);
                Method start = MatchingPanel.class.getDeclaredMethod("startRound");
                start.setAccessible(true);
                for (String difficulty : List.of("초급", "중급", "고급")) {
                    study.showDifficultySelection();
                    button(study, difficulty).doClick(0);
                    List<String[]> words = study.getStudyWordPairs();
                    check(words.size() == 20, "20 study words");
                    MatchingPanel matching = new MatchingPanel(() -> {}, words);
                    matching.setWordPool(words);
                    Set<String> pool = new HashSet<>();
                    for (String[] word : words) pool.add(word[0]);
                    for (int round = 0; round < 10; round++) {
                        start.invoke(matching);
                        String[][] pairs = (String[][]) selected.get(matching);
                        Set<String> unique = new HashSet<>();
                        for (String[] pair : pairs) {
                            check(pool.contains(pair[0]), "Matching only uses current lesson");
                            unique.add(pair[0]);
                        }
                        check(pairs.length == 6 && unique.size() == 6, "Six distinct pairs");
                    }
                }
            } catch (Exception error) {
                throw new RuntimeException(error);
            }
        });
        System.out.println("PASS: review persistence/export, 20-word lessons and six-pair lesson matching");
    }
}
