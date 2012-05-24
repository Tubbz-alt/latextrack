/**
 ************************ 80 columns *******************************************
 * AuthorCellRenderer
 *
 * Created on Aug 6, 2010.
 *
 * Copyright 2009-2010, SRI International.
 */
package com.sri.ltc.editor;

import javax.swing.*;
import java.awt.*;

/**
 * @author linda
 */
@SuppressWarnings("serial")
public final class AuthorCellRenderer extends JLabel implements ListCellRenderer{

    public AuthorCellRenderer() {
        setOpaque(true);
    }

    public Component getListCellRendererComponent(JList list,
                                                  Object value,
                                                  int index,
                                                  boolean isSelected,
                                                  boolean cellHasFocus) {
        if (value instanceof AuthorCell) {
            AuthorCell author = (AuthorCell) value;
            setText(author.label);
            Icon icon = new ColorIcon(author.getColor());
            setIcon(icon);
            setDisabledIcon(icon);
            setEnabled(author.limited);
        } else {
            setText(value.toString());
            setIcon(null);
        }

        // deal with default colors
        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }

        return this;
    }
}