package starlightcarnival;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

import static starlightcarnival.EntityCrudPanel.*;

public class ReportsPanel extends JPanel {

    private final DatabaseManager db = DatabaseManager.getInstance();
    private JPanel reportsContainer;
    private JLabel statusLabel;

    public ReportsPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(BG);
        add(buildHeader(), BorderLayout.NORTH);

        reportsContainer = new JPanel();
        reportsContainer.setLayout(new BoxLayout(reportsContainer, BoxLayout.Y_AXIS));
        reportsContainer.setBackground(BG);
        reportsContainer.setBorder(new EmptyBorder(8, 0, 16, 0));

        JScrollPane sp = new JScrollPane(reportsContainer);
        sp.setBackground(BG);
        sp.getViewport().setBackground(BG);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getVerticalScrollBar().setUnitIncrement(16);
        add(sp, BorderLayout.CENTER);

        loadAll();
    }

    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(SURFACE);
        h.setBorder(new CompoundBorder(new MatteBorder(0,0,1,0,BORDER), new EmptyBorder(12,16,12,16)));

        JLabel title = new JLabel("Park Reports");
        title.setForeground(ACCENT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setBackground(SURFACE);

        statusLabel = new JLabel("");
        statusLabel.setForeground(FG_MUTED);
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.ITALIC, 11f));

        JButton refresh = new JButton("Refresh All");
        refresh.setBackground(CARD); refresh.setForeground(ACCENT);
        refresh.setFocusPainted(false);
        refresh.setBorder(BorderFactory.createCompoundBorder(new LineBorder(ACCENT,1,true), new EmptyBorder(5,14,5,14)));
        refresh.setFont(refresh.getFont().deriveFont(Font.BOLD, 12f));
        refresh.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refresh.addActionListener(e -> { reportsContainer.removeAll(); loadAll(); });

        right.add(statusLabel); right.add(refresh);
        h.add(title, BorderLayout.WEST);
        h.add(right, BorderLayout.EAST);
        return h;
    }

    private void loadAll() {
        if (!db.isConnected()) { statusLabel.setText("No database connection."); return; }
        statusLabel.setText("Loading...");
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                report1(); report2(); report3(); report4(); report5();
                return null;
            }
            @Override protected void done() {
                statusLabel.setText("Done");
                reportsContainer.revalidate(); reportsContainer.repaint();
            }
        }.execute();
    }

    //  Report 1: Revenue by Location & Admission Type

    private void report1() {
        try {
            List<Map<String, Object>> rows = db.query(
                "SELECT l.venue_name AS location, a.admission_type, c.age_group, " +
                "COUNT(*) AS admissions, " +
                "COALESCE(SUM(a.entry_fee_paid), 0) AS total_revenue, " +
                "COUNT(DISTINCT a.customer_id) AS unique_customers " +
                "FROM ADMISSION a " +
                "LEFT JOIN LOCATION l ON l.location_id = a.location_id " +
                "LEFT JOIN CUSTOMER c ON c.customer_id = a.customer_id " +
                "GROUP BY l.venue_name, a.admission_type, c.age_group " +
                "ORDER BY total_revenue DESC"
            );
            addCard("Revenue by Location and Admission Type",
                "Admission revenue grouped by location and ticket type",
                new String[]{"Location", "Admission Type", "Age Group", "Admissions", "Total Revenue", "Unique Customers"},
                toStringRows(rows, "location","admission_type","age_group","admissions","total_revenue","unique_customers"));
        } catch (SQLException e) { addError("Revenue by Location", e); }
    }

    //  Report 2: Ride Maintenance Summary

    private void report2() {
        try {
            List<Map<String, Object>> rows = db.query(
                "SELECT a.name AS ride_name, r.ride_type, " +
                "COUNT(ml.log_id) AS maintenance_count, " +
                "COALESCE(SUM(ml.parts_cost), 0) AS total_parts_cost, " +
                "COUNT(DISTINCT ml.technician_id) AS technicians_involved " +
                "FROM RIDE r " +
                "JOIN ATTRACTION a ON a.attraction_id = r.attraction_id " +
                "LEFT JOIN MAINTENANCE_LOG ml ON ml.ride_id = r.attraction_id " +
                "GROUP BY a.name, r.ride_type " +
                "ORDER BY total_parts_cost DESC"
            );
            addCard("Ride Maintenance Summary",
                "Maintenance cost per ride with technician details",
                new String[]{"Ride Name","Ride Type","Maint. Count","Total Parts Cost","Technicians"},
                toStringRows(rows,"ride_name","ride_type","maintenance_count","total_parts_cost","technicians_involved"));
        } catch (SQLException e) { addError("Ride Maintenance", e); }
    }

    //  Report 3: Top Products by Stall

    private void report3() {
        try {
            List<Map<String, Object>> rows = db.query(
                "SELECT p.name AS product_name, p.category, a.name AS stall_name, " +
                "p.price, " +
                "COALESCE(SUM(ss.stock_quantity), 0) AS total_stock " +
                "FROM PRODUCT p " +
                "LEFT JOIN STALL_STOCK ss ON ss.product_id = p.product_id " +
                "LEFT JOIN STALL s ON s.attraction_id = ss.stall_id " +
                "LEFT JOIN ATTRACTION a ON a.attraction_id = s.attraction_id " +
                "GROUP BY p.product_id, p.name, p.category, a.name, p.price " +
                "ORDER BY total_stock DESC"
            );
            addCard("Products by Stall Stock",
                "Products with stall assignments and stock levels",
                new String[]{"Product","Category","Stall","Price","Total Stock"},
                toStringRows(rows,"product_name","category","stall_name","price","total_stock"));
        } catch (SQLException e) { addError("Products", e); }
    }

    //  Report 4: Employee Distribution

    private void report4() {
        try {
            List<Map<String, Object>> rows = db.query(
                "SELECT l.venue_name AS location, e.employee_type, " +
                "CASE " +
                "WHEN op.employee_id IS NOT NULL THEN 'Operator' " +
                "WHEN te.employee_id IS NOT NULL THEN 'Technician' " +
                "WHEN v.employee_id IS NOT NULL THEN 'Vendor' " +
                "WHEN p.employee_id IS NOT NULL THEN 'Performer' " +
                "WHEN gs.employee_id IS NOT NULL THEN 'General Staff' " +
                "ELSE 'Unassigned' END AS subtype_record, " +
                "COUNT(DISTINCT e.employee_id) AS employee_count, " +
                "ROUND(AVG(e.hourly_wage), 2) AS avg_wage, " +
                "MIN(e.hire_date) AS earliest_hire " +
                "FROM EMPLOYEE e " +
                "LEFT JOIN LOCATION l ON l.location_id = e.current_location_id " +
                "LEFT JOIN OPERATOR op ON op.employee_id = e.employee_id " +
                "LEFT JOIN TECHNICIAN te ON te.employee_id = e.employee_id " +
                "LEFT JOIN VENDOR v ON v.employee_id = e.employee_id " +
                "LEFT JOIN PERFORMER p ON p.employee_id = e.employee_id " +
                "LEFT JOIN GENERAL_STAFF gs ON gs.employee_id = e.employee_id " +
                "GROUP BY l.venue_name, e.employee_type, subtype_record " +
                "ORDER BY l.venue_name, employee_count DESC"
            );
            addCard("Employee Distribution by Location",
                "Employee count and wages by type per location",
                new String[]{"Location","Type","Subtype Record","Count","Avg Wage","Earliest Hire"},
                toStringRows(rows,"location","employee_type","subtype_record","employee_count","avg_wage","earliest_hire"));
        } catch (SQLException e) { addError("Employee Distribution", e); }
    }

    //  Report 5: Customer Loyalty & Balance

    private void report5() {
        try {
            List<Map<String, Object>> rows = db.query(
                "SELECT CONCAT(c.first_name, ' ', c.last_name) AS customer_name, " +
                "c.age_group, c.card_status, " +
                "c.card_balance, c.loyalty_points, " +
                "COUNT(t.transaction_id) AS total_transactions, " +
                "COALESCE(SUM(t.total_amount), 0) AS total_spent, " +
                "COALESCE(SUM(p.amount), 0) AS total_paid " +
                "FROM CUSTOMER c " +
                "LEFT JOIN `TRANSACTION` t ON t.customer_id = c.customer_id " +
                "LEFT JOIN PAYMENT p ON p.transaction_id = t.transaction_id " +
                "GROUP BY c.customer_id, c.first_name, c.last_name, c.age_group, c.card_status, c.card_balance, c.loyalty_points " +
                "ORDER BY total_spent DESC " +
                "LIMIT 50"
            );
            addCard("Customer Loyalty and Spending",
                "Top customers ranked by total spend with balance and points",
                new String[]{"Customer","Age Group","Status","Card Balance","Loyalty Pts","Transactions","Total Spent","Total Paid"},
                toStringRows(rows,"customer_name","age_group","card_status","card_balance","loyalty_points","total_transactions","total_spent","total_paid"));
        } catch (SQLException e) { addError("Customer Loyalty", e); }
    }

    //  Card Builder

    private void addCard(String title, String desc, String[] cols, List<String[]> rows) {
        SwingUtilities.invokeLater(() -> {
            JPanel card = new JPanel(new BorderLayout());
            card.setBackground(CARD);
            card.setBorder(new CompoundBorder(
                new EmptyBorder(8, 14, 0, 14),
                new CompoundBorder(new LineBorder(BORDER, 1, true), BorderFactory.createEmptyBorder())
            ));
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

            JPanel cardHeader = new JPanel(new BorderLayout());
            cardHeader.setBackground(SURFACE);
            cardHeader.setBorder(new EmptyBorder(10, 14, 10, 14));

            JPanel titleBox = new JPanel();
            titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
            titleBox.setBackground(SURFACE);

            JLabel tl = new JLabel(title);
            tl.setForeground(ACCENT);
            tl.setFont(tl.getFont().deriveFont(Font.BOLD, 13f));

            JLabel dl = new JLabel(desc);
            dl.setForeground(FG_MUTED);
            dl.setFont(dl.getFont().deriveFont(Font.ITALIC, 10f));

            JLabel countLbl = new JLabel(rows.size() + " rows");
            countLbl.setForeground(FG_MUTED);
            countLbl.setFont(countLbl.getFont().deriveFont(Font.PLAIN, 10f));

            titleBox.add(tl);
            titleBox.add(Box.createVerticalStrut(2));
            titleBox.add(dl);
            cardHeader.add(titleBox, BorderLayout.WEST);
            cardHeader.add(countLbl, BorderLayout.EAST);
            card.add(cardHeader, BorderLayout.NORTH);

            if (rows.isEmpty()) {
                JLabel empty = new JLabel("No data yet. Add records first.");
                empty.setForeground(FG_MUTED);
                empty.setHorizontalAlignment(JLabel.CENTER);
                empty.setBorder(new EmptyBorder(20, 0, 20, 0));
                card.add(empty, BorderLayout.CENTER);
            } else {
                DefaultTableModel tm = new DefaultTableModel(cols, 0) {
                    @Override public boolean isCellEditable(int r, int c) { return false; }
                };
                for (String[] r : rows) tm.addRow(r);

                JTable t = new JTable(tm) {
                    @Override public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                        Component c = super.prepareRenderer(r, row, col);
                        c.setBackground(isRowSelected(row) ? SEL_BG : (row%2==0?CARD:ROW_ALT));
                        c.setForeground(isRowSelected(row) ? ACCENT : FG);
                        return c;
                    }
                };
                t.setBackground(CARD); t.setForeground(FG);
                t.setGridColor(BORDER); t.setRowHeight(24);
                t.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
                t.getTableHeader().setBackground(SURFACE);
                t.getTableHeader().setForeground(new Color(160, 190, 255));
                t.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
                t.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

                JScrollPane sp = new JScrollPane(t);
                sp.setPreferredSize(new Dimension(0, Math.min(rows.size()*25+32, 260)));
                sp.setBackground(CARD); sp.getViewport().setBackground(CARD);
                sp.setBorder(BorderFactory.createEmptyBorder());
                card.add(sp, BorderLayout.CENTER);
            }

            reportsContainer.add(card);
            reportsContainer.add(Box.createVerticalStrut(6));
            reportsContainer.revalidate();
            reportsContainer.repaint();
        });
    }

    private void addError(String title, Exception e) {
        SwingUtilities.invokeLater(() -> {
            JLabel lbl = new JLabel(title + ": " + e.getMessage());
            lbl.setForeground(ACCENT2);
            lbl.setBorder(new EmptyBorder(8, 14, 8, 14));
            reportsContainer.add(lbl);
        });
    }

    //  Util

    private List<String[]> toStringRows(List<Map<String, Object>> rows, String... cols) {
        List<String[]> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String[] r = new String[cols.length];
            for (int i = 0; i < cols.length; i++) r[i] = str(row.get(cols[i]));
            result.add(r);
        }
        return result;
    }

    private static String str(Object o) { return o == null ? "" : o.toString(); }
}
