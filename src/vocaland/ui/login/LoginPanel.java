package vocaland.ui.login;

import vocaland.ui.util.ImagePaths;

import vocaland.ui.components.RoundedBorder;
import vocaland.ui.components.RoundedButton;
import vocaland.ui.components.RoundedPanel;
import vocaland.ui.components.BackgroundPanel;
import vocaland.ui.util.LogoIcon;

import java.awt.*;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Temporary local profile chooser. Existing names reopen the same profile. */
public final class LoginPanel extends BackgroundPanel {
    public LoginPanel(Consumer<String> login) {
        setLayout(new GridBagLayout());
        JPanel card = new RoundedPanel(new GridLayout(0, 1, 0, 14));
        card.setBackground(new Color(255, 255, 255, 240));
        card.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                new RoundedBorder(new Color(209, 228, 187), 2), new EmptyBorder(30, 36, 30, 36)));
        JLabel title = new JLabel("보카랜드 프로필", SwingConstants.CENTER);
        title.setFont(new Font(Font.DIALOG, Font.BOLD, 26));
        ImageIcon titleIcon = LogoIcon.load(ImagePaths.LOGO_CREATE_PROFILE, 336, 67);
        if (titleIcon != null) {
            title.setIcon(titleIcon);
            title.setText("");
        }
        title.getAccessibleContext().setAccessibleName("보카랜드 프로필");
        JTextField name = new JTextField(20);
        name.setFont(new Font(Font.DIALOG, Font.PLAIN, 20));
        name.setHorizontalAlignment(JTextField.CENTER);
        name.getAccessibleContext().setAccessibleName("프로필 이름");
        JButton start = new RoundedButton("이 이름으로 시작");
        ImageIcon startIcon = LogoIcon.load(ImagePaths.TEXT_START, 70, 21);
        if (startIcon != null) {
            start.setIcon(startIcon);
            start.setText("");
        }
        start.getAccessibleContext().setAccessibleName("이 이름으로 시작");
        Runnable submit = () -> login.accept(name.getText());
        start.addActionListener(e -> submit.run());
        name.addActionListener(e -> submit.run());
        card.add(new JLabel("기존 이름은 이어하기, 새 이름은 프로필 생성", SwingConstants.CENTER));
        card.add(name);
        JPanel startRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        startRow.setOpaque(false);
        startRow.add(start);
        card.add(startRow);
        card.add(new JLabel("비밀번호 없이 이 PC에 저장됩니다.", SwingConstants.CENTER));
        JPanel content = new JPanel(new BorderLayout(0, 24));
        content.setOpaque(false);
        content.add(title, BorderLayout.NORTH);
        content.add(card, BorderLayout.CENTER);
        add(content);
    }
}
