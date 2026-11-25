package vocaland.repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import vocaland.model.Word;

/*
 * WordRepository - words.txt 읽기
 *
 * 단어는 프로그램이 바꾸지 않는 데이터라 읽기만 합니다.
 *
 * [백엔드용 - 인스턴스]
 * findAll()        : 모든 단어 (처음 한 번 읽은 뒤 메모리에 저장)
 * findById(id)     : ID로 찾기, 없으면 null
 *
 * [프론트용 - static]
 * read(path)       : 경로의 파일을 바로 읽어 목록 반환 (ID 중복 제거, 형식 오류면 예외)
 */
public class WordRepository {
    public static final String FILE_NAME = "words.txt";
    private static final String HEADER = "# 단어ID|영어단어|한글뜻|테마|난이도";

    private final TextFileStore store;
    private List<Word> cache;

    public WordRepository() {
        this(new TextFileStore(FILE_NAME));
    }

    public WordRepository(TextFileStore store) {
        this.store = store;
    }

    public List<Word> findAll() {
        if (cache == null) {
            cache = load();
        }
        return new ArrayList<>(cache);
    }

    public Word findById(String id) {
        for (Word word : findAll()) {
            if (word.getId().equals(id)) {
                return word;
            }
        }
        return null;
    }

    private List<Word> load() {
        List<Word> words = new ArrayList<>();
        for (String[] row : store.readRows(HEADER)) {
            if (row.length < 5 || row[1].isEmpty() || row[2].isEmpty()) {
                System.err.println("[words.txt] 잘못된 줄을 건너뜁니다: " + String.join("|", row));
                continue;
            }
            words.add(new Word(row[0], row[1], row[2], row[3], row[4]));
        }
        return words;
    }

    public static List<Word> read(Path path) throws IOException {
        Map<String, Word> unique = new LinkedHashMap<>();
        if (!Files.exists(path)) return List.of();
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.strip().startsWith("#")) continue;
            String[] fields = line.split("\\|", -1);
            if (fields.length < 3 || fields[0].isBlank() || fields[1].isBlank() || fields[2].isBlank()) {
                throw new IllegalStateException("words.txt 형식을 확인하세요.");
            }
            String id = fields[0].strip();
            String theme = fields.length >= 4 ? fields[3].strip() : "";
            String difficulty = fields.length >= 5 ? fields[4].strip() : "초급";
            unique.putIfAbsent(id, new Word(id, fields[1].strip(), fields[2].strip(), theme, difficulty));
        }
        return List.copyOf(unique.values());
    }
}
