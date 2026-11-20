package vocaland.model;

/*
 * Word - 단어 하나 (words.txt 한 줄)
 *
 * 형식 : 단어ID|영어단어|한글뜻|테마|난이도
 * 예시 : W001|apple|사과|음식|중급
 *
 * 난이도는 초급 / 중급 / 고급 (영어 단어 길이 기준: 4글자 이하 / 5~6글자 / 7글자 이상)
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

    @Override
    public String toString() {
        return english + " - " + meaning;
    }
}
