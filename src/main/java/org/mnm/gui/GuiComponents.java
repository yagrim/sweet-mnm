package org.mnm.gui;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.Component;
import java.awt.Container;

public class GuiComponents {

    static void setFontSize(JComponent component, float size) {
        component.setFont(component.getFont().deriveFont(size));
    }

    static void setFontSizeRecursively(Component component, float size) {
        if (component instanceof JComponent swingComponent) {
            setFontSize(swingComponent, size);
            if (swingComponent.getBorder() instanceof TitledBorder titledBorder) {
                titledBorder.setTitleFont(swingComponent.getFont().deriveFont(size));
            }
        }

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                setFontSizeRecursively(child, size);
            }
        }
    }

}
