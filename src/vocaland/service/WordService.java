package vocaland.service;

import java.util.ArrayList;
import java.util.List;
import vocaland.model.Word;
import vocaland.repository.WordRepository;

/*
 * WordService - 화면에서 단어를 꺼내 쓰는 곳
 *
 * getWords()              : 모든 단어
 * getThemes()             : 테마 목록 (파일에 나온 순서, 중복 없음)
 * getWordsByTheme(theme)  : 테마별 단어 (단어 학습 화면용)
 * getWordsByDifficulty(d) : 난이도별 단어 (Word.EASY / NORMAL / HARD)
 * getGameWords()          : 게임용 {영어, 뜻} 목록 (산성비 AcidRainPanel 생성자에 그대로 넘김)
 */
public class WordService {
    private final WordRepository repository;

    public WordService() {
        this(new WordRepository());
    }

    public WordService(WordRepository repository) {
        this.repository = repository;
    }

    public List<Word> getWords() {
        return repository.findAll();
    }

    public List<String> getThemes() {
        List<String> themes = new ArrayList<>();
        for (Word word : repository.findAll()) {
            if (!themes.contains(word.getTheme())) {
                themes.add(word.getTheme());
            }
        }
        return themes;
    }

    public List<Word> getWordsByTheme(String theme) {
        List<Word> result = new ArrayList<>();
        for (Word word : repository.findAll()) {
            if (word.getTheme().equals(theme)) {
                result.add(word);
            }
        }
        return result;
    }

    public List<Word> getWordsByDifficulty(String difficulty) {
        List<Word> result = new ArrayList<>();
        for (Word word : repository.findAll()) {
            if (word.getDifficulty().equals(difficulty)) {
                result.add(word);
            }
        }
        return result;
    }

    public List<String[]> getGameWords() {
        return toGameWords(repository.findAll());
    }

    // 원하는 단어만 골라서 게임에 넘길 때 사용 (예: 테마별 산성비)
    public static List<String[]> toGameWords(List<Word> words) {
        List<String[]> pairs = new ArrayList<>();
        for (Word word : words) {
            pairs.add(new String[] {word.getEnglish(), word.getMeaning()});
        }
        return pairs;
    }
}
