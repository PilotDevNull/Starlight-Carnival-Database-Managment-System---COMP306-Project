package starlightcarnival;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Use cross-platform LAF so setBackground/setForeground are respected.
                // The system LAF ignores component-level color overrides
                // on combo boxes, causing white text on white backgrounds.
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {}

            // Set combo box colors globally so every JComboBox in the app
            // inherits them without needing custom renderers or UI overrides.
            // this was a colour bug that we were having with the combo box test
            // being white with the combo box
            Color CARD    = new Color(26, 31, 50);
            Color FG      = new Color(225, 228, 245);
            Color SEL_BG  = new Color(55, 42, 10);
            Color ACCENT  = new Color(255, 160, 0);
            Color BORDER  = new Color(40, 48, 72);

            UIManager.put("ComboBox.background",              CARD);
            UIManager.put("ComboBox.foreground",              FG);
            UIManager.put("ComboBox.selectionBackground",     SEL_BG);
            UIManager.put("ComboBox.selectionForeground",     ACCENT);
            UIManager.put("ComboBox.buttonBackground",        CARD);
            UIManager.put("ComboBox.buttonShadow",            BORDER);
            UIManager.put("ComboBox.buttonDarkShadow",        BORDER);
            UIManager.put("ComboBox.buttonHighlight",         CARD);
            UIManager.put("ComboBox.disabledBackground",      CARD);
            UIManager.put("ComboBox.disabledForeground",      FG);
            UIManager.put("ComboBox.listBackground",          CARD);
            UIManager.put("ComboBox.listForeground",          FG);

            new LoginFrame().setVisible(true);
        });
    }
}
