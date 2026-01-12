import java.io.*;
import java.nio.file.*; // Required for file copy operations
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FileManager {

    // File path constants
    private static final String FILE_PATH = "events.csv"; // Main data file
    private static final String BACKUP_PATH = "events_backup.csv"; // Backup file

    // ==========================================
    // Core Function: Load Events from CSV
    // ==========================================
    public static List<Event> loadEvents() {
        List<Event> events = new ArrayList<>();
        File file = new File(FILE_PATH);

        // If file doesn't exist, return an empty list
        if (!file.exists()) {
            return events;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 5) { // Ensure data integrity
                    int id = Integer.parseInt(parts[0]);
                    String title = parts[1].replace("|", ","); // Restore commas
                    String description = parts[2].replace("|", ",");
                    LocalDateTime start = LocalDateTime.parse(parts[3], Event.FILE_FORMATTER);
                    LocalDateTime end = LocalDateTime.parse(parts[4], Event.FILE_FORMATTER);

                    events.add(new Event(id, title, description, start, end));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error loading events: " + e.getMessage());
        }
        return events;
    }

    // ==========================================
    // Core Function: Save All Events (Overwrite)
    // ==========================================
    public static void saveEvents(List<Event> events) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_PATH))) {
            for (Event event : events) {
                bw.write(event.toCSV());
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ==========================================
    // Helper: Append a Single Event
    // ==========================================
    public static void appendEvent(Event event) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_PATH, true))) {
            bw.write(event.toCSV());
            bw.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ==========================================
    // Helper: Generate Next Available ID
    // ==========================================
    public static int getNextId() {
        int maxId = 0;
        List<Event> events = loadEvents();
        for (Event e : events) {
            if (e.getId() > maxId) {
                maxId = e.getId();
            }
        }
        return maxId + 1;
    }

    // ==========================================
    // 📂 New Feature: Backup Data
    // ==========================================
    public static boolean backupEvents() {
        try {
            Path source = Paths.get(FILE_PATH);
            Path target = Paths.get(BACKUP_PATH);

            // Check if source file exists
            if (!Files.exists(source)) {
                System.out.println("No data file to backup!");
                return false;
            }

            // Perform copy (Replace existing backup if present)
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Backup successful: " + target.toAbsolutePath());
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ==========================================
    // 📂 New Feature: Restore Data
    // ==========================================
    public static boolean restoreEvents() {
        try {
            Path source = Paths.get(BACKUP_PATH);
            Path target = Paths.get(FILE_PATH);

            // Check if backup file exists
            if (!Files.exists(source)) {
                System.out.println("No backup file found!");
                return false;
            }

            // Perform restore (Overwrite current data with backup)
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Restore successful from: " + source.toAbsolutePath());
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
