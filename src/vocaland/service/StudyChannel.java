package vocaland.service;

import vocaland.model.StudyRecord;
import vocaland.model.Word;
import vocaland.model.WordCard;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/** 단어 학습 및 카드/산성비 연동용 인터페이스. 화면과 분리되어 있다. */
public interface StudyChannel {
    String ALL = "전체";
    String REVIEW = "다시 볼 단어";

    // 단어 학습: 전체 / 테마 / 다시 볼 단어 표시. 별도 객관식 학습 퀴즈는 없음.
    List<String> getThemes();
    List<Word> getWords(String theme);
    List<Word> getStudyWords(String userId, String source);
    List<Word> searchWords(String keyword);
    Word getWord(String wordId);

    // 산성비: AcidRainPanel(List<String[]>)에 전달하는 {영어, 뜻} 목록.
    List<String[]> loadWordPairs();
    /** 사용자별 오답 이력 + 수동 복습 체크를 반영한 가중치 목록. 게임 시작 때 다시 호출. */
    List<String[]> loadWeightedWordPairs(String userId);
    /** 농구는 현재 사용자별 가중치 미적용: 중복 없는 일반 단어 목록. */
    List<String[]> loadBasketballWordPairs();
    /** 최종 출제 가중치 = min(3, 오답 이력 가중치 + (복습 체크 ? 1 : 0)). */
    int getWordWeight(String userId, String wordId);
    /** 오답 이력 기반 가중치 감산까지 쌓인 연속 정답 수(0 또는 1). */
    int getCorrectStreak(String userId, String wordId);
    /** 산성비에서 단어를 놓쳤을 때 영어 단어를 ID로 찾아 오답 기록. */
    void recordWrongByEnglish(String userId, String english) throws IOException;
    /** 산성비에서 입력으로 단어를 맞혀 제거했을 때 정답 단어별로 1회 기록. */
    void recordCorrectByEnglish(String userId, String english) throws IOException;

    // 단어 선택 보조 / 산성비 뜻·철자 검증 / 농구용 기본 뜻 검증.
    Word getRandomWord();
    Word getRandomWord(String theme);
    Word getRandomWordExcluding(Set<String> excludedWordIds);
    Word getWeightedRandomWord(String userId, String theme);
    boolean checkEnglish(Word word, String input);
    boolean checkMeaning(Word word, String input);
    boolean checkAnswer(String wordId, String input);

    // 카드 짝맞추기: 6쌍(12장), 같은 단어 중복 없이 가중 추출.
    List<Word> getRandomWords(int count);
    List<Word> getRandomWords(String theme, int count);
    List<Word> getPairWords(String userId, int count);
    List<Word> getPairWords(String userId, String source, int count);
    List<WordCard> createMatchingCards(String userId, int pairCount);
    List<WordCard> createMatchingCards(String userId, String source, int pairCount);
    boolean isPair(WordCard first, WordCard second);
    /** 한 쌍을 정확히 맞췄을 때만 한 번 기록. 카드 실패는 오답으로 기록하지 않음. */
    void recordMatchedPair(String userId, WordCard first, WordCard second) throws IOException;

    // 사용자별 학습 기록 및 '다시 볼 단어' 체크.
    void setReview(String userId, String wordId, boolean selected) throws IOException;
    void markReview(String userId, String wordId) throws IOException;
    boolean isReview(String userId, String wordId);
    List<Word> getReviewedWords(String userId);
    List<Word> getWordsToReview(String userId);
    /** 게임에서 판정된 정답/오답을 기록. 단어 학습 화면에서 자동 호출하지 않는다. */
    void recordAnswer(String userId, String wordId, boolean correct) throws IOException;
    List<StudyRecord> getRecords(String userId);
    int getWrongCount(String userId, String wordId);
    List<Word> getWrongWords(String userId);
    int countRecords(String userId, StudyRecord.Result result);
}
