public class Config {

    private final int quantum;            // Q: max run time per turn in Tier 1
    private final int contextSwitchTime;  // CS: time wasted per switch
    private final int agingInterval;      // every this many units of waiting, priority improves by 1

    public Config(int quantum, int contextSwitchTime, int agingInterval) {
        if (quantum <= 0)           throw new IllegalArgumentException("Quantum must be > 0");
        if (contextSwitchTime < 0)  throw new IllegalArgumentException("CS must be >= 0");
        if (agingInterval <= 0)     throw new IllegalArgumentException("Aging interval must be > 0");
        this.quantum = quantum;
        this.contextSwitchTime = contextSwitchTime;
        this.agingInterval = agingInterval;
    }

    // Short version: aging interval is 4 by default
    public Config(int quantum, int contextSwitchTime) {
        this(quantum, contextSwitchTime, 4);
    }

    public int getQuantum()           { return quantum; }
    public int getContextSwitchTime() { return contextSwitchTime; }
    public int getAgingInterval()     { return agingInterval; }
}