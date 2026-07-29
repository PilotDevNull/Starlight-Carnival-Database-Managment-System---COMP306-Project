package starlightcarnival;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

import static starlightcarnival.EntityCrudPanel.*;

public class MainFrame extends JFrame {

    private JPanel contentArea;
    private JScrollPane sidebarScroll;
    private boolean sidebarOpen = true;
    private String activeKey = "location";
    private final java.util.Map<String, JButton> navButtons = new java.util.LinkedHashMap<>();
    private final String displayName;

    private static final Object[][] NAV_GROUPS = {
        {"Core", new Object[][]{
            {"location",     "Locations"},
            {"customer",     "Customers"},
            {"admission",    "Admissions"},
        }},
        {"Employees", new Object[][]{
            {"employee",     "All Employees"},
            {"operator",     "Operators"},
            {"technician",   "Technicians"},
            {"vendor",       "Vendors"},
            {"performer",    "Performers"},
            {"general_staff","General Staff"},
        }},
        {"Attractions", new Object[][]{
            {"attraction",   "All Attractions"},
            {"ride",         "Rides"},
            {"show",         "Shows"},
            {"stall",        "Stalls"},
        }},
        {"Commerce", new Object[][]{
            {"transaction",  "Transactions"},
            {"payment",      "Payments"},
            {"product",      "Products"},
            {"stall_stock",  "Stall Stock"},
        }},
        {"Passes & Logs", new Object[][]{
            {"access_pass",      "Access Passes"},
            {"ride_fast_pass",   "Fast Passes"},
            {"show_reservation", "Show Reservations"},
            {"maintenance_log",  "Maintenance Logs"},
        }},
        {"Analytics", new Object[][]{
            {"reports", "Reports"},
        }},
    };

    private final boolean readOnly;

    public MainFrame(String displayName, boolean readOnly) {
        this.readOnly = readOnly;
        this.displayName = displayName;
        setTitle("ParkPal - Starlight Carnival");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 700));
        setSize(1300, 800);
        setLocationRelativeTo(null);

        getContentPane().setBackground(BG);
        setLayout(new BorderLayout(0, 0));

        add(buildTopBar(), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 0));
        body.setBackground(BG);

        JPanel sidebarPanel = buildSidebar();
        sidebarScroll = new JScrollPane(sidebarPanel);
        sidebarScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sidebarScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        sidebarScroll.setBorder(new MatteBorder(0, 0, 0, 1, BORDER));
        sidebarScroll.setBackground(SURFACE);
        sidebarScroll.getViewport().setBackground(SURFACE);
        sidebarScroll.setPreferredSize(new Dimension(200, 0));
        sidebarScroll.getVerticalScrollBar().setUnitIncrement(10);

        body.add(sidebarScroll, BorderLayout.WEST);

        contentArea = new JPanel(new BorderLayout(0, 0));
        contentArea.setBackground(BG);
        body.add(contentArea, BorderLayout.CENTER);

        add(body, BorderLayout.CENTER);
        // GUEST has full read access — start on the same default page as ADMIN
        navigateTo("location");
    }

    //  Top Bar

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(SURFACE);
        bar.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER),
            new EmptyBorder(0, 12, 0, 16)
        ));
        bar.setPreferredSize(new Dimension(0, 50));

        JButton hamburger = new JButton("=");
        hamburger.setBackground(SURFACE);
        hamburger.setForeground(FG);
        hamburger.setBorderPainted(false);
        hamburger.setFocusPainted(false);
        hamburger.setFont(hamburger.getFont().deriveFont(Font.BOLD, 17f));
        hamburger.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        hamburger.setPreferredSize(new Dimension(38, 38));
        hamburger.addActionListener(e -> toggleSidebar());
        hamburger.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hamburger.setForeground(ACCENT); }
            @Override public void mouseExited(MouseEvent e)  { hamburger.setForeground(FG); }
        });

        JLabel logo = new JLabel("  Starlight Carnival");
        logo.setForeground(ACCENT);
        logo.setFont(logo.getFont().deriveFont(Font.BOLD, 16f));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 6));
        left.setBackground(SURFACE);
        left.add(hamburger);
        left.add(logo);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        right.setBackground(SURFACE);

        String role  = DatabaseManager.getInstance().getConnectedRole();
        String badge = readOnly ? " [GUEST]" : " [" + role + "]";
        JLabel userLbl = new JLabel(displayName + badge);
        userLbl.setForeground(readOnly ? FG_MUTED : ACCENT);
        userLbl.setFont(userLbl.getFont().deriveFont(Font.PLAIN, 12f));

        JButton btnLogout = new JButton("Sign Out");
        btnLogout.setBackground(SURFACE);
        btnLogout.setForeground(ACCENT2);
        btnLogout.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(ACCENT2, 1, true),
            new EmptyBorder(4, 10, 4, 10)
        ));
        btnLogout.setFocusPainted(false);
        btnLogout.setFont(btnLogout.getFont().deriveFont(Font.PLAIN, 11f));
        btnLogout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLogout.addActionListener(e -> {
            DatabaseManager.getInstance().close();
            dispose();
            new LoginFrame().setVisible(true);
        });

        right.add(userLbl);
        right.add(btnLogout);
        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    //  Sidebar

    private JPanel buildSidebar() {
        // GridBagLayout: every row is forced to full width
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(SURFACE);

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx   = 0;
        gc.gridy   = 0;
        gc.weightx = 1.0;           // stretch horizontally
        gc.weighty = 0;
        gc.fill    = GridBagConstraints.HORIZONTAL;
        gc.anchor  = GridBagConstraints.NORTHWEST;

        // Brand
        JLabel brandLbl = new JLabel("  ParkPal");
        brandLbl.setForeground(ACCENT);
        brandLbl.setFont(brandLbl.getFont().deriveFont(Font.BOLD, 15f));
        brandLbl.setOpaque(true);
        brandLbl.setBackground(new Color(18, 22, 38));
        brandLbl.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER),
            new EmptyBorder(12, 8, 12, 8)
        ));
        panel.add(brandLbl, gc);
        gc.gridy++;

        // Nav groups
        for (Object[] group : NAV_GROUPS) {
            String groupName  = (String)   group[0];
            Object[][] items  = (Object[][]) group[1];

            // Group header label - full width, left-aligned
            JLabel groupLbl = new JLabel(groupName.toUpperCase());
            groupLbl.setForeground(FG_MUTED);
            groupLbl.setFont(groupLbl.getFont().deriveFont(Font.BOLD, 9f));
            groupLbl.setOpaque(true);
            groupLbl.setBackground(SURFACE);
            groupLbl.setBorder(new EmptyBorder(10, 10, 3, 8));
            panel.add(groupLbl, gc);
            gc.gridy++;

            // Nav buttons - full width, indented 4px more than label
            for (Object[] item : items) {
                String key   = (String) item[0];
                String label = (String) item[1];
                JButton btn  = buildNavButton(key, label);
                navButtons.put(key, btn);
                panel.add(btn, gc);
                gc.gridy++;
            }
        }

        // Push everything up - filler takes remaining space
        gc.weighty = 1.0;
        gc.fill    = GridBagConstraints.BOTH;
        panel.add(new JPanel() {{ setBackground(SURFACE); }}, gc);
        gc.gridy++;
        gc.weighty = 0;
        gc.fill    = GridBagConstraints.HORIZONTAL;

        // Footer
        JLabel footer = new JLabel("  " + DatabaseManager.DBNAME + " @ " + DatabaseManager.HOST);
        footer.setForeground(new Color(70, 82, 115));
        footer.setFont(footer.getFont().deriveFont(Font.PLAIN, 10f));
        footer.setOpaque(true);
        footer.setBackground(new Color(14, 17, 30));
        footer.setBorder(new CompoundBorder(
            new MatteBorder(1, 0, 0, 0, BORDER),
            new EmptyBorder(8, 8, 8, 8)
        ));
        panel.add(footer, gc);

        return panel;
    }

    private JButton buildNavButton(String key, String label) {
        JButton btn = new JButton(label);
        btn.setBackground(SURFACE);
        btn.setForeground(FG);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setHorizontalTextPosition(SwingConstants.LEFT);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(true);
        btn.setFont(btn.getFont().deriveFont(Font.PLAIN, 12f));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // Left indent slightly more than group label so it reads as child
        btn.setBorder(new EmptyBorder(6, 22, 6, 8));
        btn.setMargin(new Insets(0, 0, 0, 0));

        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (!key.equals(activeKey)) {
                    btn.setBackground(new Color(32, 38, 60));
                    btn.setForeground(new Color(200, 205, 230));
                }
            }
            @Override public void mouseExited(MouseEvent e) {
                if (!key.equals(activeKey)) {
                    btn.setBackground(SURFACE);
                    btn.setForeground(FG);
                }
            }
        });

        btn.addActionListener(e -> navigateTo(key));
        return btn;
    }

    private void toggleSidebar() {
        sidebarOpen = !sidebarOpen;
        sidebarScroll.setVisible(sidebarOpen);
        revalidate();
        repaint();
    }

    //  Navigation

    private void navigateTo(String key) {
        JButton old = navButtons.get(activeKey);
        if (old != null) {
            old.setBackground(SURFACE);
            old.setForeground(FG);
            old.setFont(old.getFont().deriveFont(Font.PLAIN));
        }

        activeKey = key;

        JButton cur = navButtons.get(key);
        if (cur != null) {
            cur.setBackground(new Color(45, 35, 8));
            cur.setForeground(ACCENT);
            cur.setFont(cur.getFont().deriveFont(Font.BOLD));
        }

        contentArea.removeAll();

        JPanel content;
        if ("reports".equals(key)) {
            content = new ReportsPanel();
        } else {
            EntityConfig cfg = EntityConfig.ALL.get(key);
            if (cfg == null) {
                JLabel err = new JLabel("Unknown section: " + key);
                err.setForeground(ACCENT2);
                err.setHorizontalAlignment(JLabel.CENTER);
                content = new JPanel(new BorderLayout());
                content.add(err, BorderLayout.CENTER);
                content.setBackground(BG);
            } else {
                EntityCrudPanel panel = new EntityCrudPanel(cfg);
                if (readOnly) panel.setReadOnly(true);
                content = panel;
            }
        }

        contentArea.add(content, BorderLayout.CENTER);
        contentArea.revalidate();
        contentArea.repaint();
    }
}
