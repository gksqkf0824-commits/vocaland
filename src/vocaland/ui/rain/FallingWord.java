package vocaland.ui.rain;

/*
 * FallingWord - 화면에서 떨어지는 단어 하나
 *
 * fall()           : 기본 속도 × 이 단어의 속도 배율만큼 아래로 이동
 * getDisplayText() : 모드에 따라 화면에 보여줄 글자 (뜻 또는 영어)
 * getAnswerText()  : 모드에 따라 입력해야 하는 글자 (영어 또는 뜻)
 * isAnswer()       : 입력값이 정답인지 확인 (영어는 대소문자, 한글은 띄어쓰기 무시)
 * getReviewText()  : 오답 복습 목록에 보여줄 글자 ("사과 → apple")
 * isHeart()        : 하트 단어인지 (맞히면 목숨 +1)
 */
public class FallingWord {
    public static final int BOX_WIDTH = 150;
    public static final int BOX_HEIGHT = 60;

    private final String english;
    private final String meaning;
    private final double x;
    private double y;
    private final double speedRate;
    private final boolean heart;

    public FallingWord(String english, String meaning, double x, double speedRate, boolean heart) {
        this.english = english;
        this.meaning = meaning;
        this.x = x;
        this.y = -BOX_HEIGHT;
        this.speedRate = speedRate;
        this.heart = heart;
    }

    public void fall(double baseSpeed) {
        y += baseSpeed * speedRate;
    }

    public String getDisplayText(InputMode mode) {
        return mode == InputMode.SPELLING ? meaning : english;
    }

    public String getAnswerText(InputMode mode) {
        return mode == InputMode.SPELLING ? english : meaning;
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

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public boolean isHeart() {
        return heart;
    }
}
