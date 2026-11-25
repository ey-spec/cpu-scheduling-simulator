import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);

        // 1. ask which scheduler
        System.out.println("Choose scheduler:");
        System.out.println("  1) Preemptive SJF");
        System.out.println("  2) Adaptive Container Scheduler (ACS)");
        int schedulerChoice = readInt(in, "Your choice: ", 1, 2);

        // 2. ask which test case
        System.out.println();
        System.out.println("Choose test case:");
        for (int i = 1; i <= TestCases.COUNT; i++) {
            TestCases.TestCase t = TestCases.get(i);
            System.out.println("  " + i + ") " + t.name() + "  (Q=" + t.quantum() + ", CS=" + t.contextSwitch() + ")");
        }
        int caseId = readInt(in, "Your choice: ", 1, TestCases.COUNT);

        // 3. load the test case (fresh processes + its default Q and CS)
        TestCases.TestCase tc = TestCases.get(caseId);
        List<Process> processes = tc.processes();
        Config config = tc.config();

        // 4. create the recorder and the chosen scheduler
        TimelineRecorder recorder = new TimelineRecorder();
        Scheduler scheduler = (schedulerChoice == 1)
                ? new SJFScheduler(config, recorder)
                : new ACScheduler(config, recorder);

        System.out.println();
        System.out.println("Test case " + tc.id() + ": " + tc.name()
                + "  (Q=" + config.getQuantum() + ", CS=" + config.getContextSwitchTime() + ")");
        System.out.println("Scheduler: " + scheduler.getName());
        System.out.println();

        // 5. run it: this fills start/finish times inside the processes
        scheduler.run(processes);

        // 6. print the timeline and the results
        recorder.print();
        System.out.println();
        printResults(processes);
    }

    // Keeps asking until the user types a whole number between min and max
    private static int readInt(Scanner in, String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            if (!in.hasNextInt()) {
                in.next();                                   // throw away the bad text
                System.out.println("Please enter a number between " + min + " and " + max + ".");
                continue;
            }
            int value = in.nextInt();
            if (value >= min && value <= max) return value;
            System.out.println("Please enter a number between " + min + " and " + max + ".");
        }
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