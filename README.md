# Starlight Carnival Management System
<img width="3359" height="2207" alt="1142" src="https://github.com/user-attachments/assets/4e773bdb-324a-42b5-828d-d89516a5227b" />

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
<img width="873" height="554" alt="Screenshot 2026-07-29 190920" src="https://github.com/user-attachments/assets/9c184a88-7959-40e5-839f-8bb95b3be8af" />
<img width="1272" height="789" alt="Screenshot 2026-07-29 191000" src="https://github.com/user-attachments/assets/5258bb68-922c-43f0-a2e6-a773afe84385" />
<img width="1275" height="780" alt="Screenshot 2026-07-29 191010" src="https://github.com/user-attachments/assets/75ac81e9-4c72-4202-bf55-9d728f75102d" />
<img width="1273" height="787" alt="Screenshot 2026-07-29 191022" src="https://github.com/user-attachments/assets/c0321ce8-7334-4d41-aab7-fca91e259aaf" />
<img width="875" height="631" alt="Screenshot 2026-07-29 191045" src="https://github.com/user-attachments/assets/4c45d3e0-3886-489f-9788-e710b7996ac3" />
<img width="874" height="625" alt="Screenshot 2026-07-29 191054" src="https://github.com/user-attachments/assets/f8a544d0-6114-47f6-b1d0-2fdf834bb8eb" />
<img width="879" height="632" alt="Screenshot 2026-07-29 191118" src="https://github.com/user-attachments/assets/ac65431f-3007-4b83-82ca-ae43a2e64e78" />
<img width="937" height="670" alt="Screenshot 2026-07-29 191137" src="https://github.com/user-attachments/assets/c04b4241-2308-40c8-ad9c-2f39c60c054a" />
<img width="931" height="659" alt="Screenshot 2026-07-29 191145" src="https://github.com/user-attachments/assets/7c9678d9-4e97-4a38-b3ce-2c5247dc1fcf" />
<img width="938" height="671" alt="Screenshot 2026-07-29 191158" src="https://github.com/user-attachments/assets/c8319097-bec0-44ca-aa5c-3e920ad1e29d" />
<img width="1276" height="780" alt="Screenshot 2026-07-29 191223" src="https://github.com/user-attachments/assets/eb92d40a-e5cd-49f3-b950-6879fb0153c8" />
