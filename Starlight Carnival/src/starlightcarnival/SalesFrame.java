package starlightcarnival;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

import static starlightcarnival.EntityCrudPanel.*;

public class SalesFrame extends JFrame {

    private final String displayName;
    private final DatabaseManager db = DatabaseManager.getInstance();
    private JPanel mainPanel;
    private CardLayout cardLayout;

    // Pipeline state
    private int currentCustomerId = -1;
    private String currentCustomerName = "";

    public SalesFrame(String displayName) {
        this.displayName = displayName;
        setTitle("ParkPal - Sales");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 650);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(800, 550));

        getContentPane().setBackground(BG);
        setLayout(new BorderLayout(0, 0));

        add(buildTopBar(), BorderLayout.NORTH);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setBackground(BG);

        mainPanel.add(buildHomeScreen(),        "home");
        mainPanel.add(buildCardholderSearch(),  "cardholder");
        mainPanel.add(buildNewCustomerForm(),   "newcustomer");
        mainPanel.add(buildAdmissionScreen(),   "admission");
        mainPanel.add(buildPassScreen(),        "pass");
        mainPanel.add(buildSummaryScreen(),     "summary");

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

        JLabel title = new JLabel("Starlight Carnival - Sales");
        title.setForeground(ACCENT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        JLabel userLbl = new JLabel(displayName + " [SALES]");
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
        card.setPreferredSize(new Dimension(500, 380));

        JLabel heading = centreLabel("Welcome, " + displayName, ACCENT, Font.BOLD, 20f);
        JLabel sub     = centreLabel("What would you like to do?", FG_MUTED, Font.ITALIC, 12f);

        JButton btnCard   = bigButton("Cardholder Admission", ACCENT,
            "Existing customer with a smart card");
        JButton btnNew    = bigButton("New Customer", new Color(80, 180, 255),
            "Register a new customer and process admission");
        JButton btnLogout = bigButton("Logout", new Color(160, 50, 50), "Return to login");

        btnCard.addActionListener(e -> { cardLayout.show(mainPanel, "cardholder"); });
        btnNew.addActionListener(e -> { cardLayout.show(mainPanel, "newcustomer"); });
        btnLogout.addActionListener(e -> { dispose(); new LoginFrame().setVisible(true); });

        card.add(heading);
        card.add(Box.createVerticalStrut(4));
        card.add(sub);
        card.add(Box.createVerticalStrut(28));
        card.add(btnCard);
        card.add(Box.createVerticalStrut(12));
        card.add(btnNew);
        card.add(Box.createVerticalStrut(20));
        card.add(new JSeparator() {{ setMaximumSize(new Dimension(Integer.MAX_VALUE, 1)); setForeground(BORDER); }});
        card.add(Box.createVerticalStrut(16));
        card.add(btnLogout);

        p.add(card);
        return p;
    }

    //  STEP 1A: Cardholder Search

    private JTextField tfSearch;
    private JPanel searchResultsPanel;

    private JPanel buildCardholderSearch() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setBackground(BG);

        // Step header
        wrapper.add(stepHeader("Step 1 of 4 - Find Cardholder", "Search by name, email, or customer ID"), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setBackground(BG);
        center.setBorder(new EmptyBorder(24, 40, 24, 40));

        // Search bar
        JPanel searchBar = new JPanel(new BorderLayout(8, 0));
        searchBar.setBackground(BG);
        tfSearch = styledTextField();
        JButton btnSearch = pipelineButton("Search", ACCENT);
        btnSearch.addActionListener(e -> doCustomerSearch());
        tfSearch.addActionListener(e -> doCustomerSearch());
        searchBar.add(tfSearch, BorderLayout.CENTER);
        searchBar.add(btnSearch, BorderLayout.EAST);

        // Results
        searchResultsPanel = new JPanel();
        searchResultsPanel.setLayout(new BoxLayout(searchResultsPanel, BoxLayout.Y_AXIS));
        searchResultsPanel.setBackground(BG);
        JScrollPane scroll = new JScrollPane(searchResultsPanel);
        scroll.setBackground(BG);
        scroll.getViewport().setBackground(BG);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        center.add(searchBar, BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);

        // Nav
        center.add(navBar("Back to Home", "home", null, null), BorderLayout.SOUTH);

        wrapper.add(center, BorderLayout.CENTER);
        return wrapper;
    }

    private void doCustomerSearch() {
        String q = tfSearch.getText().trim();
        searchResultsPanel.removeAll();
        if (q.isEmpty()) { searchResultsPanel.revalidate(); return; }

        try {
            List<Map<String, Object>> rows = db.query(
                "SELECT customer_id, first_name, last_name, email, phone, card_status, card_balance " +
                "FROM CUSTOMER WHERE first_name LIKE ? OR last_name LIKE ? OR email LIKE ? " +
                "OR CAST(customer_id AS CHAR) LIKE ? LIMIT 20",
                "%" + q + "%", "%" + q + "%", "%" + q + "%", "%" + q + "%"
            );

            if (rows.isEmpty()) {
                JLabel none = new JLabel("  No customers found for \"" + q + "\"");
                none.setForeground(FG_MUTED);
                none.setFont(none.getFont().deriveFont(Font.ITALIC, 12f));
                none.setBorder(new EmptyBorder(12, 0, 0, 0));
                searchResultsPanel.add(none);
            } else {
                for (Map<String, Object> row : rows) {
                    searchResultsPanel.add(customerResultRow(row));
                    searchResultsPanel.add(Box.createVerticalStrut(6));
                }
            }
        } catch (SQLException e) {
            JLabel err = new JLabel("  Error: " + e.getMessage());
            err.setForeground(ACCENT2);
            searchResultsPanel.add(err);
        }
        searchResultsPanel.revalidate();
        searchResultsPanel.repaint();
    }

    private JPanel customerResultRow(Map<String, Object> row) {
        JPanel p = new JPanel(new BorderLayout(12, 0));
        p.setBackground(CARD);
        p.setBorder(new CompoundBorder(
            new LineBorder(BORDER, 1, true),
            new EmptyBorder(10, 14, 10, 14)
        ));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        String name = row.get("first_name") + " " + row.get("last_name");
        String info = row.get("email") + "  |  " + row.get("phone") +
                      "  |  Balance: R" + row.get("card_balance") +
                      "  |  Status: " + row.get("card_status");

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBackground(CARD);
        JLabel nameLbl = new JLabel(name);
        nameLbl.setForeground(FG);
        nameLbl.setFont(nameLbl.getFont().deriveFont(Font.BOLD, 13f));
        JLabel infoLbl = new JLabel(info);
        infoLbl.setForeground(FG_MUTED);
        infoLbl.setFont(infoLbl.getFont().deriveFont(Font.PLAIN, 11f));
        left.add(nameLbl);
        left.add(infoLbl);

        JButton btnSelect = pipelineButton("Select", ACCENT);
        btnSelect.addActionListener(e -> {
            currentCustomerId   = (int) row.get("customer_id");
            currentCustomerName = name;
            refreshAdmissionScreen();
            cardLayout.show(mainPanel, "admission");
        });

        p.add(left, BorderLayout.CENTER);
        p.add(btnSelect, BorderLayout.EAST);
        return p;
    }

    //  STEP 1B: New Customer Form

    private JTextField tfFirstName, tfLastName, tfEmail, tfPhone, tfDob;
    private JComboBox<String> cbAgeGroup;

    private JPanel buildNewCustomerForm() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setBackground(BG);
        wrapper.add(stepHeader("Step 1 of 4 - New Customer", "Fill in customer details"), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(BG);
        form.setBorder(new EmptyBorder(24, 60, 12, 60));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 8, 6, 8);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;

        tfFirstName = styledTextField();
        tfLastName  = styledTextField();
        tfEmail     = styledTextField();
        tfPhone     = styledTextField();
        tfDob       = styledTextField();
        tfDob.setToolTipText("Format: YYYY-MM-DD");
        cbAgeGroup  = styledCombo("Adult", "Child", "Senior", "Teen");

        int r = 0;
        r = formRow(form, gc, r, "First Name *", tfFirstName, "Last Name *", tfLastName);
        r = formRow(form, gc, r, "Email",       tfEmail,     "Phone",       tfPhone);
        r = formRow(form, gc, r, "Date of Birth (YYYY-MM-DD)", tfDob, "Age Group", cbAgeGroup);

        JButton btnNext = pipelineButton("Register and Continue", ACCENT);
        btnNext.addActionListener(e -> doRegisterNewCustomer());

        gc.gridx = 0; gc.gridy = r; gc.gridwidth = 4; gc.insets = new Insets(18, 8, 6, 8);
        form.add(btnNext, gc);

        JPanel scrollWrap = new JPanel(new BorderLayout());
        scrollWrap.setBackground(BG);
        scrollWrap.add(form, BorderLayout.NORTH);

        wrapper.add(scrollWrap, BorderLayout.CENTER);
        wrapper.add(navBar("Back to Home", "home", null, null), BorderLayout.SOUTH);
        return wrapper;
    }

    private void doRegisterNewCustomer() {
        String fn = tfFirstName.getText().trim();
        String ln = tfLastName.getText().trim();
        if (fn.isEmpty() || ln.isEmpty()) {
            showError("First Name and Last Name are required.");
            return;
        }
        try {
            String today = LocalDate.now().toString();
            long id = db.update(
                "INSERT INTO CUSTOMER (first_name, last_name, email, phone, date_of_birth, age_group, signup_date, card_balance, loyalty_points, card_status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, 0, 0, 'ACTIVE')",
                fn, ln,
                nullIfEmpty(tfEmail.getText()), nullIfEmpty(tfPhone.getText()),
                nullIfEmpty(tfDob.getText()),
                cbAgeGroup.getSelectedItem(), today
            );
            currentCustomerId   = (int) id;
            currentCustomerName = fn + " " + ln;
            refreshAdmissionScreen();
            cardLayout.show(mainPanel, "admission");
        } catch (SQLException e) {
            showError("Could not register customer: " + e.getMessage());
        }
    }

    //  STEP 2: Admission

    private JLabel lblAdmissionCustomer;
    private JComboBox<String> cbLocation, cbAdmissionType, cbAdmissionTender;
    private JTextField tfAdmissionFee;
    private JLabel lblAdmissionDate, lblAdmissionTime;

    private JPanel buildAdmissionScreen() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setBackground(BG);
        wrapper.add(stepHeader("Step 2 of 4 - Admission", "Record entry details"), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(BG);
        form.setBorder(new EmptyBorder(24, 60, 12, 60));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8, 8, 8, 8);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;

        lblAdmissionCustomer = new JLabel("");
        lblAdmissionCustomer.setForeground(ACCENT);
        lblAdmissionCustomer.setFont(lblAdmissionCustomer.getFont().deriveFont(Font.BOLD, 14f));

        cbLocation      = styledCombo();
        cbAdmissionType = styledCombo("Adult", "Child", "Senior", "VIP", "Group");
        cbAdmissionTender = styledCombo("Cash", "Credit Card", "Debit Card", "Smart Card");
        tfAdmissionFee = styledTextField();
        tfAdmissionFee.setText("0.00");

        lblAdmissionDate = dimLabel(LocalDate.now().toString());
        lblAdmissionTime = dimLabel(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));

        int r = 0;
        gc.gridx=0; gc.gridy=r; gc.gridwidth=1; gc.weightx=0;
        form.add(sideLabel("Customer"), gc);
        gc.gridx=1; gc.gridwidth=3; gc.weightx=1;
        form.add(lblAdmissionCustomer, gc); r++;

        gc.gridwidth=1; gc.weightx=0;
        r = formRow(form, gc, r, "Location",       cbLocation,      "Admission Type", cbAdmissionType);
        r = formRow(form, gc, r, "Entry Fee (R)",   tfAdmissionFee,  "Tender Type",    cbAdmissionTender);
        r = formRow(form, gc, r, "Entry Date",     lblAdmissionDate, "Entry Time",    lblAdmissionTime);

        JButton btnNext = pipelineButton("Record Admission and Continue", ACCENT);
        btnNext.addActionListener(e -> doRecordAdmission());

        gc.gridx=0; gc.gridy=r; gc.gridwidth=4; gc.insets=new Insets(18,8,6,8);
        form.add(btnNext, gc);

        wrapper.add(form, BorderLayout.CENTER);
        wrapper.add(navBar("Back", "cardholder", null, null), BorderLayout.SOUTH);
        return wrapper;
    }

    private void refreshAdmissionScreen() {
        if (lblAdmissionCustomer != null)
            lblAdmissionCustomer.setText(currentCustomerName + "  (ID: " + currentCustomerId + ")");
        if (lblAdmissionDate != null)
            lblAdmissionDate.setText(LocalDate.now().toString());
        if (lblAdmissionTime != null)
            lblAdmissionTime.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        // Reload locations
        if (cbLocation != null) {
            cbLocation.removeAllItems();
            cbLocation.addItem("");
            try {
                db.query("SELECT location_id, venue_name FROM LOCATION ORDER BY venue_name")
                  .forEach(r -> cbLocation.addItem(r.get("location_id") + " - " + r.get("venue_name")));
            } catch (SQLException e) {
                showError("Could not load locations: " + e.getMessage());
            }
        }
    }

    private int lastAdmissionId = -1;
    private int lastLocationId = -1;

    private void doRecordAdmission() {
        String locItem = (String) cbLocation.getSelectedItem();
        if (locItem == null || locItem.isEmpty()) { showError("Please select a location."); return; }
        int locId = Integer.parseInt(locItem.split(" - ")[0].trim());
        String admType = (String) cbAdmissionType.getSelectedItem();
        String tenderType = (String) cbAdmissionTender.getSelectedItem();
        double entryFee = parseMoney(tfAdmissionFee.getText());
        if (entryFee > 0 && (tenderType == null || tenderType.isEmpty())) {
            showError("Select a tender type for the admission payment.");
            return;
        }

        try {
            String today = LocalDate.now().toString();
            String now   = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

            long admId = db.update(
                "INSERT INTO ADMISSION (location_id, customer_id, entry_date, entry_time, admission_type, entry_fee_paid) " +
                "VALUES (?, ?, ?, ?, ?, ?)",
                locId, currentCustomerId, today, now, admType, entryFee
            );
            lastAdmissionId = (int) admId;
            lastLocationId = locId;

            long txId = db.update(
                "INSERT INTO `TRANSACTION` (location_id, customer_id, transaction_date, transaction_type, total_amount) " +
                "VALUES (?, ?, ?, 'Entry Fee', ?)",
                locId, currentCustomerId, today, entryFee
            );
            if (entryFee > 0) {
                db.update(
                    "INSERT INTO PAYMENT (transaction_id, amount, tender_type) VALUES (?, ?, ?)",
                    txId, entryFee, tenderType
                );
            }
            refreshPassScreen();
            cardLayout.show(mainPanel, "pass");
        } catch (SQLException e) {
            showError("Could not record admission: " + e.getMessage());
        }
    }

    //  STEP 3: Access Pass

    private JLabel lblPassCustomer;
    private JComboBox<String> cbPassType, cbRideOrShow, cbPassTender;
    private JTextField tfSeatOrSlot, tfPrice;
    private JLabel lblPassDate;

    private JPanel buildPassScreen() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setBackground(BG);
        wrapper.add(stepHeader("Step 3 of 4 - Access Pass", "Select the type of pass for this customer"), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(BG);
        form.setBorder(new EmptyBorder(24, 60, 12, 60));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8, 8, 8, 8);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;

        lblPassCustomer = new JLabel("");
        lblPassCustomer.setForeground(ACCENT);
        lblPassCustomer.setFont(lblPassCustomer.getFont().deriveFont(Font.BOLD, 14f));

        cbPassType    = styledCombo("RideFastPass", "ShowReservation");
        cbRideOrShow  = styledCombo();
        cbPassTender  = styledCombo("Cash", "Credit Card", "Debit Card", "Smart Card");
        tfSeatOrSlot  = styledTextField();
        tfPrice       = styledTextField();
        tfPrice.setText("0.00");
        lblPassDate   = dimLabel(LocalDate.now().toString());

        // When pass type changes, reload ride/show list
        cbPassType.addActionListener(e -> refreshRideShowCombo());

        int r = 0;
        gc.gridx=0; gc.gridy=r; gc.gridwidth=1; gc.weightx=0;
        form.add(sideLabel("Customer"), gc);
        gc.gridx=1; gc.gridwidth=3; gc.weightx=1;
        form.add(lblPassCustomer, gc); r++;

        gc.gridwidth=1; gc.weightx=0;
        r = formRow(form, gc, r, "Pass Type",          cbPassType,   "Ride / Show", cbRideOrShow);
        r = formRow(form, gc, r, "Seat / Time Slot",   tfSeatOrSlot, "Price (R)",   tfPrice);
        r = formRow(form, gc, r, "Tender Type",        cbPassTender, "",            new JLabel());
        r = formRow(form, gc, r, "Issue Date",         lblPassDate,  "",            new JLabel());

        JButton btnNext = pipelineButton("Confirm Pass and Continue", ACCENT);
        btnNext.addActionListener(e -> doCreatePass());

        gc.gridx=0; gc.gridy=r; gc.gridwidth=4; gc.insets=new Insets(18,8,6,8);
        form.add(btnNext, gc);

        wrapper.add(form, BorderLayout.CENTER);
        wrapper.add(navBar("Back", "admission", null, null), BorderLayout.SOUTH);
        return wrapper;
    }

    private void refreshPassScreen() {
        if (lblPassCustomer != null)
            lblPassCustomer.setText(currentCustomerName + "  (ID: " + currentCustomerId + ")");
        if (lblPassDate != null)
            lblPassDate.setText(LocalDate.now().toString());
        refreshRideShowCombo();
    }

    private void refreshRideShowCombo() {
        if (cbRideOrShow == null || cbPassType == null) return;
        cbRideOrShow.removeAllItems();
        cbRideOrShow.addItem("");
        String type = (String) cbPassType.getSelectedItem();
        try {
            if ("RideFastPass".equals(type)) {
                db.query("SELECT r.attraction_id, a.name FROM RIDE r JOIN ATTRACTION a ON a.attraction_id = r.attraction_id ORDER BY a.name")
                  .forEach(row -> cbRideOrShow.addItem(row.get("attraction_id") + " - " + row.get("name")));
            } else {
                db.query("SELECT s.attraction_id, a.name FROM `SHOW` s JOIN ATTRACTION a ON a.attraction_id = s.attraction_id ORDER BY a.name")
                  .forEach(row -> cbRideOrShow.addItem(row.get("attraction_id") + " - " + row.get("name")));
            }
        } catch (SQLException e) {
            showError("Could not load rides/shows: " + e.getMessage());
        }
    }

    private void doCreatePass() {
        String passType = (String) cbPassType.getSelectedItem();
        String rideShowItem = (String) cbRideOrShow.getSelectedItem();
        String tenderType = (String) cbPassTender.getSelectedItem();
        double price = parseMoney(tfPrice.getText());
        if (price > 0 && (tenderType == null || tenderType.isEmpty())) {
            showError("Select a tender type for the pass payment.");
            return;
        }

        try {
            String now         = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            String nowDatetime = LocalDate.now() + " " + now;  // DATETIME for issue_time / time_slot / show_time

            long txId = db.update(
                "INSERT INTO `TRANSACTION` (location_id, customer_id, transaction_date, transaction_type, total_amount) " +
                "VALUES (?, ?, ?, ?, ?)",
                lastLocationId > 0 ? lastLocationId : null, currentCustomerId,
                LocalDate.now().toString(), "Access Pass", price
            );
            if (price > 0) {
                db.update(
                    "INSERT INTO PAYMENT (transaction_id, amount, tender_type) VALUES (?, ?, ?)",
                    txId, price, tenderType
                );
            }

            long passId = db.update(
                "INSERT INTO ACCESS_PASS (transaction_id, customer_id, price, issue_time, is_redeemed, pass_type) VALUES (?, ?, ?, ?, FALSE, ?)",
                txId, currentCustomerId, price, nowDatetime, passType
            );

            if (rideShowItem != null && !rideShowItem.isEmpty()) {
                int subId = Integer.parseInt(rideShowItem.split(" - ")[0].trim());
                String seatSlot = tfSeatOrSlot.getText().trim();
                if ("RideFastPass".equals(passType)) {
                    db.update("INSERT INTO RIDE_FAST_PASS (pass_id, ride_id, time_slot) VALUES (?, ?, ?)",
                        passId, subId, nowDatetime);
                } else {
                    db.update("INSERT INTO SHOW_RESERVATION (pass_id, show_id, seat_number, show_time) VALUES (?, ?, ?, ?)",
                        passId, subId, nullIfEmpty(seatSlot), nowDatetime);
                }
            }

            refreshSummaryScreen(passType, price, (int)passId);
            cardLayout.show(mainPanel, "summary");

        } catch (SQLException e) {
            showError("Could not create pass: " + e.getMessage());
        }
    }

    //  STEP 4: Summary / Finish

    private JPanel summaryContent;

    private JPanel buildSummaryScreen() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setBackground(BG);
        wrapper.add(stepHeader("Step 4 of 4 - Complete", "Transaction successful"), BorderLayout.NORTH);

        summaryContent = new JPanel(new GridBagLayout());
        summaryContent.setBackground(BG);

        wrapper.add(summaryContent, BorderLayout.CENTER);

        JPanel nav = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 12));
        nav.setBackground(SURFACE);
        nav.setBorder(new MatteBorder(1, 0, 0, 0, BORDER));
        JButton btnHome = pipelineButton("Back to Home Screen", ACCENT);
        btnHome.addActionListener(e -> {
            currentCustomerId = -1;
            currentCustomerName = "";
            lastAdmissionId = -1;
            lastLocationId = -1;
            cardLayout.show(mainPanel, "home");
        });
        nav.add(btnHome);
        wrapper.add(nav, BorderLayout.SOUTH);

        return wrapper;
    }

    private void refreshSummaryScreen(String passType, double price, int passId) {
        summaryContent.removeAll();
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8, 8, 8, 8);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.gridx = 0; gc.gridy = 0; gc.gridwidth = 2;

        JPanel card = darkCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(30, 40, 30, 40));

        JLabel tick = centreLabel("OK", new Color(80, 220, 120), Font.BOLD, 32f);
        JLabel done = centreLabel("Admission Complete!", new Color(80, 220, 120), Font.BOLD, 18f);
        JLabel line1 = centreLabel("Customer: " + currentCustomerName, FG, Font.PLAIN, 13f);
        JLabel line2 = centreLabel("Pass Type: " + passType, FG, Font.PLAIN, 13f);
        JLabel line3 = centreLabel("Pass ID: #" + passId, FG_MUTED, Font.PLAIN, 12f);
        JLabel line4 = centreLabel("Total Charged: R" + String.format("%.2f", price), ACCENT, Font.BOLD, 15f);
        JLabel line5 = centreLabel("Date: " + LocalDate.now() + "  Time: " +
            LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")), FG_MUTED, Font.ITALIC, 11f);

        card.add(tick);
        card.add(done);
        card.add(Box.createVerticalStrut(14));
        card.add(new JSeparator() {{ setMaximumSize(new Dimension(Integer.MAX_VALUE, 1)); setForeground(BORDER); }});
        card.add(Box.createVerticalStrut(14));
        card.add(line1); card.add(line2); card.add(line3);
        card.add(Box.createVerticalStrut(10));
        card.add(line4); card.add(line5);

        summaryContent.add(card, gc);
        summaryContent.revalidate();
        summaryContent.repaint();
    }

    //  Shared UI Helpers

    private JPanel stepHeader(String title, String subtitle) {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(SURFACE);
        h.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER),
            new EmptyBorder(14, 20, 14, 20)
        ));
        JLabel t = new JLabel(title);
        t.setForeground(ACCENT);
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

    private JPanel navBar(String backLabel, String backCard, String fwdLabel, String fwdCard) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(SURFACE);
        bar.setBorder(new CompoundBorder(
            new MatteBorder(1, 0, 0, 0, BORDER),
            new EmptyBorder(10, 20, 10, 20)
        ));
        if (backLabel != null) {
            JButton b = pipelineButton(backLabel, FG_MUTED);
            b.addActionListener(e -> cardLayout.show(mainPanel, backCard));
            bar.add(b, BorderLayout.WEST);
        }
        if (fwdLabel != null && fwdCard != null) {
            JButton f = pipelineButton(fwdLabel, ACCENT);
            f.addActionListener(e -> cardLayout.show(mainPanel, fwdCard));
            bar.add(f, BorderLayout.EAST);
        }
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
        JButton b = new JButton("<html><center>" + label + "</center></html>");
        b.setForeground(color);
        b.setBackground(CARD);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(color, 1, true),
            new EmptyBorder(14, 20, 14, 20)
        ));
        b.setFont(b.getFont().deriveFont(Font.BOLD, 14f));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setAlignmentX(CENTER_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        b.setToolTipText(tooltip);
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setBackground(color.darker().darker()); }
            @Override public void mouseExited(MouseEvent e)  { b.setBackground(CARD); }
        });
        return b;
    }

    private JButton pipelineButton(String label, Color color) {
        JButton b = new JButton(label);
        b.setForeground(color);
        b.setBackground(CARD);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(color, 1, true),
            new EmptyBorder(7, 16, 7, 16)
        ));
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
        tf.setBackground(CARD);
        tf.setForeground(FG);
        tf.setCaretColor(ACCENT);
        tf.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER, 1, true), new EmptyBorder(6, 8, 6, 8)));
        tf.setFont(tf.getFont().deriveFont(Font.PLAIN, 12f));
        return tf;
    }

    private JComboBox<String> styledCombo(String... items) {
        JComboBox<String> cb = new JComboBox<>();
        cb.addItem("");
        for (String i : items) cb.addItem(i);
        cb.setBackground(CARD);
        cb.setForeground(FG);
        cb.setBorder(new LineBorder(BORDER, 1, true));
        cb.setFont(cb.getFont().deriveFont(Font.PLAIN, 12f));
        cb.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBackground(isSelected && index >= 0 ? SEL_BG : CARD);
                setForeground(isSelected && index >= 0 ? ACCENT : FG);
                setBorder(new EmptyBorder(3, 6, 3, 6));
                return this;
            }
        });
        cb.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
            @Override
            public void installUI(JComponent c) {
                super.installUI(c);
                c.setBackground(CARD);
                c.setForeground(FG);
                for (Component child : c.getComponents()) {
                    child.setBackground(CARD);
                    child.setForeground(FG);
                }
            }
            @Override
            protected javax.swing.plaf.basic.ComboPopup createPopup() {
                javax.swing.plaf.basic.BasicComboPopup popup =
                        (javax.swing.plaf.basic.BasicComboPopup) super.createPopup();
                popup.getList().setBackground(CARD);
                popup.getList().setForeground(FG);
                popup.getList().setSelectionBackground(SEL_BG);
                popup.getList().setSelectionForeground(ACCENT);
                return popup;
            }
            @Override
            public void paint(Graphics g, JComponent c) {
                g.setColor(CARD);
                g.fillRect(0, 0, c.getWidth(), c.getHeight());
                super.paint(g, c);
            }
        });
        cb.setForeground(FG);
        cb.setBackground(CARD);
        return cb;
    }

    private JLabel sideLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(FG_MUTED);
        l.setFont(l.getFont().deriveFont(Font.PLAIN, 11f));
        return l;
    }

    private JLabel dimLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(FG);
        l.setFont(l.getFont().deriveFont(Font.PLAIN, 13f));
        l.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER, 1, true), new EmptyBorder(6, 8, 6, 8)));
        l.setOpaque(true);
        l.setBackground(new Color(20, 24, 40));
        return l;
    }

    private JLabel centreLabel(String text, Color color, int style, float size) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setForeground(color);
        l.setFont(l.getFont().deriveFont(style, size));
        l.setAlignmentX(CENTER_ALIGNMENT);
        return l;
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

    private double parseMoney(String value) {
        try { return Double.parseDouble(value.trim()); }
        catch (Exception ignored) { return 0; }
    }

    private String nullIfEmpty(String s) { return (s == null || s.trim().isEmpty()) ? null : s.trim(); }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
