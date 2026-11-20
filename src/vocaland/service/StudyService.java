package vocaland.service;

import vocaland.model.StudyRecord;
import vocaland.model.Word;
import vocaland.model.WordCard;
import vocaland.repository.RecordRepository;
import vocaland.repository.WordRepository;
import vocaland.repository.TextFileStore;
import java.nio.file.Files;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/** 단어 학습, 복습 체크, 카드 짝맞추기 및 산성비 가중치 엔진. GUI와 분리되어 있다. */
public final class StudyService implements StudyChannel {
    private final WordService wordService;
    private final RecordRepository recordRepository;
    private final RecordRepository reviewRepository;
    private final Random random;
    private final List<Word> words;
    private final List<StudyRecord> records;
    private final List<StudyRecord> reviewEvents;

    /** 프로젝트 루트의 data/words.txt와 data/records.txt 사용. */
    public StudyService() throws IOException {
        this(Path.of("data", "words.txt"), Path.of("data", "records.txt"));
    }

    public StudyService(Path wordsFile, Path recordsFile) throws IOException {
        this(wordsFile, recordsFile, new Random());
    }

    public StudyService(Path wordsFile, Path recordsFile, Random random) throws IOException {
        this(wordsFile, recordsFile, siblingReviewFile(recordsFile), random);
    }

    /** 테스트 또는 팀 통합용. 단어 저장은 be-core WordRepository를 그대로 이용한다. */
    public StudyService(Path wordsFile, Path recordsFile, Path reviewsFile, Random random) throws IOException {
        this(makeWordService(wordsFile), new RecordRepository(recordsFile),
                new RecordRepository(reviewsFile), random);
    }

    /** 팀장이 만든 WordService를 직접 공유할 때 사용하는 생성자. */
    public StudyService(WordService wordService, RecordRepository recordRepository,
                        RecordRepository reviewRepository, Random random) throws IOException {
        if (wordService == null || recordRepository == null || reviewRepository == null || random == null) {
            throw new IllegalArgumentException("학습 서비스를 생성할 인자가 부족합니다.");
        }
        this.wordService = wordService;
        this.recordRepository = recordRepository;
        this.reviewRepository = reviewRepository;
        this.random = random;
        this.words = new ArrayList<>(wordService.getWords());
        if (words.isEmpty()) throw new IOException("단어가 비어 있습니다. data/words.txt를 확인하세요.");
        this.records = new ArrayList<>();
        this.reviewEvents = new ArrayList<>();
        // 예전 버전 records.txt에 남은 복습 기록을 읽을 때만 호환한다.
        for (StudyRecord entry : recordRepository.load()) {
            if (entry.getResult() == StudyRecord.Result.CORRECT
                    || entry.getResult() == StudyRecord.Result.WRONG) records.add(entry);
            else reviewEvents.add(entry);
        }
        reviewEvents.addAll(reviewRepository.load());
    }

    private static WordService makeWordService(Path file) throws IOException {
        if (file == null || !Files.isRegularFile(file)) {
            throw new IOException("팀 공통 단어 파일이 없습니다: " + file);
        }
        return new WordService(new WordRepository(new TextFileStore(file)));
    }

    private static Path siblingReviewFile(Path recordsFile) {
        if (recordsFile == null) throw new IllegalArgumentException("기록 파일 경로가 필요합니다.");
        return recordsFile.resolveSibling("reviews.txt");
    }

    @Override
    public List<Word> getWords(String theme) {
        List<Word> selected = new ArrayList<>();
        boolean all = theme == null || theme.isBlank() || ALL.equals(theme.trim());
        for (Word word : words) {
            if (all || word.getTheme().equals(theme.trim())) selected.add(word);
        }
        return Collections.unmodifiableList(selected);
    }

    @Override
    public List<Word> getStudyWords(String userId, String source) {
        if (REVIEW.equals(source)) return getWordsToReview(userId);
        return getWords(source);
    }

    @Override
    public List<String> getThemes() {
        LinkedHashSet<String> themes = new LinkedHashSet<>();
        for (Word word : words) themes.add(word.getTheme());
        return Collections.unmodifiableList(new ArrayList<>(themes));
    }

    @Override
    public List<Word> searchWords(String keyword) {
        if (keyword == null || keyword.isBlank()) return getWords(null);
        String query = keyword.strip().toLowerCase(Locale.ROOT);
        List<Word> found = new ArrayList<>();
        for (Word word : words) {
            if (word.getId().toLowerCase(Locale.ROOT).contains(query)
                    || word.getEnglish().toLowerCase(Locale.ROOT).contains(query)
                    || word.getMeaning().contains(query)
                    || word.getTheme().contains(query)
                    || word.getDifficulty().contains(query)) found.add(word);
        }
        return Collections.unmodifiableList(found);
    }

    @Override
    public Word getWord(String id) {
        if (id == null) return null;
        for (Word word : words) if (word.getId().equals(id)) return word;
        return null;
    }

    /**
     * 산성비 담당자의 AcidRainPanel(List<String[]>) 생성자용 데이터.
     * words.txt의 ID/테마/난이도는 노출하지 않고 {영어, 뜻}만 전달한다.
     * 호출마다 새 List/배열을 만들어 GUI가 목록을 섞어도 내부 데이터는 변하지 않는다.
     */
    @Override
    public List<String[]> loadWordPairs() {
        List<String[]> pairs = new ArrayList<>();
        for (Word word : words) {
            pairs.add(new String[] {word.getEnglish(), word.getMeaning()});
        }
        return pairs;
    }

    /**
     * 산성비 전용: 개인 오답 가중치와 '다시 볼 단어' 체크를 반영한 {영어, 뜻} 목록.
     * RainGame이 리스트에서 균등하게 뽑는 경우 중복된 항목의 횟수만큼
     * 당첨 확률이 증가한다. 특정 게임이 중복 제거/순차 추출을 하면
     * 기대한 가중치가 보장되지 않으므로 게임 담당자와 확인해야 한다.
     */
    @Override
    public List<String[]> loadWeightedWordPairs(String userId) {
        requireUser(userId);
        List<String[]> pairs = new ArrayList<>();
        for (Word word : words) {
            int repeats = getWordWeight(userId, word.getId());
            for (int i = 0; i < repeats; i++) {
                // GUI가 배열을 수정해도 학습 저장소는 안전하도록 개별 배열 생성
                pairs.add(new String[] {word.getEnglish(), word.getMeaning()});
            }
        }
        return pairs;
    }

    /** 농구 현재 UI 전용: 중복된 단어 없이 전달. BasketballGame은 앞의 3개를 선택지로 쓴다. */
    @Override
    public List<String[]> loadBasketballWordPairs() {
        return loadWordPairs();
    }

    /**
     * 카드/산성비 최종 출제 가중치(1~3배).
     * 오답 기록 기반 숙련도 가중치(1~3)에, 수동 '다시 볼 단어' 표시 시 +1을 더한다.
     * 최종 상한은 3배. 체크를 해제하면 수동 보너스만 사라지며 오답 이력은 유지된다.
     * 정답 2회에 따른 감소는 오답 기반 가중치에만 적용되고 수동 보너스는 유지된다.
     */
    @Override
    public int getWordWeight(String userId, String wordId) {
        int wrongWeight = calculateWeightState(userId, wordId).weight;
        return Math.min(3, wrongWeight + (isReview(userId, wordId) ? 1 : 0));
    }

    /** 가중치가 한 단계 내려가기까지 쌓인 연속 정답 수 (0 또는 1). */
    @Override
    public int getCorrectStreak(String userId, String wordId) {
        return calculateWeightState(userId, wordId).correctStreak;
    }

    private WeightState calculateWeightState(String userId, String wordId) {
        requireUser(userId);
        requireWord(wordId);
        int weight = 1;
        int streak = 0;
        // 파일에 쓰여진 순서가 곧 풀이 순서다. 동일 날짜 기록도 순서를 보존한다.
        for (StudyRecord record : records) {
            if (!record.getUserId().equals(userId) || !record.getWordId().equals(wordId)) continue;
            if (record.getResult() == StudyRecord.Result.WRONG) {
                weight = Math.min(3, weight + 1);
                streak = 0;
            } else if (record.getResult() == StudyRecord.Result.CORRECT) {
                if (weight > 1) {
                    streak++;
                    if (streak == 2) {
                        weight--;
                        streak = 0;
                    }
                } else {
                    streak = 0; // 1배는 기본값이므로 더 내려가지 않는다.
                }
            }
        }
        return new WeightState(weight, streak);
    }

    private static final class WeightState {
        final int weight;
        final int correctStreak;
        WeightState(int weight, int correctStreak) {
            this.weight = weight;
            this.correctStreak = correctStreak;
        }
    }

    /**
     * 산성비 onGameOver에서 missed의 각 FallingWord.getEnglish()로 호출한다.
     * GUI의 FallingWord 타입에 직접 의존하지 않으며 기존 records.txt 형식으로 저장한다.
     */
    @Override
    public void recordWrongByEnglish(String userId, String english) throws IOException {
        recordAnswer(userId, findWordByEnglish(english).getId(), false);
    }

    /** 산성비에서 단어를 정확히 입력해 제거한 순간 프론트가 1회 호출한다. */
    @Override
    public void recordCorrectByEnglish(String userId, String english) throws IOException {
        recordAnswer(userId, findWordByEnglish(english).getId(), true);
    }

    private Word findWordByEnglish(String english) {
        if (english == null || english.isBlank()) {
            throw new IllegalArgumentException("학습 기록에 영어 단어가 필요합니다.");
        }
        Word matched = null;
        for (Word word : words) {
            if (word.getEnglish().equalsIgnoreCase(english.trim())) {
                if (matched != null) {
                    throw new IllegalArgumentException("영어가 중복되어 단어 ID를 결정할 수 없습니다: " + english);
                }
                matched = word;
            }
        }
        if (matched == null) {
            throw new IllegalArgumentException("단어장에 없는 영어입니다: " + english);
        }
        return matched;
    }

    @Override
    public Word getRandomWord() { return getRandomWord(null); }

    @Override
    public Word getRandomWord(String theme) {
        List<Word> pool = getWords(theme);
        if (pool.isEmpty()) throw new IllegalArgumentException("출제할 단어가 없습니다: " + theme);
        return pool.get(random.nextInt(pool.size()));
    }

    /** 산성비 화면에서 이미 내려오는 단어와 중복되지 않도록 선택. 남은 단어가 없으면 null. */
    @Override
    public Word getRandomWordExcluding(Set<String> excludedWordIds) {
        Set<String> excluded = excludedWordIds == null ? Set.of() : excludedWordIds;
        List<Word> available = new ArrayList<>();
        for (Word word : words) if (!excluded.contains(word.getId())) available.add(word);
        return available.isEmpty() ? null : available.get(random.nextInt(available.size()));
    }

    /**
     * 현재 학습 가중치에 비례해 단어를 1개 추출한다.
     * 전체 가중치가 N이면 [0, N) 중 하나를 뽑는 방식이며 단어 중복 없음.
     */
    @Override
    public Word getWeightedRandomWord(String userId, String theme) {
        requireUser(userId);
        List<Word> pool = new ArrayList<>(getWords(theme));
        if (pool.isEmpty()) throw new IllegalArgumentException("출제할 단어가 없습니다: " + theme);
        return removeWeightedRandom(pool, userId);
    }

    /**
     * 단어 1개를 가중치에 따라 뽑아서 후보 목록에서 제거한다.
     * 카드 짝맞추기는 같은 단어를 두 쌍 뽑지 않으려고 제거가 필요하다.
     */
    private Word removeWeightedRandom(List<Word> pool, String userId) {
        if (pool.isEmpty()) throw new IllegalArgumentException("출제 후보가 없습니다.");
        int totalWeight = 0;
        for (Word word : pool) totalWeight += getWordWeight(userId, word.getId());
        int slot = random.nextInt(totalWeight);
        for (int i = 0; i < pool.size(); i++) {
            Word word = pool.get(i);
            slot -= getWordWeight(userId, word.getId());
            if (slot < 0) return pool.remove(i);
        }
        throw new IllegalStateException("가중치 추출 실패");
    }

    @Override
    public List<Word> getRandomWords(int count) { return getRandomWords(null, count); }

    @Override
    public List<Word> getRandomWords(String theme, int count) {
        List<Word> pool = new ArrayList<>(getWords(theme));
        checkCount(count, pool.size());
        Collections.shuffle(pool, random);
        return new ArrayList<>(pool.subList(0, count));
    }

    /** 선택 범위에서 서로 다른 단어를 랜덤 추출. 복습 단어 6개 미만일 때는 전체 단어로 채운다. */
    @Override
    public List<Word> getPairWords(String userId, int count) {
        return getPairWords(userId, ALL, count);
    }

    @Override
    public List<Word> getPairWords(String userId, String source, int count) {
        requireUser(userId);
        validatePairSource(source); // 테마 오타가 전체 카드 출제로 조용히 바뀌는 것을 방지
        if (count < 1 || count > words.size()) {
            throw new IllegalArgumentException("선택할 카드 쌍 개수가 잘못되었습니다: " + count);
        }
        // 1) 선택한 범위(전체/테마/다시 볼 단어)에서 먼저 가중치 추출
        List<Word> pool = new ArrayList<>(getStudyWords(userId, source));
        List<Word> result = new ArrayList<>();
        Set<String> used = new HashSet<>();
        while (!pool.isEmpty() && result.size() < count) {
            Word picked = removeWeightedRandom(pool, userId);
            result.add(picked);
            used.add(picked.getId());
        }
        // 2) 범위에 6개 미만이면 다른 테마 단어에서 보충(여기도 가중치 적용)
        if (result.size() < count) {
            List<Word> remaining = new ArrayList<>();
            for (Word word : words) if (!used.contains(word.getId())) remaining.add(word);
            while (!remaining.isEmpty() && result.size() < count) {
                Word picked = removeWeightedRandom(remaining, userId);
                result.add(picked);
                used.add(picked.getId());
            }
        }
        return result;
    }

    private void validatePairSource(String source) {
        if (source == null || source.isBlank() || ALL.equals(source) || REVIEW.equals(source)) return;
        for (String theme : getThemes()) {
            if (theme.equals(source)) return;
        }
        throw new IllegalArgumentException("카드 출제 범위를 확인하세요: " + source);
    }

    @Override
    public List<WordCard> createMatchingCards(String userId, int pairCount) {
        return createMatchingCards(userId, ALL, pairCount);
    }

    @Override
    public List<WordCard> createMatchingCards(String userId, String source, int pairCount) {
        List<WordCard> cards = new ArrayList<>();
        for (Word word : getPairWords(userId, source, pairCount)) {
            cards.add(new WordCard(word.getId(), word.getEnglish(), WordCard.Side.ENGLISH));
            cards.add(new WordCard(word.getId(), word.getMeaning(), WordCard.Side.KOREAN));
        }
        Collections.shuffle(cards, random);
        return cards;
    }

    @Override
    public boolean isPair(WordCard a, WordCard b) {
        return a != null && b != null && a != b
                && a.getSide() != b.getSide() && a.getWordId().equals(b.getWordId());
    }

    /**
     * 카드 GUI에서 서로 맞는 쌍이 최초로 완성된 시점에만 호출한다.
     * isPair()는 기록을 남기지 않는 순수 판정 함수다.
     * 잘못 맞춘 두 카드는 특정 단어의 오답이라고 단정할 수 없으므로 기록하지 않는다.
     */
    @Override
    public void recordMatchedPair(String userId, WordCard a, WordCard b) throws IOException {
        if (!isPair(a, b)) throw new IllegalArgumentException("서로 맞지 않는 카드입니다.");
        recordAnswer(userId, a.getWordId(), true);
    }

    /** 산성비: 내려오는 '사과'를 보고 사용자가 apple을 입력하는 경우. 영어 대소문자는 무시한다. */
    @Override
    public boolean checkEnglish(Word word, String input) {
        return word != null && input != null
                && word.getEnglish().equalsIgnoreCase(input.trim());
    }

    /** 농구/기본 뜻 확인: 영어 단어의 한글 뜻 비교. */
    @Override
    public boolean checkMeaning(Word word, String input) {
        return word != null && input != null && word.getMeaning().equals(input.trim());
    }

    @Override
    public boolean checkAnswer(String wordId, String input) {
        Word word = getWord(wordId);
        if (word == null) throw new IllegalArgumentException("존재하지 않는 단어 ID: " + wordId);
        return checkMeaning(word, input);
    }

    @Override
    public void recordAnswer(String userId, String wordId, boolean correct) throws IOException {
        requireUser(userId);
        requireWord(wordId);
        append(new StudyRecord(userId, wordId,
                correct ? StudyRecord.Result.CORRECT : StudyRecord.Result.WRONG, LocalDate.now()));
    }

    @Override
    public void setReview(String userId, String wordId, boolean selected) throws IOException {
        requireUser(userId);
        requireWord(wordId);
        if (isReview(userId, wordId) == selected) return; // 중복 클릭은 기록하지 않음
        appendReview(new StudyRecord(userId, wordId,
                selected ? StudyRecord.Result.REVIEW_ON : StudyRecord.Result.REVIEW_OFF, LocalDate.now()));
    }

    @Override
    public void markReview(String userId, String wordId) throws IOException {
        setReview(userId, wordId, true);
    }

    @Override
    public boolean isReview(String userId, String wordId) {
        requireUser(userId);
        // 같은 단어의 '마지막 복습 체크 이벤트'가 현재 상태.
        boolean selected = false;
        for (StudyRecord record : reviewEvents) {
            if (record.getUserId().equals(userId) && record.getWordId().equals(wordId)) {
                if (record.getResult().marksReview()) selected = true;
                if (record.getResult() == StudyRecord.Result.REVIEW_OFF) selected = false;
            }
        }
        return selected;
    }

    @Override
    public List<Word> getReviewedWords(String userId) {
        requireUser(userId);
        List<Word> marked = new ArrayList<>();
        for (Word word : words) if (isReview(userId, word.getId())) marked.add(word);
        return Collections.unmodifiableList(marked);
    }

    /**
     * 다시 볼 단어: 오답 기반 가중치가 2~3인 미숙련 단어 + 사용자가 직접 체크한 단어.
     * 연속 정답으로 오답 기반 가중치가 1까지 내려가면 자동 복습 대상에서 제외한다.
     * 단, 수동 체크는 사용자가 직접 해제할 때까지 유지한다.
     * 누적 오답 내역 조회(getWrongWords)는 별도로 보존한다.
     */
    @Override
    public List<Word> getWordsToReview(String userId) {
        requireUser(userId);
        List<Word> result = new ArrayList<>();
        for (Word word : words) {
            if (calculateWeightState(userId, word.getId()).weight > 1
                    || isReview(userId, word.getId())) {
                result.add(word);
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public int getWrongCount(String userId, String wordId) {
        requireUser(userId);
        int count = 0;
        for (StudyRecord r : records) {
            if (r.getUserId().equals(userId) && r.getWordId().equals(wordId)
                    && r.getResult() == StudyRecord.Result.WRONG) count++;
        }
        return count;
    }

    @Override
    public List<Word> getWrongWords(String userId) {
        requireUser(userId);
        List<Word> result = new ArrayList<>();
        for (Word word : words) if (getWrongCount(userId, word.getId()) > 0) result.add(word);
        result.sort((a, b) -> {
            int byCount = Integer.compare(getWrongCount(userId, b.getId()), getWrongCount(userId, a.getId()));
            return byCount != 0 ? byCount : a.getId().compareTo(b.getId());
        });
        return Collections.unmodifiableList(result);
    }

    @Override
    public List<StudyRecord> getRecords(String userId) {
        requireUser(userId);
        List<StudyRecord> found = new ArrayList<>();
        for (StudyRecord record : records) if (record.getUserId().equals(userId)) found.add(record);
        return Collections.unmodifiableList(found);
    }

    @Override
    public int countRecords(String userId, StudyRecord.Result result) {
        requireUser(userId);
        if (result == null) throw new IllegalArgumentException("집계할 결과가 필요합니다.");
        int n = 0;
        for (StudyRecord record : records) {
            if (record.getUserId().equals(userId) && record.getResult() == result) n++;
        }
        return n;
    }

    private void append(StudyRecord record) throws IOException {
        recordRepository.append(record); // 파일 쓰기가 성공했을 때에만 메모리에 반영
        records.add(record);
    }

    private void appendReview(StudyRecord entry) throws IOException {
        reviewRepository.append(entry);
        reviewEvents.add(entry);
    }

    private static void requireUser(String id) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("사용자 ID가 필요합니다.");
    }

    private void requireWord(String wordId) {
        if (getWord(wordId) == null) throw new IllegalArgumentException("없는 단어 ID: " + wordId);
    }

    private static void checkCount(int count, int available) {
        if (count < 1 || count > available)
            throw new IllegalArgumentException("요청한 단어 수 " + count + ", 사용 가능 " + available);
    }
}
