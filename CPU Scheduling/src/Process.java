public class Process {

    // Which tier the process is in
    public enum Tier { TIER_1, TIER_2 }

    // Fixed input data
    private final String id;
    private final int arrivalTime;
    private final int burstTime;
    private final int initialPriority;   // smaller number = higher priority
    private int currentPriority;
    // Changes during the simulation
    private int remainingTime;
    private Tier tier = Tier.TIER_1;     // every process starts in Tier 1
    private int startTime = -1;          // first time it got the CPU (-1 = not yet)
    private int finishTime = -1;         // -1 = not finished
    private int tier2EntryTime = -1;     // when it was demoted (used for aging)

    public Process(String id, int arrivalTime, int burstTime, int initialPriority) {
        this.id = id;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.initialPriority = initialPriority;
        this.currentPriority = initialPriority;
        this.remainingTime = burstTime;  // nothing executed yet
    }


    // Execute for 'units' time units
    public void run(int units) {
        remainingTime -= units;
    }

    public boolean isFinished() {
        return remainingTime == 0;
    }

    // Called when the process is demoted to Tier 2
    public void demote(int currentTime) {
        this.tier = Tier.TIER_2;
        this.tier2EntryTime = currentTime;
    }

    // Remember only the FIRST time it runs (needed for response time)
    public void markStartIfFirst(int currentTime) {
        if (startTime == -1) startTime = currentTime;
    }

    // Getters / setter
    public String getId()            { return id; }
    public int getArrivalTime()      { return arrivalTime; }
    public int getBurstTime()        { return burstTime; }
    public int getInitialPriority()  { return initialPriority; }
    public int getRemainingTime()    { return remainingTime; }
    public Tier getTier()            { return tier; }
    public int getStartTime()        { return startTime; }
    public int getFinishTime()       { return finishTime; }
    public int getTier2EntryTime()   { return tier2EntryTime; }
    public void setFinishTime(int t) { this.finishTime = t; }
    public int getCurrentPriority()        { return currentPriority; }
    public void setCurrentPriority(int p)  { this.currentPriority = p; }
}
