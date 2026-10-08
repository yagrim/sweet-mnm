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

    static Object get(Object instance, String name) {
        try {
            Field field = instance.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(instance);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    static <T> T getField(Object instance, String fieldName, Class<T> type) {
        try {
            Class<?> clazz = instance.getClass();

            while (clazz != null) {
                try {
                    Field field = clazz.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    return (T) field.get(instance);
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
