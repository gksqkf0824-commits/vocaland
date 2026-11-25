package vocaland.repository;

import vocaland.model.Word;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Shared reader for the five-column word file. */
public final class WordRepository {
    private WordRepository() {}

    public static List<Word> read(Path path) throws IOException {
        Map<String, Word> unique = new LinkedHashMap<>();
        if (!Files.exists(path)) return List.of();
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            if (line.isBlank()) continue;
            String[] fields = line.split("\\|", -1);
            if (fields.length < 3 || fields[0].isBlank() || fields[1].isBlank() || fields[2].isBlank()) {
                throw new IllegalStateException("words.txt 형식을 확인하세요.");
            }
            String id = fields[0].strip();
            unique.putIfAbsent(id, new Word(id, fields[1].strip(), fields[2].strip(),
                    fields.length >= 5 ? fields[4].strip() : "초급"));
        }
        return List.copyOf(unique.values());
    }
}
