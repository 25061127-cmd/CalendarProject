import java.util.Scanner;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class App {
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("   📅  WIX1002 PRO CALENDAR APP   ");
        System.out.println("==========================================");

        // Auto-backup on startup
        if (FileManager.backupData()) {
            System.out.println("✅ System startup: Data backup successful.");
        }

        boolean running = true;
        while (running) {
            System.out.println("\n--- MAIN MENU ---");
            System.out.println("1. 📅 View All Events");
            System.out.println("2. ➕ Create New Event");
            System.out.println("3. 🔍 Search Events");
            System.out.println("4. 🗑️ Delete Event");
            System.out.println("5. 📊 View Statistics");
            System.out.println("6. 💾 Manual Backup");
            System.out.println("0. 🚪 Exit");
            System.out.print("Select an option: ");

            String choice = scanner.nextLine();
            switch (choice) {
                case "1":
                    viewEvents();
                    break;
                case "2":
                    createNewEvent();
                    break;
                case "3":
                    searchEventFlow();
                    break;
                case "4":
                    deleteEventFlow();
                    break;
                case "5":
                    SchedulerLogic.showStatistics();
                    break;
                case "6":
                    if (FileManager.backupData())
                        System.out.println("✅ Data backed up to event_backup.csv");
                    break;
                case "0":
                    running = false;
                    System.out.println("Goodbye!");
                    break;
                default:
                    System.out.println("Invalid option, please try again.");
            }
        }
    }

    // 1. View Events
    private static void viewEvents() {
        List<Event> events = FileManager.loadEvents();
        if (events.isEmpty()) {
            System.out.println("📭 No events found.");
        } else {
            System.out.println("\n--- EVENT LIST (Sorted by Date) ---");
            for (Event e : events) {
                System.out.println(e);
            }
        }
    }

    // 2. Create Event (With Conflict Detection)
    private static void createNewEvent() {
        System.out.println("\n--- CREATE NEW EVENT ---");
        System.out.print("Enter Title: ");
        String title = scanner.nextLine();
        System.out.print("Enter Description: ");
        String desc = scanner.nextLine();

        LocalDateTime start = readDateTime("Start Time (yyyy-MM-dd HH:mm): ");
        LocalDateTime end = readDateTime("End Time   (yyyy-MM-dd HH:mm): ");

        if (end.isBefore(start)) {
            System.out.println("❌ Error: End time cannot be before start time.");
            return;
        }

        // Check for conflicts
        if (SchedulerLogic.hasConflict(start, end)) {
            System.out.print("⚠️ Conflict detected! Force save anyway? (y/n): ");
            String confirm = scanner.nextLine();
            if (!confirm.equalsIgnoreCase("y")) {
                System.out.println("Creation cancelled.");
                return;
            }
        }

        Event newEvent = new Event(FileManager.getNextId(), title, desc, start, end);
        FileManager.appendEvent(newEvent);
        System.out.println("✅ Event [" + title + "] created successfully!");
    }

    // 3. Search Workflow
    private static void searchEventFlow() {
        System.out.print("Enter keyword to search: ");
        String keyword = scanner.nextLine();
        List<Event> results = SchedulerLogic.searchEvents(keyword);
        if (results.isEmpty()) {
            System.out.println("No matching events found.");
        } else {
            System.out.println("🔍 Found " + results.size() + " matches:");
            for (Event e : results)
                System.out.println(e);
        }
    }

    // 4. Delete Workflow
    private static void deleteEventFlow() {
        viewEvents(); // Show list first so user knows the ID
        System.out.print("Enter Event ID to delete: ");
        try {
            int id = Integer.parseInt(scanner.nextLine());
            if (SchedulerLogic.deleteEvent(id)) {
                System.out.println("✅ Event ID " + id + " has been deleted.");
            } else {
                System.out.println("❌ Event ID " + id + " not found.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a numeric ID.");
        }
    }

    // Helper: Read Date Input safely
    private static LocalDateTime readDateTime(String prompt) {
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return LocalDateTime.parse(input, inputFormatter);
            } catch (DateTimeParseException e) {
                System.out.println("Format Error! Please use: yyyy-MM-dd HH:mm (e.g., 2025-10-20 14:30)");
            }
        }
    }
}