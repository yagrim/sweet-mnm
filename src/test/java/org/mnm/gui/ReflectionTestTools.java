package org.mnm.gui;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
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

    static JLabel getLabel(Object instance, String name) {
        return (JLabel) get(instance, name);
    }

    static JProgressBar getProgressBar(Object instance, String name) {
        return (JProgressBar) get(instance, name);
    }

    static Object get(Object instance, String name) {
        try {
            Field field = instance.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(instance);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
}
