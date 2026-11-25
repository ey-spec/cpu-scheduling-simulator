import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;

public class ExperimentRunner {

    // Values to try (change them freely)
    private static final int[] QUANTA = {1, 2, 3, 4, 5, 6, 8};
    private static final int[] SWITCH_TIMES = {0, 1, 2, 3};

    public static void main(String[] args) {
        StringBuilder csv = new StringBuilder();    // for the file (comma separated)
        StringBuilder table = new StringBuilder();  // for the console (aligned columns)

        csv.append("testCase,scheduler,Q,CS,avgWaiting,avgTurnaround,avgResponse,cpuUtilization\n");
        table.append(String.format("%-9s %-10s %-3s %-3s %-12s %-14s %-12s %s%n",
                "testCase", "scheduler", "Q", "CS", "avgWaiting", "avgTurnaround", "avgResponse", "cpuUtilization"));

        for (TestCases.TestCase tc : TestCases.all()) {

            // SJF does not use Q, so run it once per CS value
            for (int cs : SWITCH_TIMES) {
                List<Process> procs = tc.processes();                    // fresh processes
                Config config = tc.config(1, cs);                        // Q is ignored by SJF
                new SJFScheduler(config, new TimelineRecorder()).run(procs);
                addRow(csv, table, tc.id(), "SJF", "-", cs, procs);
            }

            // ACS uses both Q and CS
            for (int q : QUANTA) {
                for (int cs : SWITCH_TIMES) {
                    List<Process> procs = tc.processes();                // fresh processes
                    Config config = tc.config(q, cs);
                    new ACScheduler(config, new TimelineRecorder()).run(procs);
                    addRow(csv, table, tc.id(), "ACS", String.valueOf(q), cs, procs);
                }
            }
        }

        System.out.print(table);

        try (PrintWriter w = new PrintWriter("experiments.csv")) {
            w.print(csv);
            System.out.println("\nSaved to experiments.csv");
        } catch (IOException e) {
            System.out.println("\nCould not save experiments.csv: " + e.getMessage());
        }
    }

    // Adds one result line to the CSV text and one aligned line to the console table
    private static void addRow(StringBuilder csv, StringBuilder table,
                               int testCase, String scheduler, String q, int cs, List<Process> procs) {
        double waiting = MetricsCalculator.averageWaiting(procs);
        double turnaround = MetricsCalculator.averageTurnaround(procs);
        double response = MetricsCalculator.averageResponse(procs);
        double utilization = MetricsCalculator.cpuUtilization(procs);

        csv.append(String.format(Locale.US, "%d,%s,%s,%d,%.2f,%.2f,%.2f,%.1f%n",
                testCase, scheduler, q, cs, waiting, turnaround, response, utilization));

        table.append(String.format(Locale.US, "%-9d %-10s %-3s %-3d %-12.2f %-14.2f %-12.2f %.1f%n",
                testCase, scheduler, q, cs, waiting, turnaround, response, utilization));
    }
}