package vocaland.model;

/*
 * User - 사용자 한 명 (users.txt 한 줄)
 *
 * 형식 : 사용자ID|닉네임|누적경험치|코인
 * 예시 : U001|조용현|120|350
 *
 * addExp(amount)    : 경험치 증가 (학습/게임 보상)
 * addCoins(amount)  : 코인 증가 (게임 보상)
 * useCoins(amount)  : 코인 사용 (상점 구매), 부족하면 false
 */
public class User {
    private final String id;
    private final String nickname;
    private int exp;
    private int coins;

    public User(String id, String nickname, int exp, int coins) {
        if (exp < 0 || coins < 0) {
            throw new IllegalArgumentException("경험치와 코인은 0 이상이어야 합니다.");
        }
        this.id = id;
        this.nickname = nickname;
        this.exp = exp;
        this.coins = coins;
    }

    public String getId() {
        return id;
    }

    public String getNickname() {
        return nickname;
    }

    public int getExp() {
        return exp;
    }

    public int getCoins() {
        return coins;
    }

    public void addExp(int amount) {
        checkPositive(amount);
        exp += amount;
    }

    public void addCoins(int amount) {
        checkPositive(amount);
        coins += amount;
    }

    public boolean useCoins(int amount) {
        checkPositive(amount);
        if (coins < amount) {
            return false;
        }
        coins -= amount;
        return true;
    }

    private void checkPositive(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("0 이상의 값만 사용할 수 있습니다: " + amount);
        }
    }

    // 콤보박스/리스트에 넣었을 때 닉네임이 보이도록
    @Override
    public String toString() {
        return nickname;
    }
}
