package vocaland.repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import vocaland.model.Word;

/** Persists per-profile review marks without changing the existing file format. */
public final class ReviewRepository {
    private final Path file;

    public ReviewRepository(Path file) {
        this.file = file;
    }

    public Set<String> read() throws IOException {
        try {
            return new LinkedHashSet<>(Files.readAllLines(file, StandardCharsets.UTF_8));
        } catch (NoSuchFileException missing) {
            return new LinkedHashSet<>();
        }
    }

    public void save(Set<String> entries) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, "review-", ".tmp");
        try {
            Files.write(temporary, entries, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public void export(Path target, List<Word> words) throws IOException {
        Files.write(target, words.stream().map(word -> word.english() + "\t" + word.meaning()).toList(),
                StandardCharsets.UTF_8);
    }
}
