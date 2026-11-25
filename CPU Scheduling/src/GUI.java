import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GUI extends JPanel {

    // ---------- colours and fonts ----------
    private static final Color BG = new Color(0xF1F5F9);
    private static final Color CARD = Color.WHITE;
    private static final Color LINE = new Color(0xE2E8F0);
    private static final Color ACCENT = new Color(0x4F46E5);
    private static final Color ACCENT_HOVER = new Color(0x4338CA);
    private static final Color TEXT = new Color(0x0F172A);
    private static final Color MUTED = new Color(0x64748B);
    private static final Color[] PALETTE = {
            new Color(0x6366F1), new Color(0x10B981), new Color(0xF59E0B), new Color(0xEF4444),
            new Color(0x06B6D4), new Color(0xEC4899), new Color(0x84CC16), new Color(0x8B5CF6),
            new Color(0xF97316), new Color(0x14B8A6)
    };

    private static Font font(int style, int size) {
        return new Font("Segoe UI", style, size);
    }

    // ---------- input widgets ----------
    private final JRadioButton sjfRadio = new JRadioButton("Preemptive SJF", true);
    private final JRadioButton acsRadio = new JRadioButton("Adaptive Container Scheduler (ACS)");
    private final JComboBox<String> caseBox = new JComboBox<>();
    private final JSpinner quantumSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 100, 1));
    private final JSpinner csSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 100, 1));
    private final JSpinner agingSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 100, 1));
    private final DefaultTableModel inputModel;
    private final JTable inputTable;
    private boolean loadingCase = false;       // true while we fill the table ourselves

    // ---------- output widgets ----------
    private final JLabel statusLabel = new JLabel(" ");
    private final MetricCard turnaroundCard = new MetricCard("Avg Turnaround", new Color(0x6366F1));
    private final MetricCard waitingCard = new MetricCard("Avg Waiting", new Color(0xF59E0B));
    private final MetricCard responseCard = new MetricCard("Avg Response", new Color(0x10B981));
    private final MetricCard utilizationCard = new MetricCard("CPU Utilization", new Color(0xEC4899));
    private final GanttPanel gantt = new GanttPanel();
    private final DefaultTableModel resultModel;
    private final JTextArea timelineArea = new JTextArea();

    public GUI() {
        setLayout(new BorderLayout());
        setBackground(BG);

        inputModel = new DefaultTableModel(new Object[]{"Process", "Arrival", "Burst", "Priority"}, 0) {
            @Override public Class<?> getColumnClass(int c) { return c == 0 ? String.class : Integer.class; }
        };
        inputTable = new JTable(inputModel);

        resultModel = new DefaultTableModel(
                new Object[]{"Process", "Arrival", "Burst", "Finish", "Turnaround", "Waiting", "Response"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        add(buildHeader(), BorderLayout.NORTH);
        add(buildSidebar(), BorderLayout.WEST);
        add(buildCenter(), BorderLayout.CENTER);

        // when the user edits the table by hand, the test case box switches to "Custom"
        inputModel.addTableModelListener(e -> {
            if (!loadingCase && caseBox.getSelectedIndex() != 0) {
                loadingCase = true;
                caseBox.setSelectedIndex(0);
                loadingCase = false;
            }
        });
        caseBox.addActionListener(e -> {
            if (!loadingCase && caseBox.getSelectedIndex() > 0) loadCase(caseBox.getSelectedIndex());
        });
        sjfRadio.addActionListener(e -> updateSettingsState());
        acsRadio.addActionListener(e -> updateSettingsState());

        caseBox.setSelectedIndex(1);          // start with test case 1 loaded
        updateSettingsState();
    }

    // =====================================================================
    //  Building the screen
    // =====================================================================

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, new Color(0x4338CA), getWidth(), 0, new Color(0x7C3AED)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        header.setBorder(new EmptyBorder(14, 24, 14, 24));

        JLabel title = new JLabel("CPU Scheduling Simulator");
        title.setFont(font(Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        JLabel subtitle = new JLabel("Preemptive SJF  \u2022  Adaptive Container Scheduler (ACS)");
        subtitle.setFont(font(Font.PLAIN, 13));
        subtitle.setForeground(new Color(0xE0E7FF));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(title);
        left.add(Box.createVerticalStrut(2));
        left.add(subtitle);

        statusLabel.setFont(font(Font.BOLD, 13));
        statusLabel.setForeground(Color.WHITE);

        header.add(left, BorderLayout.WEST);
        header.add(statusLabel, BorderLayout.EAST);
        return header;
    }

    private JComponent buildSidebar() {
        JPanel side = new JPanel();
        side.setOpaque(false);
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(new EmptyBorder(16, 16, 16, 8));

        // --- scheduler ---
        ButtonGroup group = new ButtonGroup();
        group.add(sjfRadio);
        group.add(acsRadio);
        for (JRadioButton r : new JRadioButton[]{sjfRadio, acsRadio}) {
            r.setOpaque(false);
            r.setFont(font(Font.PLAIN, 13));
            r.setForeground(TEXT);
            r.setFocusPainted(false);
        }
        JPanel schedulerBody = new JPanel(new GridLayout(2, 1, 0, 2));
        schedulerBody.setOpaque(false);
        schedulerBody.add(sjfRadio);
        schedulerBody.add(acsRadio);
        side.add(titledCard("Scheduler", schedulerBody));
        side.add(Box.createVerticalStrut(12));

        // --- test case ---
        caseBox.setFont(font(Font.PLAIN, 13));
        caseBox.addItem("Custom (edit the table)");
        for (int i = 1; i <= TestCases.COUNT; i++) {
            caseBox.addItem(i + " - " + TestCases.get(i).name());
        }
        side.add(titledCard("Test case", caseBox));
        side.add(Box.createVerticalStrut(12));

        // --- settings ---
        JPanel settings = new JPanel(new GridLayout(3, 2, 8, 8));
        settings.setOpaque(false);
        settings.add(settingLabel("Quantum (Q)"));
        settings.add(quantumSpinner);
        settings.add(settingLabel("Context switch (CS)"));
        settings.add(csSpinner);
        settings.add(settingLabel("Aging interval"));
        settings.add(agingSpinner);
        side.add(titledCard("Settings", settings));
        side.add(Box.createVerticalStrut(12));

        // --- processes ---
        styleTable(inputTable);
        inputTable.setRowHeight(26);
        JScrollPane tableScroll = new JScrollPane(inputTable);
        tableScroll.setColumnHeaderView(inputTable.getTableHeader());
        tableScroll.setBorder(new LineBorder(LINE));
        tableScroll.getViewport().setBackground(CARD);
        tableScroll.setPreferredSize(new Dimension(280, 170));

        FlatButton add = new FlatButton("+ Add", LINE, TEXT, new Color(0xCBD5E1));
        FlatButton remove = new FlatButton("Remove", LINE, TEXT, new Color(0xCBD5E1));
        FlatButton clear = new FlatButton("Clear", LINE, TEXT, new Color(0xCBD5E1));
        add.addActionListener(e -> addProcessRow());
        remove.addActionListener(e -> removeSelectedRows());
        clear.addActionListener(e -> {
            stopEditing();
            inputModel.setRowCount(0);
        });
        JPanel buttons = new JPanel(new GridLayout(1, 3, 8, 0));
        buttons.setOpaque(false);
        buttons.add(add);
        buttons.add(remove);
        buttons.add(clear);

        JPanel processBody = new JPanel(new BorderLayout(0, 8));
        processBody.setOpaque(false);
        processBody.add(tableScroll, BorderLayout.CENTER);
        processBody.add(buttons, BorderLayout.SOUTH);
        side.add(titledCard("Processes", processBody));
        side.add(Box.createVerticalStrut(14));

        // --- run ---
        FlatButton run = new FlatButton("Run simulation", ACCENT, Color.WHITE, ACCENT_HOVER);
        run.setFont(font(Font.BOLD, 15));
        run.setAlignmentX(Component.LEFT_ALIGNMENT);
        run.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        run.addActionListener(e -> runSimulation());
        side.add(run);

        JScrollPane scroll = new JScrollPane(side, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setPreferredSize(new Dimension(360, 0));
        return scroll;
    }

    private JComponent buildCenter() {
        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(16, 8, 16, 16));

        // four metric cards on top
        JPanel cards = new JPanel(new GridLayout(1, 4, 12, 0));
        cards.setOpaque(false);
        cards.add(turnaroundCard);
        cards.add(waitingCard);
        cards.add(responseCard);
        cards.add(utilizationCard);
        center.add(cards, BorderLayout.NORTH);

        // tab 1: Gantt chart + per-process results
        JScrollPane ganttScroll = new JScrollPane(gantt, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        ganttScroll.setBorder(null);
        ganttScroll.getViewport().setBackground(CARD);
        JComponent ganttCard = titledCard("Gantt chart", ganttScroll);

        JTable resultTable = new JTable(resultModel);
        styleTable(resultTable);
        resultTable.setRowHeight(30);
        JScrollPane resultScroll = new JScrollPane(resultTable);
        resultScroll.setColumnHeaderView(resultTable.getTableHeader());
        resultScroll.setBorder(new LineBorder(LINE));
        resultScroll.getViewport().setBackground(CARD);
        JComponent resultCard = titledCard("Results per process", resultScroll);

        JPanel overview = new JPanel(new BorderLayout(0, 12));
        overview.setOpaque(false);
        overview.add(ganttCard, BorderLayout.NORTH);
        overview.add(resultCard, BorderLayout.CENTER);

        // tab 2: the text timeline
        timelineArea.setEditable(false);
        timelineArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        timelineArea.setForeground(TEXT);
        timelineArea.setMargin(new Insets(8, 10, 8, 10));
        JScrollPane timelineScroll = new JScrollPane(timelineArea);
        timelineScroll.setBorder(new LineBorder(LINE));
        JComponent timelineCard = titledCard("Time-step log (queues at every step)", timelineScroll);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(font(Font.BOLD, 13));
        tabs.addTab("Overview", overview);
        tabs.addTab("Timeline log", timelineCard);
        tabs.setOpaque(false);
        center.add(tabs, BorderLayout.CENTER);
        return center;
    }

    // A white rounded card with a small title and some content
    private static JComponent titledCard(String title, JComponent content) {
        JPanel card = new JPanel(new BorderLayout(0, 8)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.setColor(LINE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(12, 14, 14, 14));
        JLabel label = new JLabel(title);
        label.setFont(font(Font.BOLD, 13));
        label.setForeground(TEXT);
        card.add(label, BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));
        return card;
    }

    private static JLabel settingLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(font(Font.PLAIN, 13));
        l.setForeground(MUTED);
        return l;
    }

    // Common look for both tables
    private static void styleTable(JTable table) {
        table.setFont(font(Font.PLAIN, 13));
        table.setForeground(TEXT);
        table.setGridColor(LINE);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);
        table.setSelectionBackground(new Color(0xE0E7FF));
        table.setSelectionForeground(TEXT);

        DefaultTableCellRenderer cell = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                                     boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, false, row, col);
                setHorizontalAlignment(CENTER);
                setBorder(new EmptyBorder(0, 6, 0, 6));
                setBackground(sel ? t.getSelectionBackground() : (row % 2 == 0 ? CARD : new Color(0xF8FAFC)));
                setForeground(TEXT);
                return this;
            }
        };
        table.setDefaultRenderer(Object.class, cell);
        table.setDefaultRenderer(String.class, cell);
        table.setDefaultRenderer(Integer.class, cell);

        DefaultTableCellRenderer head = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                                     boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, v, false, false, row, col);
                setHorizontalAlignment(CENTER);
                setFont(font(Font.BOLD, 12));
                setForeground(MUTED);
                setBackground(new Color(0xF1F5F9));
                setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, LINE));
                setOpaque(true);
                return this;
            }
        };
        table.getTableHeader().setDefaultRenderer(head);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(0, 30));
    }

    // =====================================================================
    //  Actions
    // =====================================================================

    // Q and the aging interval only matter for ACS
    private void updateSettingsState() {
        boolean acs = acsRadio.isSelected();
        quantumSpinner.setEnabled(acs);
        agingSpinner.setEnabled(acs);
    }

    private void loadCase(int id) {
        TestCases.TestCase tc = TestCases.get(id);
        stopEditing();
        loadingCase = true;
        inputModel.setRowCount(0);
        for (Process p : tc.processes()) {
            inputModel.addRow(new Object[]{p.getId(), p.getArrivalTime(), p.getBurstTime(), p.getInitialPriority()});
        }
        quantumSpinner.setValue(tc.quantum());
        csSpinner.setValue(tc.contextSwitch());
        loadingCase = false;
    }

    private void addProcessRow() {
        stopEditing();
        Set<String> used = new HashSet<>();
        for (int r = 0; r < inputModel.getRowCount(); r++) used.add(String.valueOf(inputModel.getValueAt(r, 0)));
        int n = inputModel.getRowCount() + 1;
        while (used.contains("P" + n)) n++;
        inputModel.addRow(new Object[]{"P" + n, 0, 1, 1});
        int last = inputModel.getRowCount() - 1;
        inputTable.setRowSelectionInterval(last, last);
        inputTable.scrollRectToVisible(inputTable.getCellRect(last, 0, true));
    }

    private void removeSelectedRows() {
        stopEditing();
        int[] rows = inputTable.getSelectedRows();
        for (int i = rows.length - 1; i >= 0; i--) inputModel.removeRow(rows[i]);
    }

    private void stopEditing() {
        if (inputTable.isEditing()) inputTable.getCellEditor().stopCellEditing();
    }

    // Reads the table and builds fresh Process objects (throws a readable error if something is wrong)
    private List<Process> readProcesses() {
        stopEditing();
        List<Process> list = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (int r = 0; r < inputModel.getRowCount(); r++) {
            String id = String.valueOf(inputModel.getValueAt(r, 0)).trim();
            if (id.isEmpty() || id.equals("null")) throw new IllegalArgumentException("Row " + (r + 1) + ": the process name is empty.");
            if (id.equalsIgnoreCase("CS") || id.equalsIgnoreCase("idle"))
                throw new IllegalArgumentException("\"" + id + "\" is reserved. Please use another process name.");
            if (!ids.add(id)) throw new IllegalArgumentException("Two processes are named \"" + id + "\".");

            int arrival = number(inputModel.getValueAt(r, 1), r, "Arrival");
            int burst = number(inputModel.getValueAt(r, 2), r, "Burst");
            int priority = number(inputModel.getValueAt(r, 3), r, "Priority");
            if (arrival < 0) throw new IllegalArgumentException(id + ": arrival time cannot be negative.");
            if (burst <= 0) throw new IllegalArgumentException(id + ": burst time must be at least 1.");
            list.add(new Process(id, arrival, burst, priority));
        }
        if (list.isEmpty()) throw new IllegalArgumentException("Add at least one process.");
        return list;
    }

    private static int number(Object value, int row, String column) {
        if (!(value instanceof Number)) {
            throw new IllegalArgumentException("Row " + (row + 1) + ": " + column + " must be a whole number.");
        }
        return ((Number) value).intValue();
    }

    void runSimulation() {
        try {
            List<Process> processes = readProcesses();
            Config config = new Config(
                    ((Number) quantumSpinner.getValue()).intValue(),
                    ((Number) csSpinner.getValue()).intValue(),
                    ((Number) agingSpinner.getValue()).intValue());

            TimelineRecorder recorder = new TimelineRecorder();
            Scheduler scheduler = sjfRadio.isSelected()
                    ? new SJFScheduler(config, recorder)
                    : new ACScheduler(config, recorder);
            scheduler.run(processes);

            showResults(scheduler, config, processes, recorder);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Please check your input", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void showResults(Scheduler scheduler, Config config, List<Process> processes, TimelineRecorder recorder) {
        // header status
        String settings = sjfRadio.isSelected()
                ? "CS=" + config.getContextSwitchTime()
                : "Q=" + config.getQuantum() + "  CS=" + config.getContextSwitchTime() + "  Aging=" + config.getAgingInterval();
        statusLabel.setText(scheduler.getName() + "   |   " + settings);

        // metric cards
        turnaroundCard.setValue(String.format("%.2f", MetricsCalculator.averageTurnaround(processes)), "time units");
        waitingCard.setValue(String.format("%.2f", MetricsCalculator.averageWaiting(processes)), "time units");
        responseCard.setValue(String.format("%.2f", MetricsCalculator.averageResponse(processes)), "time units");
        utilizationCard.setValue(String.format("%.1f%%", MetricsCalculator.cpuUtilization(processes)), "of the time busy");

        // per-process table
        resultModel.setRowCount(0);
        int end = 0;
        for (Process p : processes) {
            resultModel.addRow(new Object[]{
                    p.getId(), p.getArrivalTime(), p.getBurstTime(), p.getFinishTime(),
                    MetricsCalculator.turnaround(p), MetricsCalculator.waiting(p), MetricsCalculator.response(p)});
            end = Math.max(end, p.getFinishTime());
        }

        // gantt chart: turn the recorder rows into blocks
        Map<String, Color> colors = new LinkedHashMap<>();
        for (int i = 0; i < processes.size(); i++) {
            colors.put(processes.get(i).getId(), PALETTE[i % PALETTE.length]);
        }
        List<Segment> segments = new ArrayList<>();
        List<String[]> rows = recorder.getRunningRows();
        for (int i = 0; i < rows.size(); i++) {
            int start = Integer.parseInt(rows.get(i)[0]);
            String label = rows.get(i)[1];
            int stop = (i + 1 < rows.size()) ? Integer.parseInt(rows.get(i + 1)[0]) : end;
            Segment last = segments.isEmpty() ? null : segments.get(segments.size() - 1);
            if (last != null && last.label.equals(label) && last.end == start) {
                last.end = stop;                       // same block continues
            } else {
                segments.add(new Segment(label, start, stop));
            }
        }
        gantt.setData(segments, colors, end);

        // text log
        timelineArea.setText(recorder.toText().replace("\r\n", "\n"));
        timelineArea.setCaretPosition(0);
    }

    // =====================================================================
    //  Small helper classes
    // =====================================================================

    private static class Segment {
        final String label;     // "P1", "CS" or "idle"
        final int start;
        int end;

        Segment(String label, int start, int end) {
            this.label = label;
            this.start = start;
            this.end = end;
        }
    }

    // Draws the Gantt chart
    private static class GanttPanel extends JPanel {
        private static final int UNIT = 30;      // pixels per time unit
        private static final int LEFT = 24;
        private List<Segment> segments = new ArrayList<>();
        private Map<String, Color> colors = new LinkedHashMap<>();
        private int endTime = 0;

        GanttPanel() {
            setOpaque(true);
            setBackground(CARD);
        }

        void setData(List<Segment> segments, Map<String, Color> colors, int endTime) {
            this.segments = segments;
            this.colors = colors;
            this.endTime = endTime;
            revalidate();
            repaint();
        }

        @Override public Dimension getPreferredSize() {
            return new Dimension(Math.max(560, LEFT * 2 + endTime * UNIT), 175);
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            if (segments.isEmpty()) {
                g2.setColor(MUTED);
                g2.setFont(font(Font.PLAIN, 14));
                g2.drawString("Pick a scheduler and press \"Run simulation\" to see the chart.", LEFT, 70);
                g2.dispose();
                return;
            }

            int barY = 24, barH = 56;
            boolean hasCs = false, hasIdle = false;

            for (Segment s : segments) {
                int x = LEFT + s.start * UNIT;
                int w = (s.end - s.start) * UNIT;
                if (s.label.equals("CS")) {
                    hasCs = true;
                    drawContextSwitch(g2, x, barY, w, barH);
                } else if (s.label.equals("idle")) {
                    hasIdle = true;
                    drawIdle(g2, x, barY, w, barH);
                } else {
                    g2.setColor(colors.getOrDefault(s.label, ACCENT));
                    g2.fillRoundRect(x + 1, barY, w - 2, barH, 10, 10);
                    drawCentered(g2, s.label, Color.WHITE, font(Font.BOLD, 15), x, barY, w, barH);
                }
            }

            // time axis
            int axisY = barY + barH + 12;
            g2.setColor(new Color(0x94A3B8));
            g2.drawLine(LEFT, axisY, LEFT + endTime * UNIT, axisY);
            g2.setFont(font(Font.PLAIN, 12));
            FontMetrics fm = g2.getFontMetrics();
            int lastRight = -100;
            List<Integer> ticks = new ArrayList<>();
            for (Segment s : segments) ticks.add(s.start);
            ticks.add(endTime);
            for (int t : ticks) {
                int x = LEFT + t * UNIT;
                g2.setColor(new Color(0x94A3B8));
                g2.drawLine(x, axisY, x, axisY + 5);
                String txt = String.valueOf(t);
                int tw = fm.stringWidth(txt);
                if (x - tw / 2 > lastRight + 6) {
                    g2.setColor(MUTED);
                    g2.drawString(txt, x - tw / 2, axisY + 20);
                    lastRight = x + tw / 2;
                }
            }

            // legend
            int ly = axisY + 42, lx = LEFT;
            for (Map.Entry<String, Color> e : colors.entrySet()) {
                g2.setColor(e.getValue());
                g2.fillRoundRect(lx, ly, 14, 14, 4, 4);
                g2.setColor(TEXT);
                g2.drawString(e.getKey(), lx + 20, ly + 12);
                lx += 20 + fm.stringWidth(e.getKey()) + 18;
            }
            if (hasCs) {
                drawContextSwitch(g2, lx, ly, 14, 14);
                g2.setColor(TEXT);
                g2.drawString("Context switch", lx + 20, ly + 12);
                lx += 20 + fm.stringWidth("Context switch") + 18;
            }
            if (hasIdle) {
                drawIdle(g2, lx, ly, 14, 14);
                g2.setColor(TEXT);
                g2.drawString("Idle", lx + 20, ly + 12);
            }
            g2.dispose();
        }

        private void drawContextSwitch(Graphics2D g2, int x, int y, int w, int h) {
            Shape oldClip = g2.getClip();
            g2.clipRect(x + 1, y, Math.max(1, w - 2), h);
            g2.setColor(new Color(0xE2E8F0));
            g2.fillRect(x + 1, y, w - 2, h);
            g2.setColor(new Color(0x94A3B8));
            for (int i = -h; i < w; i += 7) g2.drawLine(x + i, y + h, x + i + h, y);
            g2.setClip(oldClip);
            g2.setColor(new Color(0x94A3B8));
            g2.drawRect(x + 1, y, Math.max(1, w - 3), h - 1);
        }

        private void drawIdle(Graphics2D g2, int x, int y, int w, int h) {
            g2.setColor(new Color(0xF8FAFC));
            g2.fillRect(x + 1, y, w - 2, h);
            Stroke old = g2.getStroke();
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{4f, 3f}, 0f));
            g2.setColor(new Color(0x94A3B8));
            g2.drawRect(x + 1, y, Math.max(1, w - 3), h - 1);
            g2.setStroke(old);
            if (h > 30) drawCentered(g2, "idle", MUTED, font(Font.PLAIN, 12), x, y, w, h);
        }

        // Draws text in the middle of a rectangle, only if it fits
        private void drawCentered(Graphics2D g2, String text, Color color, Font f, int x, int y, int w, int h) {
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(text);
            if (tw + 6 > w) return;
            g2.setColor(color);
            g2.drawString(text, x + (w - tw) / 2, y + (h + fm.getAscent() - fm.getDescent()) / 2);
        }
    }

    // A small statistic card (title, big number, unit)
    private static class MetricCard extends JPanel {
        private final Color accent;
        private final JLabel value = new JLabel("-");
        private final JLabel unit = new JLabel(" ");

        MetricCard(String title, Color accent) {
            this.accent = accent;
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(new EmptyBorder(12, 20, 12, 12));

            JLabel t = new JLabel(title);
            t.setFont(font(Font.BOLD, 12));
            t.setForeground(MUTED);
            value.setFont(font(Font.BOLD, 28));
            value.setForeground(TEXT);
            unit.setFont(font(Font.PLAIN, 12));
            unit.setForeground(MUTED);

            add(t);
            add(Box.createVerticalStrut(2));
            add(value);
            add(unit);
        }

        void setValue(String text, String unitText) {
            value.setText(text);
            unit.setText(unitText);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(CARD);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.setColor(LINE);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.setColor(accent);
            g2.fillRoundRect(8, 14, 4, getHeight() - 28, 4, 4);       // coloured stripe on the left
            g2.dispose();
        }
    }

    // A button we paint ourselves so the colours look the same on every computer
    private static class FlatButton extends JButton {
        private final Color base;
        private final Color hover;
        private boolean over = false;

        FlatButton(String text, Color base, Color textColor, Color hover) {
            super(text);
            this.base = base;
            this.hover = hover;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setForeground(textColor);
            setFont(font(Font.BOLD, 13));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(8, 12, 8, 12));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { over = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { over = false; repaint(); }
            });
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getModel().isPressed() ? hover.darker() : (over ? hover : base));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // =====================================================================
    //  Start the window
    // =====================================================================

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // keep the default look if the system one is not available
        }
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("CPU Scheduling Simulator");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(new GUI());
            frame.setSize(1320, 840);
            frame.setMinimumSize(new Dimension(1100, 700));
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}