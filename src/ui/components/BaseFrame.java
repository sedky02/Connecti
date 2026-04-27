package ui.components;

import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Component;
import java.awt.Container;

public abstract class BaseFrame extends JFrame {
    protected static final Font APP_FONT = new Font("SansSerif", Font.PLAIN, 14);

    protected BaseFrame(String title, int width, int height) {
        super(title);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(new Dimension(width, height));
        setLocationRelativeTo(null);
        SwingUtilities.invokeLater(() -> applyFont(this));
    }

    protected void applyFont(Component root) {
        if (root == null) {
            return;
        }
        root.setFont(APP_FONT);
        if (root instanceof JComponent) {
            ((JComponent) root).setFont(APP_FONT);
        }
        if (root instanceof Container) {
            for (Component component : ((Container) root).getComponents()) {
                applyFont(component);
            }
        }
    }
}
