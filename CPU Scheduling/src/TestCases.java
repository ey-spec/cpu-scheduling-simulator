import java.util.ArrayList;
import java.util.List;

public class TestCases {

    // A test case = its name + default Config (Q, CS). Processes are built fresh on demand.
    public record TestCase(int id, String name, int quantum, int contextSwitch) {
        public List<Process> processes() { return TestCases.build(id); }
        public Config config()  { return new Config(quantum, contextSwitch); }
        // Same processes, different Q / CS (used by ExperimentRunner)
        public Config config(int q, int cs) { return new Config(q, cs); }
    }

    public static final int COUNT = 5;

    public static TestCase get(int id) {
        return switch (id) {
            case 1 -> new TestCase(1, "Given test case 1", 3, 0);
            case 2 -> new TestCase(2, "Given test case 2", 3, 1);
            case 3 -> new TestCase(3, "Aging with long waits", 2, 1);
            case 4 -> new TestCase(4, "Idle gap between arrivals", 3, 1);
            case 5 -> new TestCase(5, "Simultaneous arrivals at quantum end", 3, 0);
            default -> throw new IllegalArgumentException("Unknown test case: " + id);
        };
    }

    public static List<TestCase> all() {
        List<TestCase> list = new ArrayList<>();
        for (int i = 1; i <= COUNT; i++) list.add(get(i));
        return list;
    }

    // Process(id, arrival, burst, priority) - always returns NEW objects
    static List<Process> build(int id) {
        List<Process> l = new ArrayList<>();
        switch (id) {
            case 1 -> {
                l.add(new Process("P1", 0, 4, 2));
                l.add(new Process("P2", 1, 7, 3));
                l.add(new Process("P3", 3, 2, 1));
            }
            case 2 -> {
                l.add(new Process("P1", 0, 5, 4));
                l.add(new Process("P2", 1, 6, 2));
                l.add(new Process("P3", 4, 4, 3));
            }
            case 3 -> {  // several long jobs end up in Tier 2, so aging matters
                l.add(new Process("P1", 0, 8, 5));
                l.add(new Process("P2", 1, 6, 1));
                l.add(new Process("P3", 2, 5, 3));
                l.add(new Process("P4", 3, 3, 2));
            }
            case 4 -> {  // CPU goes idle from t=2 until t=10
                l.add(new Process("P1", 0, 2, 1));
                l.add(new Process("P2", 10, 3, 2));
            }
            case 5 -> {  // P2 and P3 arrive exactly when P1's quantum expires
                l.add(new Process("P1", 0, 5, 2));
                l.add(new Process("P2", 3, 4, 1));
                l.add(new Process("P3", 3, 2, 3));
            }
            default -> throw new IllegalArgumentException("Unknown test case: " + id);
        }
        return l;
    }
}