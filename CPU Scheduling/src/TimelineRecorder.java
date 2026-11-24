import java.util.ArrayList;
import java.util.List;

public class TimelineRecorder {

    // One row of the timeline
    private static class Entry {
        final int time;
        final String running;   // "P1", "CS" (context switch), or "idle"
        final String tier1;
        final String tier2;
        final String note;      // optional

        Entry(int time, String running, String tier1, String tier2, String note) {
            this.time = time;
            this.running = running;
            this.tier1 = tier1;
            this.tier2 = tier2;
            this.note = note;
        }
    }

    private final List<Entry> entries = new ArrayList<>();

    // Saves a snapshot. Pass tier2 = null if the scheduler has only one queue.
    public void record(int time, String running,
                       List<Process> tier1, List<Process> tier2, String note) {
        entries.add(new Entry(time, running, describe(tier1), describe(tier2), note));
    }

    // Same, without a note
    public void record(int time, String running,
                       List<Process> tier1, List<Process> tier2) {
        record(time, running, tier1, tier2, "");
    }

    // Prints the whole timeline
    public void print() {
        System.out.printf("%-6s %-8s %s%n", "Time", "Running", "Queues");
        for (Entry e : entries) {
            String label1 = (e.tier2 == null) ? "Ready queue: " : "Tier 1: ";
            System.out.printf("%-6d %-8s %s%s%n", e.time, e.running, label1, e.tier1);
            if (e.tier2 != null) {
                System.out.printf("%-6s %-8s Tier 2: %s%n", "", "", e.tier2);
            }
            if (!e.note.isEmpty()) {
                System.out.printf("%-6s %-8s Note: %s%n", "", "", e.note);
            }
        }
    }

    // Empties the log so the same recorder can be reused for another run
    public void clear() {
        entries.clear();
    }

    // Turns a list of processes into text like: [P1 (remaining: 4, Priority: 2), ...]
    private static String describe(List<Process> list) {
        if (list == null) return null;
        if (list.isEmpty()) return "[Empty]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            Process p = list.get(i);
            if (i > 0) sb.append(", ");
            sb.append(p.getId())
                    .append(" (remaining: ").append(p.getRemainingTime())
                    .append(", Priority: ").append(p.getCurrentPriority())
                    .append(")");
        }
        return sb.append("]").toString();
    }
}