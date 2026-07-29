package starlightcarnival;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

import static starlightcarnival.EntityCrudPanel.*;

public class MaintenanceFrame extends JFrame {

    private final String displayName;
    private final DatabaseManager db = DatabaseManager.getInstance();
    private JPanel mainPanel;
    private CardLayout cardLayout;

    public MaintenanceFrame(String displayName) {
        this.displayName = displayName;
        setTitle("ParkPal - Maintenance");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(960, 680);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(800, 560));

        getContentPane().setBackground(BG);
        setLayout(new BorderLayout(0, 0));
        add(buildTopBar(), BorderLayout.NORTH);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setBackground(BG);
        mainPanel.add(buildHomeScreen(),   "home");
        mainPanel.add(buildNewLogScreen(), "newlog");
        mainPanel.add(buildViewScreen(),   "view");

        add(mainPanel, BorderLayout.CENTER);
        cardLayout.show(mainPanel, "home");
    }

    //  Top Bar

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(SURFACE);
        bar.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER),
            new EmptyBorder(0, 16, 0, 16)
        ));
        bar.setPreferredSize(new Dimension(0, 50));

        JLabel title = new JLabel("Starlight Carnival - Maintenance");
        title.setForeground(new Color(80, 200, 255));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        JLabel userLbl = new JLabel(displayName + " [MAINTENANCE]");
        userLbl.setForeground(FG_MUTED);
        userLbl.setFont(userLbl.getFont().deriveFont(Font.PLAIN, 12f));

        bar.add(title,   BorderLayout.WEST);
        bar.add(userLbl, BorderLayout.EAST);
        return bar;
    }

    //  HOME SCREEN

    private JPanel buildHomeScreen() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(BG);

        JPanel card = darkCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(40, 60, 40, 60));
        card.setPreferredSize(new Dimension(480, 340));

        Color accentBlue = new Color(80, 180, 255);

        JLabel heading = centreLabel("Maintenance Portal", accentBlue, Font.BOLD, 20f);
        JLabel sub     = centreLabel("Welcome, " + displayName, FG_MUTED, Font.ITALIC, 12f);

        JButton btnNew    = bigButton("New Maintenance Record",    accentBlue,          "Log a new maintenance job");
        JButton btnView   = bigButton("View Maintenance Records",  new Color(150, 220, 130), "Browse all maintenance logs");
        JButton btnLogout = bigButton("Logout",                    new Color(160, 50, 50),  "Return to login");

        btnNew.addActionListener(e -> { refreshNewLogScreen(); cardLayout.show(mainPanel, "newlog"); });
        btnView.addActionListener(e -> { loadViewScreen();     cardLayout.show(mainPanel, "view"); });
        btnLogout.addActionListener(e -> { dispose(); new LoginFrame().setVisible(true); });

        card.add(heading);
        card.add(Box.createVerticalStrut(4));
        card.add(sub);
        card.add(Box.createVerticalStrut(28));
        card.add(btnNew);
        card.add(Box.createVerticalStrut(12));
        card.add(btnView);
        card.add(Box.createVerticalStrut(20));
        card.add(new JSeparator() {{ setMaximumSize(new Dimension(Integer.MAX_VALUE, 1)); setForeground(BORDER); }});
        card.add(Box.createVerticalStrut(16));
        card.add(btnLogout);

        p.add(card);
        return p;
    }

    //  NEW LOG SCREEN

    private JComboBox<String> cbRide, cbTechnician;
    private JTextArea tfIssue, tfAction;
    private JTextField tfPartsCost;
    private JLabel lblLogDate, lblLogTime;

    private JPanel buildNewLogScreen() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setBackground(BG);
        wrapper.add(sectionHeader("New Maintenance Record", "Log details of a maintenance job"), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(BG);
        form.setBorder(new EmptyBorder(20, 48, 12, 48));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(7, 8, 7, 8);
        gc.fill = GridBagConstraints.HORIZONTAL;

        cbRide       = styledCombo();
        cbTechnician = styledCombo();
        tfPartsCost  = styledTextField();
        tfPartsCost.setText("0.00");
        lblLogDate   = dimLabel(LocalDate.now().toString());
        lblLogTime   = dimLabel(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        tfIssue      = styledTextArea();
        tfAction     = styledTextArea();

        int r = 0;
        r = formRow(form, gc, r, "Ride *",         cbRide,      "Technician *",  cbTechnician);
        r = formRow(form, gc, r, "Date",            lblLogDate,  "Time",          lblLogTime);
        r = formRow(form, gc, r, "Parts Cost (R)",  tfPartsCost, "",              new JLabel());

        // Issue and action span full width
        gc.gridx=0; gc.gridy=r; gc.gridwidth=1; gc.weightx=0;
        form.add(sideLabel("Issue Found"), gc);
        gc.gridx=0; gc.gridy=r+1; gc.gridwidth=4; gc.weightx=1;
        form.add(new JScrollPane(tfIssue) {{ setBorder(new LineBorder(BORDER,1,true)); setPreferredSize(new Dimension(0,70)); }}, gc);
        r += 2;

        gc.gridx=0; gc.gridy=r; gc.gridwidth=1; gc.weightx=0;
        form.add(sideLabel("Action Taken"), gc);
        gc.gridx=0; gc.gridy=r+1; gc.gridwidth=4; gc.weightx=1;
        form.add(new JScrollPane(tfAction) {{ setBorder(new LineBorder(BORDER,1,true)); setPreferredSize(new Dimension(0,70)); }}, gc);
        r += 2;

        JButton btnSave = actionButton("Save Maintenance Log", new Color(80, 180, 255));
        btnSave.addActionListener(e -> doSaveLog());
        gc.gridx=0; gc.gridy=r; gc.gridwidth=4; gc.insets=new Insets(16,8,6,8);
        form.add(btnSave, gc);

        JScrollPane scrollForm = new JScrollPane(form);
        scrollForm.setBackground(BG);
        scrollForm.getViewport().setBackground(BG);
        scrollForm.setBorder(BorderFactory.createEmptyBorder());

        wrapper.add(scrollForm, BorderLayout.CENTER);
        wrapper.add(backBar(), BorderLayout.SOUTH);
        return wrapper;
    }

    private void refreshNewLogScreen() {
        if (cbRide != null) {
            cbRide.removeAllItems(); cbRide.addItem("");
            try {
                db.query("SELECT r.attraction_id, a.name FROM RIDE r JOIN ATTRACTION a ON a.attraction_id = r.attraction_id ORDER BY a.name")
                  .forEach(row -> cbRide.addItem(row.get("attraction_id") + " - " + row.get("name")));
            } catch (SQLException ignored) {}
        }
        if (cbTechnician != null) {
            cbTechnician.removeAllItems(); cbTechnician.addItem("");
            try {
                db.query("SELECT t.employee_id, e.first_name, e.last_name FROM TECHNICIAN t JOIN EMPLOYEE e ON e.employee_id = t.employee_id ORDER BY e.last_name")
                  .forEach(row -> cbTechnician.addItem(row.get("employee_id") + " - " + row.get("first_name") + " " + row.get("last_name")));
            } catch (SQLException ignored) {}
        }
        if (lblLogDate != null) lblLogDate.setText(LocalDate.now().toString());
        if (lblLogTime != null) lblLogTime.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        if (tfIssue  != null) tfIssue.setText("");
        if (tfAction != null) tfAction.setText("");
        if (tfPartsCost != null) tfPartsCost.setText("0.00");
    }

    private void doSaveLog() {
        String rideItem = (String) cbRide.getSelectedItem();
        String techItem = (String) cbTechnician.getSelectedItem();
        if (rideItem == null || rideItem.isEmpty()) { showError("Select a ride."); return; }
        if (techItem == null || techItem.isEmpty()) { showError("Select a technician."); return; }

        int rideId = Integer.parseInt(rideItem.split(" - ")[0].trim());
        int techId = Integer.parseInt(techItem.split(" - ")[0].trim());
        double cost = 0;
        try { cost = Double.parseDouble(tfPartsCost.getText().trim()); } catch (NumberFormatException ignored) {}

        try {
            db.update(
                "INSERT INTO MAINTENANCE_LOG (ride_id, technician_id, maintenance_date, issue_found, action_taken, parts_cost) VALUES (?, ?, ?, ?, ?, ?)",
                rideId, techId, LocalDate.now().toString(),
                nullIfEmpty(tfIssue.getText()), nullIfEmpty(tfAction.getText()), cost
            );
            JOptionPane.showMessageDialog(this, "Maintenance log saved!", "Saved", JOptionPane.INFORMATION_MESSAGE);
            cardLayout.show(mainPanel, "home");
        } catch (SQLException e) {
            showError("Could not save log: " + e.getMessage());
        }
    }

    //  VIEW SCREEN

    private DefaultTableModel viewTableModel;

    private JPanel buildViewScreen() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setBackground(BG);
        wrapper.add(sectionHeader("Maintenance Records", "All logged maintenance jobs"), BorderLayout.NORTH);

        String[] cols = {"ID", "Ride", "Technician", "Date", "Issue Found", "Action Taken", "Parts Cost"};
        viewTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(viewTableModel) {
            @Override public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                c.setBackground(isRowSelected(row) ? SEL_BG : (row%2==0 ? CARD : ROW_ALT));
                c.setForeground(isRowSelected(row) ? ACCENT : FG);
                return c;
            }
        };
        table.setBackground(CARD);
        table.setForeground(FG);
        table.setGridColor(BORDER);
        table.setRowHeight(28);
        table.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        table.getTableHeader().setBackground(SURFACE);
        table.getTableHeader().setForeground(new Color(80, 200, 255));
        table.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JScrollPane sp = new JScrollPane(table);
        sp.setBackground(BG);
        sp.getViewport().setBackground(BG);
        sp.setBorder(BorderFactory.createEmptyBorder());

        wrapper.add(sp, BorderLayout.CENTER);
        wrapper.add(backBar(), BorderLayout.SOUTH);
        return wrapper;
    }

    private void loadViewScreen() {
        viewTableModel.setRowCount(0);
        try {
            List<Map<String, Object>> rows = db.query(
                "SELECT ml.log_id, a.name AS ride_name, " +
                "CONCAT(e.first_name, ' ', e.last_name) AS technician, " +
                "ml.maintenance_date, ml.issue_found, ml.action_taken, ml.parts_cost " +
                "FROM MAINTENANCE_LOG ml " +
                "LEFT JOIN RIDE r ON r.attraction_id = ml.ride_id " +
                "LEFT JOIN ATTRACTION a ON a.attraction_id = r.attraction_id " +
                "LEFT JOIN TECHNICIAN t ON t.employee_id = ml.technician_id " +
                "LEFT JOIN EMPLOYEE e ON e.employee_id = t.employee_id " +
                "ORDER BY ml.maintenance_date DESC, ml.log_id DESC"
            );
            for (Map<String, Object> row : rows) {
                viewTableModel.addRow(new Object[]{
                    row.get("log_id"), row.get("ride_name"), row.get("technician"),
                    row.get("maintenance_date"), row.get("issue_found"),
                    row.get("action_taken"), "R " + row.get("parts_cost")
                });
            }
        } catch (SQLException e) {
            showError("Could not load records: " + e.getMessage());
        }
    }

    //  Shared UI Helpers

    private JPanel sectionHeader(String title, String subtitle) {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(SURFACE);
        h.setBorder(new CompoundBorder(
            new MatteBorder(0,0,1,0,BORDER), new EmptyBorder(14,20,14,20)));
        JLabel t = new JLabel(title);
        t.setForeground(new Color(80, 200, 255));
        t.setFont(t.getFont().deriveFont(Font.BOLD, 15f));
        JLabel s = new JLabel(subtitle);
        s.setForeground(FG_MUTED);
        s.setFont(s.getFont().deriveFont(Font.ITALIC, 11f));
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBackground(SURFACE);
        left.add(t); left.add(s);
        h.add(left, BorderLayout.WEST);
        return h;
    }

    private JPanel backBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 10));
        bar.setBackground(SURFACE);
        bar.setBorder(new MatteBorder(1,0,0,0,BORDER));
        JButton b = actionButton("Back to Home", FG_MUTED);
        b.addActionListener(e -> cardLayout.show(mainPanel, "home"));
        bar.add(b);
        return bar;
    }

    private JPanel darkCard() {
        return new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 14, 14);
                g2.dispose();
            }
        };
    }

    private JButton bigButton(String label, Color color, String tooltip) {
        JButton b = new JButton(label);
        b.setForeground(color); b.setBackground(CARD); b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(new LineBorder(color,1,true), new EmptyBorder(14,20,14,20)));
        b.setFont(b.getFont().deriveFont(Font.BOLD, 14f));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setAlignmentX(CENTER_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        b.setToolTipText(tooltip);
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setBackground(color.darker().darker()); }
            @Override public void mouseExited(MouseEvent e)  { b.setBackground(CARD); }
        });
        return b;
    }

    private JButton actionButton(String label, Color color) {
        JButton b = new JButton(label);
        b.setForeground(color); b.setBackground(CARD); b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(new LineBorder(color,1,true), new EmptyBorder(8,16,8,16)));
        b.setFont(b.getFont().deriveFont(Font.BOLD, 12f));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setBackground(color.darker().darker()); }
            @Override public void mouseExited(MouseEvent e)  { b.setBackground(CARD); }
        });
        return b;
    }

    private JTextField styledTextField() {
        JTextField tf = new JTextField();
        tf.setBackground(CARD); tf.setForeground(FG); tf.setCaretColor(ACCENT);
        tf.setBorder(BorderFactory.createCompoundBorder(new LineBorder(BORDER,1,true), new EmptyBorder(6,8,6,8)));
        tf.setFont(tf.getFont().deriveFont(Font.PLAIN, 12f));
        return tf;
    }

    private JTextArea styledTextArea() {
        JTextArea ta = new JTextArea(3, 20);
        ta.setBackground(CARD); ta.setForeground(FG); ta.setCaretColor(ACCENT);
        ta.setLineWrap(true); ta.setWrapStyleWord(true);
        ta.setFont(ta.getFont().deriveFont(Font.PLAIN, 12f));
        return ta;
    }

    private JComboBox<String> styledCombo(String... items) {
        JComboBox<String> cb = new JComboBox<>();
        cb.addItem(""); for (String i : items) cb.addItem(i);
        cb.setBackground(CARD); cb.setForeground(FG);
        cb.setBorder(new LineBorder(BORDER, 1, true));
        cb.setFont(cb.getFont().deriveFont(Font.PLAIN, 12f));
        cb.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                super.getListCellRendererComponent(l, v, i, s, f);
                setBackground(s ? SEL_BG : CARD); setForeground(s ? ACCENT : FG);
                return this;
            }
        });
        return cb;
    }

    private JLabel sideLabel(String text) {
        JLabel l = new JLabel(text); l.setForeground(FG_MUTED);
        l.setFont(l.getFont().deriveFont(Font.PLAIN, 11f)); return l;
    }

    private JLabel dimLabel(String text) {
        JLabel l = new JLabel(text); l.setForeground(FG);
        l.setFont(l.getFont().deriveFont(Font.PLAIN, 13f));
        l.setBorder(BorderFactory.createCompoundBorder(new LineBorder(BORDER,1,true), new EmptyBorder(6,8,6,8)));
        l.setOpaque(true); l.setBackground(new Color(20,24,40)); return l;
    }

    private JLabel centreLabel(String text, Color color, int style, float size) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setForeground(color); l.setFont(l.getFont().deriveFont(style, size));
        l.setAlignmentX(CENTER_ALIGNMENT); return l;
    }

    private int formRow(JPanel p, GridBagConstraints gc, int row,
                        String l1, Component c1, String l2, Component c2) {
        gc.gridwidth=1; gc.weightx=0; gc.gridy=row;
        gc.gridx=0; p.add(sideLabel(l1), gc);
        gc.gridx=1; gc.weightx=1; p.add(c1, gc);
        gc.gridx=2; gc.weightx=0; p.add(sideLabel(l2), gc);
        gc.gridx=3; gc.weightx=1; p.add(c2, gc);
        return row + 1;
    }

    private String nullIfEmpty(String s) { return (s==null||s.trim().isEmpty()) ? null : s.trim(); }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
