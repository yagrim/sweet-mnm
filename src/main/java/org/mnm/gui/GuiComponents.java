package org.mnm.gui;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GuiComponents {

    private static final Logger logger = LoggerFactory.getLogger(GuiComponents.class);

    static void scaleUi(float scale) {
        UIDefaults defaults = UIManager.getDefaults();
        Enumeration<Object> keys = defaults.keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            try {
                Object value = defaults.get(key);
                if (value instanceof Font font) {
                    defaults.put(key, new FontUIResource(font.deriveFont(font.getSize2D() * scale)));
                }
            } catch (Exception e) {
                logger.debug("Could not initialize UI component: " + key, e);
            }
        }
        scaleIcons(scale);
    }

    static void scaleIcons(float scale) {
        scaleIcon("CheckBox.icon", scale);
        scaleIcon("RadioButton.icon", scale);
        scaleIcon("OptionPane.errorIcon", scale);
        scaleIcon("OptionPane.informationIcon", scale);
    }

    static void scaleIcon(String key, float scale) {
        Icon icon = UIManager.getIcon(key);
        if (icon instanceof ScaledIcon scaledIcon) {
            icon = scaledIcon.delegate();
        }
        if (icon != null) {
            UIManager.put(key, new ScaledIcon(icon, scale));
        }
    }

    static void setFontSize(JComponent component, float size) {
        component.setFont(component.getFont().deriveFont(size));
    }

    private record ScaledIcon(Icon delegate, float scale) implements Icon {

        @Override
        public int getIconWidth() {
            return Math.round(delegate.getIconWidth() * scale);
        }

        @Override
        public int getIconHeight() {
            return Math.round(delegate.getIconHeight() * scale);
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D scaledGraphics = (Graphics2D) graphics.create();
            try {
                scaledGraphics.translate(x, y);
                scaledGraphics.scale(scale, scale);
                delegate.paintIcon(component, scaledGraphics, 0, 0);
            } finally {
                scaledGraphics.dispose();
            }
        }
    }
}
