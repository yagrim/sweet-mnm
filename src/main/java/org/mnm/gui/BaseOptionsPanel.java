package org.mnm.gui;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import java.awt.Component;

public abstract class BaseOptionsPanel extends JPanel {

    public BaseOptionsPanel(String title) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            title,
            TitledBorder.LEFT,
            TitledBorder.DEFAULT_POSITION
        ), BorderFactory.createEmptyBorder(2, 2, 0, 0)));
    }
}
