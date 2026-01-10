import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.time.LocalDateTime;

public class FileManager {
    private static final String FILE_PATH = "event.csv";
    private static final String BACKUP_PATH = "event_backup.csv";

    // 1. Load all events from CSV
    public static List<Event> loadEvents() {
        List<Event> events = new ArrayList<>();
        File file = new File(FILE_PATH);
        if (!file.exists())
            return events;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line = br.readLine(); // Skip header
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty())
                    continue; // Skip empty lines
                String[] data = line.split(",");
                if (data.length >= 5) {
                    try {
                        Event e = new Event(
                                Integer.parseInt(data[0]),
                                data[1],
                                data[2],
                                // Parse using the specific ISO formatter required by PDF
                                LocalDateTime.parse(data[3], Event.FILE_FORMATTER),
                                LocalDateTime.parse(data[4], Event.FILE_FORMATTER));
                        events.add(e);
                    } catch (Exception e) {
                        System.err.println("Skipping corrupted line: " + line);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
        Collections.sort(events); // Sort events by date automatically
        return events;
    }

    // 2. Save all events (Overwrite mode - used for Deletion)
    public static void saveAllEvents(List<Event> events) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_PATH))) {
            bw.write("eventId,title,description,startDateTime,endDateTime"); // Header
            bw.newLine();
            for (Event e : events) {
                bw.write(e.toCSV());
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 3. Append a single event (Used for Creation)
    public static void appendEvent(Event event) {
        boolean newFile = !new File(FILE_PATH).exists();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_PATH, true))) {
            if (newFile) {
                bw.write("eventId,title,description,startDateTime,endDateTime");
                bw.newLine();
            }
            bw.write(event.toCSV());
            bw.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 4. Auto-generate the next ID
    public static int getNextId() {
        int maxId = 0;
        for (Event e : loadEvents()) {
            if (e.getId() > maxId)
                maxId = e.getId();
        }
        return maxId + 1;
    }

    // 5. Backup Functionality (Bonus Feature)
    public static boolean backupData() {
        try {
            Files.copy(Paths.get(FILE_PATH), Paths.get(BACKUP_PATH), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            System.out.println("Backup failed: " + e.getMessage());
            return false;
        }
    }
}
