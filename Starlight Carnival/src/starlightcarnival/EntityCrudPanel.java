package starlightcarnival;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

public class EntityCrudPanel extends JPanel {

    private final EntityConfig config;
    private final DatabaseManager db = DatabaseManager.getInstance();
    private boolean readOnly = false;

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        btnAdd.setVisible(!readOnly && config.allowInsert);
        btnEdit.setVisible(!readOnly);
        btnDelete.setVisible(!readOnly);
    }

    private DefaultTableModel tableModel;
    private JTable table;
    private List<Map<String, Object>> currentRows = new ArrayList<>();
    private final Map<String, JComponent> formFields = new LinkedHashMap<>();
    private final Map<String, JComponent> subtypeFormFields = new LinkedHashMap<>();
    private final Map<String, EntityConfig.FieldConfig> subtypeFieldConfigs = new LinkedHashMap<>();
    private final Map<String, List<JComponent>> subtypeComponents = new LinkedHashMap<>();

    private JButton btnAdd, btnEdit, btnDelete, btnSave, btnCancel;
    private boolean editMode = false;
    private int editingRow = -1;

    //  Theme
    public static final Color BG       = new Color(12, 14, 24);
    public static final Color SURFACE  = new Color(18, 22, 36);
    public static final Color CARD     = new Color(26, 31, 50);
    public static final Color ACCENT   = new Color(255, 160, 0);
    public static final Color ACCENT2  = new Color(255, 70, 110);
    public static final Color FG       = new Color(225, 228, 245);
    public static final Color FG_MUTED = new Color(110, 120, 155);
    public static final Color BORDER   = new Color(40, 48, 72);
    public static final Color ROW_ALT  = new Color(22, 27, 44);
    public static final Color SEL_BG   = new Color(55, 42, 10);

    public EntityCrudPanel(EntityConfig config) {
        this.config = config;
        setLayout(new BorderLayout(0, 0));
        setBackground(BG);

        add(buildToolbar(), BorderLayout.NORTH);
        add(buildTablePanel(), BorderLayout.CENTER);
        add(buildFormPanel(), BorderLayout.SOUTH);

        setFormVisible(false);
        loadData();
    }

    //  Toolbar

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        bar.setBackground(SURFACE);
        bar.setBorder(new MatteBorder(0, 0, 1, 0, BORDER));

        btnAdd    = accentBtn("+ Add",    ACCENT);
        btnEdit   = accentBtn("Edit",     new Color(80, 150, 255));
        btnDelete = accentBtn("Delete",   ACCENT2);
        btnAdd.setVisible(config.allowInsert);

        btnAdd.addActionListener(e -> openAddForm());
        btnEdit.addActionListener(e -> openEditForm());
        btnDelete.addActionListener(e -> deleteSelected());

        JTextField searchField = new JTextField(18);
        styleInput(searchField);
        searchField.setToolTipText("Filter rows...");
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { filterTable(searchField.getText()); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { filterTable(searchField.getText()); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filterTable(searchField.getText()); }
        });

        bar.add(btnAdd); bar.add(btnEdit); bar.add(btnDelete);
        bar.add(Box.createHorizontalStrut(16));
        JLabel searchLbl = new JLabel("Search:");
        searchLbl.setForeground(FG_MUTED);
        bar.add(searchLbl);
        bar.add(searchField);

        // No DB warning label
        if (!db.isConnected()) {
            JLabel warn = new JLabel("  No database connection - data unavailable");
            warn.setForeground(ACCENT2);
            warn.setFont(warn.getFont().deriveFont(Font.ITALIC, 11f));
            bar.add(warn);
        }

        return bar;
    }

    private JButton accentBtn(String text, Color color) {
        JButton b = new JButton(text);
        b.setForeground(color);
        b.setBackground(CARD);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(color, 1, true),
            new EmptyBorder(5, 14, 5, 14)
        ));
        b.setFont(b.getFont().deriveFont(Font.BOLD, 12f));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setBackground(color.darker().darker()); }
            @Override public void mouseExited(MouseEvent e)  { b.setBackground(CARD); }
        });
        return b;
    }

    //  Table

    private JScrollPane buildTablePanel() {
        List<String> colNames = new ArrayList<>();
        for (String pk : config.primaryKeys) colNames.add(pk.toUpperCase());
        for (EntityConfig.FieldConfig f : config.fields) colNames.add(f.label);

        tableModel = new DefaultTableModel(colNames.toArray(), 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                c.setBackground(isRowSelected(row) ? SEL_BG : (row % 2 == 0 ? CARD : ROW_ALT));
                c.setForeground(isRowSelected(row) ? ACCENT : FG);
                return c;
            }
        };

        table.setBackground(CARD);
        table.setForeground(FG);
        table.setGridColor(BORDER);
        table.setRowHeight(28);
        table.setShowGrid(true);
        table.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        table.setSelectionBackground(SEL_BG);
        table.setSelectionForeground(ACCENT);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.getTableHeader().setBackground(new Color(16, 20, 34));
        table.getTableHeader().setForeground(ACCENT);
        table.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        table.getTableHeader().setBorder(new MatteBorder(0, 0, 1, 0, BORDER));

        JScrollPane sp = new JScrollPane(table);
        sp.setBackground(BG);
        sp.getViewport().setBackground(BG);
        sp.setBorder(BorderFactory.createEmptyBorder());
        return sp;
    }

    //  Form

    private JPanel formPanel;
    private JPanel subtypePanel;
    private JLabel formTitle;

    private JPanel buildFormPanel() {
        formPanel = new JPanel(new BorderLayout(0, 0));
        formPanel.setBackground(SURFACE);
        formPanel.setBorder(new MatteBorder(1, 0, 0, 0, BORDER));

        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(new Color(20, 24, 40));
        titleBar.setBorder(new EmptyBorder(8, 14, 8, 14));
        formTitle = new JLabel("Add " + config.label);
        formTitle.setForeground(ACCENT);
        formTitle.setFont(formTitle.getFont().deriveFont(Font.BOLD, 13f));
        titleBar.add(formTitle, BorderLayout.WEST);

        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setBackground(SURFACE);
        fieldsPanel.setBorder(new EmptyBorder(10, 14, 10, 14));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 6, 4, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0, col = 0;
        for (EntityConfig.FieldConfig fc : config.fields) {
            gc.gridx = col * 2; gc.gridy = row; gc.weightx = 0;
            JLabel lbl = new JLabel(fc.label + (fc.required ? " *" : ""));
            lbl.setForeground(fc.required ? ACCENT : FG_MUTED);
            lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 11f));
            fieldsPanel.add(lbl, gc);

            gc.gridx = col * 2 + 1; gc.weightx = 1.0;
            JComponent input = buildInputField(fc);
            if (fc.key.equals(config.subtypeDiscriminatorKey) && input instanceof JComboBox) {
                ((JComboBox<?>) input).addActionListener(e -> updateSubtypeFields());
            }
            formFields.put(fc.key, input);
            fieldsPanel.add(input, gc);

            col++;
            if (col == 3) { col = 0; row++; }
        }

        JPanel fieldsContainer = new JPanel();
        fieldsContainer.setLayout(new BoxLayout(fieldsContainer, BoxLayout.Y_AXIS));
        fieldsContainer.setBackground(SURFACE);
        fieldsContainer.add(fieldsPanel);
        if (!config.subtypes.isEmpty()) fieldsContainer.add(buildSubtypePanel());

        JScrollPane scrollFields = new JScrollPane(fieldsContainer);
        scrollFields.setBackground(SURFACE);
        scrollFields.getViewport().setBackground(SURFACE);
        scrollFields.setBorder(BorderFactory.createEmptyBorder());
        scrollFields.setPreferredSize(new Dimension(0, config.subtypes.isEmpty() ? 170 : 250));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        btnPanel.setBackground(SURFACE);
        btnSave   = accentBtn("Save",   ACCENT);
        btnCancel = accentBtn("Cancel", FG_MUTED);
        btnSave.addActionListener(e -> saveRecord());
        btnCancel.addActionListener(e -> cancelForm());
        btnPanel.add(btnCancel);
        btnPanel.add(btnSave);

        formPanel.add(titleBar, BorderLayout.NORTH);
        formPanel.add(scrollFields, BorderLayout.CENTER);
        formPanel.add(btnPanel, BorderLayout.SOUTH);
        return formPanel;
    }

    private JPanel buildSubtypePanel() {
        JPanel p = new JPanel(new GridBagLayout());
        subtypePanel = p;
        p.setBackground(SURFACE);
        p.setBorder(new CompoundBorder(
            new MatteBorder(1, 0, 0, 0, BORDER),
            new EmptyBorder(8, 14, 10, 14)
        ));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 6, 4, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridx = 0; gc.gridy = 0; gc.gridwidth = 6; gc.weightx = 1.0;
        JLabel header = new JLabel("Subtype Details");
        header.setForeground(ACCENT);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 12f));
        p.add(header, gc);
        subtypeComponents.computeIfAbsent("__header__", k -> new ArrayList<>()).add(header);

        int row = 1;
        for (EntityConfig.SubtypeConfig subtype : config.subtypes.values()) {
            int col = 0;
            for (EntityConfig.FieldConfig fc : subtype.fields) {
                String key = subtypeFieldKey(subtype.discriminatorValue, fc.key);
                gc.gridwidth = 1;
                gc.gridx = col * 2; gc.gridy = row; gc.weightx = 0;
                JLabel lbl = new JLabel(fc.label + (fc.required ? " *" : ""));
                lbl.setForeground(fc.required ? ACCENT : FG_MUTED);
                lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 11f));
                p.add(lbl, gc);

                gc.gridx = col * 2 + 1; gc.weightx = 1.0;
                JComponent input = buildInputField(fc);
                subtypeFormFields.put(key, input);
                subtypeFieldConfigs.put(key, fc);
                p.add(input, gc);

                subtypeComponents.computeIfAbsent(subtype.discriminatorValue, k -> new ArrayList<>()).add(lbl);
                subtypeComponents.get(subtype.discriminatorValue).add(input);

                col++;
                if (col == 3) { col = 0; row++; }
            }
            if (col != 0) row++;
        }
        updateSubtypeFields();
        return p;
    }

    private String subtypeFieldKey(String subtypeValue, String fieldKey) {
        return subtypeValue + "." + fieldKey;
    }

    private EntityConfig.SubtypeConfig selectedSubtype() {
        if (config.subtypeDiscriminatorKey == null) return null;
        JComponent c = formFields.get(config.subtypeDiscriminatorKey);
        if (!(c instanceof JComboBox)) return null;
        Object selected = ((JComboBox<?>) c).getSelectedItem();
        String value = Objects.toString(selected, "").trim();
        if (value.isEmpty()) return null;
        return config.subtypes.get(value);
    }

    private void updateSubtypeFields() {
        if (config.subtypes.isEmpty()) return;
        EntityConfig.SubtypeConfig activeSubtype = editMode ? null : selectedSubtype();
        String activeValue = activeSubtype == null ? null : activeSubtype.discriminatorValue;

        List<JComponent> headerComponents = subtypeComponents.get("__header__");
        if (headerComponents != null) {
            for (JComponent c : headerComponents) c.setVisible(activeValue != null);
        }
        for (Map.Entry<String, List<JComponent>> entry : subtypeComponents.entrySet()) {
            if ("__header__".equals(entry.getKey())) continue;
            boolean visible = entry.getKey().equals(activeValue);
            for (JComponent c : entry.getValue()) c.setVisible(visible);
        }
        if (subtypePanel != null) subtypePanel.setVisible(activeValue != null);
        revalidate();
        repaint();
    }

    private JComponent buildInputField(EntityConfig.FieldConfig fc) {
        if (fc.type == EntityConfig.FieldType.SELECT && fc.options != null) {
            JComboBox<String> cb = new JComboBox<>();
            cb.addItem(""); for (String o : fc.options) cb.addItem(o);
            styleInput(cb); return cb;
        }
        if (fc.type == EntityConfig.FieldType.BOOLEAN) {
            JComboBox<String> cb = new JComboBox<>(new String[]{"", "Yes", "No"});
            styleInput(cb); return cb;
        }
        if (fc.type == EntityConfig.FieldType.TEXTAREA) {
            JTextArea ta = new JTextArea(3, 20);
            ta.setLineWrap(true); ta.setWrapStyleWord(true);
            ta.setBackground(CARD); ta.setForeground(FG); ta.setCaretColor(ACCENT);
            ta.setFont(ta.getFont().deriveFont(Font.PLAIN, 12f));
            JScrollPane sp = new JScrollPane(ta);
            sp.setBorder(new LineBorder(BORDER, 1, true));
            return sp;
        }
        if (fc.foreignKey != null) {
            JComboBox<String> cb = new JComboBox<>();
            cb.addItem(""); styleInput(cb);
            if (db.isConnected()) loadFkOptions(fc, cb);
            return cb;
        }
        JTextField tf = new JTextField(16);
        styleInput(tf); return tf;
    }

    private void styleInput(JComponent c) {
        c.setBackground(CARD); c.setForeground(FG);
        c.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER, 1, true), new EmptyBorder(3, 6, 3, 6)
        ));
        c.setFont(c.getFont().deriveFont(Font.PLAIN, 12f));
        if (c instanceof JTextField) ((JTextField) c).setCaretColor(ACCENT);
        if (c instanceof JComboBox) {
            JComboBox<?> combo = (JComboBox<?>) c;
            combo.setForeground(FG);
            combo.setBackground(CARD);
            combo.setRenderer(new DefaultListCellRenderer() {
                @Override public Component getListCellRendererComponent(
                        JList<?> l, Object v, int i, boolean s, boolean f) {
                    super.getListCellRendererComponent(l, v, i, s, f);
                    setBackground(s && i >= 0 ? SEL_BG : CARD);
                    setForeground(s && i >= 0 ? ACCENT : FG);
                    setBorder(new EmptyBorder(2, 4, 2, 4));
                    return this;
                }
            });
            combo.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
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
                @Override protected javax.swing.plaf.basic.ComboPopup createPopup() {
                    javax.swing.plaf.basic.BasicComboPopup popup = (javax.swing.plaf.basic.BasicComboPopup) super.createPopup();
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
            combo.setForeground(FG);
            combo.setBackground(CARD);
        }
    }

    //  FK Dropdowns

    private void loadFkOptions(EntityConfig.FieldConfig fc, JComboBox<String> cb) {
        try {
            String sql = "SELECT " + fc.foreignKey.valueField + ", " + fc.foreignKey.labelField
                       + " FROM " + fc.foreignKey.table
                       + " ORDER BY " + fc.foreignKey.labelField;
            for (Map<String, Object> row : db.query(sql)) {
                Object id  = row.get(fc.foreignKey.valueField);
                Object lbl = row.get(fc.foreignKey.labelField);
                cb.addItem(id + " - " + lbl);
            }
        } catch (SQLException e) {
            // Surface the error so permission problems are visible, not silently blank
            showError("Could not load options for \"" + fc.label + "\": " + e.getMessage());
        }
    }

    //  Data Load

    public void loadData() {
        if (!db.isConnected()) return;  // skip if no DB
        tableModel.setRowCount(0); currentRows.clear();
        try {
            String sql = "SELECT * FROM " + config.table
                       + " ORDER BY " + String.join(", ", config.primaryKeys) + " DESC LIMIT 500";
            List<Map<String, Object>> rows = db.query(sql);
            currentRows.addAll(rows);
            for (Map<String, Object> row : rows) {
                List<Object> rd = new ArrayList<>();
                for (String pk : config.primaryKeys) rd.add(row.get(pk));
                for (EntityConfig.FieldConfig fc : config.fields)
                    rd.add(Objects.toString(row.get(fc.key), ""));
                tableModel.addRow(rd.toArray());
            }
        } catch (SQLException e) { showError("Load failed: " + e.getMessage()); }
    }

    private void filterTable(String query) {
        tableModel.setRowCount(0);
        String q = query.toLowerCase();
        for (Map<String, Object> row : currentRows) {
            boolean match = q.isEmpty() || row.values().stream()
                .anyMatch(v -> v != null && v.toString().toLowerCase().contains(q));
            if (match) {
                List<Object> rd = new ArrayList<>();
                for (String pk : config.primaryKeys) rd.add(row.get(pk));
                for (EntityConfig.FieldConfig fc : config.fields)
                    rd.add(Objects.toString(row.get(fc.key), ""));
                tableModel.addRow(rd.toArray());
            }
        }
    }

    //  CRUD

    private void openAddForm() {
        if (!db.isConnected()) { showError("No database connection."); return; }
        if (!config.allowInsert) {
            showError(config.label + " records are created from the parent tab.");
            return;
        }
        editMode = false; editingRow = -1;
        formTitle.setText("Add " + config.label);
        clearForm(); refreshFkDropdowns();
        updateSubtypeFields();
        // Warn if a required FK dropdown is empty (parent record must be added first)
        for (EntityConfig.FieldConfig fc : config.fields) {
            if (fc.foreignKey != null && fc.required) {
                JComponent c = formFields.get(fc.key);
                if (c instanceof JComboBox) {
                    @SuppressWarnings("unchecked") JComboBox<String> cb = (JComboBox<String>) c;
                    if (cb.getItemCount() <= 1) {
                        showError("No " + fc.label + " records found.\n"
                            + "Please add a " + fc.label + " first before adding a " + config.label + ".");
                        return;
                    }
                }
            }
        }
        setFormVisible(true);
    }

    private void openEditForm() {
        if (!db.isConnected()) { showError("No database connection."); return; }
        int sel = table.getSelectedRow();
        if (sel < 0) { showError("Select a row to edit."); return; }
        editMode = true; editingRow = sel;
        formTitle.setText("Edit " + config.label);
        Map<String, Object> row = findRowByPkValues(getPkValuesFromTable(sel));
        if (row == null) { showError("Row not found."); return; }
        refreshFkDropdowns(); populateForm(row); updateSubtypeFields(); setFormVisible(true);
    }

    private void deleteSelected() {
        if (!db.isConnected()) { showError("No database connection."); return; }
        int sel = table.getSelectedRow();
        if (sel < 0) { showError("Select a row to delete."); return; }
        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete this " + config.label + "?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        List<Object> pkValues = getPkValuesFromTable(sel);
        try {
            db.update("DELETE FROM " + config.table + " WHERE " + buildPkWhereClause(), pkValues.toArray());
            loadData(); showSuccess("Deleted successfully.");
        } catch (SQLException e) { showError("Delete failed: " + e.getMessage()); }
    }

    private void saveRecord() {
        if (!db.isConnected()) { showError("No database connection."); return; }
        Map<String, Object> values = collectFormValues();
        if (values == null) return;
        try {
            if (!editMode) {
                if (!config.subtypes.isEmpty()) {
                    if (!saveCompositeRecord(values)) return;
                } else {
                    insertValues(config.table, values);
                }
                showSuccess(config.label + " added!");
            } else {
                List<Object> pkValues = getPkValuesFromTable(editingRow);
                List<String> sets = new ArrayList<>();
                List<Object> params = new ArrayList<>();
                for (Map.Entry<String, Object> e : values.entrySet()) {
                    sets.add(e.getKey() + " = ?"); params.add(e.getValue());
                }
                params.addAll(pkValues);
                db.update("UPDATE " + config.table + " SET " + String.join(", ", sets)
                        + " WHERE " + buildPkWhereClause(), params.toArray());
                showSuccess(config.label + " updated!");
            }
            setFormVisible(false); loadData();
        } catch (SQLException e) { showError("Save failed: " + e.getMessage()); }
    }

    private long insertValues(String tableName, Map<String, Object> values) throws SQLException {
        List<String> cols = new ArrayList<>(values.keySet());
        String sql = "INSERT INTO " + tableName + " (" + String.join(", ", cols) + ") VALUES ("
                   + String.join(", ", java.util.Collections.nCopies(cols.size(), "?")) + ")";
        return db.update(sql, values.values().toArray());
    }

    private long insertValuesAndReturnGeneratedId(String tableName, Map<String, Object> values) throws SQLException {
        List<String> cols = new ArrayList<>(values.keySet());
        String sql = "INSERT INTO " + tableName + " (" + String.join(", ", cols) + ") VALUES ("
                   + String.join(", ", java.util.Collections.nCopies(cols.size(), "?")) + ")";
        return db.insertAndReturnGeneratedId(sql, values.values().toArray());
    }

    private boolean saveCompositeRecord(Map<String, Object> parentValues) throws SQLException {
        EntityConfig.SubtypeConfig subtype = selectedSubtype();
        if (subtype == null) {
            throw new SQLException("Select a valid subtype for " + config.label + ".");
        }
        Map<String, Object> subtypeValues = collectSubtypeValues(subtype);
        if (subtypeValues == null) return false;

        db.inTransaction(() -> {
            long parentId = insertValuesAndReturnGeneratedId(config.table, parentValues);
            if (parentId < 0) {
                throw new SQLException("Could not read generated " + config.primaryKey + ".");
            }
            Map<String, Object> childValues = new LinkedHashMap<>();
            childValues.put(subtype.parentKey, parentId);
            childValues.putAll(subtypeValues);
            if (subtypeKeyExists(subtype, parentId)) {
                updateSubtypeValues(subtype, parentId, subtypeValues);
            } else {
                insertValues(subtype.table, childValues);
            }
            return parentId;
        });
        return true;
    }

    private boolean subtypeKeyExists(EntityConfig.SubtypeConfig subtype, long parentId) throws SQLException {
        String sql = "SELECT COUNT(*) AS row_count FROM " + subtype.table + " WHERE " + subtype.parentKey + " = ?";
        List<Map<String, Object>> rows = db.query(sql, parentId);
        if (rows.isEmpty()) return false;
        Object count = rows.get(0).get("row_count");
        return count instanceof Number && ((Number) count).intValue() > 0;
    }

    private void updateSubtypeValues(EntityConfig.SubtypeConfig subtype,
                                     long parentId,
                                     Map<String, Object> subtypeValues) throws SQLException {
        if (subtypeValues.isEmpty()) return;
        List<String> sets = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        for (Map.Entry<String, Object> e : subtypeValues.entrySet()) {
            sets.add(e.getKey() + " = ?");
            params.add(e.getValue());
        }
        params.add(parentId);
        db.update("UPDATE " + subtype.table + " SET " + String.join(", ", sets)
                + " WHERE " + subtype.parentKey + " = ?", params.toArray());
    }

    private void cancelForm() { setFormVisible(false); clearForm(); }

    //  Form Helpers

    private void setFormVisible(boolean v) {
        formPanel.setVisible(v); revalidate(); repaint();
    }

    private void clearForm() {
        clearFields(config.fields, formFields, null);
        for (EntityConfig.SubtypeConfig subtype : config.subtypes.values()) {
            clearFields(subtype.fields, subtypeFormFields, subtype.discriminatorValue);
        }
        updateSubtypeFields();
    }

    private void clearFields(List<EntityConfig.FieldConfig> fields,
                             Map<String, JComponent> components,
                             String subtypeValue) {
        for (EntityConfig.FieldConfig fc : fields) {
            String key = subtypeValue == null ? fc.key : subtypeFieldKey(subtypeValue, fc.key);
            JComponent c = components.get(key);
            if (c instanceof JTextField) ((JTextField)c).setText("");
            else if (c instanceof JComboBox) ((JComboBox<?>)c).setSelectedIndex(0);
            else if (c instanceof JScrollPane) {
                Component inner = ((JScrollPane)c).getViewport().getView();
                if (inner instanceof JTextArea) ((JTextArea)inner).setText("");
            }
        }
    }

    private void populateForm(Map<String, Object> row) {
        for (EntityConfig.FieldConfig fc : config.fields) {
            String val = Objects.toString(row.get(fc.key), "");
            JComponent c = formFields.get(fc.key);
            if (c instanceof JTextField) { ((JTextField)c).setText(val); }
            else if (c instanceof JScrollPane) {
                Component inner = ((JScrollPane)c).getViewport().getView();
                if (inner instanceof JTextArea) ((JTextArea)inner).setText(val);
            } else if (c instanceof JComboBox) {
                @SuppressWarnings("unchecked") JComboBox<String> cb = (JComboBox<String>)c;
                if (fc.type == EntityConfig.FieldType.BOOLEAN) {
                    cb.setSelectedItem("1".equals(val) || "true".equalsIgnoreCase(val) ? "Yes" : "No");
                } else if (fc.foreignKey != null) {
                    for (int i = 0; i < cb.getItemCount(); i++) {
                        String item = (String) cb.getItemAt(i);
                        if (item.startsWith(val + " -") || item.equals(val)) { cb.setSelectedIndex(i); break; }
                    }
                } else { cb.setSelectedItem(val); }
            }
        }
    }

    private void refreshFkDropdowns() {
        if (!db.isConnected()) return;
        refreshFkDropdowns(config.fields, formFields, null);
        for (EntityConfig.SubtypeConfig subtype : config.subtypes.values()) {
            refreshFkDropdowns(subtype.fields, subtypeFormFields, subtype.discriminatorValue);
        }
    }

    private void refreshFkDropdowns(List<EntityConfig.FieldConfig> fields,
                                    Map<String, JComponent> components,
                                    String subtypeValue) {
        for (EntityConfig.FieldConfig fc : fields) {
            if (fc.foreignKey != null) {
                String key = subtypeValue == null ? fc.key : subtypeFieldKey(subtypeValue, fc.key);
                JComponent c = components.get(key);
                if (c instanceof JComboBox) {
                    @SuppressWarnings("unchecked") JComboBox<String> cb = (JComboBox<String>)c;
                    cb.removeAllItems(); cb.addItem(""); loadFkOptions(fc, cb);
                }
            }
        }
    }

    private Map<String, Object> collectFormValues() {
        return collectValues(config.fields, formFields, null);
    }

    private Map<String, Object> collectSubtypeValues(EntityConfig.SubtypeConfig subtype) {
        return collectValues(subtype.fields, subtypeFormFields, subtype.discriminatorValue);
    }

    private Map<String, Object> collectValues(List<EntityConfig.FieldConfig> fields,
                                              Map<String, JComponent> components,
                                              String subtypeValue) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (EntityConfig.FieldConfig fc : fields) {
            String componentKey = subtypeValue == null ? fc.key : subtypeFieldKey(subtypeValue, fc.key);
            JComponent c = components.get(componentKey);
            Object val = null;
            if (c instanceof JTextField) {
                String t = ((JTextField)c).getText().trim(); val = t.isEmpty() ? null : t;
            } else if (c instanceof JScrollPane) {
                Component inner = ((JScrollPane)c).getViewport().getView();
                if (inner instanceof JTextArea) { String t = ((JTextArea)inner).getText().trim(); val = t.isEmpty() ? null : t; }
            } else if (c instanceof JComboBox) {
                @SuppressWarnings("unchecked") JComboBox<String> cb = (JComboBox<String>)c;
                String sel = Objects.toString(cb.getSelectedItem(), "").trim();
                if (!sel.isEmpty()) {
                    if (fc.type == EntityConfig.FieldType.BOOLEAN) val = "Yes".equals(sel) ? 1 : 0;
                    else if (fc.foreignKey != null) {
                        int d = sel.indexOf(" -"); val = d > 0 ? sel.substring(0, d) : sel;
                    } else val = sel;
                }
            }
            if (fc.required && (val == null || val.toString().isEmpty())) {
                showError(fc.label + " is required."); return null;
            }
            values.put(fc.key, val);
        }
        return values;
    }

    private List<Object> getPkValuesFromTable(int rowIndex) {
        List<Object> values = new ArrayList<>();
        for (int i = 0; i < config.primaryKeys.size(); i++) {
            values.add(tableModel.getValueAt(rowIndex, i));
        }
        return values;
    }

    private String buildPkWhereClause() {
        List<String> parts = new ArrayList<>();
        for (String pk : config.primaryKeys) parts.add(pk + " = ?");
        return String.join(" AND ", parts);
    }

    private Map<String, Object> findRowByPkValues(List<Object> pkValues) {
        for (Map<String, Object> row : currentRows) {
            boolean match = true;
            for (int i = 0; i < config.primaryKeys.size(); i++) {
                String pk = config.primaryKeys.get(i);
                if (!Objects.toString(pkValues.get(i), "").equals(Objects.toString(row.get(pk), ""))) {
                    match = false;
                    break;
                }
            }
            if (match) return row;
        }
        return null;
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
    private void showSuccess(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Success", JOptionPane.INFORMATION_MESSAGE);
    }
}
