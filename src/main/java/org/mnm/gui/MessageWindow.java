package org.mnm.gui;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.lang.reflect.InvocationTargetException;

class MessageWindow {

    static void showInfoMessageDialogSync(String message) {
        showMessageDialogSync(message, JOptionPane.INFORMATION_MESSAGE);
    }

    static void showErrorMessageDialogSync(String message) {
        showMessageDialogSync(message, JOptionPane.ERROR_MESSAGE);
    }

    private static void showMessageDialogSync(String message, int type) {
        try {
            if (SwingUtilities.isEventDispatchThread()) {
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
