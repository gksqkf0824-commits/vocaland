package vocaland.repository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/*
 * TextFileStore - data 폴더의 텍스트 파일을 읽고 쓰는 공통 클래스
 *
 * 모든 Repository가 이 클래스를 사용해서 파일 형식을 통일합니다.
 *  - 위치    : 실행 폴더 기준 상대경로 data/
 *  - 인코딩  : UTF-8
 *  - 구분자  : |  (한 줄 = 데이터 하나)
 *  - 주석    : # 으로 시작하는 줄과 빈 줄은 읽을 때 건너뜀
 *
 * readRows(header)  : 각 줄을 | 로 나눈 배열 목록으로 반환 (파일이 없으면 header를 넣어 새로 만듦)
 * writeRows(header, rows) : header 주석 + 데이터 줄로 파일 전체를 다시 씀
 * join(values...)   : 값들을 | 로 이어 한 줄로 만듦 (| 가 들어간 값은 거부)
 *
 * 사용 예)
 *   TextFileStore store = new TextFileStore("users.txt");
 *   for (String[] row : store.readRows(HEADER)) { ... row[0], row[1] ... }
 */
public class TextFileStore {
    public static final String DATA_DIR = "data";
    public static final String DELIMITER = "|";
    private static final String COMMENT = "#";
    private static final char BOM = 0xFEFF;

    private final Path path;

    public TextFileStore(String fileName) {
        this(Paths.get(DATA_DIR, fileName));
    }

    // 테스트할 때 다른 위치의 파일을 쓰기 위한 생성자
    public TextFileStore(Path path) {
        this.path = path;
    }

    public Path getPath() {
        return path;
    }

    public List<String[]> readRows(String header) {
        createIfMissing(header);

        List<String[]> rows = new ArrayList<>();
        try {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                String trimmed = stripBom(line).trim();
                if (trimmed.isEmpty() || trimmed.startsWith(COMMENT)) {
                    continue;
                }
                // -1 : 끝에 있는 빈 칸도 버리지 않고 유지
                String[] values = trimmed.split("\\|", -1);
                for (int i = 0; i < values.length; i++) {
                    values[i] = values[i].trim();
                }
                rows.add(values);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(path + " 파일을 읽지 못했습니다.", e);
        }
        return rows;
    }

    public void writeRows(String header, List<String> lines) {
        List<String> output = new ArrayList<>();
        if (header != null) {
            output.add(header);
        }
        output.addAll(lines);

        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(path, output, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(path + " 파일에 저장하지 못했습니다.", e);
        }
    }

    public static String join(Object... values) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            String value = String.valueOf(values[i]);
            if (value.contains(DELIMITER)) {
                throw new IllegalArgumentException("값에 '|' 문자를 쓸 수 없습니다: " + value);
            }
            if (i > 0) {
                line.append(DELIMITER);
            }
            line.append(value);
        }
        return line.toString();
    }

    private void createIfMissing(String header) {
        if (Files.exists(path)) {
            return;
        }
        writeRows(header, new ArrayList<>());
    }

    // 메모장 등으로 저장하면 파일 맨 앞에 BOM(U+FEFF)이 붙을 수 있어 제거
    private String stripBom(String line) {
        if (!line.isEmpty() && line.charAt(0) == BOM) {
            return line.substring(1);
        }
        return line;
    }
}
