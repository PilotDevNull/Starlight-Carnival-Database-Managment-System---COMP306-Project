package starlightcarnival;
// ==========================================================
// EntityConfig
// ==========================================================
// This class stores configuration metadata for every entity
// used in the system. It defines database table structures,
// field types, foreign key relationships, subtype inheritance,
// dropdown options, insert permissions, and GUI form behavior.
// The configuration allows forms and CRUD operations to be
// generated dynamically while keeping the Java application
// aligned with the final SQL database structure.
// ==========================================================

import java.util.*;

public class EntityConfig {

    public enum FieldType { TEXT, NUMBER, DATE, TIME, DATETIME, SELECT, BOOLEAN, TEXTAREA }

    public static class ForeignKey {
        public final String table, valueField, labelField;
        public ForeignKey(String table, String valueField, String labelField) {
            this.table = table; this.valueField = valueField; this.labelField = labelField;
        }
    }

    public static class FieldConfig {
        public final String key, label;
        public final FieldType type;
        public final boolean required;
        public final String[] options;
        public final ForeignKey foreignKey;

        public FieldConfig(String key, String label, FieldType type, boolean required,
                           String[] options, ForeignKey fk) {
            this.key = key; this.label = label; this.type = type;
            this.required = required; this.options = options; this.foreignKey = fk;
        }
    }

    public static class SubtypeConfig {
        public final String discriminatorValue, table, parentKey, label;
        public final List<FieldConfig> fields;

        public SubtypeConfig(String discriminatorValue, String table, String parentKey,
                             String label, List<FieldConfig> fields) {
            this.discriminatorValue = discriminatorValue;
            this.table = table;
            this.parentKey = parentKey;
            this.label = label;
            this.fields = Collections.unmodifiableList(fields);
        }
    }

    public final String table, primaryKey, label;
    public final List<String> primaryKeys;
    public final List<FieldConfig> fields;
    public final boolean allowInsert;
    public final String subtypeDiscriminatorKey;
    public final Map<String, SubtypeConfig> subtypes;

    public EntityConfig(String table, String primaryKey, String label, List<FieldConfig> fields) {
        this(table, primaryKey, label, fields, true);
    }

    public EntityConfig(String table, String primaryKey, String label, List<FieldConfig> fields,
                        boolean allowInsert) {
        this(table, primaryKey, label, fields, allowInsert, null, Collections.emptyMap());
    }

    public EntityConfig(String table, String primaryKey, String label, List<FieldConfig> fields,
                        boolean allowInsert, String subtypeDiscriminatorKey,
                        Map<String, SubtypeConfig> subtypes) {
        this.table = table;
        this.primaryKey = primaryKey;
        this.primaryKeys = Collections.singletonList(primaryKey);
        this.label = label;
        this.fields = fields;
        this.allowInsert = allowInsert;
        this.subtypeDiscriminatorKey = subtypeDiscriminatorKey;
        this.subtypes = Collections.unmodifiableMap(new LinkedHashMap<>(subtypes));
    }

    public EntityConfig(String table, String[] primaryKeys, String label, List<FieldConfig> fields) {
        this(table, primaryKeys, label, fields, true);
    }

    public EntityConfig(String table, String[] primaryKeys, String label, List<FieldConfig> fields,
                        boolean allowInsert) {
        this.table = table;
        this.primaryKey = primaryKeys[0];
        this.primaryKeys = Collections.unmodifiableList(Arrays.asList(primaryKeys));
        this.label = label;
        this.fields = fields;
        this.allowInsert = allowInsert;
        this.subtypeDiscriminatorKey = null;
        this.subtypes = Collections.emptyMap();
    }

    // All entities
    public static final Map<String, EntityConfig> ALL = new LinkedHashMap<>();

    static {
        // LOCATION
        ALL.put("location", new EntityConfig("LOCATION", "location_id", "Location", Arrays.asList(
            f("city",           "City",           FieldType.TEXT,   true),
            f("venue_name",     "Venue Name",     FieldType.TEXT,   true),
            f("address",        "Address",        FieldType.TEXT,   false),
            f("arrival_date",   "Arrival Date",   FieldType.DATE,   true),
            f("departure_date", "Departure Date", FieldType.DATE,   true),
            f("rental_cost",    "Rental Cost",    FieldType.NUMBER, false)
        )));

        // CUSTOMER
        ALL.put("customer", new EntityConfig("CUSTOMER", "customer_id", "Customer", Arrays.asList(
            f("first_name",    "First Name",    FieldType.TEXT,   true),
            f("last_name",     "Last Name",     FieldType.TEXT,   true),
            f("email",         "Email",         FieldType.TEXT,   false),
            f("phone",         "Phone",         FieldType.TEXT,   false),
            f("date_of_birth", "Date of Birth", FieldType.DATE,   false),
            fs("age_group",    "Age Group",     false, "Adult","Child","Senior"),
            f("card_balance",  "Card Balance",  FieldType.NUMBER, false),
            f("loyalty_points","Loyalty Points",FieldType.NUMBER, false),
            fs("card_status",  "Card Status",   false, "ACTIVE","SUSPENDED","EXPIRED")
        )));

        // EMPLOYEE
        Map<String, SubtypeConfig> employeeSubtypes = new LinkedHashMap<>();
        employeeSubtypes.put("Operator", st("Operator", "OPERATOR", "employee_id", "Operator", Arrays.asList(
            fs("ride_responsibility", "Ride Responsibility", false, "Thrills","Kiddies","Motion")
        )));
        employeeSubtypes.put("Technician", st("Technician", "TECHNICIAN", "employee_id", "Technician", Arrays.asList(
            fs("specialization", "Specialization", false, "Hydraulics","Electrical","General"),
            f("toolbox_id", "Toolbox ID", FieldType.TEXT, false),
            f("emergency_contact", "Emergency Contact", FieldType.TEXT, false)
        )));
        employeeSubtypes.put("Vendor", st("Vendor", "VENDOR", "employee_id", "Vendor", Arrays.asList(
            fk("stall_id", "Stall", FieldType.NUMBER, false, "STALL","attraction_id","lot_number")
        )));
        employeeSubtypes.put("Performer", st("Performer", "PERFORMER", "employee_id", "Performer", Arrays.asList(
            fk("show_id", "Show", FieldType.NUMBER, false, "`SHOW`","attraction_id","genre")
        )));
        employeeSubtypes.put("General", st("General", "GENERAL_STAFF", "employee_id", "General Staff", Arrays.asList(
            fs("role", "Role", false, "Sales","Manager","Cleaner","Security")
        )));
        ALL.put("employee", new EntityConfig("EMPLOYEE", "employee_id", "Employee", Arrays.asList(
            f("first_name",          "First Name",  FieldType.TEXT,   true),
            f("last_name",           "Last Name",   FieldType.TEXT,   true),
            f("hire_date",           "Hire Date",   FieldType.DATE,   false),
            f("hourly_wage",         "Hourly Wage", FieldType.NUMBER, false),
            fk("current_location_id","Location",    FieldType.NUMBER, false, "LOCATION","location_id","venue_name"),
            fs("employee_type","Type",true,"General","Operator","Technician","Vendor","Performer")
        ), true, "employee_type", employeeSubtypes));

        // ATTRACTION
        Map<String, SubtypeConfig> attractionSubtypes = new LinkedHashMap<>();
        attractionSubtypes.put("Ride", st("Ride", "RIDE", "attraction_id", "Ride", Arrays.asList(
            fs("ride_type", "Ride Type", false, "Thrills","Kiddies","Motion"),
            fs("age_requirement", "Age Requirement", false, "Adult","Kids","All Ages")
        )));
        attractionSubtypes.put("Show", st("Show", "`SHOW`", "attraction_id", "Show", Arrays.asList(
            f("genre", "Genre", FieldType.TEXT, false),
            f("duration_min", "Duration (min)", FieldType.NUMBER, false),
            f("is_indoor", "Indoor", FieldType.BOOLEAN, false),
            f("seating_capacity", "Seating Capacity", FieldType.NUMBER, false)
        )));
        attractionSubtypes.put("Stall", st("Stall", "STALL", "attraction_id", "Stall", Arrays.asList(
            f("lot_number", "Lot Number", FieldType.TEXT, false),
            fs("stall_type", "Stall Type", false, "Food","Merchandise","Game")
        )));
        ALL.put("attraction", new EntityConfig("ATTRACTION", "attraction_id", "Attraction", Arrays.asList(
            f("name",                "Name",   FieldType.TEXT,   true),
            fs("status",             "Status", true,  "Operational","Closed","Maintenance"),
            fk("current_location_id","Location",FieldType.NUMBER,false,"LOCATION","location_id","venue_name"),
            fs("attraction_type","Type",true,"Ride","Show","Stall")
        ), true, "attraction_type", attractionSubtypes));

        // RIDE
        ALL.put("ride", new EntityConfig("RIDE", "attraction_id", "Ride", Arrays.asList(
            fk("attraction_id",  "Attraction",      FieldType.NUMBER, true, "ATTRACTION","attraction_id","name"),
            fs("ride_type",      "Ride Type",       false, "Thrills","Kiddies","Motion"),
            fs("age_requirement", "Age Requirement", false, "Adult","Kids","All Ages")
        ), false));

        // SHOW
        ALL.put("show", new EntityConfig("`SHOW`", "attraction_id", "Show", Arrays.asList(
            fk("attraction_id",  "Attraction",       FieldType.NUMBER, true, "ATTRACTION","attraction_id","name"),
            f("genre",           "Genre",            FieldType.TEXT,   false),
            f("duration_min",    "Duration (min)",   FieldType.NUMBER, false),
            f("is_indoor",       "Indoor",           FieldType.BOOLEAN,false),
            f("seating_capacity","Seating Capacity", FieldType.NUMBER, false)
        ), false));

        // STALL
        ALL.put("stall", new EntityConfig("STALL", "attraction_id", "Stall", Arrays.asList(
            fk("attraction_id","Attraction", FieldType.NUMBER, true, "ATTRACTION","attraction_id","name"),
            f("lot_number",    "Lot Number", FieldType.TEXT, false),
            fs("stall_type",   "Stall Type", false, "Food","Merchandise","Game")
        ), false));

        // ADMISSION
        ALL.put("admission", new EntityConfig("ADMISSION", "admission_id", "Admission", Arrays.asList(
            fk("location_id",  "Location",        FieldType.NUMBER, true,  "LOCATION","location_id","venue_name"),
            fk("customer_id",  "Customer",         FieldType.NUMBER, false, "CUSTOMER","customer_id","first_name"),
            f("entry_date",    "Entry Date",       FieldType.DATE,   false),
            f("entry_time",    "Entry Time",       FieldType.TIME,   false),
            fs("admission_type","Admission Type",  false, "Adult","Child","Senior"),
            f("entry_fee_paid","Entry Fee Paid",   FieldType.NUMBER, false),
            f("exit_time",     "Exit Time",        FieldType.TIME,   false)
        )));

        // TRANSACTION
        ALL.put("transaction", new EntityConfig("`TRANSACTION`", "transaction_id", "Transaction", Arrays.asList(
            fk("location_id",      "Location",  FieldType.NUMBER, false, "LOCATION","location_id","venue_name"),
            fk("employee_id",      "Employee",  FieldType.NUMBER, false, "EMPLOYEE","employee_id","first_name"),
            fk("customer_id",      "Customer",  FieldType.NUMBER, false, "CUSTOMER","customer_id","first_name"),
            f("transaction_date",  "Date",      FieldType.DATETIME, false),
            fs("transaction_type", "Type",      false, "Entry Fee","Card Top-Up","Stall Purchase","Refund","Access Pass"),
            f("total_amount",      "Total Amount",FieldType.NUMBER,false)
        )));

        // PAYMENT
        ALL.put("payment", new EntityConfig("PAYMENT", "payment_id", "Payment", Arrays.asList(
            fk("transaction_id","Transaction", FieldType.NUMBER, false, "`TRANSACTION`","transaction_id","transaction_id"),
            f("amount",         "Amount",      FieldType.NUMBER, false),
            fs("tender_type",   "Tender Type", false, "Cash","Credit Card","Debit Card","Smart Card")
        )));

        // ACCESS_PASS
        Map<String, SubtypeConfig> accessPassSubtypes = new LinkedHashMap<>();
        accessPassSubtypes.put("RideFastPass", st("RideFastPass", "RIDE_FAST_PASS", "pass_id", "Ride Fast Pass", Arrays.asList(
            fk("ride_id", "Ride", FieldType.NUMBER, true, "RIDE","attraction_id","attraction_id"),
            f("time_slot", "Time Slot", FieldType.DATETIME, true)
        )));
        accessPassSubtypes.put("ShowReservation", st("ShowReservation", "SHOW_RESERVATION", "pass_id", "Show Reservation", Arrays.asList(
            fk("show_id", "Show", FieldType.NUMBER, true, "`SHOW`","attraction_id","genre"),
            f("seat_number", "Seat Number", FieldType.TEXT, false),
            f("show_time", "Show Time", FieldType.DATETIME, true)
        )));
        ALL.put("access_pass", new EntityConfig("ACCESS_PASS", "pass_id", "Access Pass", Arrays.asList(
            fk("transaction_id","Transaction", FieldType.NUMBER, true, "`TRANSACTION`","transaction_id","transaction_id"),
            fk("customer_id",   "Customer",    FieldType.NUMBER, false, "CUSTOMER","customer_id","first_name"),
            f("price",          "Price",       FieldType.NUMBER, false),
            f("issue_time",     "Issue Time",  FieldType.DATETIME, false),
            f("is_redeemed",    "Redeemed",    FieldType.BOOLEAN,false),
            fs("pass_type",     "Pass Type",   true, "RideFastPass","ShowReservation")
        ), true, "pass_type", accessPassSubtypes));

        // SHOW_RESERVATION
        ALL.put("show_reservation", new EntityConfig("SHOW_RESERVATION", "pass_id", "Show Reservation", Arrays.asList(
            fk("pass_id",     "Access Pass", FieldType.NUMBER, true,  "ACCESS_PASS","pass_id","pass_id"),
            fk("show_id",     "Show",        FieldType.NUMBER, true, "`SHOW`","attraction_id","genre"),
            f("seat_number",  "Seat Number", FieldType.TEXT,   false),
            f("show_time",    "Show Time",   FieldType.DATETIME, true)
        ), false));

        // RIDE_FAST_PASS
        ALL.put("ride_fast_pass", new EntityConfig("RIDE_FAST_PASS", "pass_id", "Ride Fast Pass", Arrays.asList(
            fk("pass_id",  "Access Pass", FieldType.NUMBER, true,  "ACCESS_PASS","pass_id","pass_id"),
            fk("ride_id",  "Ride",        FieldType.NUMBER, true, "RIDE","attraction_id","attraction_id"),
            f("time_slot", "Time Slot",   FieldType.DATETIME, true)
        ), false));

        // OPERATOR
        ALL.put("operator", new EntityConfig("OPERATOR", "employee_id", "Operator", Arrays.asList(
            fk("employee_id","Employee", FieldType.NUMBER, true,  "EMPLOYEE","employee_id","first_name"),
            fs("ride_responsibility", "Ride Responsibility", false, "Thrills","Kiddies","Motion")
        ), false));

        // TECHNICIAN
        ALL.put("technician", new EntityConfig("TECHNICIAN", "employee_id", "Technician", Arrays.asList(
            fk("employee_id",      "Employee",         FieldType.NUMBER, true,  "EMPLOYEE","employee_id","first_name"),
            fs("specialization",    "Specialization",  false, "Hydraulics","Electrical","General"),
            f("toolbox_id",        "Toolbox ID",       FieldType.TEXT, false),
            f("emergency_contact", "Emergency Contact",FieldType.TEXT,   false)
        ), false));

        // VENDOR
        ALL.put("vendor", new EntityConfig("VENDOR", "employee_id", "Vendor", Arrays.asList(
            fk("employee_id","Employee", FieldType.NUMBER, true,  "EMPLOYEE","employee_id","first_name"),
            fk("stall_id",   "Stall",    FieldType.NUMBER, false, "STALL","attraction_id","lot_number")
        ), false));

        // PERFORMER
        ALL.put("performer", new EntityConfig("PERFORMER", "employee_id", "Performer", Arrays.asList(
            fk("employee_id","Employee", FieldType.NUMBER, true,  "EMPLOYEE","employee_id","first_name"),
            fk("show_id",    "Show",     FieldType.NUMBER, false, "`SHOW`","attraction_id","genre")
        ), false));

        // GENERAL_STAFF
        ALL.put("general_staff", new EntityConfig("GENERAL_STAFF", "employee_id", "General Staff", Arrays.asList(
            fk("employee_id","Employee", FieldType.NUMBER, true, "EMPLOYEE","employee_id","first_name"),
            fs("role","Role",false,"Sales","Manager","Cleaner","Security")
        ), false));

        // PRODUCT
        ALL.put("product", new EntityConfig("PRODUCT", "product_id", "Product", Arrays.asList(
            f("name",     "Name",     FieldType.TEXT,   true),
            fs("category", "Category", false, "Food","Beverage","Toy","Apparel"),
            f("price",    "Price",    FieldType.NUMBER, false)
        )));

        // STALL_STOCK
        ALL.put("stall_stock", new EntityConfig("STALL_STOCK", new String[]{"product_id", "stall_id"}, "Stall Stock", Arrays.asList(
            fk("product_id",    "Product",        FieldType.NUMBER, true, "PRODUCT","product_id","name"),
            fk("stall_id",      "Stall",          FieldType.NUMBER, true, "STALL","attraction_id","lot_number"),
            f("stock_quantity", "Stock Quantity",  FieldType.NUMBER, false)
        )));

        // MAINTENANCE_LOG
        ALL.put("maintenance_log", new EntityConfig("MAINTENANCE_LOG", "log_id", "Maintenance Log", Arrays.asList(
            fk("ride_id",         "Ride",          FieldType.NUMBER,  false, "RIDE","attraction_id","attraction_id"),
            fk("technician_id",   "Technician",    FieldType.NUMBER,  false, "TECHNICIAN","employee_id","specialization"),
            f("maintenance_date", "Date",           FieldType.DATETIME,false),
            f("issue_found",      "Issue Found",    FieldType.TEXTAREA,false),
            f("action_taken",     "Action Taken",   FieldType.TEXTAREA,false),
            f("parts_cost",       "Parts Cost",     FieldType.NUMBER,  false)
        )));
    }

    // Builders
    private static FieldConfig f(String key, String label, FieldType type, boolean req) {
        return new FieldConfig(key, label, type, req, null, null);
    }
    private static FieldConfig fs(String key, String label, boolean req, String... opts) {
        return new FieldConfig(key, label, FieldType.SELECT, req, opts, null);
    }
    private static FieldConfig fk(String key, String label, FieldType type, boolean req,
                                   String table, String valueField, String labelField) {
        return new FieldConfig(key, label, type, req, null, new ForeignKey(table, valueField, labelField));
    }
    private static SubtypeConfig st(String discriminatorValue, String table, String parentKey,
                                    String label, List<FieldConfig> fields) {
        return new SubtypeConfig(discriminatorValue, table, parentKey, label, fields);
    }
}
