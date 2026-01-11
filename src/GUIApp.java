import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class GUIApp extends JFrame {

    private YearMonth currentMonth;
    private LocalDate selectedDate; // Currently selected date
    private JPanel calendarGrid;
    private JLabel monthLabel;

    // Reference to data logic
    private List<Event> allEvents;

    public GUIApp() {
        // 1. Basic Window Setup
        setTitle("Calendar Application");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Load data
        allEvents = FileManager.loadEvents();
        currentMonth = YearMonth.now();
        selectedDate = LocalDate.now();

        // 2. Menu Bar Setup
        setupMenuBar();

        // 3. Main Tabbed Panel
        JTabbedPane tabbedPane = new JTabbedPane();

        // --- Tab 1: Calendar View ---
        JPanel calendarPanel = createCalendarTab();
        tabbedPane.addTab("Calendar", calendarPanel);

        // --- Tab 2: Events List View ---
        JPanel eventsPanel = createEventsListTab(); // Reusing previous logic
        tabbedPane.addTab("Events", eventsPanel);

        // --- Tab 3: Statistics ---
        tabbedPane.addTab("Statistics", new JLabel("Statistics Page (To be implemented)", SwingConstants.CENTER));

        // --- Tab 4: Backup & Restore ---
        tabbedPane.addTab("Backup & Restore", new JLabel("Backup Page (To be implemented)", SwingConstants.CENTER));

        add(tabbedPane);
    }

    // ==========================================
    // Core Logic for Building Calendar Tab (Tab 1)
    // ==========================================
    private JPanel createCalendarTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        // A. Top Control Bar (View Switcher + Navigation)
        JPanel topControlPanel = new JPanel(new BorderLayout());
        topControlPanel.setBackground(new Color(240, 240, 245));
        topControlPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Left: View Switcher (Radio Buttons)
        JPanel viewPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        viewPanel.setOpaque(false);
        viewPanel.add(new JLabel("View: "));
        JRadioButton weekBtn = new JRadioButton("Week");
        JRadioButton monthBtn = new JRadioButton("Month", true); // Default selection
        JRadioButton listBtn = new JRadioButton("List");
        ButtonGroup group = new ButtonGroup();
        group.add(weekBtn);
        group.add(monthBtn);
        group.add(listBtn);
        viewPanel.add(weekBtn);
        viewPanel.add(monthBtn);
        viewPanel.add(listBtn);

        // Center/Right: Month Navigation (e.g., 2026-01)
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        navPanel.setOpaque(false);
        JButton prevBtn = new JButton("←");
        JButton nextBtn = new JButton("→");
        monthLabel = new JLabel(currentMonth.format(DateTimeFormatter.ofPattern("yyyy-MM")));
        monthLabel.setFont(new Font("Arial", Font.BOLD, 18));

        // Navigation Button Events
        prevBtn.addActionListener(e -> {
            currentMonth = currentMonth.minusMonths(1);
            refreshCalendar();
        });
        nextBtn.addActionListener(e -> {
            currentMonth = currentMonth.plusMonths(1);
            refreshCalendar();
        });

        navPanel.add(prevBtn);
        navPanel.add(monthLabel);
        navPanel.add(nextBtn);

        topControlPanel.add(viewPanel, BorderLayout.WEST);
        topControlPanel.add(navPanel, BorderLayout.CENTER);

        // B. Weekday Headers (Sun, Mon, Tue...)
        JPanel headerPanel = new JPanel(new GridLayout(1, 7));
        headerPanel.setBackground(Color.WHITE);
        String[] days = { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };
        for (String d : days) {
            JLabel lbl = new JLabel(d, SwingConstants.CENTER);
            lbl.setFont(new Font("Arial", Font.BOLD, 12));
            lbl.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
            headerPanel.add(lbl);
        }

        // C. Date Grid (Core Component)
        calendarGrid = new JPanel(new GridLayout(0, 7, 5, 5)); // 5px gap
        calendarGrid.setBackground(Color.WHITE);
        calendarGrid.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Assembly
        JPanel centerContainer = new JPanel(new BorderLayout());
        centerContainer.add(headerPanel, BorderLayout.NORTH);
        centerContainer.add(calendarGrid, BorderLayout.CENTER);

        panel.add(topControlPanel, BorderLayout.NORTH);
        panel.add(centerContainer, BorderLayout.CENTER);

        // Initial Display
        refreshCalendar();

        return panel;
    }

    // --- Core Algorithm: Refresh Calendar Grid ---
    private void refreshCalendar() {
        monthLabel.setText(currentMonth.format(DateTimeFormatter.ofPattern("yyyy-MM")));
        calendarGrid.removeAll(); // Clear old cells

        // 1. Calculate the day of the week for the first day of the current month
        LocalDate firstDayOfMonth = currentMonth.atDay(1);
        int dayOfWeekValue = firstDayOfMonth.getDayOfWeek().getValue();
        // Java: Monday=1 ... Sunday=7. Usually, calendars start with Sunday at column
        // 0.
        // Convert: If dayOfWeekValue is 7 (Sunday), startOffset should be 0.
        int startOffset = (dayOfWeekValue % 7);

        // 2. Fill in remaining days from the previous month (Gray)
        LocalDate prevMonthLastDay = currentMonth.minusMonths(1).atEndOfMonth();
        for (int i = startOffset - 1; i >= 0; i--) {
            LocalDate d = prevMonthLastDay.minusDays(i);
            calendarGrid.add(createDayCell(d, false));
        }

        // 3. Fill in days of the current month
        int daysInMonth = currentMonth.lengthOfMonth();
        for (int i = 1; i <= daysInMonth; i++) {
            LocalDate d = currentMonth.atDay(i);
            calendarGrid.add(createDayCell(d, true));
        }

        // 4. Fill in days of the next month (to fill the grid visually)
        int totalSlots = 42; // 6 rows * 7 columns
        int filledSlots = startOffset + daysInMonth;
        int remainingSlots = totalSlots - filledSlots;
        for (int i = 1; i <= remainingSlots; i++) {
            LocalDate d = currentMonth.plusMonths(1).atDay(i);
            calendarGrid.add(createDayCell(d, false));
        }

        calendarGrid.revalidate();
        calendarGrid.repaint();
    }

    // --- Create individual day cells ---
    private JPanel createDayCell(LocalDate date, boolean isCurrentMonth) {
        JPanel cell = new JPanel(new BorderLayout());
        cell.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230))); // Light gray border
        cell.setBackground(Color.WHITE);

        // Number Label
        JLabel dayLabel = new JLabel(String.valueOf(date.getDayOfMonth()));
        dayLabel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Style Logic
        if (!isCurrentMonth) {
            dayLabel.setForeground(Color.LIGHT_GRAY); // Dim color for non-current month
        } else {
            // Check for events
            long eventCount = allEvents.stream()
                    .filter(e -> e.getStartDateTime().toLocalDate().equals(date))
                    .count();

            if (eventCount > 0) {
                JLabel dot = new JLabel("● " + eventCount + " events");
                dot.setForeground(new Color(76, 175, 80)); // Green indicator
                dot.setFont(new Font("Arial", Font.PLAIN, 10));
                cell.add(dot, BorderLayout.SOUTH);
            }

            // Selected State (Simulating blue highlight)
            if (date.equals(selectedDate)) {
                cell.setBackground(new Color(33, 150, 243)); // Blue background
                dayLabel.setForeground(Color.WHITE);
            }
        }

        cell.add(dayLabel, BorderLayout.NORTH);

        // Click Event: Select this date
        cell.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                selectedDate = date;
                // Auto-switch month if a gray date is clicked
                if (!YearMonth.from(date).equals(currentMonth)) {
                    currentMonth = YearMonth.from(date);
                }
                refreshCalendar(); // Redraw to update highlights
            }
        });

        return cell;
    }

    // ==========================================
    // Build Events List View (Tab 2 - Reusing previous logic)
    // ==========================================
    private JPanel createEventsListTab() {
        JPanel panel = new JPanel(new BorderLayout());

        // Simplified table logic for demonstration
        String[] columnNames = { "ID", "Title", "Time", "Description" };
        DefaultTableModel model = new DefaultTableModel(columnNames, 0);
        JTable table = new JTable(model);

        // Populate data
        for (Event e : allEvents) {
            model.addRow(new Object[] {
                    e.getId(), e.getTitle(), e.getStartDateTime(), e.getDescription()
            });
        }

        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();
        JButton refreshBtn = new JButton("Refresh Data");
        refreshBtn.addActionListener(e -> {
            allEvents = FileManager.loadEvents(); // Reload from file
            model.setRowCount(0);
            for (Event ev : allEvents) {
                model.addRow(new Object[] { ev.getId(), ev.getTitle(), ev.getStartDateTime(), ev.getDescription() });
            }
        });
        btnPanel.add(refreshBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    // ==========================================
    // Menu Bar Setup
    // ==========================================
    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.add(new JMenuItem("New Event"));
        fileMenu.add(new JMenuItem("Exit"));

        JMenu viewMenu = new JMenu("View");
        viewMenu.add(new JMenuItem("Day View"));
        viewMenu.add(new JMenuItem("Week View"));

        JMenu toolsMenu = new JMenu("Tools");
        JMenu helpMenu = new JMenu("Help");

        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        menuBar.add(toolsMenu);
        menuBar.add(Box.createHorizontalGlue()); // Push Help to the right
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    public static void main(String[] args) {
        // Set Look and Feel to System Default (Native OS look)
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> new GUIApp().setVisible(true));
    }
}