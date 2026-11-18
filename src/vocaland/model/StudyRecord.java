package vocaland.model;

import java.time.LocalDate;

/** records.txt 한 줄: 사용자ID|단어ID|결과|날짜. */
public final class StudyRecord {
    public enum Result {
        CORRECT("정답"), WRONG("오답"),
        REVIEW_ON("복습등록"), REVIEW_OFF("복습해제"),
        /** 예전 be-study 시험판에서 쓰던 값. 불러올 때 복습 등록으로 취급. */
        REVIEWED("학습");

        private final String fileValue;
        Result(String fileValue) { this.fileValue = fileValue; }
        public String getFileValue() { return fileValue; }

        public static Result fromFileValue(String raw) {
            for (Result value : values()) {
                if (value.fileValue.equals(raw)) return value;
            }
            throw new IllegalArgumentException("알 수 없는 학습 결과: " + raw);
        }

        public boolean marksReview() { return this == REVIEW_ON || this == REVIEWED; }
    }

    private final String userId;
    private final String wordId;
    private final Result result;
    private final LocalDate date;

    public StudyRecord(String userId, String wordId, Result result, LocalDate date) {
        this.userId = checkId(userId, "사용자 ID");
        this.wordId = checkId(wordId, "단어 ID");
        if (result == null || date == null) throw new IllegalArgumentException("결과와 날짜가 필요합니다.");
        this.result = result;
        this.date = date;
    }

    private static String checkId(String value, String name) {
        if (value == null || value.isBlank() || value.contains("|") || value.contains("\n") || value.contains("\r")) {
            throw new IllegalArgumentException(name + " 형식이 올바르지 않습니다.");
        }
        return value.trim();
    }

    public String getUserId() { return userId; }
    public String getWordId() { return wordId; }
    public Result getResult() { return result; }
    public LocalDate getDate() { return date; }
}
