# Starlight Carnival Management System
# See Project Documentation for More information

Java Swing desktop application for the COMP306 Starlight Carnival database project.

## Project Structure

```text
Starlight_Carnival/
|-- src/starlightcarnival/
|   |-- Main.java
|   |-- LoginFrame.java
|   |-- MainFrame.java
|   |-- EntityCrudPanel.java
|   |-- ReportsPanel.java
|   |-- EntityConfig.java
|   `-- DatabaseManager.java
|-- lib/mysql-connector-j.jar
|-- starlight_carnival.sql
|-- run.bat----
```

## Setup

1. Run `starlight_carnival.sql` in MySQL before starting the application.
## How to Run the `starlight_carnival.sql`
## Use the following commands to add and run the database
1. Open MySQLShell
2. `\connect root@localhost`  and enter your local database password
3. create the database with `CREATE DATABASE starlight_carnival;`
4. Run `USE starlight_carnival;` to select the database
5. Use `SOURCE C: YOUR FILE PATH HERE TO THE .SQL`
   Example: `SOURCE C:/Users/PilotDrake1/Desktop/Starlight Carnival Complete/Starlight Carnival/starlight_carnival.sql;`

2. Confirm `lib/mysql-connector-j.jar` exists in the lib folder (It should be there since it was place by us)
## NOTE, To just run the program you can use the run.bat wihout importing it into IntelliJ MAKE SURE the .SQL database has first been added as you will get an error when logging in with the correct details.
3. Start the app with `run.bat` on Windows

The SQL file creates database accounts for the application:

| Username | Password | Role |
| --- | --- | --- |
| `starlight_admin` | `admin123` | Admin |
| `starlight_sales` | `sales123` | Sales |
| `starlight_maintenance` | `maint123` | Maintenance |
| `starlight_guest` | `guest123` | Read-only guest |

These are MySQL accounts, not rows in a business `USER` table.

## Database Summary

The schema includes the core carnival entities, three disjoint subtype hierarchies, role-oriented views, stored procedures, and triggers.

- `ATTRACTION` has exactly one functional subtype: `RIDE`, `SHOW`, or `STALL`.
- `EMPLOYEE` has exactly one functional subtype: `OPERATOR`, `TECHNICIAN`, `VENDOR`, `PERFORMER`, or `GENERAL_STAFF`.
- `ACCESS_PASS` is specialized through `RIDE_FAST_PASS` and `SHOW_RESERVATION`.

Stored procedures: `AddLoyaltyPoints`, `DeductBalance`  
Triggers include payment validation, loyalty-point updates, and subtype-disjointness checks.
