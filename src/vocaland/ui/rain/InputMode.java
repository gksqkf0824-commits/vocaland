package vocaland.ui.rain;

/*
 * InputMode - 산성비 입력 모드
 *
 * SPELLING   : 한글 뜻이 떨어지고 영어 철자를 입력
 * MEANING    : 영어 단어가 떨어지고 한글 뜻을 입력
 * getLabel() : 모드 선택 버튼에 보일 이름
 * getGuide() : 입력창 위 안내 문구
 */
public enum InputMode {
    SPELLING("영어 철자 입력", "뜻에 맞는 영어 단어를 입력하세요."),
    MEANING("한글 뜻 입력", "영어 단어의 뜻을 한글로 입력하세요.");

    private final String label;
    private final String guide;

    InputMode(String label, String guide) {
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
