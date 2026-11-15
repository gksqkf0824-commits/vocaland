package vocaland.repository;

import java.util.ArrayList;
import java.util.List;
import vocaland.model.User;

/*
 * UserRepository - users.txt 읽기 / 쓰기
 *
 * findAll()        : 모든 사용자
 * findById(id)     : ID로 찾기, 없으면 null
 * existsNickname() : 같은 닉네임이 있는지 (대소문자·앞뒤 공백 무시)
 * save(user)       : 새 사용자면 추가, 있던 사용자면 내용 갱신 후 파일 저장
 * nextId()         : 다음 사용자 ID (U001, U002, ...)
 */
public class UserRepository {
    public static final String FILE_NAME = "users.txt";
    private static final String HEADER = "# 사용자ID|닉네임|누적경험치|코인";

    private final TextFileStore store;

    public UserRepository() {
        this(new TextFileStore(FILE_NAME));
    }

    public UserRepository(TextFileStore store) {
        this.store = store;
    }

    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        for (String[] row : store.readRows(HEADER)) {
            User user = toUser(row);
            if (user != null) {
                users.add(user);
            }
        }
        return users;
    }

    public User findById(String id) {
        for (User user : findAll()) {
            if (user.getId().equals(id)) {
                return user;
            }
        }
        return null;
    }

    public boolean existsNickname(String nickname) {
        String target = nickname.trim();
        for (User user : findAll()) {
            if (user.getNickname().equalsIgnoreCase(target)) {
                return true;
            }
        }
        return false;
    }

    public void save(User user) {
        List<User> users = findAll();
        boolean updated = false;

        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getId().equals(user.getId())) {
                users.set(i, user);
                updated = true;
                break;
            }
        }
        if (!updated) {
            users.add(user);
        }

        List<String> lines = new ArrayList<>();
        for (User u : users) {
            lines.add(toLine(u));
        }
        store.writeRows(HEADER, lines);
    }

    public String nextId() {
        int max = 0;
        for (User user : findAll()) {
            String id = user.getId();
            if (id.startsWith("U")) {
                try {
                    max = Math.max(max, Integer.parseInt(id.substring(1)));
                } catch (NumberFormatException ignored) {
                    // 형식이 다른 ID는 번호 계산에서 제외
                }
            }
        }
        return String.format("U%03d", max + 1);
    }

    private String toLine(User user) {
        return TextFileStore.join(
                user.getId(), user.getNickname(), user.getExp(), user.getCoins()
        );
    }

    // 잘못된 줄은 프로그램을 멈추지 않고 건너뜀 (콘솔에 경고만 출력)
    private User toUser(String[] row) {
        try {
            String id = row[0];
            String nickname = row[1];
            int exp = row.length > 2 && !row[2].isEmpty() ? Integer.parseInt(row[2]) : 0;
            int coins = row.length > 3 && !row[3].isEmpty() ? Integer.parseInt(row[3]) : 0;
            return new User(id, nickname, exp, coins);
        } catch (RuntimeException e) {
            System.err.println("[users.txt] 잘못된 줄을 건너뜁니다: " + String.join("|", row));
            return null;
        }
    }
}
