package vocaland.ui.basketball;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * BasketballGame - 농구 게임 물리 계산 및 규칙 (각도, 힘, 중력, 점수, 목숨, 오답 기록, 익스트림 물리 엔진)
 *
 * startNewGame()        : 점수·목숨 초기화 후 플레이 시작
 * toReadyState()        : 모드 선택 대기 상태로 전환
 * nextQuestion()        : 새 문제와 3개의 골대 선택지 생성
 * adjustAngle()         : 발사 각도 조절 (0° ~ 90°)
 * adjustPower()         : 발사 힘 조절 (12 ~ 28)
 * shoot()               : 현재 각도와 힘으로 공 발사 (vx, vy 계산)
 * updatePhysics()       : 난이도별(일반/익스트림) 한 프레임 물리 이동 및 판정 (0:진행, 1:정답, 2:오답, 3:게임오버)
 * updateNormalPhysics() : 이지/하드 모드용 기존 골대 판정 및 바닥 도달 삭제 처리
 * updateExtremePhysics(): 익스트림 모드용 백보드/림 탄성 반사, 끼임 방지, 그물 낙하 물리 엔진
 * resolveCircleRim()    : 림 앞/뒷턱 원형 충돌체와 공 사이의 탄성 반사 계산
 * resolveBackboard()    : 골대 백보드(사각 판)와 공 사이의 충돌 및 반사 계산
 * setFloorY()           : 현재 화면 비율에 맞춘 논리적 바닥 Y좌표 동기화
 * resetBall()           : 공 위치를 시작 지점으로 초기화
 */
public class BasketballGame {

    public enum State { READY, PLAYING, GAME_OVER }
    public enum Difficulty { EASY, HARD, EXTREME }

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

    public static final double BALL_SIZE = 43.0;
    public static final double BALL_DRAW_RADIUS = BALL_SIZE / 2.0;
    public static final double BALL_HIT_RADIUS = 16.5;
    public static final double GRAVITY = 0.68;

    public static final int HOOP_X = 438;
    public static final int[] HOOP_Y = {54, 176, 298};
    public static final int HOOP_W = 114;
    public static final int HOOP_H = 96;

    private final List<String[]> wordPairs;
    private BasketballMode mode = BasketballMode.MEANING;
    private Difficulty difficulty = Difficulty.EASY;
    private boolean extremeHardTrajectory = true;
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
    private double prevBallCenterY = BALL_START_Y + BALL_DRAW_RADIUS;
    private double vx = 0;
    private double vy = 0;
    private double angleDeg = 52.0;
    private double power = 19.5;

    private boolean isShooting = false;
    private boolean scoredThisShot = false;
    private int pendingResultAfterFall = 0;
    private int stuckFrames = 0;
    private double floorY = BASE_H;

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
        scoredThisShot = false;
        pendingResultAfterFall = 0;
        stuckFrames = 0;
        prevBallCenterY = ballY + BALL_DRAW_RADIUS;

        double rad = Math.toRadians(angleDeg);
        vx = power * Math.cos(rad);
        vy = -power * Math.sin(rad);
    }

    public int updatePhysics() {
        if (state != State.PLAYING || !isShooting) {
            return 0;
        }
        if (difficulty == Difficulty.EXTREME) {
            return updateExtremePhysics();
        } else {
            return updateNormalPhysics();
        }
    }

    private int updateNormalPhysics() {
        ballX += vx;
        ballY += vy;
        vy += GRAVITY;

        double ballCenterX = ballX + BALL_DRAW_RADIUS;
        double ballCenterY = ballY + BALL_DRAW_RADIUS;

        for (int i = 0; i < 3; i++) {
            int rimLeft = HOOP_X + 20;
            int rimRight = HOOP_X + 85;
            int rimTop = HOOP_Y[i] + 24;
            int rimBottom = HOOP_Y[i] + 60;

            if (ballCenterX >= rimLeft && ballCenterX <= rimRight
                    && ballCenterY >= rimTop && ballCenterY <= rimBottom) {

                isShooting = false;
                if (i == answerHoopIndex) {
                    score += (difficulty == Difficulty.HARD) ? 150 : 100;
                    resetBall();
                    nextQuestion();
                    return 1;
                } else {
                    return processMiss(choices[i]);
                }
            }
        }

        if (ballY + BALL_SIZE >= floorY) {
            isShooting = false;
            return processMiss("(골대 빗나감)");
        }
        return 0;
    }

    private int updateExtremePhysics() {
        int subSteps = 4;
        double dt = 1.0 / subSteps;
        int immediateEvent = 0;

        double speed = Math.hypot(vx, vy);
        if (speed < 0.65) {
            stuckFrames++;
        } else {
            stuckFrames = 0;
        }
        boolean ignoreRimCollision = (stuckFrames > 55);

        for (int step = 0; step < subSteps; step++) {
            prevBallCenterY = ballY + BALL_DRAW_RADIUS;
            ballX += vx * dt;
            ballY += vy * dt;
            vy += GRAVITY * dt;

            double cx = ballX + BALL_DRAW_RADIUS;
            double cy = ballY + BALL_DRAW_RADIUS;

            for (int i = 0; i < 3; i++) {
                double hy = HOOP_Y[i];

                double boardLeft = HOOP_X + 79.0;
                double boardRight = HOOP_X + 99.0;
                double boardTop = hy + 10.0;
                double boardBottom = hy + 64.0;
                resolveBackboard(boardLeft, boardTop, boardRight, boardBottom);

                double rimY = hy + 52.0;
                double frontRimX = HOOP_X + 24.0;
                double backRimX = HOOP_X + 74.0;
                double rimRadius = 2.7;

                if (!ignoreRimCollision && !scoredThisShot) {
                    resolveCircleRim(frontRimX, rimY, rimRadius);
                    resolveCircleRim(backRimX, rimY, rimRadius);
                }

                cx = ballX + BALL_DRAW_RADIUS;
                cy = ballY + BALL_DRAW_RADIUS;

                if (!scoredThisShot && vy > 0) {
                    boolean withinRimOpening = (cx > frontRimX + 2.0) && (cx < backRimX - 2.0);
                    boolean crossedRimLine = (prevBallCenterY <= rimY) && (cy >= rimY);

                    if (withinRimOpening && crossedRimLine) {
                        scoredThisShot = true;
                        stuckFrames = 0;
                        vx *= 0.40;
                        vy *= 0.65;

                        if (i == answerHoopIndex) {
                            score += 200;
                            pendingResultAfterFall = 1;
                            immediateEvent = 1;
                        } else {
                            pendingResultAfterFall = applyMissState(choices[i]);
                        }
                    }
                }

                if (scoredThisShot && cy > rimY && cy < hy + 90.0
                        && cx > HOOP_X + 18.0 && cx < HOOP_X + 76.0) {
                    double netCenterX = (frontRimX + backRimX) / 2.0;
                    vx += (netCenterX - cx) * 0.018 * dt;
                    vx *= Math.pow(0.93, dt);
                }
            }

            if (ballY + BALL_SIZE >= floorY) {
                isShooting = false;
                if (scoredThisShot) {
                    int finalRes = pendingResultAfterFall;
                    if (finalRes == 3) {
                        state = State.GAME_OVER;
                        return 3;
                    }
                    resetBall();
                    nextQuestion();
                    return (finalRes == 2) ? 2 : 0;
                } else {
                    return processMiss("(골대 빗나감)");
                }
            }
        }

        return immediateEvent;
    }

    private void resolveBackboard(double left, double top, double right, double bottom) {
        double cx = ballX + BALL_DRAW_RADIUS;
        double cy = ballY + BALL_DRAW_RADIUS;

        double closestX = Math.max(left, Math.min(cx, right));
        double closestY = Math.max(top, Math.min(cy, bottom));

        double dx = cx - closestX;
        double dy = cy - closestY;
        double distSq = dx * dx + dy * dy;

        if (distSq < BALL_HIT_RADIUS * BALL_HIT_RADIUS) {
            double dist = Math.sqrt(distSq);
            double nx, ny, overlap;

            if (dist > 0.0001) {
                nx = dx / dist;
                ny = dy / dist;
                overlap = BALL_HIT_RADIUS - dist;
            } else {
                nx = -1.0;
                ny = 0.0;
                overlap = BALL_HIT_RADIUS;
            }

            ballX += nx * overlap;
            ballY += ny * overlap;

            double dot = vx * nx + vy * ny;
            if (dot < 0) {
                double restitution = 0.62;
                vx -= (1.0 + restitution) * dot * nx;
                vy -= (1.0 + restitution) * dot * ny;
                vy *= 0.92;
            }
        }
    }

    private void resolveCircleRim(double rimX, double rimY, double rimRadius) {
        double cx = ballX + BALL_DRAW_RADIUS;
        double cy = ballY + BALL_DRAW_RADIUS;

        double dx = cx - rimX;
        double dy = cy - rimY;
        double minDist = BALL_HIT_RADIUS + rimRadius;
        double distSq = dx * dx + dy * dy;

        if (distSq < minDist * minDist && distSq > 0.0001) {
            double dist = Math.sqrt(distSq);
            double nx = dx / dist;
            double ny = dy / dist;
            double overlap = minDist - dist;

            ballX += nx * overlap;
            ballY += ny * overlap;

            double dot = vx * nx + vy * ny;
            if (dot < 0) {
                double restitution = 0.56;
                vx -= (1.0 + restitution) * dot * nx;
                vy -= (1.0 + restitution) * dot * ny;

                if (Math.abs(nx) < 0.12 && ny < -0.9) {
                    vx += (dx >= 0) ? 0.45 : -0.45;
                }
            }
        }
    }

    private int applyMissState(String selectedChoice) {
        lives--;
        missedEnglishWords.add(currentEnglish);
        wrongRecords.add(new WrongRecord(getQuestionText(), getAnswerText(), selectedChoice));
        return (lives <= 0) ? 3 : 2;
    }

    private int processMiss(String selectedChoice) {
        int res = applyMissState(selectedChoice);
        if (res == 3) {
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
        prevBallCenterY = BALL_START_Y + BALL_DRAW_RADIUS;
        vx = 0;
        vy = 0;
        isShooting = false;
        scoredThisShot = false;
        pendingResultAfterFall = 0;
        stuckFrames = 0;
    }

    public void setFloorY(double floorY) {
        this.floorY = Math.max(BASE_H, floorY);
    }

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

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
        if (difficulty == Difficulty.EXTREME) {
            this.extremeHardTrajectory = true;
        }
    }

    public boolean isExtremeMode() {
        return difficulty == Difficulty.EXTREME;
    }

    public boolean isExtremeHardTrajectory() {
        return extremeHardTrajectory;
    }

    public void setExtremeHardTrajectory(boolean extremeHardTrajectory) {
        this.extremeHardTrajectory = extremeHardTrajectory;
    }

    public boolean isShortTrajectory() {
        if (difficulty == Difficulty.HARD) {
            return true;
        }
        if (difficulty == Difficulty.EXTREME) {
            return extremeHardTrajectory;
        }
        return false;
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

    public boolean isScoredThisShot() {
        return scoredThisShot;
    }
}