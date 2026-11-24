import java.util.ArrayList;
import java.util.List;

public class SJFScheduler implements Scheduler {

    private final Config config;
    private final TimelineRecorder recorder;

    public SJFScheduler(Config config, TimelineRecorder recorder) {
        this.config = config;
        this.recorder = recorder;
    }

    @Override
    public String getName() { return "Preemptive SJF"; }

    @Override
    public void run(List<Process> processes) {
        int time = 0;
        int finished = 0;
        Process last = null;                       // who ran most recently

        while (finished < processes.size()) {

            // 1. who is ready right now?
            List<Process> ready = readyList(processes, time);

            // 2. nobody -> CPU idle for 1 unit
            if (ready.isEmpty()) {
                recorder.record(time, "idle", ready, null);
                last = null;          // after idle, the next job starts with no CS
                time++;
                continue;
            }

            // 3. shortest remaining time (ties: keep the current one, else earlier arrival)
            Process chosen = pickShortest(ready, last);

            // 4. switching to a different job costs CS
            int cs = config.getContextSwitchTime();
            if (last != null && last != chosen && cs > 0) {
                recorder.record(time, "CS", ready, null,
                        "Context switch saves " + last.getId() + " and loads " + chosen.getId());
                time += cs;
            }

            // 5. run 1 unit (first start is marked AFTER the CS)
            chosen.markStartIfFirst(time);

            List<Process> waiting = readyList(processes, time);  // includes arrivals during the CS
            waiting.remove(chosen);
            recorder.record(time, chosen.getId(), waiting, null);

            chosen.run(1);
            time++;

            // 6. finished?
            if (chosen.isFinished()) {
                chosen.setFinishTime(time);
                finished++;
            }

            // 7. remember who ran
            last = chosen;
        }
    }

    // Processes that have arrived and are not finished
    private List<Process> readyList(List<Process> processes, int time) {
        List<Process> ready = new ArrayList<>();
        for (Process p : processes) {
            if (p.getArrivalTime() <= time && !p.isFinished()) ready.add(p);
        }
        return ready;
    }

    // Smallest remaining time. Tie: keep 'last' (no pointless CS), else earlier arrival.
    private Process pickShortest(List<Process> ready, Process last) {
        Process best = null;
        for (Process p : ready) {
            if (best == null || p.getRemainingTime() < best.getRemainingTime()) {
                best = p;
            } else if (p.getRemainingTime() == best.getRemainingTime()) {
                if (p == last) best = p;
                else if (best != last && p.getArrivalTime() < best.getArrivalTime()) best = p;
            }
        }
        return best;
    }
}