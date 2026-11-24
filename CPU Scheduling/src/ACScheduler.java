import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ACScheduler implements Scheduler {

    private final Config config;
    private final TimelineRecorder recorder;

    private List<Process> pending;                          // not arrived yet, sorted by arrival
    private final List<Process> tier1 = new ArrayList<>();  // Round Robin queue (FIFO)
    private final List<Process> tier2 = new ArrayList<>();  // non-preemptive priority pool

    public ACScheduler(Config config, TimelineRecorder recorder) {
        this.config = config;
        this.recorder = recorder;
    }

    @Override
    public String getName() { return "Adaptive Container Scheduler (ACS)"; }

    @Override
    public void run(List<Process> processes) {
        pending = new ArrayList<>(processes);
        pending.sort(Comparator.comparingInt(Process::getArrivalTime));   // stable sort
        tier1.clear();
        tier2.clear();

        int time = 0;
        int finished = 0;
        Process last = null;

        while (finished < processes.size()) {

            admitArrivals(time);

            // 1. choose who runs next (not removed from its queue yet, so a CS row can show it)
            Process chosen;
            boolean fromTier1;
            if (!tier1.isEmpty()) {
                chosen = tier1.get(0);
                fromTier1 = true;
            } else if (!tier2.isEmpty()) {
                chosen = pickFromTier2(time);      // aging is applied here, at decision time
                fromTier1 = false;
            } else {
                recorder.record(time, "idle", tier1, tier2);
                last = null;                       // no CS after idle
                time++;
                continue;
            }

            // 2. context switch
            int cs = config.getContextSwitchTime();
            if (last != null && last != chosen && cs > 0) {
                recorder.record(time, "CS", tier1, tier2,
                        "Context switch saves " + last.getId() + " and loads " + chosen.getId());
                time += cs;
                admitArrivals(time);               // arrivals during the CS
            }

            // 3. take it out of its queue and run its block
            if (fromTier1) tier1.remove(chosen); else tier2.remove(chosen);
            chosen.markStartIfFirst(time);

            int units = fromTier1
                    ? Math.min(config.getQuantum(), chosen.getRemainingTime())   // Tier 1: at most Q
                    : chosen.getRemainingTime();                                 // Tier 2: to completion

            for (int i = 0; i < units; i++) {
                recorder.record(time, chosen.getId(), tier1, tier2);
                chosen.run(1);
                time++;
                admitArrivals(time);
            }

            // 4. finished, or used the whole quantum -> demote to Tier 2
            if (chosen.isFinished()) {
                chosen.setFinishTime(time);
                finished++;
            } else {                               // only a Tier 1 process can get here
                chosen.demote(time);
                tier2.add(chosen);
            }

            last = chosen;
        }
    }

    // Moves every process that has arrived into Tier 1 (in arrival order)
    private void admitArrivals(int time) {
        while (!pending.isEmpty() && pending.get(0).getArrivalTime() <= time) {
            tier1.add(pending.remove(0));
        }
    }

    // Applies aging to everyone in Tier 2, then returns the best one
    private Process pickFromTier2(int time) {
        Process best = null;
        for (Process p : tier2) {
            int waited = time - p.getTier2EntryTime();
            p.setCurrentPriority(p.getInitialPriority() - waited / config.getAgingInterval());

            if (best == null
                    || p.getCurrentPriority() < best.getCurrentPriority()
                    || (p.getCurrentPriority() == best.getCurrentPriority()
                    && p.getTier2EntryTime() < best.getTier2EntryTime())) {
                best = p;
            }
        }
        return best;
    }
}