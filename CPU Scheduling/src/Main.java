import java.util.List;

public class Main {

    public static void main(String[] args) {
        int caseId = 2;                                  // change to 1..5

        // 1. load the test case (fresh processes + its default Q and CS)
        TestCases.TestCase tc = TestCases.get(caseId);
        List<Process> processes = tc.processes();
        Config config = tc.config();

        // 2. create the recorder and the scheduler
        TimelineRecorder recorder = new TimelineRecorder();
        Scheduler scheduler = new ACScheduler(config, recorder);

        System.out.println("Test case " + tc.id() + ": " + tc.name()
                + "  (Q=" + config.getQuantum() + ", CS=" + config.getContextSwitchTime() + ")");
        System.out.println("Scheduler: " + scheduler.getName());
        System.out.println();

        // 3. run it: this fills start/finish times inside the processes
        scheduler.run(processes);

        // 4. print the timeline and the results
        recorder.print();
        System.out.println();
        printResults(processes);
    }

    private static void printResults(List<Process> processes) {
        System.out.printf("%-8s %-8s %-8s %-11s %-8s %-8s%n",
                "Process", "Arrival", "Finish", "Turnaround", "Waiting", "Response");

        for (Process p : processes) {
            System.out.printf("%-8s %-8d %-8d %-11d %-8d %-8d%n",
                    p.getId(),
                    p.getArrivalTime(),
                    p.getFinishTime(),
                    MetricsCalculator.turnaround(p),
                    MetricsCalculator.waiting(p),
                    MetricsCalculator.response(p));
        }

        System.out.println();
        System.out.printf("Average Turnaround Time: %.2f time units%n", MetricsCalculator.averageTurnaround(processes));
        System.out.printf("Average Waiting Time:    %.2f time units%n", MetricsCalculator.averageWaiting(processes));
        System.out.printf("Average Response Time:   %.2f time units%n", MetricsCalculator.averageResponse(processes));
        System.out.printf("CPU Utilization:         %.1f%%%n", MetricsCalculator.cpuUtilization(processes));
    }
}