package org.mnm.gui;

import javax.swing.*;

public class GuiComponents {

    static void setFontSize(JComponent component, float size) {
        component.setFont(component.getFont().deriveFont(size));
    }

}
