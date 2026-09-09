package org.mnm.gui;

import javax.swing.JComponent;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;

public class GuiComponents {

    static void setFontSize(JComponent component, float size) {
        component.setFont(component.getFont().deriveFont(size));
    }

    static void scaleFontSize(JComponent component, float scale) {
        Font font = component.getFont();
        int size = font.getSize();
        component.setFont(font.deriveFont(size * scale));
    }

    static void scaleFontSizeRecursively(Component component, float scale) {
        if (component instanceof JComponent swingComponent) {
            Font font1 = swingComponent.getFont();
            setFontSize(swingComponent, font1.getSize() * scale);
            setTitledBorderFont(swingComponent.getBorder(), swingComponent.getFont());
        }

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                scaleFontSizeRecursively(child, scale);
            }
        }
    }

    private static void setTitledBorderFont(Border border, Font font) {
        if (border instanceof TitledBorder titledBorder) {
            titledBorder.setTitleFont(font);
        } else if (border instanceof CompoundBorder compoundBorder) {
            setTitledBorderFont(compoundBorder.getOutsideBorder(), font);
            setTitledBorderFont(compoundBorder.getInsideBorder(), font);
        }
    }

}
