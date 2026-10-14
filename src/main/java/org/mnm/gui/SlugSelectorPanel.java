package org.mnm.gui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.util.Comparator;
import java.util.List;

import org.mnm.client.SlugSelection;
import org.mnm.events.ClientEventHandler;

import static org.mnm.gui.Style.BASE_FONT_SIZE;
import static org.mnm.gui.Style.SCALE;

/**
 * Shows the game slugs available in the config database and allows selecting one.
 */
class SlugSelectorPanel extends JPanel {

    private static final int MAX_VISIBLE_CHARS = 8;

    private final JComboBox<String> slugs;
    // JComboBox fires ActionEvents when items are changed programmatically, those must not notify a selection
    private boolean updatingItems = false;

    SlugSelectorPanel(GuiCommand.SelectSlugAction selectSlugAction, float uiScaling) {
        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        this.setBorder(BorderFactory.createEmptyBorder(0, 0, 2 * SCALE, 0));

        JLabel label = new JLabel("Game");
        label.setToolTipText("Game slug used to install, repair and play");
        // Only disabled when there are no slugs, otherwise a disabled selector looks like an error to users
        slugs = new JComboBox<>();
        slugs.setEnabled(false);

        GuiComponents.setFontSize(label, BASE_FONT_SIZE * uiScaling);
        GuiComponents.setFontSize(slugs, BASE_FONT_SIZE * uiScaling);
        this.setMaximumSize(new Dimension(Integer.MAX_VALUE, slugs.getPreferredSize().height));

        slugs.addActionListener(_ -> {
            if (!updatingItems && getSelectedSlug() != null) {
                ClientEventHandler.getInstance()
                    .refresh(selectSlugAction.select(getSelectedSlug()));
            }
        });
        adjustWidth(List.of());

        // Align to the right
        add(Box.createHorizontalGlue());
        add(label);
        add(Box.createHorizontalStrut(SCALE));
        add(slugs);
    }

    void setItems(List<String> items, String selectedSlug) {
        updatingItems = true;
        try {
            slugs.removeAllItems();
            items.forEach(slugs::addItem);
            slugs.setSelectedItem(selectedSlug);
        } finally {
            updatingItems = false;
        }
        slugs.setEnabled(!items.isEmpty());
        adjustWidth(items);
    }

    /**
     * Fits the dropdown to the longest slug, up to {@link #MAX_VISIBLE_CHARS}.
     */
    private void adjustWidth(List<String> items) {
        final String longest = items.stream()
            .max(Comparator.comparingInt(String::length))
            .orElse(SlugSelection.DEFAULT_SLUG);
        slugs.setPrototypeDisplayValue(longest.length() > MAX_VISIBLE_CHARS ? longest.substring(0, MAX_VISIBLE_CHARS) : longest);
        slugs.setMaximumSize(slugs.getPreferredSize());
        revalidate();
    }

    String getSelectedSlug() {
        return (String) slugs.getSelectedItem();
    }
}
