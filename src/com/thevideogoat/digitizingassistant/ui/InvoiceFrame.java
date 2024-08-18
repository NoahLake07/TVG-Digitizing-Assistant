package com.thevideogoat.digitizingassistant.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class InvoiceFrame extends JFrame {

    private static final Color PRIMARY_COLOR = new Color(255, 38, 0);
    private static final Color SECONDARY_COLOR = new Color(53, 72, 81);

    private static final Font MAIN_FONT = new Font("Arial", Font.PLAIN, 14);
    private static final Font LARGE_HEADER_FONT = new Font("Arial", Font.BOLD, 30);
    private static final Font HEADER_FONT = new Font("Arial", Font.BOLD, 18);
    private static final Font BOLD_FONT = new Font("Arial", Font.BOLD, 14);

    public InvoiceFrame() {
    }

    public InvoiceFrame(String clientName, String date, ArrayList<InvoiceItem> items) {
        setTitle("Invoice");
        setSize(600, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(mainPanel);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);

        BufferedImage tvgIcon = null;
        ImageIcon icon = null;
        try {
            tvgIcon = ImageIO.read(Objects.requireNonNull(DigitizingAssistant.class.getResourceAsStream("/TVG Logo (Linear).png")));
            int scaleFactor = 13;
            icon = new ImageIcon(tvgIcon.getScaledInstance(tvgIcon.getWidth() / scaleFactor, tvgIcon.getHeight() / scaleFactor, Image.SCALE_SMOOTH));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        JLabel logoLabel = new JLabel(icon);
        headerPanel.add(logoLabel, BorderLayout.EAST);

        JLabel titleLabel = new JLabel("INVOICE", SwingConstants.LEFT);
        titleLabel.setFont(LARGE_HEADER_FONT);
        titleLabel.setForeground(PRIMARY_COLOR);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        JPanel detailsPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        detailsPanel.setBackground(Color.WHITE);
        detailsPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        JLabel clientLabel = new JLabel("Client: " + clientName);
        clientLabel.setFont(MAIN_FONT);
        clientLabel.setForeground(SECONDARY_COLOR);
        detailsPanel.add(clientLabel);

        JLabel dateLabel = new JLabel("Date: " + date);
        dateLabel.setFont(MAIN_FONT);
        dateLabel.setForeground(SECONDARY_COLOR);
        detailsPanel.add(dateLabel);

        mainPanel.add(detailsPanel, BorderLayout.CENTER);

        String[] columnNames = {"ITEM", "QTY", "PRICE", "TOTAL", "NOTES"};
        Object[][] data = convertItemsToData(items);

        // Calculate subtotal
        double subtotal = 0.0;
        for (InvoiceItem item : items) {
            subtotal += item.total;
        }

        JTable table = new JTable(data, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table.setFont(MAIN_FONT);
        table.setRowHeight(30);
        table.getTableHeader().setFont(HEADER_FONT);
        table.getTableHeader().setBackground(PRIMARY_COLOR);
        table.getTableHeader().setForeground(Color.WHITE);

        JScrollPane tableScrollPane = new JScrollPane(table);
        mainPanel.add(tableScrollPane, BorderLayout.CENTER);

        JPanel subtotalPanel = new JPanel(new BorderLayout());
        JLabel subtotalLabel = new JLabel("Subtotal: $" + subtotal);
        subtotalLabel.setFont(BOLD_FONT);
        subtotalPanel.add(subtotalLabel, BorderLayout.EAST);
        mainPanel.add(subtotalPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private Object[][] convertItemsToData(ArrayList<InvoiceItem> items) {
        Object[][] data = new Object[items.size()][5];
        for (int i = 0; i < items.size(); i++) {
            InvoiceItem item = items.get(i);
            data[i][0] = item.itemName;
            data[i][1] = item.qty;
            data[i][2] = item.price;
            data[i][3] = item.total;
            data[i][4] = item.notes;
        }
        return data;
    }

    public class InvoiceItem {

        String itemName;
        int qty;
        double price, total;
        String notes;

        public InvoiceItem(String itemName, int qty, double price) {
            this(itemName, qty, price, "");
        }

        public InvoiceItem(String itemName, int qty, double price, String notes) {
            this.itemName = itemName;
            this.qty = qty;
            this.price = price;
            this.total = qty * price;
            this.notes = notes;
        }
    }

    private ArrayList<InvoiceItem> getTestItems() {
        ArrayList<InvoiceItem> items = new ArrayList<>();
        items.add(new InvoiceItem("Item 1", 1, 40.00));
        items.add(new InvoiceItem("Item 2", 2, 30.00));

        return items;
    }

    public static void main(String[] args) {
        new InvoiceFrame("John Doe", "08-17-2024", new InvoiceFrame().getTestItems());
    }
}