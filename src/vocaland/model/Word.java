package vocaland.model;

import java.util.Objects;

/*
 * Word - 단어 하나 (words.txt 한 줄)
 *
 * 형식 : 단어ID|영어단어|한글뜻|테마|난이도
 * 예시 : W001|apple|사과|음식|초급
 *
 * 난이도는 초급 / 중급 / 고급
 *
 * 백엔드(be-core, be-study)는 getEnglish() 같은 getter를 쓰고,
 * 프론트(main)는 english() 같은 record 스타일 메서드를 써서 둘 다 제공합니다.
 */
public class Word {
    public static final String EASY = "초급";
    public static final String NORMAL = "중급";
    public static final String HARD = "고급";

    private final String id;
    private final String english;
    private final String meaning;
    private final String theme;
    private final String difficulty;

    public Word(String id, String english, String meaning, String theme, String difficulty) {
        this.id = id;
        this.english = english;
        this.meaning = meaning;
        this.theme = theme;
        this.difficulty = difficulty;
    }

    // main 프론트용: 테마 없이 만들 때
    public Word(String id, String english, String meaning, String difficulty) {
        this(id, english, meaning, "", difficulty);
    }

    // ---- 백엔드용 getter ----
    public String getId() {
        return id;
    }

    public String getEnglish() {
        return english;
    }

    public String getMeaning() {
        return meaning;
    }

    public String getTheme() {
        return theme;
    }

    public String getDifficulty() {
        return difficulty;
    }

    // ---- 프론트용 record 스타일 메서드 ----
    public String id() {
        return id;
    }

    public String english() {
        return english;
    }

    public String meaning() {
        return meaning;
    }

    public String theme() {
        return theme;
    }

    public String difficulty() {
        return difficulty;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Word)) return false;
        Word other = (Word) o;
        return Objects.equals(id, other.id)
                && Objects.equals(english, other.english)
                && Objects.equals(meaning, other.meaning)
                && Objects.equals(theme, other.theme)
                && Objects.equals(difficulty, other.difficulty);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, english, meaning, theme, difficulty);
    }

    @Override
    public String toString() {
        return english + " - " + meaning;
    }
}
