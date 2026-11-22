package vocaland.ui.rain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/*
 * RainGame - 산성비 게임 규칙 (점수, 목숨, 레벨, 코인, 하트 단어, 정답 판정)
 *
 * reset()            : 처음 상태로 초기화
 * start()            : 고른 모드로 게임 시작
 * update()           : 한 프레임 진행 (단어 생성 → 이동 → 바닥 충돌), 바닥에 닿은 단어 반환
 * submit()           : 입력값 판정, 맞힌 단어 반환 (오답이면 null)
 * getLastPoints()    : 마지막으로 맞힌 단어로 얻은 점수
 * getPointsPerWord() : 현재 레벨의 정답 점수 (100, 120, 140 ...)
 * getCoins()         : 점수로 받는 코인 (점수 ÷ 100)
 * currentSpeed()     : 현재 레벨의 낙하 속도
 * nextSpawnDelay()   : 다음 단어가 나올 때까지 시간 (평균 ± 랜덤)
 * spawnWord()        : 새 단어 하나 생성 (시작 5초 뒤부터 약 10% 확률로 하트 단어)
 * pickWord()         : 화면에 없는 단어 우선으로 고르기
 * pickX()            : 다른 단어와 겹치지 않는 가로 위치 고르기
 */
public class RainGame {

    public enum State { READY, PLAYING, GAME_OVER }

    public static final int START_LIVES = 3;
    public static final int MAX_LIVES = 3;
    public static final int CORRECT_PER_LEVEL = 10;
    private static final int BASE_POINTS = 100;
    private static final int POINTS_PER_LEVEL = 20;
    private static final int SCORE_PER_COIN = 100;

    private static final double BASE_SPEED = 0.8;
    private static final double SPEED_PER_LEVEL = 0.15;
    private static final double MIN_SPEED_RATE = 0.8;
    private static final double MAX_SPEED_RATE = 1.3;

    private static final int BASE_SPAWN_MS = 3000;
    private static final int SPAWN_MS_PER_LEVEL = 200;
    private static final int MIN_SPAWN_MS = 1200;
    private static final int SPAWN_RANDOM_MS = 700;

    private static final int HEART_START_MS = 5000;
    private static final double HEART_CHANCE = 0.1;

    private static final int SIDE_MARGIN = 12;

    private final List<String[]> wordPool;
    private final List<FallingWord> fallingWords = new ArrayList<>();
    private final List<FallingWord> missedWords = new ArrayList<>();
    private final Random random = new Random();

    private State state = State.READY;
    private InputMode mode = InputMode.SPELLING;
    private int score;
    private int lives;
    private int level;
    private int correctCount;
    private int lastPoints;
    private int playMs;
    private int msUntilSpawn;

    public RainGame(List<String[]> wordPool) {
        if (wordPool == null || wordPool.isEmpty()) {
            throw new IllegalArgumentException("단어 목록이 비어 있습니다.");
        }
        this.wordPool = wordPool;
        reset();
    }

    public void reset() {
        fallingWords.clear();
        missedWords.clear();
        score = 0;
        lives = START_LIVES;
        level = 1;
        correctCount = 0;
        lastPoints = 0;
        playMs = 0;
        msUntilSpawn = 0;
        state = State.READY;
    }

    public void start(InputMode mode) {
        reset();
        this.mode = mode;
        state = State.PLAYING;
    }

    public List<FallingWord> update(int elapsedMs, int areaWidth, int groundY) {
        List<FallingWord> landed = new ArrayList<>();
        if (state != State.PLAYING) {
            return landed;
        }

        playMs += elapsedMs;
        msUntilSpawn -= elapsedMs;
        if (msUntilSpawn <= 0) {
            spawnWord(areaWidth);
            msUntilSpawn = nextSpawnDelay();
        }

        double speed = currentSpeed();
        Iterator<FallingWord> it = fallingWords.iterator();
        while (it.hasNext()) {
            FallingWord word = it.next();
            word.fall(speed);

            if (word.getY() + FallingWord.BOX_HEIGHT >= groundY) {
                it.remove();
                landed.add(word);
                missedWords.add(word);
                lives--;
            }
        }

        if (lives <= 0) {
            lives = 0;
            state = State.GAME_OVER;
        }
        return landed;
    }

    public FallingWord submit(String input) {
        lastPoints = 0;
        if (state != State.PLAYING || input == null || input.isBlank()) {
            return null;
        }

        FallingWord target = null;
        for (FallingWord word : fallingWords) {
            if (word.isAnswer(input, mode)
                    && (target == null || word.getY() > target.getY())) {
                target = word;
            }
        }
        if (target == null) {
            return null;
        }

        fallingWords.remove(target);
        lastPoints = getPointsPerWord();
        score += lastPoints;
        correctCount++;
        level = correctCount / CORRECT_PER_LEVEL + 1;
        if (target.isHeart()) {
            lives = Math.min(MAX_LIVES, lives + 1);
        }
        return target;
    }

    public int getLastPoints() {
        return lastPoints;
    }

    public int getPointsPerWord() {
        return BASE_POINTS + POINTS_PER_LEVEL * (level - 1);
    }

    public int getCoins() {
        return score / SCORE_PER_COIN;
    }

    private double currentSpeed() {
        return BASE_SPEED + SPEED_PER_LEVEL * (level - 1);
    }

    private int nextSpawnDelay() {
        int average = Math.max(MIN_SPAWN_MS, BASE_SPAWN_MS - SPAWN_MS_PER_LEVEL * (level - 1));
        int randomPart = random.nextInt(SPAWN_RANDOM_MS * 2 + 1) - SPAWN_RANDOM_MS;
        return Math.max(400, average + randomPart);
    }

    private void spawnWord(int areaWidth) {
        String[] pair = pickWord();
        String english = pair[0];
        String meaning = pair[1];
        double x = pickX(areaWidth, FallingWord.BOX_WIDTH / 2);
        double rate = MIN_SPEED_RATE + random.nextDouble() * (MAX_SPEED_RATE - MIN_SPEED_RATE);
        boolean heart = playMs >= HEART_START_MS && random.nextDouble() < HEART_CHANCE;

        fallingWords.add(new FallingWord(english, meaning, x, rate, heart));
    }

    private String[] pickWord() {
        String[] pair = wordPool.get(random.nextInt(wordPool.size()));
        for (int i = 0; i < 10 && isOnScreen(pair[0]); i++) {
            pair = wordPool.get(random.nextInt(wordPool.size()));
        }
        return pair;
    }

    private boolean isOnScreen(String english) {
        for (FallingWord word : fallingWords) {
            if (word.getEnglish().equals(english)) {
                return true;
            }
        }
        return false;
    }

    private double pickX(int areaWidth, int halfWidth) {
        int min = SIDE_MARGIN + halfWidth;
        int max = areaWidth - SIDE_MARGIN - halfWidth;
        if (max <= min) {
            return areaWidth / 2.0;
        }

        double x = min;
        for (int tries = 0; tries < 15; tries++) {
            x = min + random.nextInt(max - min + 1);
            if (!overlapsTopWords(x, halfWidth)) {
                break;
            }
        }
        return x;
    }

    private boolean overlapsTopWords(double x, int halfWidth) {
        for (FallingWord word : fallingWords) {
            if (word.getY() > FallingWord.BOX_HEIGHT * 3) {
                continue;
            }
            if (Math.abs(x - word.getX()) < halfWidth * 2 + 10) {
                return true;
            }
        }
        return false;
    }

    public State getState() {
        return state;
    }

    public InputMode getMode() {
        return mode;
    }

    public int getScore() {
        return score;
    }

    public int getLives() {
        return lives;
    }

    public int getLevel() {
        return level;
    }

    public List<FallingWord> getFallingWords() {
        return Collections.unmodifiableList(fallingWords);
    }

    public List<FallingWord> getMissedWords() {
        return Collections.unmodifiableList(missedWords);
    }
}
