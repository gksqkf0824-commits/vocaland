package vocaland.ui.basketball;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/*
 * BasketballGame - 농구 게임 물리 계산 및 규칙 (각도, 힘, 중력, 점수, 목숨, 오답 기록, 익스퍼트 물리 엔진)
 *
 * loadWordTiersFromFile()   : data/words.txt에서 단어별 난이도(초급/중급/고급) 정보 로드
 * startNewGame()            : 점수·목숨 초기화 후 플레이 시작
 * toReadyState()            : 모드 선택 대기 상태로 전환
 * nextQuestion()            : 새 문제와 3개의 골대 선택지 생성
 * adjustAngle()             : 발사 각도 조절 (0° ~ 90°)
 * adjustPower()             : 발사 힘 조절 (13.5 ~ 32.5)
 * shoot()                   : 현재 각도와 힘으로 공 발사 (vx, vy 계산)
 * updatePhysics()           : 난이도별(일반/익스퍼트) 한 프레임 물리 이동 및 판정 (0:진행, 1:정답, 2:오답, 3:게임오버)
 * updateEffect()            : 연출 타이머 진행 및 연출 완료 시 다음 단어 전환/게임오버 확정
 * startEffect()             : 성공(폭죽) / 오답('아닙니다!') / 홈런('HOME RUN') / 낙하('저런!') 연출 시작
 * updateNormalPhysics()     : 이지/하드 모드용 기존 골대 판정 및 바닥 도달 삭제 처리
 * updateExpertPhysics()     : 익스퍼트 모드용 백보드/림 탄성 반사, 끼임 방지, 그물 낙하 물리 엔진
 * resolveBackboard()        : 골대 백보드(사각 판)와 공 사이의 충돌 및 반사 계산
 * resolveCircleRim()        : 림 앞/뒷턱 원형 충돌체와 공 사이의 탄성 반사 계산
 * calculateScoreGain()      : 난이도 및 황금 세트(콤보) 보너스를 반영한 획득 점수 계산
 * applyMissState()          : 오답/실패 처리, 오답 노트 기록 및 네온 세트 방어 확률 판정
 * resetBall()               : 공 위치를 시작 지점으로 초기화
 * setFloorY()               : 현재 화면 비율에 맞춘 논리적 바닥 Y좌표 동기화
 * toReadyStateKeepProgress(): 진행 중인 점수를 유지한 채 모드 선택 화면으로 복귀
 * setSetEffect()            : 상점에서 장착한 세트 효과(클래식/네온/황금) 적용
 */
public class BasketballGame {

    public enum State { READY, PLAYING, GAME_OVER }
    public enum Difficulty { EASY, HARD, EXPERT }
    public enum EffectType { NONE, FIREWORKS, WRONG_HOOP, HOME_RUN, MISS_DROP }
    public enum SetEffect { CLASSIC, NEON, GOLD }

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
    public static final double GRAVITY = 0.72;
    public static final double MIN_POWER = 13.5;
    public static final double MAX_POWER = 32.5;

    public static final int HOOP_X = 438;
    public static final int[] HOOP_Y = {54, 176, 298};
    public static final int HOOP_W = 114;
    public static final int HOOP_H = 96;

    public static final int DURATION_FIREWORKS = 48;
    public static final int DURATION_TEXT_POP = 42;
    public static final int DURATION_HOME_RUN = 74;

    private final List<String[]> wordPairs;
    private final Map<String, String> wordTierMap = new HashMap<>(); // 영어단어 -> 초급/중급/고급
    private final Deque<String> recent8Words = new ArrayDeque<>();
    private final Set<String> wrongBoostWords = new HashSet<>();
    private int streakCount = 0; // 연속 정답 티어 상승용

    private BasketballMode mode = BasketballMode.MEANING;
    private Difficulty difficulty = Difficulty.EASY;
    private boolean expertHardTrajectory = true;
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
    private double power = 21.5;

    private boolean isShooting = false;
    private boolean scoredThisShot = false;
    private int pendingResultAfterFall = 0;
    private int stuckFrames = 0;
    private double floorY = BASE_H;

    private EffectType activeEffect = EffectType.NONE;
    private int effectFrame = 0;
    private int effectDuration = 0;
    private int pendingPostEffectResult = 0;
    private boolean nextQuestionSwappedInEffect = false;
    private boolean hideBallDuringEffect = false;
    private double fireworkX = HOOP_X + 50.0;
    private double fireworkY = HOOP_Y[1] + 40.0;
    private int lastEarnedPoints = 0;

    private SetEffect setEffect = SetEffect.CLASSIC;
    private boolean neonFirstShieldUsed = false;
    private boolean lastMissBlockedByNeon = false;
    private int blockedHeartIndex = -1;
    private int goldComboCount = 0;
    private int lastBrokenGoldCombo = 0;
    private long comboFadeOutStartMs = 0L;

    public BasketballGame(List<String[]> wordPairs) {
        this.wordPairs = wordPairs;
        loadWordTiersFromFile();
        nextQuestion();
    }

    private void loadWordTiersFromFile() {
        try {
            Path p = Path.of("data", "words.txt");
            if (Files.exists(p)) {
                for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                    String[] f = line.split("\\|");
                    if (f.length >= 5) {
                        wordTierMap.put(f[1].trim(), f[4].trim());
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    public void startNewGame() {
        score = 0;
        lives = 3;
        streakCount = 0;
        recent8Words.clear();
        wrongBoostWords.clear();
        missedEnglishWords.clear();
        wrongRecords.clear();
        clearEffect();
        neonFirstShieldUsed = false;
        goldComboCount = 0;
        lastBrokenGoldCombo = 0;
        comboFadeOutStartMs = 0L;
        resetBall();
        nextQuestion();
        state = State.PLAYING;
    }

    public void toReadyState() {
        state = State.READY;
        mode = BasketballMode.MEANING;
        difficulty = Difficulty.EASY;
        expertHardTrajectory = true;
        angleDeg = 52.0;
        power = 21.5;
        score = 0;
        lives = 3;
        streakCount = 0;
        goldComboCount = 0;
        lastBrokenGoldCombo = 0; comboFadeOutStartMs = 0L;
        neonFirstShieldUsed = false;
        missedEnglishWords.clear();
        wrongRecords.clear();
        clearEffect();
        resetBall();
        nextQuestion();
    }

    private void clearEffect() {
        activeEffect = EffectType.NONE;
        effectFrame = 0;
        effectDuration = 0;
        pendingPostEffectResult = 0;
        nextQuestionSwappedInEffect = false;
        hideBallDuringEffect = false;
        lastMissBlockedByNeon = false;
        blockedHeartIndex = -1;
    }

    public void nextQuestion() {
        List<String[]> candidates = new ArrayList<>();
        for (String[] pair : wordPairs) {
            if (!recent8Words.contains(pair[0])) {
                candidates.add(pair);
            }
        }
        if (candidates.size() < 3) {
            candidates = new ArrayList<>(wordPairs);
        }

        double r = Math.random();
        String targetTier;
        if (streakCount <= 1) {
            targetTier = (r < 0.80) ? "초급" : (r < 0.96 ? "중급" : "고급");
        } else if (streakCount <= 4) {
            targetTier = (r < 0.35) ? "초급" : (r < 0.85 ? "중급" : "고급");
        } else {
            targetTier = (r < 0.15) ? "초급" : (r < 0.50 ? "중급" : "고급");
        }

        int totalWeight = 0;
        for (String[] pair : candidates) {
            String tier = wordTierMap.getOrDefault(pair[0], "초급");
            int weight = tier.equals(targetTier) ? 3 : 1;
            if (wrongBoostWords.contains(pair[0])) {
                weight *= 4;
            }
            totalWeight += weight;
        }

        double pick = Math.random() * totalWeight;
        String[] selectedPair = candidates.get(0);
        for (String[] pair : candidates) {
            String tier = wordTierMap.getOrDefault(pair[0], "초급");
            int weight = tier.equals(targetTier) ? 3 : 1;
            if (wrongBoostWords.contains(pair[0])) {
                weight *= 4;
            }
            pick -= weight;
            if (pick <= 0) {
                selectedPair = pair;
                break;
            }
        }
        
        currentEnglish = selectedPair[0];
        currentMeaning = selectedPair[1];

        recent8Words.addLast(currentEnglish);
        if (recent8Words.size() > 8) {
            recent8Words.removeFirst();
        }

        List<String[]> distractors = new ArrayList<>(wordPairs);
        Collections.shuffle(distractors);

        int idx = (mode == BasketballMode.MEANING) ? 1 : 0;
        String correctChoice = selectedPair[idx];
        distractors.removeIf(p -> p[0].equals(currentEnglish) || p[idx].equals(correctChoice));

        List<String> optionList = new ArrayList<>();
        optionList.add(correctChoice);
        optionList.add(distractors.get(0)[idx]);
        optionList.add(distractors.get(1)[idx]);
        Collections.shuffle(optionList);

        for (int i = 0; i < 3; i++) {
            choices[i] = optionList.get(i);
            if (choices[i].equals(correctChoice)) {
                answerHoopIndex = i;
            }
        }
    }

    public void adjustAngle(double delta) {
        if (state != State.PLAYING || isShooting || isEffectPlaying()) {
            return;
        }
        angleDeg = Math.max(0.0, Math.min(90.0, angleDeg + delta));
    }

    public void adjustPower(double delta) {
        if (state != State.PLAYING || isShooting || isEffectPlaying()) {
            return;
        }
        power = Math.max(MIN_POWER, Math.min(MAX_POWER, power + delta));
    }

    public void shoot() {
        if (state != State.PLAYING || isShooting || isEffectPlaying()) {
            return;
        }
        isShooting = true;
        scoredThisShot = false;
        pendingResultAfterFall = 0;
        stuckFrames = 0;
        hideBallDuringEffect = false;
        prevBallCenterY = ballY + BALL_DRAW_RADIUS;

        double rad = Math.toRadians(angleDeg);
        vx = power * Math.cos(rad);
        vy = -power * Math.sin(rad);
    }

    public int updatePhysics() {
        if (state != State.PLAYING) {
            return 0;
        }
        if (isEffectPlaying()) {
            return updateEffect();
        }
        if (!isShooting) {
            return 0;
        }
        if (difficulty == Difficulty.EXPERT) {
            return updateExpertPhysics();
        } else {
            return updateNormalPhysics();
        }
    }

    private int updateEffect() {
        effectFrame++;

        if (activeEffect == EffectType.HOME_RUN
                && !nextQuestionSwappedInEffect
                && effectFrame >= (int) (effectDuration * 0.72)
                && pendingPostEffectResult != 3) {
            hideBallDuringEffect = false;
            resetBall();
            nextQuestion();
            nextQuestionSwappedInEffect = true;
        }

        if (effectFrame >= effectDuration) {
            int finalResult = pendingPostEffectResult;
            boolean alreadySwapped = nextQuestionSwappedInEffect;
            clearEffect();

            if (finalResult == 3) {
                state = State.GAME_OVER;
                return 3;
            }
            if (!alreadySwapped) {
                resetBall();
                nextQuestion();
            }
        }
        return 0;
    }

    private void startEffect(EffectType type, int duration, int postResult, boolean hideBall) {
        this.activeEffect = type;
        this.effectFrame = 0;
        this.effectDuration = duration;
        this.pendingPostEffectResult = postResult;
        this.nextQuestionSwappedInEffect = false;
        this.hideBallDuringEffect = hideBall;
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
                    calculateScoreGain();
                    fireworkX = HOOP_X + 52.0;
                    fireworkY = HOOP_Y[i] + 45.0;
                    startEffect(EffectType.FIREWORKS, DURATION_FIREWORKS, 1, true);
                    return 1;
                } else {
                    int missRes = applyMissState(choices[i]);
                    startEffect(EffectType.WRONG_HOOP, DURATION_TEXT_POP, missRes, true);
                    return 2;
                }
            }
        }

        if (ballY >= floorY + 15.0) {
            isShooting = false;
            boolean isHomeRun = (ballX + BALL_DRAW_RADIUS) > (HOOP_X + HOOP_W);
            if (isHomeRun) {
                int missRes = applyMissState("(골대 넘어감)");
                if (lastMissBlockedByNeon) {
                    startEffect(EffectType.MISS_DROP, DURATION_TEXT_POP, missRes, true);
                } else {
                    startEffect(EffectType.HOME_RUN, DURATION_HOME_RUN, missRes, true);
                }
            } else {
                int missRes = applyMissState("(골대 빗나감)");
                startEffect(EffectType.MISS_DROP, DURATION_TEXT_POP, missRes, true);
            }
            return 2;
        }
        return 0;
    }

    private int updateExpertPhysics() {
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
                            calculateScoreGain();
                            fireworkX = (frontRimX + backRimX) / 2.0;
                            fireworkY = rimY;
                            pendingResultAfterFall = 1;
                            immediateEvent = 1;
                        } else {
                            pendingResultAfterFall = applyMissState(choices[i]);
                            immediateEvent = 2;
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

            if (ballY >= floorY + 15.0) {
                isShooting = false;
                if (scoredThisShot) {
                    if (pendingResultAfterFall == 1) {
                        startEffect(EffectType.FIREWORKS, DURATION_FIREWORKS, 1, true);
                    } else {
                        startEffect(EffectType.WRONG_HOOP, DURATION_TEXT_POP, pendingResultAfterFall, true);
                    }
                    return immediateEvent;
                } else {
                    boolean isHomeRun = (ballX + BALL_DRAW_RADIUS) > (HOOP_X + HOOP_W);
                    if (isHomeRun) {
                        int missRes = applyMissState("(골대 넘어감)");
                        if (lastMissBlockedByNeon) {
                            startEffect(EffectType.MISS_DROP, DURATION_TEXT_POP, missRes, true);
                        } else {
                            startEffect(EffectType.HOME_RUN, DURATION_HOME_RUN, missRes, true);
                        }
                    } else {
                        int missRes = applyMissState("(골대 빗나감)");
                        startEffect(EffectType.MISS_DROP, DURATION_TEXT_POP, missRes, true);
                    }
                    return 2;
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

    private int getBaseScoreForCurrentDifficulty() {
        if (difficulty == Difficulty.HARD) {
            return 140;
        }
        if (difficulty == Difficulty.EXPERT) {
            return expertHardTrajectory ? 300 : 120;
        }
        return 100;
    }

    private int calculateScoreGain() {
        streakCount++;
        wrongBoostWords.remove(currentEnglish);

        int basePoints = getBaseScoreForCurrentDifficulty();
        int bonus = 0;
        if (setEffect == SetEffect.GOLD) {
            goldComboCount++;
            bonus = goldComboCount * 10;
        }
        lastEarnedPoints = basePoints + bonus;
        score += lastEarnedPoints;
        return lastEarnedPoints;
    }

    private int applyMissState(String selectedChoice) {
        lastMissBlockedByNeon = false;
        blockedHeartIndex = -1;
        if (goldComboCount > 0) {
            lastBrokenGoldCombo = goldComboCount;
            comboFadeOutStartMs = System.currentTimeMillis();
        }
        goldComboCount = 0;
        streakCount = 0;
        wrongBoostWords.add(currentEnglish);

        missedEnglishWords.add(currentEnglish);
        wrongRecords.add(new WrongRecord(getQuestionText(), getAnswerText(), selectedChoice));

        if (setEffect == SetEffect.NEON) {
            boolean blocked = false;
            if (!neonFirstShieldUsed) {
                neonFirstShieldUsed = true;
                blocked = true;
            } else if (Math.random() < 0.20) {
                blocked = true;
            }
            if (blocked) {
                lastMissBlockedByNeon = true;
                blockedHeartIndex = Math.max(0, lives - 1);
                return 2;
            }
        }

        lives--;
        return (lives <= 0) ? 3 : 2;
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
        hideBallDuringEffect = false;
    }

    public void setFloorY(double floorY) {
        this.floorY = Math.max(BASE_H, floorY);
    }

    public String getCurrentEnglish() {
        return currentEnglish;
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
        if (difficulty == Difficulty.EXPERT) {
            this.expertHardTrajectory = true;
        }
    }

    public boolean isExpertMode() {
        return difficulty == Difficulty.EXPERT;
    }

    public boolean isExpertHardTrajectory() {
        return expertHardTrajectory;
    }

    public void setExpertHardTrajectory(boolean expertHardTrajectory) {
        this.expertHardTrajectory = expertHardTrajectory;
        this.angleDeg = 52.0;
        this.power = 21.5;
    }

    public boolean isShortTrajectory() {
        if (difficulty == Difficulty.HARD) {
            return true;
        }
        if (difficulty == Difficulty.EXPERT) {
            return expertHardTrajectory;
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

    public boolean isBallHidden() {
        return hideBallDuringEffect;
    }

    public boolean isEffectPlaying() {
        return activeEffect != EffectType.NONE;
    }

    public EffectType getActiveEffect() {
        return activeEffect;
    }

    public double getEffectProgress() {
        if (effectDuration <= 0) {
            return 1.0;
        }
        return Math.min(1.0, (double) effectFrame / effectDuration);
    }

    public double getFireworkX() {
        return fireworkX;
    }

    public double getFireworkY() {
        return fireworkY;
    }

    public void toReadyStateKeepProgress() {
        state = State.READY;
        clearEffect();
        resetBall();
    }

    public void resetGoldCombo() {
        goldComboCount = 0;
        lastBrokenGoldCombo = 0;
        comboFadeOutStartMs = 0L;
    }

    public SetEffect getSetEffect() { return setEffect; }
    public void setSetEffect(SetEffect setEffect) {
        this.setEffect = setEffect;
    }
    public boolean isGoldComboActive() { return setEffect == SetEffect.GOLD && goldComboCount > 0; }
    public int getGoldComboCount() { return goldComboCount; }
    public int getLastBrokenGoldCombo() { return lastBrokenGoldCombo; }
    public long getComboFadeOutStartMs() { return comboFadeOutStartMs; }
    public int getLastEarnedPoints() { return lastEarnedPoints; }
    public boolean isLastMissBlockedByNeon() { return lastMissBlockedByNeon; }
    public int getBlockedHeartIndex() { return blockedHeartIndex; }
}