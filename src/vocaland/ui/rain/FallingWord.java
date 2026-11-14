package vocaland.ui.rain;

/*
 * FallingWord - 화면에서 떨어지는 단어 하나
 *
 * fall()           : 기본 속도 × 이 단어의 속도 배율만큼 아래로 이동
 * getDisplayText() : 모드에 따라 화면에 보여줄 글자 (뜻 또는 영어)
 * isAnswer()       : 입력값이 정답인지 확인 (영어는 대소문자, 한글은 띄어쓰기 무시)
 * getReviewText()  : 오답 복습 목록에 보여줄 글자 ("사과 → apple")
 */
public class FallingWord {
    public static final int BOX_HEIGHT = 32;

    private final String english;
    private final String meaning;
    private final double x;
    private double y;
    private final double speedRate;

    public FallingWord(String english, String meaning, double x, double speedRate) {
        this.english = english;
        this.meaning = meaning;
        this.x = x;
        this.y = -BOX_HEIGHT;
        this.speedRate = speedRate;
    }

    public void fall(double baseSpeed) {
        y += baseSpeed * speedRate;
    }

    public String getDisplayText(InputMode mode) {
        return mode == InputMode.SPELLING ? meaning : english;
    }

    public boolean isAnswer(String input, InputMode mode) {
        if (mode == InputMode.SPELLING) {
            return english.equalsIgnoreCase(input.trim());
        }
        return removeSpaces(meaning).equals(removeSpaces(input));
    }

    public String getReviewText() {
        return meaning + "  →  " + english;
    }

    private static String removeSpaces(String text) {
        return text.replaceAll("\\s+", "");
    }

    public String getEnglish() {
        return english;
    }

    public String getMeaning() {
        return meaning;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
}
