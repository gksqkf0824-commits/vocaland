package vocaland.service;

import vocaland.model.Word;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** Local profile and game data. All paths are relative to the project directory. */
public final class LocalData {

    public record User(String id, String name, int xp, int coins) {}

    public record Item(String id, String name, String type, int price, String image) {}

    public record Rank(String userId, String name, int score, int position) {}

    private final Path directory;
    private String currentId;
    // words.txt 는 프로그램이 바꾸지 않으므로 처음 한 번만 읽어 둠 (단어 맞힐 때마다 600줄을 다시 읽던 렉 수정)
    private List<Word> wordCache;

    public LocalData(Path directory) {
        this.directory = directory;
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        List<String> session = read("session.txt");
        if (!session.isEmpty() && users().stream().anyMatch(u -> u.id().equals(session.get(0)))) currentId = session.get(0);
    }

    private List<String> read(String file) {
        try {
            return Files.exists(directory.resolve(file)) ? Files.readAllLines(directory.resolve(file), StandardCharsets.UTF_8).stream().filter(s -> !s.isBlank()).toList() : List.of();
        } catch (IOException e) {
            throw new UncheckedIOException("데이터 읽기 실패: " + file, e);
        }
    }

    private void write(String file, List<String> lines) {
        Path tmp = null;
        try {
            tmp = Files.createTempFile(directory, "save-", ".tmp");
            Files.write(tmp, lines, StandardCharsets.UTF_8);
            try {
                Files.move(tmp, directory.resolve(file), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, directory.resolve(file), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("데이터 저장 실패: " + file, e);
        } finally {
            if (tmp != null) try {
                Files.deleteIfExists(tmp);
            } catch (IOException ignored) {
            }
        }
    }

    // Roll back earlier files if a later write fails. Each file is replaced atomically.
    private void save(Map<String, List<String>> changes) {
        Map<String, List<String>> previous = new LinkedHashMap<>();
        changes.keySet().forEach(f -> previous.put(f, read(f)));
        try {
            changes.forEach(this::write);
        } catch (RuntimeException e) {
            previous.forEach((file, lines) -> {
                try {
                    write(file, lines);
                } catch (RuntimeException rollback) {
                    e.addSuppressed(rollback);
                }
            }
            );
            throw e;
        }
    }

    // 파일 전체를 다시 쓰지 않고 끝에 한 줄만 덧붙임 (records.txt 가 길어져도 느려지지 않게)
    private void append(String file, String row) {
        try {
            Path path = directory.resolve(file);
            boolean needsNewline = false;
            if (Files.exists(path) && Files.size(path) > 0) {
                try (java.nio.channels.SeekableByteChannel ch = Files.newByteChannel(path)) {
                    java.nio.ByteBuffer last = java.nio.ByteBuffer.allocate(1);
                    ch.position(ch.size() - 1).read(last);
                    needsNewline = last.get(0) != '\n';
                }
            }
            String text = (needsNewline ? System.lineSeparator() : "") + row + System.lineSeparator();
            Files.writeString(path, text, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException("데이터 저장 실패: " + file, e);
        }
    }

    public List<User> users() {
        return read("users.txt").stream().map(s -> {
            String[] f = s.split("\\|", -1);
            if (f.length != 4) throw new IllegalStateException("users.txt 형식을 확인하세요.");
            return new User(f[0], f[1], Integer.parseInt(f[2]), Integer.parseInt(f[3]));
        }
        ).toList();
    }

    public User current() {
        return users().stream().filter(u -> u.id().equals(currentId)).findFirst().orElse(null);
    }

    public User login(String input) {
        String name = input.strip();
        if (name.isEmpty() || name.length() > 20 || name.contains("|") || name.chars().anyMatch(Character::isISOControl)) throw new IllegalArgumentException("이름은 1~20자로 입력하세요. 줄바꿈과 |는 사용할 수 없어요.");
        User found = users().stream().filter(u -> u.name().equals(name)).findFirst().orElse(null);
        if (found == null) {
            int next = users().stream().mapToInt(u -> Integer.parseInt(u.id().substring(1))).max().orElse(0) + 1;
            found = new User(String.format("U%03d", next), name, 0, 0);
            List<String> rows = new ArrayList<>(read("users.txt"));
            rows.add(userLine(found));
            List<String> owned = new ArrayList<>(read("inventory.txt"));
            for (String id : List.of("C03", "B01", "H01", "BG01")) owned.add(found.id() + "|" + id + "|true");
            Map<String, List<String>> changes = new LinkedHashMap<>();
            changes.put("users.txt", rows);
            changes.put("inventory.txt", owned);
            changes.put("session.txt", List.of(found.id()));
            save(changes);
        }
        else write("session.txt", List.of(found.id()));
        currentId = found.id();
        return found;
    }

    public void logout() {
        write("session.txt", List.of());
        currentId = null;
    }

    private User requireUser() {
        User u = current();
        if (u == null) throw new IllegalStateException("로그인이 필요해요.");
        return u;
    }

    private String userLine(User u) {
        return u.id() + "|" + u.name() + "|" + u.xp() + "|" + u.coins();
    }

    private List<String> withUser(User user) {
        return users().stream().map(u -> userLine(u.id().equals(user.id()) ? user : u)).toList();
    }

    public List<Word> words() {
        if (wordCache != null) return wordCache;
        List<Word> result;
        try {
            result = vocaland.repository.WordRepository.read(directory.resolve("words.txt"));
        } catch (IOException error) {
            throw new UncheckedIOException("단어 데이터 읽기 실패", error);
        }
        if (result.size() < 3 || result.stream().map(Word::meaning).distinct().count() < 3 || result.stream().map(Word::english).distinct().count() < 3) throw new IllegalStateException("게임에 사용할 서로 다른 단어와 뜻이 3개 이상 필요해요.");
        wordCache = result;
        return result;
    }

    public List<String[]> wordPairs() {
        return words().stream().map(w -> new String[]{
            w.english(), w.meaning()
        }
        ).toList();
    }

    public List<Item> items() {
        return read("items.txt").stream().map(row -> {
            String[] f = row.split("\\|", -1); return new Item(f[0], f[1], f[2], Integer.parseInt(f[3]), f[4]);
        }
        ).toList();
    }

    public boolean owns(String itemId) {
        String prefix = requireUser().id() + "|" + itemId + "|";
        return read("inventory.txt").stream().anyMatch(s -> s.startsWith(prefix));
    }
    /** Owned item IDs mapped to their equipped state, read once for the store. */

    public Map<String, Boolean> inventoryStates() {
        String userId = requireUser().id();
        Map<String, Boolean> states = new HashMap<>();
        for (String row : read("inventory.txt")) {
            String[] fields = row.split("\\|", -1);
            if (fields.length >= 3 && fields[0].equals(userId)) {
                states.merge(fields[1], Boolean.parseBoolean(fields[2]), (a, b) -> a || b);
            }
        }
        return Map.copyOf(states);
    }

    public boolean equipped(String itemId) {
        return read("inventory.txt").contains(requireUser().id() + "|" + itemId + "|true");
    }

    public String equippedImage(String type) {
        return equippedImage(requireUser().id(), type);
    }

    public String equippedImage(String userId, String type) {
        Set<String> inventory = new HashSet<>(read("inventory.txt"));
        return items().stream().filter(i -> i.type().equals(type)
        && inventory.contains(userId + "|" + i.id() + "|true")).map(Item::image).findFirst().orElse(null);
    }

    public void buyOrEquip(String itemId) {
        User user = requireUser();
        Item item = items().stream().filter(i -> i.id().equals(itemId)).findFirst().orElseThrow();
        boolean owned = owns(itemId);
        if (!owned && user.coins() < item.price()) throw new IllegalArgumentException("코인이 부족합니다!");
        Set<String> categoryIds = new HashSet<>();
        items().stream().filter(i -> i.type().equals(item.type())).forEach(i -> categoryIds.add(i.id()));
        List<String> inventory = new ArrayList<>();
        for (String row : read("inventory.txt")) {
            String[] f = row.split("\\|", -1);
            if (f[0].equals(user.id()) && categoryIds.contains(f[1])) row = f[0] + "|" + f[1] + "|" + f[1].equals(itemId);
            inventory.add(row);
        }
        if (!owned) inventory.add(user.id() + "|" + itemId + "|true");
        Map<String, List<String>> changes = new LinkedHashMap<>();
        changes.put("users.txt", withUser(new User(user.id(), user.name(), user.xp(), user.coins() - (owned ? 0 : item.price()))));
        changes.put("inventory.txt", inventory);
        save(changes);
    }

    public void finishGame(String game, String difficulty, int score) {
        if (!Set.of("RAIN", "BASKETBALL").contains(game) || score < 0) throw new IllegalArgumentException("잘못된 게임 점수입니다.");
        User user = requireUser();
        List<String> scores = new ArrayList<>(read("scores.txt"));
        scores.add(user.id() + "|" + game + "|" + difficulty + "|" + score + "|" + LocalDateTime.now(ZoneId.of("Asia/Seoul")).withNano(0));
        Map<String, List<String>> changes = new LinkedHashMap<>();
        changes.put("scores.txt", scores);
        changes.put("users.txt", withUser(new User(user.id(), user.name(), Math.addExact(user.xp(), score), Math.addExact(user.coins(), score / 10))));
        save(changes);
    }

    public List<Rank> ranks(String game) {
        Map<String, Integer> best = new HashMap<>();
        for (String row : read("scores.txt")) {
            String[] f = row.split("\\|", -1);
            if (f[1].equals(game)) best.merge(f[0], Integer.parseInt(f[3]), Math::max);
        }
        List<User> sorted = users().stream().filter(u -> best.containsKey(u.id())).sorted(Comparator.<User>comparingInt(u -> best.get(u.id())).reversed().thenComparing(User::id)).toList();
        List<Rank> result = new ArrayList<>();
        int position = 0, last = -1;
        for (int i = 0; i < sorted.size(); i++) {
            User u = sorted.get(i);
            int score = best.get(u.id());
            if (score != last) position = i + 1;
            result.add(new Rank(u.id(), u.name(), score, position));
            last = score;
        }
        return result;
    }

    public void recordAnswer(String english, boolean correct) {
        Word word = words().stream().filter(w -> w.english().equals(english)).findFirst().orElse(null);
        if (word != null) append("records.txt", requireUser().id() + "|" + word.id() + "|" + (correct ? "정답" : "오답") + "|" + LocalDate.now(ZoneId.of("Asia/Seoul")));
    }
}
