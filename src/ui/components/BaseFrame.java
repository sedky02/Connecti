package ui.components;

import javax.swing.JFrame;
import java.awt.Dimension;

public abstract class BaseFrame extends JFrame {
    protected BaseFrame(String title, int width, int height) {
        super(title);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(new Dimension(width, height));
        setLocationRelativeTo(null);
    }
}