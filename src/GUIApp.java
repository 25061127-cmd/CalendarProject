import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GUIApp extends JFrame {

    // Core Data
    private List<Event> allEvents;
    private YearMonth currentMonth;

    // UI Components
    private JPanel calendarGrid;
    private JLabel monthLabel;
    private DefaultTableModel listTableModel;

    // Constructor: Entry Point
    public GUIApp() {
        // 1. Basic Window Setup
        setTitle("Calendar App (Full Version)");
        setSize(1100, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center the window

        // 2. Load Data
        refreshData();
        currentMonth = YearMonth.now();

        // 3. Setup Menu Bar (Basic Requirement)
        setupMenuBar();

        // 4. Build Main Interface (Tabbed Layout)
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Arial", Font.PLAIN, 14));

        // Tab 1: Calendar Grid View (Basic)
        tabbedPane.addTab(" Calendar View", createCalendarTab());

        // Tab 2: Event Management (Basic)
        tabbedPane.addTab(" Manage Events", createListTab());

        // Tab 3: Statistics Dashboard (Additional Feature)
        tabbedPane.addTab(" Statistics", createStatsTab());

        add(tabbedPane);

        // 5. Check Reminders on Launch (Additional Feature)
        // This runs after the UI is built
        SwingUtilities.invokeLater(this::checkRemindersOnLaunch);
    }

    // Basic Requirement: Backup & Restore (Implemented via Menu Bar)

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Data Management Menu
        JMenu dataMenu = new JMenu(" Data Management");
        JMenuItem backupItem = new JMenuItem("Backup Data");
        JMenuItem restoreItem = new JMenuItem("Restore Data");

        // Logic: Backup
        backupItem.addActionListener(e -> {
            boolean success = FileManager.backupEvents();
            if (success)
                JOptionPane.showMessageDialog(this, " Backup created successfully!");
            else
                JOptionPane.showMessageDialog(this, " Backup failed!", "Error", JOptionPane.ERROR_MESSAGE);
        });

        // Logic: Restore
        restoreItem.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    " Restore will overwrite current data. Continue?", "Confirm Restore", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                boolean success = FileManager.restoreEvents();
                if (success) {
                    refreshData(); // Reload data into memory
                    refreshCalendarGrid(); // Refresh UI
                    loadTable(null);
                    JOptionPane.showMessageDialog(this, " Data restored successfully!");
                } else {
                    JOptionPane.showMessageDialog(this, " Restore failed!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        dataMenu.add(backupItem);
        dataMenu.add(restoreItem);
        menuBar.add(dataMenu);

        // Help Menu
        JMenu helpMenu = new JMenu("Help");
        helpMenu.add(new JMenuItem("About"));
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    // Basic Requirement: Calendar View (Tab 1)

    private JPanel createCalendarTab() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Navigation Buttons
        JButton prev = new JButton(" < ");
        JButton next = new JButton(" > ");
        monthLabel = new JLabel("", SwingConstants.CENTER);
        monthLabel.setFont(new Font("Arial", Font.BOLD, 20));

        prev.addActionListener(e -> {
            currentMonth = currentMonth.minusMonths(1);
            refreshCalendarGrid();
        });
        next.addActionListener(e -> {
            currentMonth = currentMonth.plusMonths(1);
            refreshCalendarGrid();
        });

        top.add(prev, BorderLayout.WEST);
        top.add(monthLabel, BorderLayout.CENTER);
        top.add(next, BorderLayout.EAST);

        // Weekday Headers
        JPanel header = new JPanel(new GridLayout(1, 7));
        for (String d : new String[] { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" }) {
            JLabel l = new JLabel(d, SwingConstants.CENTER);
            l.setFont(new Font("Arial", Font.BOLD, 12));
            header.add(l);
        }

        // Calendar Grid
        calendarGrid = new JPanel(new GridLayout(0, 7, 2, 2));
        JPanel center = new JPanel(new BorderLayout());
        center.add(header, BorderLayout.NORTH);
        center.add(calendarGrid, BorderLayout.CENTER);

        panel.add(top, BorderLayout.NORTH);
        panel.add(center, BorderLayout.CENTER);
        refreshCalendarGrid();
        return panel;
    }

    // ✅ FIXED: Refresh the calendar grid cells with Locale Fix
    private void refreshCalendarGrid() {
        // Safety check
        if (currentMonth == null)
            currentMonth = YearMonth.now();

        // 1. Fix: Force English Locale and Black Color
        monthLabel.setForeground(Color.BLACK);
        monthLabel.setText(currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.ENGLISH)));

        calendarGrid.removeAll();

        LocalDate firstDay = currentMonth.atDay(1);
        int startOffset = firstDay.getDayOfWeek().getValue() % 7; // Sunday is 0
        int daysInMonth = currentMonth.lengthOfMonth();

        // Fill empty slots for previous month
        for (int i = 0; i < startOffset; i++)
            calendarGrid.add(new JLabel(""));

        // Fill actual days
        for (int i = 1; i <= daysInMonth; i++) {
            LocalDate date = currentMonth.atDay(i);
            calendarGrid.add(createDayCell(date));
        }

        // Fill empty slots for next month to maintain grid shape
        int total = startOffset + daysInMonth;
        while (total < 42) {
            calendarGrid.add(new JLabel(""));
            total++;
        }

        calendarGrid.revalidate();
        calendarGrid.repaint();

        // 2. Fix: Force refresh parent container
        if (monthLabel.getParent() != null) {
            monthLabel.getParent().revalidate();
            monthLabel.getParent().repaint();
        }
    }

    // Create individual day cell
    private JPanel createDayCell(LocalDate date) {
        JPanel cell = new JPanel(new BorderLayout());
        cell.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        cell.setBackground(Color.WHITE);

        JLabel dayNum = new JLabel(" " + date.getDayOfMonth());
        cell.add(dayNum, BorderLayout.NORTH);

        // Check for events on this day
        long count = allEvents.stream().filter(e -> e.getStartDateTime().toLocalDate().equals(date)).count();
        if (count > 0) {
            JLabel dot = new JLabel(" ● " + count + " events");
            dot.setForeground(new Color(33, 150, 243));
            cell.add(dot, BorderLayout.CENTER);
            cell.setBackground(new Color(240, 248, 255)); // Highlight background
        }

        // Highlight today's date
        if (date.equals(LocalDate.now()))
            cell.setBorder(BorderFactory.createLineBorder(Color.RED, 2));

        // Click listener to view details
        cell.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                showDayDetail(date);
            }
        });
        return cell;
    }

    // Basic Requirement: Manage Events (List, Search, Edit, Delete)

    private JPanel createListTab() {
        JPanel panel = new JPanel(new BorderLayout());

        // Top: Search Bar
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField searchField = new JTextField(20);
        JButton searchBtn = new JButton(" Search");
        JButton resetBtn = new JButton("Reset");
        top.add(new JLabel("Keyword:"));
        top.add(searchField);
        top.add(searchBtn);
        top.add(resetBtn);

        // Center: Table
        String[] cols = { "ID", "Title", "Start", "End", "Description" };
        listTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable table = new JTable(listTableModel);

        // Bottom: Action Buttons (Edit & Delete)
        JPanel bottom = new JPanel();
        JButton editBtn = new JButton(" Edit Selected"); // Req: Update
        JButton delBtn = new JButton(" Delete Selected"); // Req: Delete
        bottom.add(editBtn);
        bottom.add(delBtn);

        // Event Listeners
        searchBtn.addActionListener(e -> loadTable(searchField.getText()));
        resetBtn.addActionListener(e -> {
            searchField.setText("");
            loadTable(null);
        });

        // Delete Logic
        delBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select a row first!");
                return;
            }
            int id = (int) table.getValueAt(row, 0);

            if (JOptionPane.showConfirmDialog(this, "Delete event " + id + "?", "Confirm",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                SchedulerLogic.deleteEvent(id); // Call logic layer
                refreshData();
                loadTable(null);
                refreshCalendarGrid();
            }
        });

        // Edit Logic (Update)
        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select a row first!");
                return;
            }
            int id = (int) table.getValueAt(row, 0);

            // Find the specific Event object
            Event target = allEvents.stream().filter(ev -> ev.getId() == id).findFirst().orElse(null);
            if (target != null) {
                showEditDialog(target); // Open Edit Dialog
            }
        });

        loadTable(null);
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    // Helper to load data into the table with optional filtering
    private void loadTable(String keyword) {
        listTableModel.setRowCount(0);
        for (Event e : allEvents) {
            if (keyword == null || keyword.isEmpty() || e.getTitle().toLowerCase().contains(keyword.toLowerCase())) {
                listTableModel.addRow(new Object[] {
                        e.getId(), e.getTitle(),
                        e.getStartDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                        e.getEndDateTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                        e.getDescription()
                });
            }
        }
    }

    // Basic Requirement: Dialog Logic (View Details, Add, Edit)

    private void showDayDetail(LocalDate date) {
        JDialog d = new JDialog(this, "Events on " + date, true); // Modal dialog
        d.setSize(400, 400);
        d.setLayout(new BorderLayout());
        d.setLocationRelativeTo(this);

        DefaultTableModel m = new DefaultTableModel(new String[] { "Time", "Title" }, 0);
        allEvents.stream().filter(e -> e.getStartDateTime().toLocalDate().equals(date))
                .forEach(e -> m.addRow(new String[] { e.getStartDateTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                        e.getTitle() }));

        JButton addBtn = new JButton("add New Event");
        addBtn.addActionListener(e -> {
            d.dispose();
            showEventDialog(null, date);
        }); // null means Add Mode

        d.add(new JScrollPane(new JTable(m)), BorderLayout.CENTER);
        d.add(addBtn, BorderLayout.SOUTH);
        d.setVisible(true);
    }

    private void showEditDialog(Event event) {
        showEventDialog(event, null); // event not null means Edit Mode
    }

    // Unified Dialog for Add and Edit
    private void showEventDialog(Event eventToEdit, LocalDate defaultDate) {
        boolean isEdit = (eventToEdit != null);
        JDialog d = new JDialog(this, isEdit ? "Edit Event" : "New Event", true);
        d.setSize(350, 320);
        d.setLayout(new GridLayout(6, 2, 10, 10));
        d.setLocationRelativeTo(this);

        // Pre-fill data if in Edit mode
        JTextField tF = new JTextField(isEdit ? eventToEdit.getTitle() : "");
        JTextField dF = new JTextField(isEdit ? eventToEdit.getDescription() : "");

        String startStr = isEdit ? eventToEdit.getStartDateTime().format(Event.PRINT_FORMATTER)
                : (defaultDate + " 09:00");
        String endStr = isEdit ? eventToEdit.getEndDateTime().format(Event.PRINT_FORMATTER)
                : (defaultDate + " 10:00");

        JTextField sF = new JTextField(startStr);
        JTextField eF = new JTextField(endStr);

        d.add(new JLabel(" Title:"));
        d.add(tF);
        d.add(new JLabel(" Description:"));
        d.add(dF);
        d.add(new JLabel(" Start (yyyy-MM-dd HH:mm):"));
        d.add(sF);
        d.add(new JLabel(" End (yyyy-MM-dd HH:mm):"));
        d.add(eF);
        d.add(new JLabel(""));

        JButton saveBtn = new JButton("Save");
        d.add(saveBtn);

        saveBtn.addActionListener(e -> {
            try {
                LocalDateTime s = LocalDateTime.parse(sF.getText(), Event.PRINT_FORMATTER);
                LocalDateTime en = LocalDateTime.parse(eF.getText(), Event.PRINT_FORMATTER);

                if (en.isBefore(s)) {
                    JOptionPane.showMessageDialog(d, "Time Error! End time cannot be before Start time.");
                    return;
                }

                // Conflict Detection (part of basic integrity check here)
                if (hasConflict(s, en, isEdit ? eventToEdit.getId() : -1)) {
                    if (JOptionPane.showConfirmDialog(d, " Time Conflict Detected! Save anyway?", "Warning",
                            JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
                        return;
                }

                if (isEdit) {
                    // Update Logic: Delete old, add new (Simulating Update)
                    SchedulerLogic.deleteEvent(eventToEdit.getId());
                    Event newEv = new Event(eventToEdit.getId(), tF.getText(), dF.getText(), s, en); // Keep original ID
                    FileManager.appendEvent(newEv);
                } else {
                    // Create Logic
                    Event newEv = new Event(FileManager.getNextId(), tF.getText(), dF.getText(), s, en);
                    FileManager.appendEvent(newEv);
                }

                // Refresh everything
                refreshData();
                refreshCalendarGrid();
                loadTable(null);
                JOptionPane.showMessageDialog(d, "Saved Successfully!");
                d.dispose();

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(d, "Invalid Date Format! Please use yyyy-MM-dd HH:mm");
            }
        });
        d.setVisible(true);
    }

    // Additional Feature 1: Reminders (Notifications on Startup)

    private void checkRemindersOnLaunch() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next24h = now.plusHours(24);

        // Filter events happening in the next 24 hours
        List<Event> upcoming = allEvents.stream()
                .filter(e -> e.getStartDateTime().isAfter(now) && e.getStartDateTime().isBefore(next24h))
                .sorted()
                .collect(Collectors.toList());

        if (!upcoming.isEmpty()) {
            StringBuilder msg = new StringBuilder(" Reminder: You have " + upcoming.size() + " upcoming events!\n\n");
            for (Event e : upcoming) {
                long hours = ChronoUnit.HOURS.between(now, e.getStartDateTime());
                msg.append(String.format("- %s (in %d hours)\n", e.getTitle(), hours));
            }
            JOptionPane.showMessageDialog(this, msg.toString(), "Daily Reminder", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // Additional Feature 2: Statistics (Dashboard Tab)

    private JPanel createStatsTab() {
        JPanel container = new JPanel(new BorderLayout());
        JPanel panel = new JPanel(new GridLayout(2, 2, 20, 20));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setBackground(new Color(240, 240, 245));

        // Logic to calculate and display stats
        Runnable refreshStats = () -> {
            panel.removeAll();

            // Stat 1: Total Events
            panel.add(createStatCard("Total Events", String.valueOf(allEvents.size()), new Color(33, 150, 243)));

            // Stat 2: Events This Month
            long thisMonth = allEvents.stream()
                    .filter(e -> YearMonth.from(e.getStartDateTime()).equals(YearMonth.now()))
                    .count();
            panel.add(createStatCard("This Month", String.valueOf(thisMonth), new Color(76, 175, 80)));

            // Stat 3: Busiest Day of Week
            Map<DayOfWeek, Long> counts = allEvents.stream()
                    .collect(Collectors.groupingBy(e -> e.getStartDateTime().getDayOfWeek(), Collectors.counting()));
            DayOfWeek busiest = counts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);
            panel.add(
                    createStatCard("Busiest Day", busiest == null ? "-" : busiest.toString(), new Color(255, 152, 0)));

            // Stat 4: Upcoming Events
            long future = allEvents.stream()
                    .filter(e -> e.getStartDateTime().isAfter(LocalDateTime.now()))
                    .count();
            panel.add(createStatCard("Upcoming", String.valueOf(future), new Color(156, 39, 176)));

            panel.revalidate();
            panel.repaint();
        };

        // Initial Load
        refreshStats.run();

        // Refresh Button
        JButton refreshBtn = new JButton("Refresh Statistics");
        refreshBtn.addActionListener(e -> {
            refreshData();
            refreshStats.run();
        });

        container.add(panel, BorderLayout.CENTER);
        container.add(refreshBtn, BorderLayout.SOUTH);
        return container;
    }

    // Helper to create a styled statistic card
    private JPanel createStatCard(String title, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createLineBorder(color, 2));
        card.setBackground(Color.WHITE);

        JLabel t = new JLabel(title, SwingConstants.CENTER);
        t.setFont(new Font("Arial", Font.BOLD, 16));
        t.setBorder(new EmptyBorder(10, 0, 0, 0));

        JLabel v = new JLabel(value, SwingConstants.CENTER);
        v.setFont(new Font("Arial", Font.BOLD, 40));
        v.setForeground(color);

        card.add(t, BorderLayout.NORTH);
        card.add(v, BorderLayout.CENTER);
        return card;
    }

    // Helper Methods

    // Check for time conflicts
    private boolean hasConflict(LocalDateTime start, LocalDateTime end, int ignoreId) {
        for (Event e : allEvents) {
            if (e.getId() == ignoreId)
                continue; // Skip self
            // Logic: (StartA < EndB) and (EndA > StartB) means overlap
            if (start.isBefore(e.getEndDateTime()) && end.isAfter(e.getStartDateTime())) {
                return true;
            }
        }
        return false;
    }

    private void refreshData() {
        allEvents = FileManager.loadEvents();
    }

    // Main Method
    public static void main(String[] args) {
        // Run on Event Dispatch Thread for thread safety
        SwingUtilities.invokeLater(() -> new GUIApp().setVisible(true));
    }
}