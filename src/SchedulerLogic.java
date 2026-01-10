import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class SchedulerLogic {

    // Feature: Conflict Detection
    // Returns true if the new time slot overlaps with an existing event
    public static boolean hasConflict(LocalDateTime start, LocalDateTime end) {
        List<Event> events = FileManager.loadEvents();
        for (Event e : events) {
            // Logic: (StartA < EndB) and (EndA > StartB) means overlap
            if (start.isBefore(e.getEndDateTime()) && end.isAfter(e.getStartDateTime())) {
                System.out.println("⚠️  CONFLICT WARNING: Overlaps with event [" + e.getTitle() + "]");
                return true;
            }
        }
        return false;
    }

    // Feature: Search by Keyword (Title or Description)
    public static List<Event> searchEvents(String keyword) {
        List<Event> allEvents = FileManager.loadEvents();
        String lowerKeyword = keyword.toLowerCase();

        return allEvents.stream()
                .filter(e -> e.getTitle().toLowerCase().contains(lowerKeyword) ||
                        e.getDescription().toLowerCase().contains(lowerKeyword))
                .collect(Collectors.toList());
    }

    // Feature: Delete Event by ID
    public static boolean deleteEvent(int id) {
        List<Event> events = FileManager.loadEvents();
        boolean removed = events.removeIf(e -> e.getId() == id);
        if (removed) {
            FileManager.saveAllEvents(events); // Rewrite the file
        }
        return removed;
    }

    // Feature: Statistics Dashboard
    public static void showStatistics() {
        List<Event> events = FileManager.loadEvents();
        if (events.isEmpty()) {
            System.out.println("No data available for statistics.");
            return;
        }
        long totalMinutes = 0;
        LocalDateTime now = LocalDateTime.now();
        int futureEvents = 0;

        for (Event e : events) {
            totalMinutes += Duration.between(e.getStartDateTime(), e.getEndDateTime()).toMinutes();
            if (e.getStartDateTime().isAfter(now))
                futureEvents++;
        }

        System.out.println("\n📊 === YOUR TIME ANALYSIS ===");
        System.out.println("Total Events: " + events.size());
        System.out.println("Upcoming Events: " + futureEvents);
        System.out.println("Total Scheduled Time: " + totalMinutes / 60 + " Hours " + totalMinutes % 60 + " Minutes");
        System.out.println("=============================");
    }
}