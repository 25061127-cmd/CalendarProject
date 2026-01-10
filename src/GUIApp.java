import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

public class GUIApp extends JFrame {
    private JTable eventTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;

    public GUIApp() {
        // 1. Setup Main Window
        setTitle("WIX1002 Calendar App (GUI Version)");
        setSize(850, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center the window
        setLayout(new BorderLayout(10, 10));

        // 2. Top Panel: Search Bar
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchField = new JTextField(20);
        JButton searchBtn = new JButton("🔍 Search");
        JButton reloadBtn = new JButton("🔄 Show All");

        topPanel.add(new JLabel("Keyword:"));
        topPanel.add(searchField);
        topPanel.add(searchBtn);
        topPanel.add(reloadBtn);
        add(topPanel, BorderLayout.NORTH);

        // 3. Center Panel: Table to display events
        // Table Headers
        String[] columnNames = { "ID", "Title", "Start Time", "End Time", "Description" };
        // Create model and disable cell editing
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        eventTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(eventTable);
        add(scrollPane, BorderLayout.CENTER);

        // 4. Bottom Panel: Action Buttons
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addBtn = new JButton("➕ New Event");
        JButton deleteBtn = new JButton("🗑️ Delete Selected");
        JButton statsBtn = new JButton("📊 View Statistics");

        // Button Styling (Optional)
        addBtn.setBackground(new Color(76, 175, 80)); // Green
        addBtn.setForeground(Color.WHITE);
        deleteBtn.setBackground(new Color(244, 67, 54)); // Red
        deleteBtn.setForeground(Color.WHITE);

        bottomPanel.add(statsBtn);
        bottomPanel.add(deleteBtn);
        bottomPanel.add(addBtn);
        add(bottomPanel, BorderLayout.SOUTH);

        // 5. Event Listeners (Logic Connection)

        // Load initial data
        loadTableData(null);

        // Reload/Reset Button
        reloadBtn.addActionListener(e -> {
            searchField.setText("");
            loadTableData(null);
        });

        // Search Button
        searchBtn.addActionListener(e -> {
            String keyword = searchField.getText().trim();
            loadTableData(keyword);
        });

        // Add Button -> Open Dialog
        addBtn.addActionListener(e -> showAddEventDialog());

        // Delete Button
        deleteBtn.addActionListener(e -> deleteSelectedEvent());

        // Stats Button
        statsBtn.addActionListener(e -> showStatisticsDialog());
    }

    // --- Helper: Load Data into Table ---
    private void loadTableData(String keyword) {
        // Clear existing data
        tableModel.setRowCount(0);

        List<Event> events;
        if (keyword == null || keyword.isEmpty()) {
            events = FileManager.loadEvents();
        } else {
            events = SchedulerLogic.searchEvents(keyword);
        }

        for (Event e : events) {
            Object[] rowData = {
                    e.getId(),
                    e.getTitle(),
                    e.getStartDateTime().format(Event.PRINT_FORMATTER),
                    e.getEndDateTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")), // Only show time
                                                                                                      // for end
                    e.getDescription()
            };
            tableModel.addRow(rowData);
        }
    }

    // --- Helper: Delete Selected Row ---
    private void deleteSelectedEvent() {
        int selectedRow = eventTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a row in the table first!");
            return;
        }

        int id = (int) tableModel.getValueAt(selectedRow, 0); // Get ID from column 0
        String title = (String) tableModel.getValueAt(selectedRow, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete [" + title + "]?", "Confirm Delete", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            if (SchedulerLogic.deleteEvent(id)) {
                loadTableData(null); // Refresh table
                JOptionPane.showMessageDialog(this, "Deleted successfully!");
            }
        }
    }

    // --- Helper: Show Statistics ---
    private void showStatisticsDialog() {
        List<Event> events = FileManager.loadEvents();
        String msg = "Total Events: " + events.size() + "\n" +
                "Data is automatically backed up to your local CSV file.";
        JOptionPane.showMessageDialog(this, msg, "Statistics", JOptionPane.INFORMATION_MESSAGE);
    }

    // --- Core: Add Event Dialog ---
    private void showAddEventDialog() {
        JDialog dialog = new JDialog(this, "Create New Event", true);
        dialog.setSize(400, 300);
        dialog.setLayout(new GridLayout(5, 2, 10, 10));
        dialog.setLocationRelativeTo(this);

        JTextField titleField = new JTextField();
        JTextField descField = new JTextField();
        // Default values for easier testing
        JTextField startField = new JTextField("2025-10-20 10:00");
        JTextField endField = new JTextField("2025-10-20 11:00");

        dialog.add(new JLabel("  Title:"));
        dialog.add(titleField);
        dialog.add(new JLabel("  Description:"));
        dialog.add(descField);
        dialog.add(new JLabel("  Start (yyyy-MM-dd HH:mm):"));
        dialog.add(startField);
        dialog.add(new JLabel("  End (yyyy-MM-dd HH:mm):"));
        dialog.add(endField);

        JButton saveBtn = new JButton("Save");
        dialog.add(new JLabel("")); // Spacer
        dialog.add(saveBtn);

        saveBtn.addActionListener(e -> {
            try {
                String title = titleField.getText();
                String desc = descField.getText();
                LocalDateTime start = LocalDateTime.parse(startField.getText(), Event.PRINT_FORMATTER);
                LocalDateTime end = LocalDateTime.parse(endField.getText(), Event.PRINT_FORMATTER);

                if (end.isBefore(start)) {
                    JOptionPane.showMessageDialog(dialog, "End time cannot be before start time!", "Input Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Conflict Detection Logic
                if (SchedulerLogic.hasConflict(start, end)) {
                    int choice = JOptionPane.showConfirmDialog(dialog,
                            "⚠️ Conflict detected! Save anyway?", "Conflict Warning", JOptionPane.YES_NO_OPTION);
                    if (choice != JOptionPane.YES_OPTION)
                        return;
                }

                // Save to file
                Event newEvent = new Event(FileManager.getNextId(), title, desc, start, end);
                FileManager.appendEvent(newEvent);

                JOptionPane.showMessageDialog(dialog, "Saved successfully!");
                dialog.dispose(); // Close dialog
                loadTableData(null); // Refresh table

            } catch (DateTimeParseException ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid Date Format! Use: yyyy-MM-dd HH:mm", "Format Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setVisible(true);
    }

    // Main Entry Point
    public static void main(String[] args) {
        // Run UI on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            new GUIApp().setVisible(true);
        });
    }
}