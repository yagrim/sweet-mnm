package org.mnm.gui;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Font;
import java.lang.reflect.InvocationTargetException;

class MessageDialog {

    static void showInfoMessageDialogSync(String message) {
        showMessageDialogSync(message, JOptionPane.INFORMATION_MESSAGE, null);
    }

    // Use when we want to scale the window regardless of actual configuration
    static void showInfoMessageDialogSync(String message, Float scale) {
        showMessageDialogSync(message, JOptionPane.INFORMATION_MESSAGE, scale);
    }

    static void showErrorMessageDialogSync(String message) {
        showMessageDialogSync(message, JOptionPane.ERROR_MESSAGE, null);
    }

    private static void showMessageDialogSync(String message, int type, Float scale) {
        try {
            if (SwingUtilities.isEventDispatchThread()) {
                if (scale != null) {
                    UIManager.put("OptionPane.messageFont", new Font("Dialog", Font.PLAIN, Math.round(18 * scale)));
                    UIManager.put("OptionPane.buttonFont", new Font("Dialog", Font.PLAIN, Math.round(15 * scale)));
                    GuiComponents.scaleIcons(scale);
                }
                JOptionPane.showMessageDialog(null, message, "Error", type);
            } else {
                String title = type == JOptionPane.ERROR_MESSAGE ? "Error" : "Info";
                SwingUtilities.invokeAndWait(() -> JOptionPane.showMessageDialog(null, message, title, type));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while showing dialog", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Failed to show dialog", e.getCause());
        }
    }
}
