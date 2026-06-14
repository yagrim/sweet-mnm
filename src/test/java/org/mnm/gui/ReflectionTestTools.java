package org.mnm.gui;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.lang.reflect.Field;

class ReflectionTestTools {

    static JButton getButton(JPanel handler, String name) {
        try {
            Field field = handler.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return (JButton) field.get(handler);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
}
