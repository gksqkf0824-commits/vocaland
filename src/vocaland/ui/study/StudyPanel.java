package vocaland.ui.study;

import vocaland.ui.util.ImagePaths;

import vocaland.ui.components.RoundedButton;
import vocaland.ui.components.RoundedPanel;
import vocaland.ui.components.RoundedBorder;
import vocaland.ui.theme.Theme;
import vocaland.repository.WordRepository;
import vocaland.repository.ReviewRepository;
import vocaland.model.Word;

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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private final String userPrefix;
    private final ReviewRepository reviews = new ReviewRepository(REVIEW_FILE);

    private static final Color BACKGROUND = new Color(0xFAFBF8);
    private static final Color DIVIDER = new Color(0xDCE5CF);

    private final CardLayout screenLayout = new CardLayout();
    private final JPanel results = new JPanel(new BorderLayout(0, 18));
    private final Map<String, Word> viewedWords = new LinkedHashMap<>();
    private final Set<String> reviewEntries = new LinkedHashSet<>();
    private final List<Word> allWords;
    private List<Word> words = List.of();
    private final Runnable goMain;

    private final ImageIcon nextIcon =
            loadIcon(ImagePaths.ROOT + ImagePaths.TEXT_NEXT, 105, 30, false);
    private final ImageIcon finishIcon =
            loadIcon(ImagePaths.ROOT + ImagePaths.TEXT_VIEW_RESULTS, 95, 27, false);

    private final JLabel english = new JLabel("", SwingConstants.CENTER);
    private final JLabel meaning = new JLabel("", SwingConstants.CENTER);
    private final JLabel progressText = new JLabel("", SwingConstants.CENTER);
    private final JCheckBox reviewCheck = new JCheckBox("다시 볼 단어 표시");

    private final RoundedButton previous = imageButton(
            "이전 단어",
            loadIcon(ImagePaths.ROOT + ImagePaths.TEXT_PREVIOUS, 110, 35, false),
            140, 50
    );
    private final RoundedButton next = imageButton("다음 단어", nextIcon, 140, 50);

    private int wordIndex;
    private boolean englishHidden;
    private boolean meaningHidden;
    private RoundedButton hideEnglish;
    private RoundedButton hideMeaning;
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

    public StudyPanel(String userId, Runnable goMain, Runnable openPair) {
        this.userPrefix = userId + "|";
        this.goMain = goMain;
        allWords = loadWords();
        loadReviewEntries();

        setLayout(screenLayout);
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(22, 30, 22, 30));

        JPanel study = transparentPanel(new BorderLayout(0, 18));
        study.add(createStudyHeader(), BorderLayout.NORTH);
        study.add(createStudyCard(), BorderLayout.CENTER);
        study.add(createNavigation(openPair), BorderLayout.SOUTH);
        bindNavigationKey(study, "RIGHT", "nextWord", next);
        bindNavigationKey(study, "LEFT", "previousWord", previous);

        results.setOpaque(false);
        add(study, "study");
        add(results, "results");
        add(createDifficultySelection(), "difficulty");

        refreshCard();
        screenLayout.show(this, "difficulty");
    }

    public void showDifficultySelection() {
        refreshReviews();
        screenLayout.show(this, "difficulty");
    }

    public List<String[]> getStudyWordPairs() {
        return words.stream().map(word -> new String[] {word.english(), word.meaning()}).toList();
    }

    private JPanel createDifficultySelection() {
        JPanel selection = transparentPanel(new java.awt.GridBagLayout());
        JPanel card = new RoundedPanel(new BorderLayout(0, 24));
        card.setBackground(Color.WHITE);
        card.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                new RoundedBorder(Theme.PRIMARY, 2), new EmptyBorder(28, 36, 28, 36)));
        JLabel title = new JLabel("학습 난이도 선택", JLabel.CENTER);
        styleLabel(title, 26);
        ImageIcon difficultyTitleIcon = loadIcon(ImagePaths.ROOT + ImagePaths.TEXT_STUDY_DIFFICULTY, 240, 40, true);
        if (difficultyTitleIcon != null) {
            title.setIcon(difficultyTitleIcon);
            title.setText("");
        }
        title.getAccessibleContext().setAccessibleName("학습 난이도 선택");
        card.add(title, BorderLayout.NORTH);
        JPanel choices = transparentPanel(new GridLayout(1, 3, 14, 0));
        for (String difficulty : List.of("초급", "중급", "고급")) {
            RoundedButton button = new RoundedButton(difficulty);
            button.setFont(new Font(Font.DIALOG, Font.BOLD, 20));
            String imageName = switch (difficulty) {
                case "초급" -> "beginner";
                case "중급" -> "intermediate";
                default -> "advanced";
            };
            ImageIcon difficultyIcon = loadIcon(ImagePaths.ROOT + ImagePaths.TEXT_DIRECTORY + imageName + ".png", 80, 26, true);
            if (difficultyIcon != null) {
                double scale = "고급".equals(difficulty) ? 0.60 * 0.95 : 0.60;
                difficultyIcon = new ImageIcon(difficultyIcon.getImage().getScaledInstance(
                        Math.max(1, (int) Math.round(difficultyIcon.getIconWidth() * scale)),
                        Math.max(1, (int) Math.round(difficultyIcon.getIconHeight() * scale)),
                        Image.SCALE_SMOOTH));
            }
            setButtonImage(button, difficulty, difficultyIcon);
            button.addActionListener(event -> {
                java.util.ArrayList<Word> candidates = new java.util.ArrayList<>(
                        allWords.stream().filter(word -> difficulty.equals(word.difficulty())).toList());
                java.util.Collections.shuffle(candidates);
                words = List.copyOf(candidates.subList(0, Math.min(20, candidates.size())));
                restartStudy();
                screenLayout.show(this, "study");
            });
            choices.add(button);
        }
        card.add(choices, BorderLayout.CENTER);
        card.add(createMainButton(false), BorderLayout.SOUTH);
        selection.add(card);
        return selection;
    }

    private void bindNavigationKey(JPanel study, String key, String actionName,
            RoundedButton button) {
        study.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(javax.swing.KeyStroke.getKeyStroke(key), actionName);
        javax.swing.AbstractAction action = new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                if (study.isShowing() && button.isEnabled()) {
                    button.doClick(0);
                }
            }
        };
        study.getActionMap().put(actionName, action);
        // A focused checkbox consumes arrow keys before window-level bindings.
        reviewCheck.getInputMap(javax.swing.JComponent.WHEN_FOCUSED)
                .put(javax.swing.KeyStroke.getKeyStroke(key), actionName);
        reviewCheck.getActionMap().put(actionName, action);
    }

    private JPanel createStudyHeader() {
        JLabel title = new JLabel("학습");
        styleLabel(title, 28);

        ImageIcon titleIcon = loadIcon(ImagePaths.ROOT + ImagePaths.LOGO_STUDY, 170, 50, true);
        if (titleIcon != null) {
            title.setIcon(titleIcon);
            title.setText("");
            title.getAccessibleContext().setAccessibleName("학습");
        }

        JPanel actions = row(FlowLayout.RIGHT);
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

        hideEnglish = imageButton(
                "단어 가리기",
                loadIcon(ImagePaths.ROOT + ImagePaths.TEXT_HIDE_ENGLISH, 96, 24, false),
                128, 42
        );
        hideEnglish.addActionListener(event -> {
            englishHidden = !englishHidden;
            refreshCard();
        });

        hideMeaning = imageButton(
                "뜻 가리기",
                loadIcon(ImagePaths.ROOT + ImagePaths.TEXT_HIDE_KOREAN, 96, 24, false),
                128, 42
        );
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
                showResults();
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
                loadIcon(ImagePaths.ROOT + ImagePaths.LOGO_PAIR, 130, 27, true),
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
                loadIcon(ImagePaths.ROOT + ImagePaths.TEXT_GO_MAIN, 102, 24, false),
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
        hideEnglish.setEnabled(word != null);
        hideMeaning.setEnabled(word != null);
        reviewCheck.setSelected(word != null && isMarked(word));
        previous.setEnabled(wordIndex > 0);
        next.setEnabled(word != null);

        boolean last = word != null && wordIndex == words.size() - 1;
        next.setBorder(last ? new EmptyBorder(9, 15, 13, 17)
                : new EmptyBorder(10, 17, 12, 15));
        setButtonImage(next, last ? "끝" : "다음 단어", last ? finishIcon : nextIcon);
    }

    private void restartStudy() {
        viewedWords.clear();
        wordIndex = 0;
        englishHidden = false;
        meaningHidden = false;
        refreshCard();
    }

    private void showResults() {
        List<Word> viewed = List.copyOf(viewedWords.values());
        List<Word> marked = viewed.stream()
                .filter(this::isMarked)
                .toList();

        results.removeAll();

        JLabel title = new JLabel("학습 결과");
        styleLabel(title, 28);
        ImageIcon resultIcon = loadIcon(ImagePaths.ROOT + ImagePaths.LOGO_RESULT, 210, 60, true);
        if (resultIcon != null) {
            title.setIcon(resultIcon);
            title.setText("");
        }
        title.getAccessibleContext().setAccessibleName("학습 결과");

        RoundedButton back = new RoundedButton("다시 학습");
        back.setFont(new Font(Font.DIALOG, Font.BOLD, 16));
        back.setPreferredSize(new Dimension(116, 42));
        back.setBorder(new EmptyBorder(12, 15, 13, 15));
        ImageIcon lookAgainIcon = loadIcon(ImagePaths.ROOT + ImagePaths.TEXT_STUDY_RESTART, 76, 17, true);
        if (lookAgainIcon != null) {
            back.setIcon(lookAgainIcon);
            back.setText("");
        }
        back.getAccessibleContext().setAccessibleName("다시 학습");
        back.addActionListener(event -> {
            restartStudy();
            screenLayout.show(this, "study");
        });

        JPanel actions = row(FlowLayout.RIGHT);
        actions.add(back);
        actions.add(createMainButton(true));

        JPanel header = transparentPanel(new BorderLayout());
        header.add(title, BorderLayout.WEST);
        header.add(actions, BorderLayout.EAST);

        JPanel lists = transparentPanel(new GridLayout(1, 2, 16, 0));
        lists.add(createWordList("오늘 학습한 단어", viewed, false));
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
        heading.setHorizontalAlignment(JLabel.CENTER);
        heading.setBorder(new EmptyBorder(8, 0, 8, 0));
        styleLabel(heading, 20);
        ImageIcon headingIcon = loadIcon(reviewOnly
                ? ImagePaths.ROOT + ImagePaths.TEXT_REVIEW_WORDS : ImagePaths.ROOT + ImagePaths.TEXT_STUDIED_WORDS,
                210, 32, true);
        if (headingIcon != null) {
            if (reviewOnly) {
                headingIcon = new ImageIcon(headingIcon.getImage().getScaledInstance(
                        Math.max(1, (int) Math.round(headingIcon.getIconWidth() * 0.98)),
                        Math.max(1, (int) Math.round(headingIcon.getIconHeight() * 0.98)),
                        Image.SCALE_SMOOTH));
            }
            heading.setIcon(headingIcon);
            heading.setText("");
        }
        heading.setOpaque(false);
        heading.getAccessibleContext().setAccessibleName(title + " · " + entries.size() + "개");
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
        list.setMargin(new Insets(18, 20, 18, 20));
        list.setCaretPosition(0);

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        JPanel listCard = new RoundedPanel(new BorderLayout());
        listCard.setBackground(Color.WHITE);
        listCard.setBorder(new RoundedBorder(Theme.PRIMARY, 2));
        listCard.add(scroll, BorderLayout.CENTER);
        panel.add(listCard, BorderLayout.CENTER);

        if (reviewOnly) {
            RoundedButton export = new RoundedButton("복습 단어 저장");
            export.setFont(new Font(Font.DIALOG, Font.BOLD, 16));
            export.setPreferredSize(new Dimension(0, 48));
            export.setEnabled(!entries.isEmpty());
            export.addActionListener(event -> exportReview(entries));
            panel.add(export, BorderLayout.SOUTH);
        }

        return panel;
    }

    private List<Word> loadWords() {
        try {
            return WordRepository.read(WORDS_FILE);
        } catch (IOException | IllegalStateException error) {
            wordLoadError = error.getMessage();
            return List.of();
        }
    }

    public void refreshReviews() {
        reviewEntries.clear();
        loadReviewEntries();
        refreshCard();
    }

    private void loadReviewEntries() {
        try {
            reviewEntries.addAll(reviews.read());
            reviewAvailable = true;
            reviewCheck.setToolTipText(null);
        } catch (IOException error) {
            reviewAvailable = false;
            reviewCheck.setToolTipText("복습 파일을 읽을 수 없어요. " + error.getMessage());
        }
    }

    private boolean isMarked(Word word) {
        return reviewEntries.contains(userPrefix + word.id());
    }

    private void saveReviewSelection() {
        Word word = words.get(wordIndex);
        String entry = userPrefix + word.id();
        Set<String> updated = new LinkedHashSet<>(reviewEntries);

        if (reviewCheck.isSelected()) {
            updated.add(entry);
        } else {
            updated.remove(entry);
        }

        try {
            reviews.save(updated);
            reviewEntries.clear();
            reviewEntries.addAll(updated);
        } catch (IOException error) {
            reviewCheck.setSelected(isMarked(word));
            showSaveError("복습 표시를 저장할 수 없어요.", error);
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

            reviews.export(path, entries);
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
        vocaland.ui.components.BackgroundPanel.paintBackground(this, graphics);
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

    private static ImageIcon loadIcon(String path, int width, int height, boolean trim) {
        return vocaland.ui.util.ImageAssets.load(path, width, height, trim ? 16 : -1, false);
    }


}
