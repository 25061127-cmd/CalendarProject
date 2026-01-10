import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

//sort event by time
public class Event implements Comparable<Event> {
    private int id;
    private String title;
    private String description;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;

    public static final DateTimeFormatter FILE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    public static final DateTimeFormatter PRINT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public Event(int id, String title, String description, LocalDateTime start, LocalDateTime end) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.startDateTime = start;
        this.endDateTime = end;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    public String toCSV() {
        String safeTitle = title.replace(",", "|");
        String safeDesc = description.replace(",", "|");
        return id + "," + safeTitle + "," + safeDesc + "," +
                startDateTime.format(FILE_FORMATTER) + "," + endDateTime.format(FILE_FORMATTER);
    }

    @Override
    public String toString() {
        return String.format("ID:%-3d | %s -> %s | %-20s | %s",
                id,
                startDateTime.format(PRINT_FORMATTER),
                endDateTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                title,
                description);
    }

    // rank by start time
    @Override
    public int compareTo(Event other) {
        return this.startDateTime.compareTo(other.startDateTime);
    }
}