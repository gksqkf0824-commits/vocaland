package vocaland;

import javax.swing.SwingUtilities;
import vocaland.ui.MainFrame;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                MainFrame frame = new MainFrame();
                frame.setVisible(true);
            } catch (RuntimeException error) {
                javax.swing.JOptionPane.showMessageDialog(null, error.getMessage(),
                        "데이터를 불러올 수 없어요", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}