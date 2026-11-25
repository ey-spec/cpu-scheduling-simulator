r"""
Reads experiments.csv (made by ExperimentRunner) and saves one graph per
test case and per measure into a folder called "graphs".

Run:   python make_graphs.py                 (csv in the same folder)
       python make_graphs.py path\to\experiments.csv
Needs: pip install matplotlib
"""
import csv
import os
import sys
from collections import defaultdict

import matplotlib
matplotlib.use("Agg")                 # save to files, no window needed
import matplotlib.pyplot as plt

# ---------- settings ----------
CSV_FILE = sys.argv[1] if len(sys.argv) > 1 else "experiments.csv"
OUT_DIR = "graphs"
MEASURES = [
    ("avgWaiting", "waiting", "Average waiting time"),
    ("avgTurnaround", "turnaround", "Average turnaround time"),
]
COLORS = ["#4472C4", "#ED7D31", "#7F7F7F", "#FFC000"]    # one colour per CS line

# ---------- read the csv (ACS rows only, because only ACS uses Q) ----------
# data[testCase][CS][Q] = {"avgWaiting": .., "avgTurnaround": ..}
data = defaultdict(lambda: defaultdict(dict))
with open(CSV_FILE, newline="") as f:
    for row in csv.DictReader(f):
        if row["scheduler"] != "ACS":
            continue
        tc, q, cs = int(row["testCase"]), int(row["Q"]), int(row["CS"])
        data[tc][cs][q] = {m[0]: float(row[m[0]]) for m in MEASURES}

os.makedirs(OUT_DIR, exist_ok=True)

# ---------- one graph per test case and measure ----------
for tc in sorted(data):
    for column, short_name, label in MEASURES:
        fig, ax = plt.subplots(figsize=(8, 4.8))
        all_values = []

        for i, cs in enumerate(sorted(data[tc])):
            qs = sorted(data[tc][cs])
            ys = [data[tc][cs][q][column] for q in qs]
            all_values += ys
            ax.plot(qs, ys, marker="o", linewidth=2, color=COLORS[i % len(COLORS)], label=f"CS = {cs}")

        ax.set_title(f"Test case {tc} - ACS: {label.lower()} vs. quantum", fontsize=13, pad=12)
        ax.set_xlabel("Quantum (Q)")
        ax.set_ylabel(f"{label} (time units)")
        ax.set_xticks(sorted(next(iter(data[tc].values())).keys()))
        ax.set_ylim(bottom=0)
        ax.grid(True, alpha=0.3)
        ax.legend(title="Context switch", loc="best")

        # a flat graph deserves an explanation
        if max(all_values) == min(all_values):
            ax.text(0.5, 0.55, "All values are equal:\nno processes overlap, so Q and CS change nothing",
                    transform=ax.transAxes, ha="center", fontsize=10, color="#555555")

        fig.tight_layout()
        path = os.path.join(OUT_DIR, f"testcase{tc}_{short_name}.png")
        fig.savefig(path, dpi=200)
        plt.close(fig)
        print("saved", path)

print(f"\nDone: {len(data) * len(MEASURES)} graphs in the '{OUT_DIR}' folder.")