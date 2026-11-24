import java.util.List;

public final class MetricsCalculator {

    private MetricsCalculator() { }

    // ---------- Per process ----------
    public static int turnaround(Process p) {
        checkFinished(p);
        return p.getFinishTime() - p.getArrivalTime();
    }

    public static int waiting(Process p) {
        return turnaround(p) - p.getBurstTime();
    }

    public static int response(Process p) {
        checkFinished(p);
        return p.getStartTime() - p.getArrivalTime();
    }

    // ---------- Averages ----------
    public static double averageTurnaround(List<Process> processes) {
        int sum = 0;
        for (Process p : processes) sum += turnaround(p);
        return (double) sum / processes.size();
    }

    public static double averageWaiting(List<Process> processes) {
        int sum = 0;
        for (Process p : processes) sum += waiting(p);
        return (double) sum / processes.size();
    }

    public static double averageResponse(List<Process> processes) {
        int sum = 0;
        for (Process p : processes) sum += response(p);
        return (double) sum / processes.size();
    }

    // ---------- CPU utilization (%) ----------
    public static double cpuUtilization(List<Process> processes) {
        int busy = 0;        // time spent doing real work
        int totalTime = 0;   // time when the last process finished
        for (Process p : processes) {
            busy += p.getBurstTime();
            totalTime = Math.max(totalTime, p.getFinishTime());
        }
        return busy * 100.0 / totalTime;
    }

    // ---------- Safety check ----------
    private static void checkFinished(Process p) {
        if (p.getFinishTime() == -1) {
            throw new IllegalStateException(p.getId() + " has not finished yet");
        }
    }
}