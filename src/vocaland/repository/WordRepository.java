package vocaland.repository;

import java.util.ArrayList;
import java.util.List;
import vocaland.model.Word;

/*
 * WordRepository - words.txt 읽기
 *
 * 단어는 프로그램이 바꾸지 않는 데이터라 읽기만 합니다.
 * 처음 한 번 읽은 뒤에는 메모리에 저장해 둔 목록을 사용합니다.
 *
 * findAll()        : 모든 단어
 * findById(id)     : ID로 찾기, 없으면 null
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
}
