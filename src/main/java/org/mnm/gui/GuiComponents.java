package org.mnm.gui;

import javax.swing.JComponent;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.FontUIResource;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.util.Enumeration;

public class GuiComponents {

    static void scaleUiFontSizes(float scale) {
        UIDefaults defaults = UIManager.getDefaults();
        Enumeration<Object> keys = defaults.keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = defaults.get(key);
            if (value instanceof Font font) {
                defaults.put(key, new FontUIResource(font.deriveFont(font.getSize2D() * scale)));
            }
        }
    }

    static void setFontSize(JComponent component, float size) {
        component.setFont(component.getFont().deriveFont(size));
    }
}
