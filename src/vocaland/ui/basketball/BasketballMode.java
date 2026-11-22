package vocaland.ui.basketball;

/*
 * BasketballMode - 농구 게임 출제 모드
 *
 * MEANING        : 영어 단어를 보고 한글 뜻 골대로 공 던지기
 * SPELLING       : 한글 뜻을 보고 영어 단어 골대로 공 던지기
 * getLabel()     : 모드 선택 버튼에 표시할 이름
 * getGuide()     : 하단 조작법 안내 문구
 */
public enum BasketballMode {
    MEANING("영어 보고 뜻 맞히기", "제시된 영어 단어의 한글 뜻 골대로 공을 던지세요!"),
    SPELLING("뜻 보고 영어 맞히기", "제시된 한글 뜻에 맞는 영어 단어 골대로 공을 던지세요!");

    private final String label;
    private final String guide;

    BasketballMode(String label, String guide) {
        this.label = label;
        this.guide = guide;
    }

    public String getLabel() {
        return label;
    }

    public String getGuide() {
        return guide;
    }
}