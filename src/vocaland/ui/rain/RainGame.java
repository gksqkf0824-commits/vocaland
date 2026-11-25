package vocaland.ui.rain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/*
 * RainGame - 산성비 게임 규칙 (점수, 목숨, 레벨, 코인, 하트 단어, 정답 판정)
 *
 * setWordPool()      : 출제할 단어 목록 교체 (같은 단어가 여러 번 들어 있으면 그만큼 자주 나옴)
 * recordMiss() / recordCorrect() : 게임 중 실시간 가중치 조절 (놓치면 +1, 연속 2번 맞히면 -1, 1~3배)
 * getUniqueMissedWords() / getMissCount() : 놓친 단어를 한 번씩, 단어별 놓친 횟수 (화면 표시용)
 * reset()            : 처음 상태로 초기화
 * start()            : 고른 모드로 게임 시작
 * update()           : 한 프레임 진행 (단어 생성 → 이동 → 바닥 충돌), 바닥에 닿은 단어 반환
 * submit()           : 입력값 판정, 맞힌 단어 반환 (오답이면 null)
 * getLastPoints()    : 마지막으로 맞힌 단어로 얻은 점수
 * getPointsPerWord() : 현재 레벨의 정답 점수 (100, 120, 140 ...)
 * getCoins()         : 점수로 받는 코인 (점수 ÷ 10, LocalData.finishGame과 같은 규칙)
 * currentSpeed()     : 현재 레벨의 낙하 속도
 * nextSpawnDelay()   : 다음 단어가 나올 때까지 시간 (평균 ± 랜덤)
 * spawnWord()        : 새 단어 하나 생성 (시작 5초 뒤부터 약 10% 확률로 하트 단어)
 * pickTier()         : 레벨별 비율(TIER_WEIGHTS)로 초급/중급/고급 중 하나 고르기
 * pickWord()         : 고른 난이도에서 화면에 없고 최근 RECENT_LIMIT개 안에 안 나온 단어 우선으로 고르기
 * pickX()            : 다른 단어와 겹치지 않는 가로 위치 고르기
 */
public class RainGame {

    public enum State { READY, PLAYING, GAME_OVER }

    public static final int START_LIVES = 3;
    public static final int MAX_LIVES = 3;
    public static final int CORRECT_PER_LEVEL = 10;
    private static final int BASE_POINTS = 100;
    private static final int POINTS_PER_LEVEL = 20;
    private static final int SCORE_PER_COIN = 10;

    private static final double BASE_SPEED = 0.88;
    private static final double SPEED_PER_LEVEL = 0.165;
    private static final double MIN_SPEED_RATE = 0.8;
    private static final double MAX_SPEED_RATE = 1.3;

    private static final int BASE_SPAWN_MS = 3000;
    private static final int SPAWN_MS_PER_LEVEL = 200;
    private static final int MIN_SPAWN_MS = 1200;
    private static final int SPAWN_RANDOM_MS = 700;

    private static final int HEART_START_MS = 5000;
    private static final double HEART_CHANCE = 0.1;

    private static final int SIDE_MARGIN = 12;

    private static final String[] TIERS = {"초급", "중급", "고급"};
    private static final int[][] TIER_WEIGHTS = {
            {100, 0, 0},
            {80, 20, 0},
            {60, 35, 5},
            {40, 45, 15},
    };
    private static final int LEVELS_PER_TIER_STEP = 2;
    private static final int RECENT_LIMIT = 8;
    private static final int PICK_TRIES = 30;
    private static final int MAX_WEIGHT = 3;
    private static final int CORRECT_TO_LOWER = 2;

    private List<String[]> wordPool;
    private final Map<String, List<String[]>> poolByTier = new LinkedHashMap<>();
    private final Deque<String> recentWords = new ArrayDeque<>();
    private final Map<String, String[]> pairByWord = new HashMap<>();
    private final Map<String, Integer> weights = new HashMap<>();
    private final Map<String, Integer> streaks = new HashMap<>();
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
        setWordPool(wordPool);
        reset();
    }

    public void setWordPool(List<String[]> wordPool) {
        if (wordPool == null || wordPool.isEmpty()) {
            return;
        }
        this.wordPool = new ArrayList<>(wordPool);
        poolByTier.clear();
        pairByWord.clear();
        weights.clear();
        streaks.clear();
        for (String tier : TIERS) {
            poolByTier.put(tier, new ArrayList<>());
        }
        for (String[] pair : wordPool) {
            if (pair.length > 2 && poolByTier.containsKey(pair[2])) {
                poolByTier.get(pair[2]).add(pair);
            }
            pairByWord.putIfAbsent(pair[0], pair);
            weights.merge(pair[0], 1, Integer::sum);
        }
    }

    private void recordMiss(String english) {
        streaks.put(english, 0);
        changeWeight(english, 1);
    }

    private void recordCorrect(String english) {
        if (weights.getOrDefault(english, 1) <= 1) {
            streaks.put(english, 0);
            return;
        }
        int streak = streaks.getOrDefault(english, 0) + 1;
        if (streak >= CORRECT_TO_LOWER) {
            changeWeight(english, -1);
            streak = 0;
        }
        streaks.put(english, streak);
    }

    private void changeWeight(String english, int delta) {
        String[] pair = pairByWord.get(english);
        int weight = weights.getOrDefault(english, 1);
        int next = Math.max(1, Math.min(MAX_WEIGHT, weight + delta));
        if (pair == null || next == weight) {
            return;
        }
        List<String[]> tier = pair.length > 2 ? poolByTier.get(pair[2]) : null;
        if (delta > 0) {
            wordPool.add(pair);
            if (tier != null) {
                tier.add(pair);
            }
        } else {
            removeOne(wordPool, english);
            if (tier != null) {
                removeOne(tier, english);
            }
        }
        weights.put(english, next);
    }

    private static void removeOne(List<String[]> list, String english) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i)[0].equals(english)) {
                list.remove(i);
                return;
            }
        }
    }

    public void reset() {
        fallingWords.clear();
        missedWords.clear();
        recentWords.clear();
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
                recordMiss(word.getEnglish());
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
        recordCorrect(target.getEnglish());
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

    private List<String[]> pickTier() {
        int[] weights = TIER_WEIGHTS[Math.min((level - 1) / LEVELS_PER_TIER_STEP, TIER_WEIGHTS.length - 1)];
        int total = 0;
        for (int i = 0; i < TIERS.length; i++) {
            if (!poolByTier.get(TIERS[i]).isEmpty()) {
                total += weights[i];
            }
        }
        if (total == 0) {
            return wordPool;
        }
        int roll = random.nextInt(total);
        for (int i = 0; i < TIERS.length; i++) {
            List<String[]> tier = poolByTier.get(TIERS[i]);
            if (tier.isEmpty()) {
                continue;
            }
            roll -= weights[i];
            if (roll < 0) {
                return tier;
            }
        }
        return wordPool;
    }

    private String[] pickWord() {
        List<String[]> pool = pickTier();
        String[] pair = pool.get(random.nextInt(pool.size()));
        for (int i = 0; i < PICK_TRIES && (isOnScreen(pair[0]) || recentWords.contains(pair[0])); i++) {
            pair = pool.get(random.nextInt(pool.size()));
        }
        recentWords.addLast(pair[0]);
        if (recentWords.size() > RECENT_LIMIT) {
            recentWords.removeFirst();
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

    public List<FallingWord> getUniqueMissedWords() {
        Map<String, FallingWord> unique = new LinkedHashMap<>();
        for (FallingWord word : missedWords) {
            unique.putIfAbsent(word.getEnglish(), word);
        }
        return new ArrayList<>(unique.values());
    }

    public int getMissCount(String english) {
        int count = 0;
        for (FallingWord word : missedWords) {
            if (word.getEnglish().equals(english)) {
                count++;
            }
        }
        return count;
    }
}
