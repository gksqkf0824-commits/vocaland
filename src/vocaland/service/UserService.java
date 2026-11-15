package vocaland.service;

import java.util.List;
import vocaland.model.User;
import vocaland.repository.UserRepository;

/*
 * UserService - 사용자 등록 / 선택 / 현재 사용자 관리
 *
 * 프로그램 전체에서 하나만 만들어 각 화면에 넘겨 쓰는 것을 권장합니다.
 *   UserService userService = new UserService();
 *
 * getUsers()          : 선택 화면에 보여줄 사용자 목록
 * register(nickname)  : 새 사용자 등록 후 바로 현재 사용자로 선택
 * select(id)          : 사용자 선택 (로그인 대신)
 * getCurrentUser()    : 현재 사용자, 선택 전이면 null
 * hasCurrentUser()    : 사용자를 선택했는지
 * saveCurrentUser()   : 코인/경험치를 바꾼 뒤 파일에 반영
 *
 * 잘못된 입력은 IllegalArgumentException(한글 메시지)으로 알려주므로
 * 화면에서는 e.getMessage()를 그대로 안내창에 보여주면 됩니다.
 */
public class UserService {
    public static final int MAX_NICKNAME_LENGTH = 10;
    public static final int START_COINS = 0;

    private final UserRepository repository;
    private User currentUser;

    public UserService() {
        this(new UserRepository());
    }

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public List<User> getUsers() {
        return repository.findAll();
    }

    public User register(String nickname) {
        String name = validateNickname(nickname);

        User user = new User(repository.nextId(), name, 0, START_COINS);
        repository.save(user);
        currentUser = user;
        return user;
    }

    public User select(String id) {
        User user = repository.findById(id);
        if (user == null) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
        }
        currentUser = user;
        return user;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean hasCurrentUser() {
        return currentUser != null;
    }

    public void saveCurrentUser() {
        if (currentUser == null) {
            throw new IllegalStateException("선택된 사용자가 없습니다.");
        }
        repository.save(currentUser);
    }

    private String validateNickname(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            throw new IllegalArgumentException("닉네임을 입력해 주세요.");
        }
        String name = nickname.trim();
        if (name.length() > MAX_NICKNAME_LENGTH) {
            throw new IllegalArgumentException(
                    "닉네임은 " + MAX_NICKNAME_LENGTH + "자 이하로 입력해 주세요."
            );
        }
        if (name.contains("|")) {
            throw new IllegalArgumentException("닉네임에 '|' 문자는 쓸 수 없습니다.");
        }
        if (repository.existsNickname(name)) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }
        return name;
    }
}
