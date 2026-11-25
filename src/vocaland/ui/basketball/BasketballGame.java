package vocaland.ui.basketball;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * BasketballGame - 농구 게임 물리 계산 및 규칙 (각도, 힘, 중력, 점수, 목숨, 오답 기록)
 *
 * startNewGame()  : 점수·목숨 초기화 후 플레이 시작
 * toReadyState()  : 모드 선택 대기 상태로 전환
 * nextQuestion()  : 새 문제와 3개의 골대 선택지 생성
 * adjustAngle()   : 발사 각도 조절 (0° ~ 90°)
 * adjustPower()   : 발사 힘 조절 (12 ~ 28)
 * shoot()         : 현재 각도와 힘으로 공 발사 (vx, vy 계산)
 * updatePhysics() : 한 프레임 물리 이동 및 골대 판정 (0:진행, 1:정답, 2:오답, 3:게임오버)
 * resetBall()     : 공 위치를 시작 지점으로 초기화
 */
public class BasketballGame {

    public enum State { READY, PLAYING, GAME_OVER }

    public static class WrongRecord {
        public final String question;
        public final String answer;
        public final String selected;

        public WrongRecord(String question, String answer, String selected) {
            this.question = question;
            this.answer = answer;
            this.selected = selected;
        }
    }

    public static final double BASE_W = 700.0;
    public static final double BASE_H = 470.0;
    public static final double BALL_START_X = 85.0;
    public static final double BALL_START_Y = 335.0;
    public static final double GRAVITY = 0.68;

    public static final int HOOP_X = 465;
    public static final int[] HOOP_Y = {70, 190, 310};
    public static final int HOOP_W = 82;
    public static final int HOOP_H = 62;

    private final List<String[]> wordPairs;
    private BasketballMode mode = BasketballMode.MEANING;
    private boolean hardMode = false;
    private State state = State.READY;

    private int score = 0;
    private int lives = 3;
    private final List<String> missedEnglishWords = new ArrayList<>();
    private final List<WrongRecord> wrongRecords = new ArrayList<>();

    private String currentEnglish = "";
    private String currentMeaning = "";
    private final String[] choices = new String[3];
    private int answerHoopIndex = 0;

    private double ballX = BALL_START_X;
    private double ballY = BALL_START_Y;
    private double vx = 0;
    private double vy = 0;
    private double angleDeg = 52.0;
    private double power = 19.5;
    private boolean isShooting = false;

    public BasketballGame(List<String[]> wordPairs) {
        this.wordPairs = wordPairs;
        nextQuestion();
    }

    public void startNewGame() {
        score = 0;
        lives = 3;
        missedEnglishWords.clear();
        wrongRecords.clear();
        resetBall();
        nextQuestion();
        state = State.PLAYING;
    }

    public void toReadyState() {
        state = State.READY;
        resetBall();
    }

    public void nextQuestion() {
        List<String[]> copy = new ArrayList<>(wordPairs);
        Collections.shuffle(copy);

        currentEnglish = copy.get(0)[0];
        currentMeaning = copy.get(0)[1];

        int idx = (mode == BasketballMode.MEANING) ? 1 : 0;
        String correctChoice = copy.get(0)[idx];

        List<String> optionList = new ArrayList<>();
        optionList.add(copy.get(0)[idx]);
        optionList.add(copy.get(1)[idx]);
        optionList.add(copy.get(2)[idx]);
        Collections.shuffle(optionList);

        for (int i = 0; i < 3; i++) {
            choices[i] = optionList.get(i);
            if (choices[i].equals(correctChoice)) {
                answerHoopIndex = i;
            }
        }
    }

    public void adjustAngle(double delta) {
        if (state != State.PLAYING || isShooting) {
            return;
        }
        angleDeg = Math.max(0.0, Math.min(90.0, angleDeg + delta));
    }

    public void adjustPower(double delta) {
        if (state != State.PLAYING || isShooting) {
            return;
        }
        power = Math.max(12.0, Math.min(28.0, power + delta));
    }

    public void shoot() {
        if (state != State.PLAYING || isShooting) {
            return;
        }
        isShooting = true;
        double rad = Math.toRadians(angleDeg);
        vx = power * Math.cos(rad);
        vy = -power * Math.sin(rad);
    }

    public int updatePhysics() {
        if (state != State.PLAYING || !isShooting) {
            return 0;
        }

        ballX += vx;
        ballY += vy;
        vy += GRAVITY;

        double ballCenterX = ballX + 22;
        double ballCenterY = ballY + 22;

        for (int i = 0; i < 3; i++) {
            int rimLeft = HOOP_X + 8;
            int rimRight = HOOP_X + HOOP_W - 8;
            int rimTop = HOOP_Y[i] + 12;
            int rimBottom = HOOP_Y[i] + 48;

            if (ballCenterX >= rimLeft && ballCenterX <= rimRight
                    && ballCenterY >= rimTop && ballCenterY <= rimBottom) {

                isShooting = false;
                if (i == answerHoopIndex) {
                    score += hardMode ? 150 : 100;
                    resetBall();
                    nextQuestion();
                    return 1;
                } else {
                    return processMiss(choices[i]);
                }
            }
        }

        if (ballX > BASE_W + 40 || ballY > BASE_H + 30) {
            isShooting = false;
            return processMiss("(골대 빗나감)");
        }
        return 0;
    }

    private int processMiss(String selectedChoice) {
        lives--;
        missedEnglishWords.add(currentEnglish);
        wrongRecords.add(new WrongRecord(getQuestionText(), getAnswerText(), selectedChoice));

        if (lives <= 0) {
            state = State.GAME_OVER;
            return 3;
        } else {
            resetBall();
            nextQuestion();
            return 2;
        }
    }

    public void resetBall() {
        ballX = BALL_START_X;
        ballY = BALL_START_Y;
        vx = 0;
        vy = 0;
        isShooting = false;
    }

    public String getCurrentEnglish() { return currentEnglish; }

    public String getQuestionText() {
        return (mode == BasketballMode.MEANING) ? currentEnglish : currentMeaning;
    }

    public String getAnswerText() {
        return (mode == BasketballMode.MEANING) ? currentMeaning : currentEnglish;
    }

    public BasketballMode getMode() {
        return mode;
    }

    public void setMode(BasketballMode mode) {
        this.mode = mode;
        nextQuestion();
    }

    public boolean isHardMode() {
        return hardMode;
    }

    public void setHardMode(boolean hardMode) {
        this.hardMode = hardMode;
    }

    public State getState() {
        return state;
    }

    public int getScore() {
        return score;
    }

    public int getLives() {
        return lives;
    }

    public List<String> getMissedEnglishWords() {
        return missedEnglishWords;
    }

    public List<WrongRecord> getWrongRecords() {
        return wrongRecords;
    }

    public String[] getChoices() {
        return choices;
    }

    public double getBallX() {
        return ballX;
    }

    public double getBallY() {
        return ballY;
    }

    public double getAngleDeg() {
        return angleDeg;
    }

    public double getPower() {
        return power;
    }

    public boolean isShooting() {
        return isShooting;
    }
}