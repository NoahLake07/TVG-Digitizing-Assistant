package com.thevideogoat.digitizingassistant.ui;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class Theme {
    // Colors
    public static final Color BACKGROUND = new Color(32, 32, 32);
    public static final Color SURFACE = new Color(45, 45, 45);
    public static final Color ACCENT = new Color(70, 130, 180);
    public static final Color ACCENT_HOVER = new Color(90, 150, 200);
    public static final Color BORDER = new Color(64, 64, 64);
    public static final Color TEXT = Color.WHITE;
    public static final Color TEXT_SECONDARY = new Color(180, 180, 180);

    // Fonts
    public static final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font NORMAL_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font SMALL_FONT = new Font("Segoe UI", Font.PLAIN, 12);

    // Common dimensions
    public static final Dimension BUTTON_SIZE = new Dimension(120, 35);
    public static final Dimension INPUT_SIZE = new Dimension(200, 30);
    public static final int PADDING = 10;

    // Styling methods
    public static void styleButton(JButton button) {
        button.setBackground(ACCENT);
        button.setForeground(TEXT);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setFont(NORMAL_FONT);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(ACCENT_HOVER);
                }
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(ACCENT);
                }
            }
        });
    }

    public static void stylePanel(JPanel panel) {
        panel.setBackground(BACKGROUND);
    }

    public static void styleList(JList<?> list) {
        list.setBackground(SURFACE);
        list.setForeground(TEXT);
        list.setSelectionBackground(ACCENT);
        list.setSelectionForeground(TEXT);
        list.setFont(NORMAL_FONT);
    }

    public static void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        scrollPane.setBackground(SURFACE);
    }

    public static void styleTextField(JTextField textField) {
        textField.setBackground(SURFACE);
        textField.setForeground(TEXT);
        textField.setCaretColor(TEXT);
        textField.setFont(NORMAL_FONT);
        textField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
    }

    public static void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setBackground(SURFACE);
        comboBox.setForeground(TEXT);
        comboBox.setFont(NORMAL_FONT);
        comboBox.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        // Style the editor component (the visible part when not dropped down)
        Component editor = comboBox.getEditor().getEditorComponent();
        if (editor instanceof JTextField) {
            JTextField textField = (JTextField) editor;
            textField.setBackground(SURFACE);
            textField.setForeground(TEXT);
            textField.setBorder(null);
        }

        // Style the renderer for both the selected item and dropdown items
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBackground(isSelected ? ACCENT : SURFACE);
                setForeground(TEXT);
                setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                return this;
            }
        });

        // Force the combo box to use our colors
        comboBox.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton button = super.createArrowButton();
                button.setBackground(SURFACE);
                button.setForeground(TEXT);
                button.setBorder(null);
                return button;
            }
        });
    }

    public static void styleSpinner(JSpinner spinner) {
        spinner.setBackground(SURFACE);
        spinner.setForeground(TEXT);
        spinner.setFont(NORMAL_FONT);
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
            tf.setBackground(SURFACE);
            tf.setForeground(TEXT);
            tf.setCaretColor(TEXT);
        }
    }
}