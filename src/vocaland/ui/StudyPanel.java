package vocaland.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

public class StudyPanel extends JPanel {
    private static final Path WORDS_FILE = Path.of("data", "words.txt");
    private static final Path REVIEW_FILE = Path.of("data", "review_words.txt");
    private static final String USER_PREFIX = "U001|";

    private static final Color BACKGROUND = new Color(0xFAFBF8);
    private static final Color DIVIDER = new Color(0xDCE5CF);

    private static final List<Word> SAMPLE_WORDS = List.of(
            new Word("DEMO001", "apple", "사과"),
            new Word("DEMO002", "pineapple", "파인애플"),
            new Word("DEMO003", "bread", "빵"),
            new Word("DEMO004", "milk", "우유"),
            new Word("DEMO005", "rice", "밥"),
            new Word("DEMO006", "egg", "달걀")
    );

    private final CardLayout screenLayout = new CardLayout();
    private final JPanel results = new JPanel(new BorderLayout(0, 18));
    private final Map<String, Word> viewedWords = new LinkedHashMap<>();
    private final Set<String> reviewEntries = new LinkedHashSet<>();
    private final List<Word> words;
    private final Runnable goMain;

    private final ImageIcon backgroundTile =
            loadIcon("images/background.png", 620, 620, false);
    private final ImageIcon nextIcon =
            loadIcon("images/text/next.png", 105, 30, false);
    private final ImageIcon finishIcon =
            loadIcon("images/text/finish.png", 105, 30, false);

    private final JLabel english = new JLabel("", SwingConstants.CENTER);
    private final JLabel meaning = new JLabel("", SwingConstants.CENTER);
    private final JLabel progressText = new JLabel("", SwingConstants.CENTER);
    private final JCheckBox reviewCheck = new JCheckBox("다시 볼 단어 표시");

    private final RoundedButton previous = imageButton(
            "이전 단어",
            loadIcon("images/text/previous.png", 110, 35, false),
            140, 50
    );
    private final RoundedButton next = imageButton("다음 단어", nextIcon, 140, 50);

    private int wordIndex;
    private boolean englishHidden;
    private boolean meaningHidden;
    private boolean reviewAvailable = true;
    private String wordLoadError;

    private final JPanel progressBar = new JPanel() {
        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);

            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                int width = getWidth();
                int height = getHeight();
                int filled = words.isEmpty() ? 0
                        : (int) Math.round((double) width * (wordIndex + 1) / words.size());

                g.setColor(DIVIDER);
                g.fillRoundRect(0, 0, width, height, height, height);
                g.setColor(Theme.PRIMARY);
                g.fillRoundRect(0, 0, filled, height, height, height);
            } finally {
                g.dispose();
            }
        }
    };

    public StudyPanel(Runnable goMain, Runnable openPair) {
        this.goMain = goMain;
        words = loadWords();
        loadReviewEntries();

        setLayout(screenLayout);
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(22, 30, 22, 30));

        JPanel study = transparentPanel(new BorderLayout(0, 18));
        study.add(createStudyHeader(), BorderLayout.NORTH);
        study.add(createStudyCard(), BorderLayout.CENTER);
        study.add(createNavigation(openPair), BorderLayout.SOUTH);

        results.setOpaque(false);
        add(study, "study");
        add(results, "results");

        refreshCard();
        screenLayout.show(this, "study");
    }

    private JPanel createStudyHeader() {
        JLabel title = new JLabel("학습");
        styleLabel(title, 28);

        ImageIcon titleIcon = loadIcon("images/study.png", 170, 50, true);
        if (titleIcon != null) {
            title.setIcon(titleIcon);
            title.setText("");
            title.getAccessibleContext().setAccessibleName("학습");
        }

        RoundedButton review = new RoundedButton("다시 볼 단어");
        review.addActionListener(event -> showResults(true));

        JPanel actions = row(FlowLayout.RIGHT);
        actions.add(review);
        actions.add(createMainButton(false));

        JPanel header = transparentPanel(new BorderLayout());
        header.add(title, BorderLayout.WEST);
        header.add(actions, BorderLayout.EAST);

        styleLabel(progressText, 18);
        progressText.setAlignmentX(CENTER_ALIGNMENT);
        progressBar.setOpaque(false);
        progressBar.setPreferredSize(new Dimension(340, 10));
        progressBar.setMaximumSize(new Dimension(340, 10));
        progressBar.setAlignmentX(CENTER_ALIGNMENT);

        JPanel progress = new JPanel();
        progress.setOpaque(false);
        progress.setLayout(new BoxLayout(progress, BoxLayout.Y_AXIS));
        progress.add(progressText);
        progress.add(Box.createVerticalStrut(6));
        progress.add(progressBar);

        JPanel top = transparentPanel(new BorderLayout(0, 12));
        top.add(header, BorderLayout.NORTH);
        top.add(progress, BorderLayout.SOUTH);
        return top;
    }

    private JPanel createStudyCard() {
        JPanel card = new JPanel(new GridLayout(2, 1)) {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);

                Graphics2D g = (Graphics2D) graphics.create();
                try {
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setColor(Color.WHITE);
                    g.fillRoundRect(0, 0, getWidth(), getHeight(), 32, 32);
                } finally {
                    g.dispose();
                }
            }
        };

        card.setOpaque(false);
        card.setBorder(new EmptyBorder(14, 24, 14, 24));

        JPanel englishSection = wordSection("영어 단어", english);
        englishSection.setBorder(new MatteBorder(0, 0, 1, 0, DIVIDER));
        card.add(englishSection);

        JPanel meaningSection = wordSection("뜻", meaning);
        meaningSection.setBorder(new EmptyBorder(10, 0, 0, 0));
        card.add(meaningSection);

        RoundedButton hideEnglish = imageButton(
                "단어 가리기",
                loadIcon("images/text/hide_english.png", 96, 24, false),
                128, 42
        );
        hideEnglish.setEnabled(!words.isEmpty());
        hideEnglish.addActionListener(event -> {
            englishHidden = !englishHidden;
            refreshCard();
        });

        RoundedButton hideMeaning = imageButton(
                "뜻 가리기",
                loadIcon("images/text/hide_korean.png", 96, 24, false),
                128, 42
        );
        hideMeaning.setEnabled(!words.isEmpty());
        hideMeaning.addActionListener(event -> {
            meaningHidden = !meaningHidden;
            refreshCard();
        });

        reviewCheck.setOpaque(false);
        reviewCheck.setForeground(Theme.TEXT);
        reviewCheck.addActionListener(event -> saveReviewSelection());

        JPanel actions = row(FlowLayout.CENTER);
        actions.add(hideEnglish);
        actions.add(hideMeaning);
        actions.add(reviewCheck);

        JPanel center = transparentPanel(new BorderLayout(0, 8));
        center.add(card, BorderLayout.CENTER);
        center.add(actions, BorderLayout.SOUTH);
        return center;
    }

    private JPanel createNavigation(Runnable openPair) {
        previous.setBorder(new EmptyBorder(6, 22, 14, 17));
        next.setBorder(new EmptyBorder(10, 17, 12, 15));

        previous.addActionListener(event -> {
            wordIndex--;
            refreshCard();
        });

        next.addActionListener(event -> {
            if (wordIndex == words.size() - 1) {
                showResults(false);
            } else {
                wordIndex++;
                refreshCard();
            }
        });

        JPanel navigation = row(FlowLayout.LEFT);
        navigation.add(previous);
        navigation.add(next);

        RoundedButton pair = imageButton(
                "짝 맞추기",
                loadIcon("images/pair.png", 130, 27, true),
                180, 50
        );
        pair.setBorder(new EmptyBorder(6, 16, 10, 16));
        pair.addActionListener(event -> openPair.run());

        JPanel footer = transparentPanel(new BorderLayout());
        footer.add(navigation, BorderLayout.WEST);
        footer.add(pair, BorderLayout.EAST);
        return footer;
    }

    private RoundedButton createMainButton(boolean resetStudy) {
        RoundedButton button = imageButton(
                "← 메인으로",
                loadIcon("images/text/go_main.png", 102, 24, false),
                132, 42
        );
        if (button.getIcon() != null) {
            button.setBorder(new EmptyBorder(13, 12, 12, 18));
        }

        button.addActionListener(event -> {
            if (resetStudy) {
                restartStudy();
            }
            screenLayout.show(this, "study");
            goMain.run();
        });
        return button;
    }

    private void refreshCard() {
        Word word = words.isEmpty() ? null : words.get(wordIndex);

        if (word == null) {
            english.setText(wordLoadError == null
                    ? "학습할 단어가 없어요." : "단어 파일을 읽을 수 없어요.");
            english.setToolTipText(wordLoadError);
            meaning.setText("—");
            progressText.setText("0/0");
        } else {
            viewedWords.putIfAbsent(word.id(), word);
            english.setText(englishHidden ? "•••" : word.english());
            meaning.setText(meaningHidden ? "•••" : word.meaning());
            progressText.setText((wordIndex + 1) + "/" + words.size());
        }

        progressBar.repaint();
        reviewCheck.setEnabled(word != null && reviewAvailable);
        reviewCheck.setSelected(word != null && isMarked(word));
        previous.setEnabled(wordIndex > 0);
        next.setEnabled(word != null);

        boolean last = word != null && wordIndex == words.size() - 1;
        setButtonImage(next, last ? "끝" : "다음 단어", last ? finishIcon : nextIcon);
    }

    private void restartStudy() {
        viewedWords.clear();
        wordIndex = 0;
        englishHidden = false;
        meaningHidden = false;
        refreshCard();
    }

    private void showResults(boolean allReviewWords) {
        List<Word> viewed = List.copyOf(viewedWords.values());
        List<Word> marked = (allReviewWords ? words : viewed).stream()
                .filter(this::isMarked)
                .toList();

        results.removeAll();

        JLabel title = new JLabel(allReviewWords ? "다시 볼 단어" : "학습 결과");
        styleLabel(title, 28);

        RoundedButton back = new RoundedButton(allReviewWords ? "학습으로" : "다시 학습");
        back.addActionListener(event -> {
            if (!allReviewWords) {
                restartStudy();
            }
            screenLayout.show(this, "study");
        });

        JPanel actions = row(FlowLayout.RIGHT);
        actions.add(back);
        actions.add(createMainButton(!allReviewWords));

        JPanel header = transparentPanel(new BorderLayout());
        header.add(title, BorderLayout.WEST);
        header.add(actions, BorderLayout.EAST);

        JPanel lists = transparentPanel(new GridLayout(1, allReviewWords ? 1 : 2, 16, 0));
        if (!allReviewWords) {
            lists.add(createWordList("오늘 학습한 단어", viewed, false));
        }
        lists.add(createWordList("다시 볼 단어", marked, true));

        results.add(header, BorderLayout.NORTH);
        results.add(lists, BorderLayout.CENTER);
        screenLayout.show(this, "results");
        results.revalidate();
        results.repaint();
    }

    private JPanel createWordList(String title, List<Word> entries, boolean reviewOnly) {
        JPanel panel = transparentPanel(new BorderLayout(0, 10));
        JLabel heading = new JLabel(title + " · " + entries.size() + "개");
        styleLabel(heading, 20);
        panel.add(heading, BorderLayout.NORTH);

        StringBuilder content = new StringBuilder();
        for (Word word : entries) {
            if (!reviewOnly && isMarked(word)) {
                content.append("★ ");
            }
            content.append(word.english())
                    .append("    ")
                    .append(word.meaning())
                    .append('\n');
        }

        if (entries.isEmpty()) {
            content.append(reviewAvailable
                    ? "다시 볼 단어로 표시한 단어가 없어요."
                    : "복습 목록을 읽을 수 없어요.");
        }

        JTextArea list = new JTextArea(content.toString());
        list.setEditable(false);
        list.setBackground(Color.WHITE);
        list.setForeground(Theme.TEXT);
        list.setFont(new Font(Font.DIALOG, Font.PLAIN, 17));
        list.setLineWrap(true);
        list.setWrapStyleWord(true);
        list.setMargin(new Insets(12, 14, 12, 14));
        list.setCaretPosition(0);

        JScrollPane scroll = new JScrollPane(list);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        panel.add(scroll, BorderLayout.CENTER);

        if (reviewOnly) {
            RoundedButton export = new RoundedButton("복습 단어 TXT로 저장");
            export.setEnabled(!entries.isEmpty());
            export.addActionListener(event -> exportReview(entries));
            panel.add(export, BorderLayout.SOUTH);
        }

        return panel;
    }

    private List<Word> loadWords() {
        try {
            Map<String, Word> unique = new LinkedHashMap<>();

            for (String line : Files.readAllLines(WORDS_FILE, StandardCharsets.UTF_8)) {
                String[] fields = line.split("\\|", -1);
                if (fields.length < 3) {
                    continue;
                }

                String id = fields[0].strip();
                String englishText = fields[1].strip();
                String meaningText = fields[2].strip();

                if (!id.isEmpty() && !englishText.isEmpty() && !meaningText.isEmpty()) {
                    unique.putIfAbsent(id, new Word(id, englishText, meaningText));
                }
            }

            return List.copyOf(unique.values());
        } catch (NoSuchFileException missing) {
            return SAMPLE_WORDS;
        } catch (IOException error) {
            wordLoadError = error.getMessage();
            return List.of();
        }
    }

    private void loadReviewEntries() {
        try {
            reviewEntries.addAll(Files.readAllLines(REVIEW_FILE, StandardCharsets.UTF_8));
        } catch (NoSuchFileException missing) {
            // 첫 복습 표시를 저장할 때 파일을 만든다.
        } catch (IOException error) {
            reviewAvailable = false;
            reviewCheck.setToolTipText("복습 파일을 읽을 수 없어요. " + error.getMessage());
        }
    }

    private boolean isMarked(Word word) {
        return reviewEntries.contains(USER_PREFIX + word.id());
    }

    private void saveReviewSelection() {
        Word word = words.get(wordIndex);
        String entry = USER_PREFIX + word.id();
        Set<String> updated = new LinkedHashSet<>(reviewEntries);

        if (reviewCheck.isSelected()) {
            updated.add(entry);
        } else {
            updated.remove(entry);
        }

        Path temporary = null;
        try {
            Files.createDirectories(REVIEW_FILE.getParent());
            temporary = Files.createTempFile(REVIEW_FILE.getParent(), "review-", ".tmp");
            Files.write(temporary, updated, StandardCharsets.UTF_8);
            Files.move(temporary, REVIEW_FILE,
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);

            reviewEntries.clear();
            reviewEntries.addAll(updated);
        } catch (IOException error) {
            reviewCheck.setSelected(isMarked(word));
            showSaveError("복습 표시를 저장할 수 없어요.", error);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // 저장 오류는 위에서 이미 안내했다.
                }
            }
        }
    }

    private void exportReview(List<Word> entries) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("복습 단어 TXT로 저장");
        chooser.setSelectedFile(new File("복습단어.txt"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path path = chooser.getSelectedFile().toPath().toAbsolutePath().normalize();
        if (!path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".txt")) {
            path = path.resolveSibling(path.getFileName() + ".txt");
        }

        try {
            if (sameFile(path, WORDS_FILE) || sameFile(path, REVIEW_FILE)) {
                JOptionPane.showMessageDialog(this,
                        "학습 데이터 파일과 다른 이름이나 위치를 선택해 주세요.",
                        "파일 확인", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (Files.exists(path) && JOptionPane.showConfirmDialog(this,
                    "이미 있는 파일이에요. 덮어쓸까요?", "파일 확인",
                    JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                return;
            }

            List<String> lines = entries.stream()
                    .map(word -> word.english() + "\t" + word.meaning())
                    .toList();
            Files.write(path, lines, StandardCharsets.UTF_8);
        } catch (IOException error) {
            showSaveError("TXT 파일을 저장할 수 없어요.", error);
        }
    }

    private static boolean sameFile(Path target, Path dataFile) throws IOException {
        return target.equals(dataFile.toAbsolutePath().normalize())
                || (Files.exists(target) && Files.exists(dataFile)
                    && Files.isSameFile(target, dataFile));
    }

    private void showSaveError(String message, IOException error) {
        JOptionPane.showMessageDialog(this, message + "\n" + error.getMessage(),
                "저장 오류", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        if (backgroundTile == null) {
            return;
        }

        int width = backgroundTile.getIconWidth();
        int height = backgroundTile.getIconHeight();

        for (int y = 0; y < getHeight(); y += height) {
            for (int x = 0; x < getWidth(); x += width) {
                backgroundTile.paintIcon(this, graphics, x, y);
            }
        }
    }

    private static JPanel wordSection(String name, JLabel value) {
        JLabel label = new JLabel(name);
        styleLabel(label, 16);
        styleLabel(value, 38);

        JPanel section = transparentPanel(new BorderLayout());
        section.add(label, BorderLayout.NORTH);
        section.add(value, BorderLayout.CENTER);
        return section;
    }

    private static JPanel row(int alignment) {
        return transparentPanel(new FlowLayout(alignment, 10, 0));
    }

    private static JPanel transparentPanel(java.awt.LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    private static void styleLabel(JLabel label, int size) {
        label.setForeground(Theme.TEXT);
        label.setFont(label.getFont().deriveFont(Font.BOLD, (float) size));
    }

    private static RoundedButton imageButton(String text, ImageIcon icon,
            int width, int height) {
        RoundedButton button = new RoundedButton(text, 18);
        button.setPreferredSize(new Dimension(width, height));
        setButtonImage(button, text, icon);
        return button;
    }

    private static void setButtonImage(RoundedButton button, String text, ImageIcon icon) {
        button.setIcon(icon);
        button.setText(icon == null ? text : "");
        button.getAccessibleContext().setAccessibleName(text);
    }

    private static ImageIcon loadIcon(String path, int maxWidth, int maxHeight,
            boolean trim) {
        ImageIcon source = new ImageIcon(path);
        int width = source.getIconWidth();
        int height = source.getIconHeight();

        if (width <= 0 || height <= 0) {
            return null;
        }

        Image image = source.getImage();
        if (trim) {
            BufferedImage buffered = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = buffered.createGraphics();
            try {
                g.drawImage(image, 0, 0, null);
            } finally {
                g.dispose();
            }
            image = trimTransparentMargins(buffered);
            width = image.getWidth(null);
            height = image.getHeight(null);
        }

        double scale = Math.min((double) maxWidth / width, (double) maxHeight / height);
        return new ImageIcon(image.getScaledInstance(
                Math.max(1, (int) Math.round(width * scale)),
                Math.max(1, (int) Math.round(height * scale)),
                Image.SCALE_SMOOTH
        ));
    }

    private static BufferedImage trimTransparentMargins(BufferedImage image) {
        int left = image.getWidth();
        int top = image.getHeight();
        int right = -1;
        int bottom = -1;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) > 16) {
                    left = Math.min(left, x);
                    top = Math.min(top, y);
                    right = Math.max(right, x);
                    bottom = Math.max(bottom, y);
                }
            }
        }

        if (right < left || bottom < top) {
            return image;
        }
        return image.getSubimage(left, top, right - left + 1, bottom - top + 1);
    }

    private record Word(String id, String english, String meaning) {}
}