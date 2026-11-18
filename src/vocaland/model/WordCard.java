package vocaland.model;

/** 짝맞추기 화면이 표시할 카드. 뒤집힘/선택 상태는 GUI에서 관리한다. */
public final class WordCard {
    public enum Side { ENGLISH, KOREAN }

    private final String wordId;
    private final String text;
    private final Side side;

    public WordCard(String wordId, String text, Side side) {
        if (wordId == null || wordId.isBlank() || text == null || text.isBlank() || side == null) {
            throw new IllegalArgumentException("카드 ID, 글자, 종류가 필요합니다.");
        }
        this.wordId = wordId;
        this.text = text;
        this.side = side;
    }

    public String getWordId() { return wordId; }
    public String getText() { return text; }
    public Side getSide() { return side; }
}
