package vocaland.repository;

import vocaland.model.StudyRecord;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** records.txt / reviews.txt: 사용자ID|단어ID|결과|날짜 (UTF-8, 팀 TextFileStore 활용). */
public final class RecordRepository {
    private final TextFileStore store;
    private final String header;

    public RecordRepository(Path file) {
        if (file == null) throw new IllegalArgumentException("기록 파일 경로가 필요합니다.");
        this.store = new TextFileStore(file);
        this.header = file.getFileName().toString().equals("reviews.txt")
                ? "# 사용자ID|단어ID|복습등록/복습해제|날짜"
                : "# 사용자ID|단어ID|정답여부|날짜";
    }

    public List<StudyRecord> load() throws IOException {
        List<StudyRecord> records = new ArrayList<>();
        if (!Files.exists(store.getPath())) return records;
        List<String[]> rows = readRows();
        for (int i = 0; i < rows.size(); i++) {
            String[] fields = rows.get(i);
            if (fields.length != 4) {
                throw new IOException(store.getPath().getFileName() + " 데이터 " + (i + 1)
                        + "번째: 항목이 4개여야 합니다.");
            }
            try {
                records.add(new StudyRecord(fields[0], fields[1],
                        StudyRecord.Result.fromFileValue(fields[2]), LocalDate.parse(fields[3])));
            } catch (IllegalArgumentException | DateTimeParseException ex) {
                throw new IOException(store.getPath().getFileName() + " 데이터 " + (i + 1)
                        + "번째: " + ex.getMessage(), ex);
            }
        }
        return records;
    }

    public void append(StudyRecord record) throws IOException {
        if (record == null) throw new IllegalArgumentException("저장할 기록이 필요합니다.");
        // 팀 공통 TextFileStore에는 append가 없어 기존 행을 읽고 새 행을 덧붙여 저장한다.
        // 팀 프로젝트 규모에서 사용하며, 동시 쓰기는 통합 시 하나의 서비스에서 관리한다.
        List<String[]> rows = readRows();
        List<String> lines = new ArrayList<>();
        for (String[] row : rows) lines.add(TextFileStore.join((Object[]) row));
        lines.add(TextFileStore.join(record.getUserId(), record.getWordId(),
                record.getResult().getFileValue(), record.getDate()));
        try {
            store.writeRows(header, lines);
        } catch (UncheckedIOException ex) {
            throw ex.getCause();
        }
    }

    private List<String[]> readRows() throws IOException {
        try {
            return store.readRows(header);
        } catch (UncheckedIOException ex) {
            throw ex.getCause();
        }
    }
}
