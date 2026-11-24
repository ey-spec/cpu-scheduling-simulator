import java.util.List;

public interface Scheduler {

    // Name shown in the output, e.g. "Preemptive SJF".
    String getName();

    // Simulates the schedule. Fills in start and finish times on the processes
    void run(List<Process> processes);
}