/*
 * Template JAVA User Interface
 * =============================
 *
 * Database Management Systems
 * Department of Computer Science & Engineering
 * University of California - Riverside
 *
 * Target DBMS: 'Postgres'
 */

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.sql.Date;
import java.sql.Array;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class EmbeddedSQL {

   private Connection _connection = null;

   static BufferedReader in = new BufferedReader(
                                new InputStreamReader(System.in));

   public EmbeddedSQL (String dbname, String dbport, String user, String passwd) throws SQLException {
      System.out.print("Connecting to database...");
      try{
         String url = "jdbc:postgresql://localhost:" + dbport + "/" + dbname;
         System.out.println ("Connection URL: " + url + "\n");

         this._connection = DriverManager.getConnection(url, user, passwd);
         System.out.println("Done");
      }catch (Exception e){
         System.err.println("Error - Unable to Connect to Database: " + e.getMessage() );
         System.out.println("Make sure you started postgres on this machine");
         System.exit(-1);
      }
   }

   public Connection getConnection() {
      return this._connection;
   }

   public void executeUpdate (String sql) throws SQLException {
      Statement stmt = this._connection.createStatement ();
      stmt.executeUpdate (sql);
      stmt.close ();
   }

   public int executeQuery (String query) throws SQLException {
      Statement stmt = this._connection.createStatement ();
      ResultSet rs = stmt.executeQuery (query);
      int rowCount = printResultSet(rs);
      stmt.close ();
      return rowCount;
   }

   public static int printResultSet(ResultSet rs) throws SQLException {
      ResultSetMetaData rsmd = rs.getMetaData();
      int numCol = rsmd.getColumnCount();

      // Store headers and compute initial column widths
      String[] headers = new String[numCol];
      int[] colWidths = new int[numCol];
      for (int i = 0; i < numCol; i++) {
         headers[i] = rsmd.getColumnLabel(i + 1);
         colWidths[i] = headers[i].length();
      }

      // Buffer all rows and expand column widths to fit the longest value in each column
      List<String[]> rows = new ArrayList<>();
      while (rs.next()) {
         String[] row = new String[numCol];
         for (int i = 0; i < numCol; i++) {
            String val = rs.getString(i + 1);
            if (val == null) {
               val = "NULL";
            }
            row[i] = val;
            if (val.length() > colWidths[i]) {
               colWidths[i] = val.length();
            }
         }
         rows.add(row);
      }

      if (rows.isEmpty()) {
         System.out.println("(No records found)");
         return 0;
      }

      // Build horizontal border line (e.g., +-----+--------+-----------+)
      StringBuilder border = new StringBuilder("+");
      for (int w : colWidths) {
         for (int j = 0; j < w + 2; j++) {
            border.append("-");
         }
         border.append("+");
      }
      String borderLine = border.toString();

      // Print table header
      System.out.println(borderLine);
      System.out.print("|");
      for (int i = 0; i < numCol; i++) {
         System.out.printf(" %-" + colWidths[i] + "s |", headers[i]);
      }
      System.out.println();
      System.out.println(borderLine);

      // Print table rows
      for (String[] row : rows) {
         System.out.print("|");
         for (int i = 0; i < numCol; i++) {
            System.out.printf(" %-" + colWidths[i] + "s |", row[i]);
         }
         System.out.println();
      }
      System.out.println(borderLine);

      return rows.size();
   }

   public static int printDetailedResultSet(ResultSet rs) throws SQLException {
      ResultSetMetaData rsmd = rs.getMetaData();
      int numCol = rsmd.getColumnCount();
      int rowCount = 0;

      // Compute max label width so scalar fields align cleanly
      int maxLabelWidth = 0;
      String[] labels = new String[numCol];
      int[] colTypes = new int[numCol];
      for (int i = 0; i < numCol; i++) {
         labels[i] = rsmd.getColumnLabel(i + 1);
         colTypes[i] = rsmd.getColumnType(i + 1);
         if (labels[i].length() > maxLabelWidth) {
            maxLabelWidth = labels[i].length();
         }
      }

      while (rs.next()) {
         rowCount++;
         System.out.println("================================================================================");
         System.out.println(" RECORD #" + rowCount);
         System.out.println("================================================================================");

         for (int i = 0; i < numCol; i++) {
            String label = labels[i];
            String val = rs.getString(i + 1);

            if (val == null) {
               System.out.printf("  %-" + maxLabelWidth + "s : NULL%n", label);
               continue;
            }

            // 1. PostgreSQL Array columns (e.g., list_of_plays, list_of_stats, schedule, field_goal_kick_lengths)
            if (colTypes[i] == Types.ARRAY) {
               System.out.printf("  %-" + maxLabelWidth + "s :%n", label);
               Array sqlArr = rs.getArray(i + 1);
               Object rawArr = (sqlArr != null) ? sqlArr.getArray() : null;
               if (rawArr instanceof Object[]) {
                  Object[] items = (Object[]) rawArr;
                  if (items.length == 0) {
                     System.out.println("      (empty list)");
                  } else {
                     for (Object item : items) {
                        System.out.println("      - " + item);
                     }
                  }
               } else {
                  // Fallback for older JDBC drivers that return "{a,b,c}" as a String
                  String stripped = val.replaceAll("^\\{|\\}$", "");
                  if (stripped.isEmpty()) {
                     System.out.println("      (empty list)");
                  } else {
                     for (String item : stripped.split(",")) {
                        System.out.println("      - " + item.replaceAll("^\"|\"$", "").trim());
                     }
                  }
               }
            }
            // 2. Long semicolon-separated text logs (e.g., play_by_play, home_team_plays, away_team_plays)
            else if (val.contains(";") && val.length() > 45) {
               System.out.printf("  %-" + maxLabelWidth + "s :%n", label);
               for (String item : val.split(";")) {
                  String trimmed = item.trim();
                  if (!trimmed.isEmpty()) {
                     System.out.println("      - " + trimmed);
                  }
               }
            }
            // 3. Standard single-line attributes
            else {
               System.out.printf("  %-" + maxLabelWidth + "s : %s%n", label, val);
            }
         }
         System.out.println();
      }

      if (rowCount == 0) {
         System.out.println("(No records found)");
      }
      return rowCount;
   }

   private static boolean hasListOrLongColumns(ResultSetMetaData rsmd) throws SQLException {
      int numCol = rsmd.getColumnCount();
      // Wide tables (more than 10 columns, such as Team_Players, Coaches, or cumulative stats)
      // overflow standard terminal widths and wrap awkwardly, so render them vertically.
      if (numCol > 10) {
         return true;
      }
      for (int i = 1; i <= numCol; i++) {
         int type = rsmd.getColumnType(i);
         String colName = rsmd.getColumnLabel(i).toLowerCase();
         if (type == Types.ARRAY
             || colName.contains("play")
             || colName.contains("list_")
             || colName.equals("schedule")
             || colName.equals("analysis")
             || colName.equals("game_plan")
             || colName.equals("diagnosis")) {
            return true;
         }
      }
      return false;
   }

   private static int printSmartResultSet(ResultSet rs) throws SQLException {
      if (hasListOrLongColumns(rs.getMetaData())) {
         return printDetailedResultSet(rs);
      } else {
         return printResultSet(rs);
      }
   }

   public void cleanup(){
      try{
         if (this._connection != null){
            this._connection.close ();
         }
      }catch (SQLException e){
         // ignored.
      }
   }


   // =========================================================================
   // STARTUP NORMALIZATION: CONVERT STORED STRINGS TO LOWERCASE
   // =========================================================================

   private static final java.util.Set<String> ENUM_CHECK_COLUMNS = new java.util.HashSet<>(
      Arrays.asList("morning_afternoon", "regular_practice_squad", "defensive_offensive", "focus", "excused_unexcused")
   );

   public static void normalizeDatabaseStringsToLower(Connection conn) {
      System.out.print("Normalizing database text records to lowercase...");
      try (Statement stmt = conn.createStatement()) {
         for (int t = 1; t <= 26; t++) {
            SearchTableConfig cfg = getTableConfig(t);
            if (cfg == null) continue;

            List<String> standardSetClauses = new ArrayList<>();
            List<SearchColumn> enumCols = new ArrayList<>();

            for (SearchColumn c : cfg.columns) {
               if (c.type == ColType.STRING) {
                  if (ENUM_CHECK_COLUMNS.contains(c.label)) {
                     enumCols.add(c);
                  } else {
                     standardSetClauses.add(c.label + " = LOWER(" + c.label + ")");
                  }
               } else if (c.type == ColType.STRING_ARRAY) {
                  standardSetClauses.add(
                     c.label + " = CASE WHEN " + c.label + " IS NULL THEN NULL " +
                     "ELSE ARRAY(SELECT LOWER(elem) FROM unnest(" + c.label + ") AS elem) END"
                  );
               }
            }

            if (!standardSetClauses.isEmpty()) {
               String sql = "UPDATE " + cfg.rawTableName + " SET " + String.join(", ", standardSetClauses);
               try {
                  stmt.executeUpdate(sql);
               } catch (SQLException ignored) {
                  // Skip if a specific table/column has a custom constraint or trigger
               }
            }

            // Attempt enum-constrained columns individually so a CHECK constraint on one
            // never blocks normalizing other columns
            for (SearchColumn ec : enumCols) {
               try {
                  stmt.executeUpdate("UPDATE " + cfg.rawTableName + " SET " + ec.label + " = LOWER(" + ec.label + ")");
               } catch (SQLException ignored) {
                  // Preserve database-enforced enum casing if a CHECK constraint requires Title Case
               }
            }
         }
         System.out.println("Done");
      } catch (SQLException e) {
         System.err.println("\n[Warning] Could not normalize all text columns: " + e.getMessage());
      }
   }

   public static void main (String[] args) {
      if (args.length != 3) {
         System.err.println (
            "Usage: " +
            "java [-classpath <classpath>] " +
            EmbeddedSQL.class.getName () +
            " <dbname> <port> <user>");
         return;
      }
      
      Greeting();
      EmbeddedSQL esql = null;
      try{
         Class.forName ("org.postgresql.Driver").newInstance ();
         String dbname = args[0];
         String dbport = args[1];
         String user = args[2];
         esql = new EmbeddedSQL (dbname, dbport, user, "");
         normalizeDatabaseStringsToLower(esql.getConnection());

         boolean keepon = true;
         while(keepon) {
            System.out.println("\nMAIN MENU");
            System.out.println("---------");
            System.out.println("0. Insert a new record into any table");
            System.out.println("1. Search and retrieve records across any table");
            System.out.println("2. Update existing records in any table (with before & after view)");
            System.out.println("3. View franchise payroll (vw_franchise_payroll)");
            System.out.println("4. Query 4");
            System.out.println("5. Query 5");
            System.out.println("6. Query 6");
            System.out.println("9. < EXIT");

            switch (readChoice()){
               case 0: QueryExample(esql); break;
               case 1: Query1(esql); break;
               case 2: Query2(esql); break;
               case 3: Query3(esql); break;
               case 4: Query4(esql); break;
               case 5: Query5(esql); break;
               case 6: Query6(esql); break;
               case 9: keepon = false; break;
               default : System.out.println("Unrecognized choice!"); break;
            }
         }
      }catch(Exception e) {
         System.err.println (e.getMessage ());
      }finally{
         try{
            if(esql != null) {
               System.out.print("Disconnecting from database...");
               esql.cleanup ();
               System.out.println("Done\n\nBye !");
            }
         }catch (Exception e) {
            // ignored.
         }
      }
   }
   
   public static void Greeting(){
      System.out.println(
         "\n\n*******************************************************\n" +
         "              User Interface      	               \n" +
         "*******************************************************\n");
   }

   public static int readChoice() {
      int input;
      do {
         System.out.print("Please make your choice: ");
         try {
            input = Integer.parseInt(in.readLine().trim());
            break;
         }catch (Exception e) {
            System.out.println("Your input is invalid!");
         }
      }while (true);
      return input;
   }

   // =========================================================================
   // SHARED TABLE MENU DISPLAY
   // =========================================================================

   private static void printAllTablesMenu(String headerTitle) {
      System.out.println("\n--- " + headerTitle + " ---");
      System.out.println("Principal Entities:");
      System.out.println("  1. Managers                  6. Team_Doctors");
      System.out.println("  2. Team_Players              7. Trainers");
      System.out.println("  3. Opposing_Players          8. Regular_Staff");
      System.out.println("  4. Coaches                   9. Games");
      System.out.println("  5. Statistical_Team         10. Practices");
      System.out.println("Cumulative Statistics (1:1 Weak Entities):");
      System.out.println(" 11. Offensive_Stats_Home     14. Defensive_Stats_Away");
      System.out.println(" 12. Offensive_Stats_Away     15. Special_Teams_Stats_Home");
      System.out.println(" 13. Defensive_Stats_Home     16. Special_Teams_Stats_Away");
      System.out.println("Associative & Event Logs:");
      System.out.println(" 17. Who_Coaches              22. Analyze_Opposing");
      System.out.println(" 18. Coach_In_Game            23. Practices_In");
      System.out.println(" 19. Team_Plays               24. Manages_reg_staff");
      System.out.println(" 20. Opposing_Plays           25. Inspects");
      System.out.println(" 21. Analyze_Team             26. Works_on");
      System.out.println("  0. Cancel / Return to Main Menu");
   }

   // =========================================================================
   // INPUT VALIDATION HELPERS
   // =========================================================================

   private static String readRequiredString(String prompt) throws IOException {
      while (true) {
         System.out.print(prompt);
         String val = in.readLine();
         if (val != null && !val.trim().isEmpty()) {
            return val.trim().toLowerCase();
         }
         System.out.println("  [Error] This field is required and cannot be empty.");
      }
   }

   private static String readOptionalString(String prompt) throws IOException {
      System.out.print(prompt);
      String val = in.readLine();
      if (val == null || val.trim().isEmpty()) {
         return null;
      }
      return val.trim().toLowerCase();
   }

   private static Integer readInt(String prompt, boolean required, Integer min, Integer max) throws IOException {
      while (true) {
         System.out.print(prompt);
         String val = in.readLine();
         if (val == null || val.trim().isEmpty()) {
            if (!required) return null;
            System.out.println("  [Error] Integer value is required.");
            continue;
         }
         try {
            int parsed = Integer.parseInt(val.trim());
            if (min != null && parsed < min) {
               System.out.println("  [Error] Value must be >= " + min + ".");
               continue;
            }
            if (max != null && parsed > max) {
               System.out.println("  [Error] Value must be <= " + max + ".");
               continue;
            }
            return parsed;
         } catch (NumberFormatException e) {
            System.out.println("  [Error] Invalid integer format.");
         }
      }
   }

   private static int readIntWithDefault(String prompt, int defaultVal, Integer min, Integer max) throws IOException {
      Integer res = readInt(prompt, false, min, max);
      return (res == null) ? defaultVal : res;
   }

   private static Double readDouble(String prompt, boolean required, Double min, boolean strictMin) throws IOException {
      while (true) {
         System.out.print(prompt);
         String val = in.readLine();
         if (val == null || val.trim().isEmpty()) {
            if (!required) return null;
            System.out.println("  [Error] Numeric value is required.");
            continue;
         }
         try {
            double parsed = Double.parseDouble(val.trim());
            if (min != null) {
               if (strictMin && parsed <= min) {
                  System.out.println("  [Error] Value must be > " + min + ".");
                  continue;
               } else if (!strictMin && parsed < min) {
                  System.out.println("  [Error] Value must be >= " + min + ".");
                  continue;
               }
            }
            return parsed;
         } catch (NumberFormatException e) {
            System.out.println("  [Error] Invalid decimal/numeric format.");
         }
      }
   }

   private static double readDoubleWithDefault(String prompt, double defaultVal, Double min, boolean strictMin) throws IOException {
      Double res = readDouble(prompt, false, min, strictMin);
      return (res == null) ? defaultVal : res;
   }

   private static Boolean readBoolean(String prompt, boolean required) throws IOException {
      while (true) {
         System.out.print(prompt);
         String val = in.readLine();
         if (val == null || val.trim().isEmpty()) {
            if (!required) return null;
            System.out.println("  [Error] Boolean value is required (true/false or yes/no).");
            continue;
         }
         String trimmed = val.trim().toLowerCase();
         if (trimmed.equals("true") || trimmed.equals("t") || trimmed.equals("yes") || trimmed.equals("y") || trimmed.equals("1")) {
            return true;
         }
         if (trimmed.equals("false") || trimmed.equals("f") || trimmed.equals("no") || trimmed.equals("n") || trimmed.equals("0")) {
            return false;
         }
         System.out.println("  [Error] Invalid boolean. Enter true/false or yes/no.");
      }
   }

   private static Date readDate(String prompt, boolean required) throws IOException {
      while (true) {
         System.out.print(prompt);
         String val = in.readLine();
         if (val == null || val.trim().isEmpty()) {
            if (!required) return null;
            System.out.println("  [Error] Date is required (YYYY-MM-DD).");
            continue;
         }
         try {
            return Date.valueOf(val.trim());
         } catch (IllegalArgumentException e) {
            System.out.println("  [Error] Invalid date format. Please use YYYY-MM-DD (e.g., 2026-09-15).");
         }
      }
   }

   private static String readEnum(String prompt, boolean required, List<String> allowedValues) throws IOException {
      while (true) {
         System.out.print(prompt);
         String val = in.readLine();
         if (val == null || val.trim().isEmpty()) {
            if (!required) return null;
            System.out.println("  [Error] Selection is required. Allowed: " + allowedValues);
            continue;
         }
         String trimmed = val.trim();
         for (String allowed : allowedValues) {
            if (allowed.equalsIgnoreCase(trimmed)) {
               return allowed;
            }
         }
         System.out.println("  [Error] Invalid option. Must be one of: " + allowedValues);
      }
   }

   private static Integer[] readIntArray(String prompt) throws IOException {
      while (true) {
         System.out.print(prompt);
         String val = in.readLine();
         if (val == null || val.trim().isEmpty()) {
            return null;
         }
         String[] parts = val.split(",");
         Integer[] result = new Integer[parts.length];
         boolean valid = true;
         for (int i = 0; i < parts.length; i++) {
            try {
               result[i] = Integer.parseInt(parts[i].trim());
            } catch (NumberFormatException e) {
               System.out.println("  [Error] Invalid integer in comma-separated list: '" + parts[i].trim() + "'");
               valid = false;
               break;
            }
         }
         if (valid) return result;
      }
   }

   private static String[] readStringArray(String prompt) throws IOException {
      System.out.print(prompt);
      String val = in.readLine();
      if (val == null || val.trim().isEmpty()) {
         return null;
      }
      String[] parts = val.split(",");
      List<String> cleaned = new ArrayList<>();
      for (String p : parts) {
         if (!p.trim().isEmpty()) {
            cleaned.add(p.trim().toLowerCase());
         }
      }
      return cleaned.isEmpty() ? null : cleaned.toArray(new String[0]);
   }

   private static void setNullableInt(PreparedStatement ps, int idx, Integer val) throws SQLException {
      if (val == null) ps.setNull(idx, Types.INTEGER);
      else ps.setInt(idx, val);
   }

   private static void setNullableDouble(PreparedStatement ps, int idx, Double val) throws SQLException {
      if (val == null) ps.setNull(idx, Types.DOUBLE);
      else ps.setDouble(idx, val);
   }

   private static void setNullableBoolean(PreparedStatement ps, int idx, Boolean val) throws SQLException {
      if (val == null) ps.setNull(idx, Types.BOOLEAN);
      else ps.setBoolean(idx, val);
   }

   private static void setNullableDate(PreparedStatement ps, int idx, Date val) throws SQLException {
      if (val == null) ps.setNull(idx, Types.DATE);
      else ps.setDate(idx, val);
   }

   private static void setNullableString(PreparedStatement ps, int idx, String val) throws SQLException {
      if (val == null) ps.setNull(idx, Types.VARCHAR);
      else ps.setString(idx, val);
   }

   private static void setNullableIntArray(Connection conn, PreparedStatement ps, int idx, Integer[] val) throws SQLException {
      if (val == null) {
         ps.setNull(idx, Types.ARRAY);
      } else {
         Array sqlArray = conn.createArrayOf("integer", val);
         ps.setArray(idx, sqlArray);
      }
   }

   private static void setNullableStringArray(Connection conn, PreparedStatement ps, int idx, String[] val, String pgBaseType) throws SQLException {
      if (val == null) {
         ps.setNull(idx, Types.ARRAY);
      } else {
         Array sqlArray = conn.createArrayOf(pgBaseType, val);
         ps.setArray(idx, sqlArray);
      }
   }

   // =========================================================================
   // QUERY 0: INSERT RECORDS INTO ANY TABLE IN THE SCHEMA
   // =========================================================================

   public static void QueryExample(EmbeddedSQL esql) {
      printAllTablesMenu("INSERT RECORD INTO TABLE");

      int choice = readChoice();
      if (choice == 0) return;

      Connection conn = esql.getConnection();
      try {
         switch (choice) {
            case 1:  insertManager(conn); break;
            case 2:  insertTeamPlayer(conn); break;
            case 3:  insertOpposingPlayer(conn); break;
            case 4:  insertCoach(conn); break;
            case 5:  insertStatisticalTeam(conn); break;
            case 6:  insertTeamDoctor(conn); break;
            case 7:  insertTrainer(conn); break;
            case 8:  insertRegularStaff(conn); break;
            case 9:  insertGame(conn); break;
            case 10: insertPractice(conn); break;
            case 11: insertOffensiveStats(conn, "Offensive_Stats_Home", "pid"); break;
            case 12: insertOffensiveStats(conn, "Offensive_Stats_Away", "opid"); break;
            case 13: insertDefensiveStats(conn, "Defensive_Stats_Home", "pid"); break;
            case 14: insertDefensiveStats(conn, "Defensive_Stats_Away", "opid"); break;
            case 15: insertSpecialTeamsStats(conn, "Special_Teams_Stats_Home", "pid"); break;
            case 16: insertSpecialTeamsStats(conn, "Special_Teams_Stats_Away", "opid"); break;
            case 17: insertWhoCoaches(conn); break;
            case 18: insertCoachInGame(conn); break;
            case 19: insertPlays(conn, "Team_Plays", "pid"); break;
            case 20: insertPlays(conn, "Opposing_Plays", "opid"); break;
            case 21: insertAnalyze(conn, "Analyze_Team", "pid"); break;
            case 22: insertAnalyze(conn, "Analyze_Opposing", "opid"); break;
            case 23: insertPracticesIn(conn); break;
            case 24: insertManagesRegStaff(conn); break;
            case 25: insertInspects(conn); break;
            case 26: insertWorksOn(conn); break;
            default: System.out.println("Invalid table selection."); break;
         }
      } catch (SQLException e) {
         System.err.println("\n[Database Error] Insert failed (SQLState: " + e.getSQLState() + "):");
         System.err.println("  " + e.getMessage());
      } catch (IOException e) {
         System.err.println("\n[Input Error] Failed reading input: " + e.getMessage());
      }
   }

   // --- 1. PRINCIPAL ENTITIES ---

   private static void insertManager(Connection conn) throws SQLException, IOException {
      System.out.println("\n-- Insert into Managers (Press Enter to skip optional fields) --");
      int mid = readInt("Manager ID (mid, required int): ", true, null, null);
      String fname = readRequiredString("First Name (required): ");
      String lname = readRequiredString("Last Name (required): ");
      Integer age = readInt("Age (optional, >= 18): ", false, 18, null);
      Double salary = readDouble("Salary (optional, >= 0): ", false, 0.0, false);
      String rank = readOptionalString("Rank (optional): ");
      Date hireDate = readDate("Hire Date (optional, YYYY-MM-DD): ", false);
      Integer exp = readInt("Experience in years (optional, >= 0): ", false, 0, null);

      String sql = "INSERT INTO Managers (mid, fname, lname, age, salary, rank, hire_date, experience) " +
                   "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
         ps.setInt(1, mid);
         ps.setString(2, fname);
         ps.setString(3, lname);
         setNullableInt(ps, 4, age);
         setNullableDouble(ps, 5, salary);
         setNullableString(ps, 6, rank);
         setNullableDate(ps, 7, hireDate);
         setNullableInt(ps, 8, exp);
         ps.executeUpdate();
         System.out.println("Success: Manager record inserted.");
      }
   }

   private static void insertTeamPlayer(Connection conn) throws SQLException, IOException {
      System.out.println("\n-- Insert into Team_Players (Press Enter to skip optional fields) --");
      int pid = readInt("Player ID (pid, required int): ", true, null, null);
      String fname = readRequiredString("First Name (required): ");
      String lname = readRequiredString("Last Name (required): ");
      Double height = readDouble("Height (optional, > 0): ", false, 0.0, true);
      Double weight = readDouble("Weight (optional, > 0): ", false, 0.0, true);
      Integer age = readInt("Age (optional, >= 18): ", false, 18, null);
      Double salary = readDouble("Salary (optional, >= 0): ", false, 0.0, false);
      Date hireDate = readDate("Hire Date (optional, YYYY-MM-DD): ", false);
      Integer exp = readInt("Experience in years (optional, >= 0): ", false, 0, null);
      String healthStatus = readOptionalString("Health Status (optional, defaults to 'Healthy'): ");
      if (healthStatus == null) healthStatus = "healthy";
      String squad = readEnum("Roster Type (required: 'Regular' or 'Practice Squad'): ",
                              true, Arrays.asList("Regular", "Practice Squad"));
      Integer jersey = readInt("Jersey Number (optional, 0-99): ", false, 0, 99);
      String positions = readRequiredString("Positions (required, e.g., 'QB' or 'WR, KR'): ").toLowerCase();
      int mid = readInt("Manager ID (mid, required existing Manager ID): ", true, null, null);
      Integer mentorPid = readInt("Mentor Player ID (optional existing pid, != pid): ", false, null, null);

      if (mentorPid != null && mentorPid == pid) {
         System.out.println("  [Error] Validation failed: A player cannot mentor themselves (mentor_pid == pid).");
         return;
      }

      String sql = "INSERT INTO Team_Players (pid, fname, lname, height, weight, age, salary, hire_date, " +
                   "experience, health_status, regular_practice_squad, jersey_number, positions, mid, mentor_pid) " +
                   "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
         ps.setInt(1, pid);
         ps.setString(2, fname);
         ps.setString(3, lname);
         setNullableDouble(ps, 4, height);
         setNullableDouble(ps, 5, weight);
         setNullableInt(ps, 6, age);
         setNullableDouble(ps, 7, salary);
         setNullableDate(ps, 8, hireDate);
         setNullableInt(ps, 9, exp);
         ps.setString(10, healthStatus);
         ps.setString(11, squad);
         setNullableInt(ps, 12, jersey);
         ps.setString(13, positions);
         ps.setInt(14, mid);
         setNullableInt(ps, 15, mentorPid);
         ps.executeUpdate();
         System.out.println("Success: Team_Players record inserted.");
      }
   }

   private static void insertOpposingPlayer(Connection conn) throws SQLException, IOException {
      System.out.println("\n-- Insert into Opposing_Players (Press Enter to skip optional fields) --");
      int opid = readInt("Opposing Player ID (opid, required int): ", true, null, null);
      String fname = readRequiredString("First Name (required): ");
      String lname = readRequiredString("Last Name (required): ");
      Double height = readDouble("Height (optional, > 0): ", false, 0.0, true);
      Double weight = readDouble("Weight (optional, > 0): ", false, 0.0, true);
      Integer age = readInt("Age (optional, >= 18): ", false, 18, null);
      Integer exp = readInt("Experience in years (optional, >= 0): ", false, 0, null);
      String team = readRequiredString("Opposing Team Name (required): ");
      String positions = readRequiredString("Positions (required, e.g., 'CB' or 'DE, EDGE'): ").toLowerCase();

      String sql = "INSERT INTO Opposing_Players (opid, fname, lname, height, weight, age, experience, team, positions) " +
                   "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
         ps.setInt(1, opid);
         ps.setString(2, fname);
         ps.setString(3, lname);
         setNullableDouble(ps, 4, height);
         setNullableDouble(ps, 5, weight);
         setNullableInt(ps, 6, age);
         setNullableInt(ps, 7, exp);
         ps.setString(8, team);
         ps.setString(9, positions);
         ps.executeUpdate();
         System.out.println("Success: Opposing_Players record inserted.");
      }
   }

   private static void insertCoach(Connection conn) throws SQLException, IOException {
      System.out.println("\n-- Insert into Coaches (Press Enter to skip optional fields) --");
      int cid = readInt("Coach ID (cid, required int): ", true, null, null);
      String fname = readRequiredString("First Name (required): ");
      String lname = readRequiredString("Last Name (required): ");
      Double salary = readDouble("Salary (optional, >= 0): ", false, 0.0, false);
      Integer age = readInt("Age (optional, >= 18): ", false, 18, null);
      String gender = readOptionalString("Gender (optional): ");
      Date hireDate = readDate("Hire Date (optional, YYYY-MM-DD): ", false);
      Integer exp = readInt("Experience in years (optional, >= 0): ", false, 0, null);
      String position = readRequiredString("Coaching Title/Position (required, e.g., 'Head Coach'): ");
      String unit = readEnum("Unit (optional: 'Offensive', 'Defensive', 'Special Teams', 'Both'): ",
                             false, Arrays.asList("Offensive", "Defensive", "Special Teams", "Both"));
      int mid = readInt("Manager ID (mid, required existing Manager ID): ", true, null, null);
      Integer reportsTo = readInt("Reports To Coach ID (optional existing cid, != cid): ", false, null, null);

      if (reportsTo != null && reportsTo == cid) {
         System.out.println("  [Error] Validation failed: A coach cannot report to themselves (reports_to_cid == cid).");
         return;
      }

      String sql = "INSERT INTO Coaches (cid, fname, lname, salary, age, gender, hire_date, experience, " +
                   "position, defensive_offensive, mid, reports_to_cid) " +
                   "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
         ps.setInt(1, cid);
         ps.setString(2, fname);
         ps.setString(3, lname);
         setNullableDouble(ps, 4, salary);
         setNullableInt(ps, 5, age);
         setNullableString(ps, 6, gender);
         setNullableDate(ps, 7, hireDate);
         setNullableInt(ps, 8, exp);
         ps.setString(9, position);
         setNullableString(ps, 10, unit);
         ps.setInt(11, mid);
         setNullableInt(ps, 12, reportsTo);
         ps.executeUpdate();
         System.out.println("Success: Coaches record inserted.");
      }
   }

   private static void insertStatisticalTeam(Connection conn) throws SQLException, IOException {
      System.out.println("\n-- Insert into Statistical_Team (Press Enter to skip optional fields) --");
      int stid = readInt("Analyst ID (stid, required int): ", true, null, null);
      String fname = readRequiredString("First Name (required): ");
      String lname = readRequiredString("Last Name (required): ");
      Double salary = readDouble("Salary (optional, >= 0): ", false, 0.0, false);
      Date hireDate = readDate("Hire Date (optional, YYYY-MM-DD): ", false);
      Integer exp = readInt("Experience in years (optional, >= 0): ", false, 0, null);
      String focus = readEnum("Focus (required: 'Offensive', 'Defensive', 'Both'): ",
                              true, Arrays.asList("Offensive", "Defensive", "Both"));

      String sql = "INSERT INTO Statistical_Team (stid, fname, lname, salary, hire_date, experience, focus) " +
                   "VALUES (?, ?, ?, ?, ?, ?, ?)";
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
         ps.setInt(1, stid);
         ps.setString(2, fname);
         ps.setString(3, lname);
         setNullableDouble(ps, 4, salary);
         setNullableDate(ps, 5, hireDate);
         setNullableInt(ps, 6, exp);
         ps.setString(7, focus);
         ps.executeUpdate();
         System.out.println("Success: Statistical_Team record inserted.");
      }
   }

   private static void insertTeamDoctor(Connection conn) throws SQLException, IOException {
      System.out.println("\n-- Insert into Team_Doctors (Press Enter to skip optional fields) --");
      int did = readInt("Doctor ID (did, required int): ", true, null, null);
      String fname = readRequiredString("First Name (required): ");
      String lname = readRequiredString("Last Name (required): ");
      Double salary = readDouble("Salary (optional, >= 0): ", false, 0.0, false);
      Integer age = readInt("Age (optional, >= 18): ", false, 18, null);
      String gender = readOptionalString("Gender (optional): ");
      Date hireDate = readDate("Hire Date (optional, YYYY-MM-DD): ", false);
      Integer exp = readInt("Experience in years (optional, >= 0): ", false, 0, null);
      String speciality = readOptionalString("Speciality (optional): ");

      String sql = "INSERT INTO Team_Doctors (did, fname, lname, salary, age, gender, hire_date, experience, speciality) " +
                   "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
         ps.setInt(1, did);
         ps.setString(2, fname);
         ps.setString(3, lname);
         setNullableDouble(ps, 4, salary);
         setNullableInt(ps, 5, age);
         setNullableString(ps, 6, gender);
         setNullableDate(ps, 7, hireDate);
         setNullableInt(ps, 8, exp);
         setNullableString(ps, 9, speciality);
         ps.executeUpdate();
         System.out.println("Success: Team_Doctors record inserted.");
      }
   }

   private static void insertTrainer(Connection conn) throws SQLException, IOException {
      System.out.println("\n-- Insert into Trainers (Press Enter to skip optional fields) --");
      int tid = readInt("Trainer ID (tid, required int): ", true, null, null);
      String fname = readRequiredString("First Name (required): ");
      String lname = readRequiredString("Last Name (required): ");
      Double salary = readDouble("Salary (optional, >= 0): ", false, 0.0, false);
      Integer age = readInt("Age (optional, >= 18): ", false, 18, null);
      String gender = readOptionalString("Gender (optional): ");
      Date hireDate = readDate("Hire Date (optional, YYYY-MM-DD): ", false);
      Integer exp = readInt("Experience in years (optional, >= 0): ", false, 0, null);

      String sql = "INSERT INTO Trainers (tid, fname, lname, salary, age, gender, hire_date, experience) " +
                   "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
         ps.setInt(1, tid);
         ps.setString(2, fname);
         ps.setString(3, lname);
         setNullableDouble(ps, 4, salary);
         setNullableInt(ps, 5, age);
         setNullableString(ps, 6, gender);
         setNullableDate(ps, 7, hireDate);
         setNullableInt(ps, 8, exp);
         ps.executeUpdate();
         System.out.println("Success: Trainers record inserted.");
      }
   }

   private static void insertRegularStaff(Connection conn) throws SQLException, IOException {
      System.out.println("\n-- Insert into Regular_Staff (Press Enter to skip optional fields) --");
      int jib = readInt("Staff ID (jib, required int): ", true, null, null);
      String fname = readRequiredString("First Name (required): ");
      String lname = readRequiredString("Last Name (required): ");
      Double salary = readDouble("Salary (optional, >= 0): ", false, 0.0, false);
      Integer age = readInt("Age (optional, >= 16): ", false, 16, null);
      Integer exp = readInt("Experience in years (optional, >= 0): ", false, 0, null);
      String jobTitle = readRequiredString("Job Title (required): ");
      String gender = readOptionalString("Gender (optional): ");

      String sql = "INSERT INTO Regular_Staff (jib, fname, lname, salary, age, experience, job_title, gender) " +
                   "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
         ps.setInt(1, jib);
         ps.setString(2, fname);
         ps.setString(3, lname);
         setNullableDouble(ps, 4, salary);
         setNullableInt(ps, 5, age);
         setNullableInt(ps, 6, exp);
         ps.setString(7, jobTitle);
         setNullableString(ps, 8, gender);
         ps.executeUpdate();
         System.out.println("Success: Regular_Staff record inserted.");
      }
   }

   private static void insertGame(Connection conn) throws SQLException, IOException {
      System.out.println("\n-- Insert into Games (Press Enter to skip optional fields) --");
      int gid = readInt("Game ID (gid, required int): ", true, null, null);
      Date gameDate = readDate("Date of Game (required, unique YYYY-MM-DD): ", true);
      String homeTeam = readRequiredString("Home Team (required): ");
      String awayTeam = readRequiredString("Away Team (required): ");
      String playByPlay = readOptionalString("Play-by-Play Notes (optional): ");
      String homePlays = readOptionalString("Home Team Plays (optional): ");
      String awayPlays = readOptionalString("Away Team Plays (optional): ");

      String sql = "INSERT INTO Games (gid, date_of_game, home_team, away_team, play_by_play, home_team_plays, away_team_plays) " +
                   "VALUES (?, ?, ?, ?, ?, ?, ?)";
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
         ps.setInt(1, gid);
         ps.setDate(2, gameDate);
         ps.setString(3, homeTeam);
         ps.setString(4, awayTeam);
         setNullableString(ps, 5, playByPlay);
         setNullableString(ps, 6, homePlays);
         setNullableString(ps, 7, awayPlays);
         ps.executeUpdate();
         System.out.println("Success: Games record inserted.");
      }
   }

   private static void insertPractice(Connection conn) throws SQLException, IOException {
      System.out.println("\n-- Insert into Practices --");
      Date practiceDate = readDate("Date of Practice (required, YYYY-MM-DD): ", true);
      String session = readEnum("Session (required: 'Morning' or 'Afternoon'): ",
                                true, Arrays.asList("Morning", "Afternoon"));

      String sql = "INSERT INTO Practices (date_of_practice, morning_afternoon) VALUES (?, ?)";
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
         ps.setDate(1, practiceDate);
         ps.setString(2, session);
         ps.executeUpdate();
         System.out.println("Success: Practices record inserted.");
      }
   }

   // --- 2. 1:1 CUMULATIVE STATISTICS TABLES ---

   private static void insertOffensiveStats(Connection conn, String tableName, String idCol) throws SQLException, IOException {
      System.out.println("\n-- Insert into " + tableName + " (Press Enter to default numeric stats to 0) --");
      int id = readInt("Player ID (" + idCol + ", required existing player ID): ", true, null, null);
      int snaps = readIntWithDefault("Snaps Played (>= 0, default 0): ", 0, 0, null);
      int passAtt = readIntWithDefault("Passing Attempts (>= 0, default 0): ", 0, 0, null);
      int comp = readIntWithDefault("Completions (>= 0, default 0): ", 0, 0, null);
      int passYds = readIntWithDefault("Passing Yards (signed int, default 0): ", 0, null, null);
      int rushAtt = readIntWithDefault("Rushing Attempts (>= 0, default 0): ", 0, 0, null);
      int rushYds = readIntWithDefault("Rushing Yards (signed int, default 0): ", 0, null, null);
      int targets = readIntWithDefault("Targets (>= 0, default 0): ", 0, 0, null);
      int catches = readIntWithDefault("Catches (>= 0, default 0): ", 0, 0, null);
      int recYds = readIntWithDefault("Receiving Yards (signed int, default 0): ", 0, null, null);
      int blockAtt = readIntWithDefault("Blocking Attempts (>= 0, default 0): ", 0, 0, null);
      int sacksAllowed = readIntWithDefault("Sacks Allowed (>= 0, default 0): ", 0, 0, null);
      int pressuresAllowed = readIntWithDefault("Pressures Allowed (>= 0, default 0): ", 0, 0, null);
      double ydsBehind = readDoubleWithDefault("Yards Gained Behind (signed float, default 0): ", 0.0, null, false);
      int fumbles = readIntWithDefault("Fumbles (>= 0, default 0): ", 0, 0, null);
      int penalties = readIntWithDefault("Penalties (>= 0, default 0): ", 0, 0, null);

      String sql = "INSERT INTO " + tableName + " (" + idCol + ", snaps_played, passing_attempts, completions, " +
                   "passing_yards, rushing_attempts, rushing_yards, targets, catches, receiving_yards, " +
                   "blocking_attempts, sacks_allowed, pressures_allowed, yards_gained_behind, fumbles, penalties) " +
                   "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
         ps.setInt(1, id);
         ps.setInt(2, snaps);
         ps.setInt(3, passAtt);
         ps.setInt(4, comp);
         ps.setInt(5, passYds);
         ps.setInt(6, rushAtt);
         ps.setInt(7, rushYds);
         ps.setInt(8, targets);
         ps.setInt(9, catches);
         ps.setInt(10, recYds);
         ps.setInt(11, blockAtt);
         ps.setInt(12, sacksAllowed);
         ps.setInt(13, pressuresAllowed);
         ps.setDouble(14, ydsBehind);
         ps.setInt(15, fumbles);
         ps.setInt(16, penalties);
         ps.executeUpdate();
         System.out.println("Success: " + tableName + " record inserted.");
      }
   }

   private static void insertDefensiveStats(Connection conn, String tableName, String idCol) throws SQLException, IOException {
      System.out.println("\n-- Insert into " + tableName + " (Press Enter to default numeric stats to 0) --");
      int id = readInt("Player ID (" + idCol + ", required existing player ID): ", true, null, null);
      int snaps = readIntWithDefault("Snaps Played (>= 0, default 0): ", 0, 0, null);
      int tackles = readIntWithDefault("Tackles (>= 0, default 0): ", 0, 0, null);
      double sacks = readDoubleWithDefault("Sacks (>= 0, e.g. 1.5, default 0): ", 0.0, 0.0, false);
      int pressures = readIntWithDefault("Pressures (>= 0, default 0): ", 0, 0, null);
      int passDef = readIntWithDefault("Pass Deflections (>= 0, default 0): ", 0, 0, null);
      int ints = readIntWithDefault("Interceptions (>= 0, default 0): ", 0, 0, null);
      int ff = readIntWithDefault("Fumbles Forced (>= 0, default 0): ", 0, 0, null);
      int fr = readIntWithDefault("Fumbles Recovered (>= 0, default 0): ", 0, 0, null);
      int penalties = readIntWithDefault("Penalties (>= 0, default 0): ", 0, 0, null);

      String sql = "INSERT INTO " + tableName + " (" + idCol + ", snaps_played, tackles, sacks, pressures, " +
                   "pass_deflections, interceptions, fumbles_forced, fumbles_recovered, penalties) " +
                   "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
         ps.setInt(1, id);
         ps.setInt(2, snaps);
         ps.setInt(3, tackles);
         ps.setDouble(4, sacks);
         ps.setInt(5, pressures);
         ps.setInt(6, passDef);
         ps.setInt(7, ints);
         ps.setInt(8, ff);
         ps.setInt(9, fr);
         ps.setInt(10, penalties);
         ps.executeUpdate();
         System.out.println("Success: " + tableName + " record inserted.");
      }
   }

   private static void insertSpecialTeamsStats(Connection conn, String tableName, String idCol) throws SQLException, IOException {
      System.out.println("\n-- Insert into " + tableName + " (Press Enter to skip/default to 0) --");
      int id = readInt("Player ID (" + idCol + ", required existing player ID): ", true, null, null);
      int snaps = readIntWithDefault("Snaps Played (>= 0, default 0): ", 0, 0, null);
      int puntAtt = readIntWithDefault("Punt Attempts (>= 0, default 0): ", 0, 0, null);
      int puntYds = readIntWithDefault("Punt Yards (signed int, default 0): ", 0, null, null);
      int fgAtt = readIntWithDefault("Field Goal Attempts (>= 0, default 0): ", 0, 0, null);
      int fgMade = readIntWithDefault("Field Goals Made (>= 0, default 0): ", 0, 0, null);
      Integer[] fgLengths = readIntArray("Field Goal Kick Lengths (optional comma-separated ints, e.g., 42, 35, 51): ");
      int xpAtt = readIntWithDefault("Extra Point Attempts (>= 0, default 0): ", 0, 0, null);
      int xpMade = readIntWithDefault("Extra Points Made (>= 0, default 0): ", 0, 0, null);
      int puntsBlocked = readIntWithDefault("Punts Blocked (>= 0, default 0): ", 0, 0, null);
      int krYds = readIntWithDefault("Kick Return Yards (signed int, default 0): ", 0, null, null);
      int prYds = readIntWithDefault("Punt Return Yards (signed int, default 0): ", 0, null, null);
      int penalties = readIntWithDefault("Penalties (>= 0, default 0): ", 0, 0, null);

      String sql = "INSERT INTO " + tableName + " (" + idCol + ", snaps_played, punt_attempts, punt_yards, " +
                   "field_goal_attempts, field_goals_made, field_goal_kick_lengths, extra_point_attempts, " +
                   "extra_points_made, punts_blocked, kick_return_yards, punt_return_yards, penalties) " +
                   "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
         ps.setInt(1, id);
         ps.setInt(2, snaps);
         ps.setInt(3, puntAtt);
         ps.setInt(4, puntYds);
         ps.setInt(5, fgAtt);
         ps.setInt(6, fgMade);
         setNullableIntArray(conn, ps, 7, fgLengths);
         ps.setInt(8, xpAtt);
         ps.setInt(9, xpMade);
         ps.setInt(10, puntsBlocked);
         ps.setInt(11, krYds);
         ps.setInt(12, prYds);
         ps.setInt(13, penalties);
         ps.executeUpdate();
         System.out.println("Success: " + tableName + " record inserted.");
      }
   }

   // --- 3. MANY-TO-MANY ASSOCIATIVE & EVENT LOG TABLES ---

   private static void insertWhoCoaches(Connection conn) throws SQLException, IOException {
      System.out.println("\n-- Insert into Who_Coaches (Press Enter to skip optional fields) --");
      int cid = readInt("Coach ID (cid, required existing cid): ", true, null, null);
      int pid = readInt("Player ID (pid, required existing pid): ", true, null, null);
      String affiliation = readOptionalString("Affiliation (optional): ");
      String gamePlan = readOptionalString("Game Plan (optional): ");
      Date date = readDate("Date (required, YYYY-MM-DD): ", true);

      String sql = "INSERT INTO Who_Coaches (cid, pid, affiliation, game_plan, date) VALUES (?, ?, ?, ?, ?)";
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
         ps.setInt(1, cid);
         ps.setInt(2, pid);
         setNullableString(ps, 3, affiliation);
         setNullableString(ps, 4, gamePlan);
         ps.setDate(5, date);
         ps.executeUpdate();
         System.out.println("Success: Who_Coaches record inserted.");
      }
   }

   private static void insertCoachInGame(Connection conn) throws SQLException, IOException {
      System.out.println("\n-- Insert into Coach_In_Game --");
      int cid = readInt("Coach ID (cid, required existing cid): ", true, null, null);
      int gid = readInt("Game ID (gid, required existing gid): ", true, null, null);

      String sql = "INSERT INTO Coach_In_Game (cid, gid) VALUES (?, ?)";
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
         ps.setInt(1, cid);
         ps.setInt(2, gid);
         ps.executeUpdate();
         System.out.println("Success: Coach_In_Game record inserted.");
      }
   }

   private static void insertPlays(Connection conn, String tableName, String idCol) throws SQLException, IOException {
      System.out.println("\n-- Insert into " + tableName + " (Press Enter to skip optional arrays) --");
      int id = readInt("Player ID (" + idCol + ", required existing player ID): ", true, null, null);
      int gid = readInt("Game ID (gid, required existing gid): ", true, null, null);
      String[] plays = readStringArray("List of Plays (optional comma-separated, e.g., Slant Pass, Screen): ");
      String[] stats = readStringArray("List of Stats (optional comma-separated, e.g., 12 yds, 1st Down): ");

      String sql = "INSERT INTO " + tableName + " (" + idCol + ", gid, list_of_plays, list_of_stats) VALUES (?, ?, ?, ?)";
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
         ps.setInt(1, id);
         ps.setInt(2, gid);
         setNullableStringArray(conn, ps, 3, plays, "text");
         setNullableStringArray(conn, ps, 4, stats, "text");
         ps.executeUpdate();
         System.out.println("Success: " + tableName + " record inserted.");
      }
   }

   private static void insertAnalyze(Connection conn, String tableName, String idCol) throws SQLException, IOException {
      System.out.println("\n-- Insert into " + tableName + " (Press Enter to skip optional fields) --");
      int stid = readInt("Analyst ID (stid, required existing stid): ", true, null, null);
      int id = readInt("Player ID (" + idCol + ", required existing player ID): ", true, null, null);
      String analysis = readOptionalString("Analysis Notes (optional): ");
      Date date = readDate("Date (required, YYYY-MM-DD): ", true);

      String sql = "INSERT INTO " + tableName + " (stid, " + idCol + ", analysis, date) VALUES (?, ?, ?, ?)";
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
         ps.setInt(1, stid);
         ps.setInt(2, id);
         setNullableString(ps, 3, analysis);
         ps.setDate(4, date);
         ps.executeUpdate();
         System.out.println("Success: " + tableName + " record inserted.");
      }
   }

   private static void insertPracticesIn(Connection conn) throws SQLException, IOException {
      System.out.println("\n-- Insert into Practices_In --");
      int pid = readInt("Player ID (pid, required existing pid): ", true, null, null);
      Date practiceDate = readDate("Date of Practice (required existing Practice date, YYYY-MM-DD): ", true);
      String session = readEnum("Session (required: 'Morning' or 'Afternoon'): ",
                                true, Arrays.asList("Morning", "Afternoon"));
      boolean didPractice = readBoolean("Did Practice? (required: true/false): ", true);
      String excusedStatus = readEnum("Excused/Unexcused (optional: 'Excused', 'Unexcused', or Enter for NULL): ",
                                      false, Arrays.asList("Excused", "Unexcused"));

      String sql = "INSERT INTO Practices_In (pid, date_of_practice, morning_afternoon, did_practice, excused_unexcused) " +
                   "VALUES (?, ?, ?, ?, ?)";
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
         ps.setInt(1, pid);
         ps.setDate(2, practiceDate);
         ps.setString(3, session);
         ps.setBoolean(4, didPractice);
         setNullableString(ps, 5, excusedStatus);
         ps.executeUpdate();
         System.out.println("Success: Practices_In record inserted.");
      }
   }

   private static void insertManagesRegStaff(Connection conn) throws SQLException, IOException {
      System.out.println("\n-- Insert into Manages_reg_staff (Press Enter to skip optional fields) --");
      int mid = readInt("Manager ID (mid, required existing mid): ", true, null, null);
      int jib = readInt("Staff ID (jib, required existing jib): ", true, null, null);
      Integer days = readInt("Days per week (optional, 1-7): ", false, 1, 7);
      String[] schedule = readStringArray("Schedule (optional comma-separated, e.g., Mon 9-5, Wed 9-5): ");

      String sql = "INSERT INTO Manages_reg_staff (mid, jib, days, schedule) VALUES (?, ?, ?, ?)";
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
         ps.setInt(1, mid);
         ps.setInt(2, jib);
         setNullableInt(ps, 3, days);
         setNullableStringArray(conn, ps, 4, schedule, "varchar");
         ps.executeUpdate();
         System.out.println("Success: Manages_reg_staff record inserted.");
      }
   }

   private static void insertInspects(Connection conn) throws SQLException, IOException {
      System.out.println("\n-- Insert into Inspects (Press Enter to skip optional fields) --");
      int did = readInt("Doctor ID (did, required existing did): ", true, null, null);
      int pid = readInt("Player ID (pid, required existing pid): ", true, null, null);
      String diagnosis = readOptionalString("Diagnosis (optional): ");
      Integer treatmentLen = readInt("Treatment Length in days (optional, >= 0): ", false, 0, null);
      Date date = readDate("Date (required, YYYY-MM-DD): ", true);

      String sql = "INSERT INTO Inspects (did, pid, diagnosis, treatment_length, date) VALUES (?, ?, ?, ?, ?)";
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
         ps.setInt(1, did);
         ps.setInt(2, pid);
         setNullableString(ps, 3, diagnosis);
         setNullableInt(ps, 4, treatmentLen);
         ps.setDate(5, date);
         ps.executeUpdate();
         System.out.println("Success: Inspects record inserted.");
      }
   }

   private static void insertWorksOn(Connection conn) throws SQLException, IOException {
      System.out.println("\n-- Insert into Works_on (Press Enter to skip optional fields) --");
      int tid = readInt("Trainer ID (tid, required existing tid): ", true, null, null);
      int pid = readInt("Player ID (pid, required existing pid): ", true, null, null);
      String condition = readOptionalString("Condition of Player (optional): ");
      Boolean recommends = readBoolean("Recommends Play? (optional: true/false or Enter for NULL): ", false);
      Date date = readDate("Date (required, YYYY-MM-DD): ", true);

      String sql = "INSERT INTO Works_on (tid, pid, condition_of_player, recommends, date) VALUES (?, ?, ?, ?, ?)";
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
         ps.setInt(1, tid);
         ps.setInt(2, pid);
         setNullableString(ps, 3, condition);
         setNullableBoolean(ps, 4, recommends);
         ps.setDate(5, date);
         ps.executeUpdate();
         System.out.println("Success: Works_on record inserted.");
      }
   }

   // =========================================================================
   // SHARED SCHEMA METADATA FOR ALL 26 TABLES (SEARCH & UPDATE)
   // =========================================================================

   private enum ColType { INT, DOUBLE, STRING, DATE, BOOLEAN, INT_ARRAY, STRING_ARRAY }

   private static class SearchColumn {
      String label;
      String sqlExpr;
      ColType type;
      boolean isPk;

      SearchColumn(String label, String sqlExpr, ColType type, boolean isPk) {
         this.label = label;
         this.sqlExpr = sqlExpr;
         this.type = type;
         this.isPk = isPk;
      }
   }

   private static class SearchTableConfig {
      String rawTableName;
      String tableName;
      String baseSelectFrom;
      String defaultOrderBy;
      List<SearchColumn> columns;

      SearchTableConfig(String rawTableName, String tableName, String baseSelectFrom, String defaultOrderBy, List<SearchColumn> columns) {
         this.rawTableName = rawTableName;
         this.tableName = tableName;
         this.baseSelectFrom = baseSelectFrom;
         this.defaultOrderBy = defaultOrderBy;
         this.columns = columns;
      }

      List<SearchColumn> getPrimaryKeys() {
         List<SearchColumn> pks = new ArrayList<>();
         for (SearchColumn c : columns) {
            if (c.isPk) pks.add(c);
         }
         return pks;
      }
   }

   private static class QueryParam {
      ColType type;
      Object value;

      QueryParam(ColType type, Object value) {
         this.type = type;
         this.value = value;
      }
   }

   private static SearchColumn pk(String label, String sqlExpr, ColType type) {
      return new SearchColumn(label, sqlExpr, type, true);
   }

   private static SearchColumn col(String label, String sqlExpr, ColType type) {
      return new SearchColumn(label, sqlExpr, type, false);
   }

   private static SearchTableConfig getTableConfig(int choice) {
      switch (choice) {
         case 1:
            return new SearchTableConfig(
               "Managers",
               "Managers",
               "SELECT m.* FROM Managers m",
               "m.mid",
               Arrays.asList(
                  pk("mid", "m.mid", ColType.INT),
                  col("fname", "m.fname", ColType.STRING),
                  col("lname", "m.lname", ColType.STRING),
                  col("age", "m.age", ColType.INT),
                  col("salary", "m.salary", ColType.DOUBLE),
                  col("rank", "m.rank", ColType.STRING),
                  col("hire_date", "m.hire_date", ColType.DATE),
                  col("experience", "m.experience", ColType.INT)
               )
            );
         case 2:
            return new SearchTableConfig(
               "Team_Players",
               "Team_Players (joined with Manager & Mentor)",
               "SELECT tp.*, m.fname AS manager_fname, m.lname AS manager_lname, " +
               "mentor.fname AS mentor_fname, mentor.lname AS mentor_lname " +
               "FROM Team_Players tp " +
               "LEFT JOIN Managers m ON tp.mid = m.mid " +
               "LEFT JOIN Team_Players mentor ON tp.mentor_pid = mentor.pid",
               "tp.pid",
               Arrays.asList(
                  pk("pid", "tp.pid", ColType.INT),
                  col("fname", "tp.fname", ColType.STRING),
                  col("lname", "tp.lname", ColType.STRING),
                  col("height", "tp.height", ColType.DOUBLE),
                  col("weight", "tp.weight", ColType.DOUBLE),
                  col("age", "tp.age", ColType.INT),
                  col("salary", "tp.salary", ColType.DOUBLE),
                  col("hire_date", "tp.hire_date", ColType.DATE),
                  col("experience", "tp.experience", ColType.INT),
                  col("health_status", "tp.health_status", ColType.STRING),
                  col("regular_practice_squad", "tp.regular_practice_squad", ColType.STRING),
                  col("jersey_number", "tp.jersey_number", ColType.INT),
                  col("positions", "tp.positions", ColType.STRING),
                  col("mid", "tp.mid", ColType.INT),
                  col("mentor_pid", "tp.mentor_pid", ColType.INT)
               )
            );
         case 3:
            return new SearchTableConfig(
               "Opposing_Players",
               "Opposing_Players",
               "SELECT op.* FROM Opposing_Players op",
               "op.opid",
               Arrays.asList(
                  pk("opid", "op.opid", ColType.INT),
                  col("fname", "op.fname", ColType.STRING),
                  col("lname", "op.lname", ColType.STRING),
                  col("height", "op.height", ColType.DOUBLE),
                  col("weight", "op.weight", ColType.DOUBLE),
                  col("age", "op.age", ColType.INT),
                  col("experience", "op.experience", ColType.INT),
                  col("team", "op.team", ColType.STRING),
                  col("positions", "op.positions", ColType.STRING)
               )
            );
         case 4:
            return new SearchTableConfig(
               "Coaches",
               "Coaches (joined with Manager & Supervisor Coach)",
               "SELECT c.*, m.fname AS manager_fname, m.lname AS manager_lname, " +
               "sup.fname AS supervisor_fname, sup.lname AS supervisor_lname " +
               "FROM Coaches c " +
               "LEFT JOIN Managers m ON c.mid = m.mid " +
               "LEFT JOIN Coaches sup ON c.reports_to_cid = sup.cid",
               "c.cid",
               Arrays.asList(
                  pk("cid", "c.cid", ColType.INT),
                  col("fname", "c.fname", ColType.STRING),
                  col("lname", "c.lname", ColType.STRING),
                  col("salary", "c.salary", ColType.DOUBLE),
                  col("age", "c.age", ColType.INT),
                  col("gender", "c.gender", ColType.STRING),
                  col("hire_date", "c.hire_date", ColType.DATE),
                  col("experience", "c.experience", ColType.INT),
                  col("position", "c.position", ColType.STRING),
                  col("defensive_offensive", "c.defensive_offensive", ColType.STRING),
                  col("mid", "c.mid", ColType.INT),
                  col("reports_to_cid", "c.reports_to_cid", ColType.INT)
               )
            );
         case 5:
            return new SearchTableConfig(
               "Statistical_Team",
               "Statistical_Team",
               "SELECT st.* FROM Statistical_Team st",
               "st.stid",
               Arrays.asList(
                  pk("stid", "st.stid", ColType.INT),
                  col("fname", "st.fname", ColType.STRING),
                  col("lname", "st.lname", ColType.STRING),
                  col("salary", "st.salary", ColType.DOUBLE),
                  col("hire_date", "st.hire_date", ColType.DATE),
                  col("experience", "st.experience", ColType.INT),
                  col("focus", "st.focus", ColType.STRING)
               )
            );
         case 6:
            return new SearchTableConfig(
               "Team_Doctors",
               "Team_Doctors",
               "SELECT td.* FROM Team_Doctors td",
               "td.did",
               Arrays.asList(
                  pk("did", "td.did", ColType.INT),
                  col("fname", "td.fname", ColType.STRING),
                  col("lname", "td.lname", ColType.STRING),
                  col("salary", "td.salary", ColType.DOUBLE),
                  col("age", "td.age", ColType.INT),
                  col("gender", "td.gender", ColType.STRING),
                  col("hire_date", "td.hire_date", ColType.DATE),
                  col("experience", "td.experience", ColType.INT),
                  col("speciality", "td.speciality", ColType.STRING)
               )
            );
         case 7:
            return new SearchTableConfig(
               "Trainers",
               "Trainers",
               "SELECT tr.* FROM Trainers tr",
               "tr.tid",
               Arrays.asList(
                  pk("tid", "tr.tid", ColType.INT),
                  col("fname", "tr.fname", ColType.STRING),
                  col("lname", "tr.lname", ColType.STRING),
                  col("salary", "tr.salary", ColType.DOUBLE),
                  col("age", "tr.age", ColType.INT),
                  col("gender", "tr.gender", ColType.STRING),
                  col("hire_date", "tr.hire_date", ColType.DATE),
                  col("experience", "tr.experience", ColType.INT)
               )
            );
         case 8:
            return new SearchTableConfig(
               "Regular_Staff",
               "Regular_Staff",
               "SELECT rs.* FROM Regular_Staff rs",
               "rs.jib",
               Arrays.asList(
                  pk("jib", "rs.jib", ColType.INT),
                  col("fname", "rs.fname", ColType.STRING),
                  col("lname", "rs.lname", ColType.STRING),
                  col("salary", "rs.salary", ColType.DOUBLE),
                  col("age", "rs.age", ColType.INT),
                  col("experience", "rs.experience", ColType.INT),
                  col("job_title", "rs.job_title", ColType.STRING),
                  col("gender", "rs.gender", ColType.STRING)
               )
            );
         case 9:
            return new SearchTableConfig(
               "Games",
               "Games",
               "SELECT g.* FROM Games g",
               "g.gid",
               Arrays.asList(
                  pk("gid", "g.gid", ColType.INT),
                  col("date_of_game", "g.date_of_game", ColType.DATE),
                  col("home_team", "g.home_team", ColType.STRING),
                  col("away_team", "g.away_team", ColType.STRING),
                  col("play_by_play", "g.play_by_play", ColType.STRING),
                  col("home_team_plays", "g.home_team_plays", ColType.STRING),
                  col("away_team_plays", "g.away_team_plays", ColType.STRING)
               )
            );
         case 10:
            return new SearchTableConfig(
               "Practices",
               "Practices",
               "SELECT p.* FROM Practices p",
               "p.date_of_practice, p.morning_afternoon",
               Arrays.asList(
                  pk("date_of_practice", "p.date_of_practice", ColType.DATE),
                  pk("morning_afternoon", "p.morning_afternoon", ColType.STRING)
               )
            );
         case 11:
         case 12: {
            boolean isHome = (choice == 11);
            String tbl = isHome ? "Offensive_Stats_Home" : "Offensive_Stats_Away";
            String playerTbl = isHome ? "Team_Players" : "Opposing_Players";
            String idCol = isHome ? "pid" : "opid";
            return new SearchTableConfig(
               tbl,
               tbl + " (joined with " + playerTbl + ")",
               "SELECT s.*, p.fname, p.lname, p.positions FROM " + tbl + " s " +
               "JOIN " + playerTbl + " p ON s." + idCol + " = p." + idCol,
               "s." + idCol,
               Arrays.asList(
                  pk(idCol, "s." + idCol, ColType.INT),
                  col("snaps_played", "s.snaps_played", ColType.INT),
                  col("passing_attempts", "s.passing_attempts", ColType.INT),
                  col("completions", "s.completions", ColType.INT),
                  col("passing_yards", "s.passing_yards", ColType.INT),
                  col("rushing_attempts", "s.rushing_attempts", ColType.INT),
                  col("rushing_yards", "s.rushing_yards", ColType.INT),
                  col("targets", "s.targets", ColType.INT),
                  col("catches", "s.catches", ColType.INT),
                  col("receiving_yards", "s.receiving_yards", ColType.INT),
                  col("blocking_attempts", "s.blocking_attempts", ColType.INT),
                  col("sacks_allowed", "s.sacks_allowed", ColType.INT),
                  col("pressures_allowed", "s.pressures_allowed", ColType.INT),
                  col("yards_gained_behind", "s.yards_gained_behind", ColType.DOUBLE),
                  col("fumbles", "s.fumbles", ColType.INT),
                  col("penalties", "s.penalties", ColType.INT)
               )
            );
         }
         case 13:
         case 14: {
            boolean isHome = (choice == 13);
            String tbl = isHome ? "Defensive_Stats_Home" : "Defensive_Stats_Away";
            String playerTbl = isHome ? "Team_Players" : "Opposing_Players";
            String idCol = isHome ? "pid" : "opid";
            return new SearchTableConfig(
               tbl,
               tbl + " (joined with " + playerTbl + ")",
               "SELECT s.*, p.fname, p.lname, p.positions FROM " + tbl + " s " +
               "JOIN " + playerTbl + " p ON s." + idCol + " = p." + idCol,
               "s." + idCol,
               Arrays.asList(
                  pk(idCol, "s." + idCol, ColType.INT),
                  col("snaps_played", "s.snaps_played", ColType.INT),
                  col("tackles", "s.tackles", ColType.INT),
                  col("sacks", "s.sacks", ColType.DOUBLE),
                  col("pressures", "s.pressures", ColType.INT),
                  col("pass_deflections", "s.pass_deflections", ColType.INT),
                  col("interceptions", "s.interceptions", ColType.INT),
                  col("fumbles_forced", "s.fumbles_forced", ColType.INT),
                  col("fumbles_recovered", "s.fumbles_recovered", ColType.INT),
                  col("penalties", "s.penalties", ColType.INT)
               )
            );
         }
         case 15:
         case 16: {
            boolean isHome = (choice == 15);
            String tbl = isHome ? "Special_Teams_Stats_Home" : "Special_Teams_Stats_Away";
            String playerTbl = isHome ? "Team_Players" : "Opposing_Players";
            String idCol = isHome ? "pid" : "opid";
            return new SearchTableConfig(
               tbl,
               tbl + " (joined with " + playerTbl + ")",
               "SELECT s.*, p.fname, p.lname, p.positions FROM " + tbl + " s " +
               "JOIN " + playerTbl + " p ON s." + idCol + " = p." + idCol,
               "s." + idCol,
               Arrays.asList(
                  pk(idCol, "s." + idCol, ColType.INT),
                  col("snaps_played", "s.snaps_played", ColType.INT),
                  col("punt_attempts", "s.punt_attempts", ColType.INT),
                  col("punt_yards", "s.punt_yards", ColType.INT),
                  col("field_goal_attempts", "s.field_goal_attempts", ColType.INT),
                  col("field_goals_made", "s.field_goals_made", ColType.INT),
                  col("field_goal_kick_lengths", "s.field_goal_kick_lengths", ColType.INT_ARRAY),
                  col("extra_point_attempts", "s.extra_point_attempts", ColType.INT),
                  col("extra_points_made", "s.extra_points_made", ColType.INT),
                  col("punts_blocked", "s.punts_blocked", ColType.INT),
                  col("kick_return_yards", "s.kick_return_yards", ColType.INT),
                  col("punt_return_yards", "s.punt_return_yards", ColType.INT),
                  col("penalties", "s.penalties", ColType.INT)
               )
            );
         }
         case 17:
            return new SearchTableConfig(
               "Who_Coaches",
               "Who_Coaches (joined with Coaches & Team_Players)",
               "SELECT wc.*, c.fname AS coach_fname, c.lname AS coach_lname, c.position AS coach_title, " +
               "tp.fname AS player_fname, tp.lname AS player_lname " +
               "FROM Who_Coaches wc " +
               "JOIN Coaches c ON wc.cid = c.cid " +
               "JOIN Team_Players tp ON wc.pid = tp.pid",
               "wc.date DESC, wc.cid, wc.pid",
               Arrays.asList(
                  pk("cid", "wc.cid", ColType.INT),
                  pk("pid", "wc.pid", ColType.INT),
                  col("affiliation", "wc.affiliation", ColType.STRING),
                  col("game_plan", "wc.game_plan", ColType.STRING),
                  pk("date", "wc.date", ColType.DATE)
               )
            );
         case 18:
            return new SearchTableConfig(
               "Coach_In_Game",
               "Coach_In_Game (joined with Coaches & Games)",
               "SELECT cig.*, c.fname AS coach_fname, c.lname AS coach_lname, c.position, " +
               "g.date_of_game, g.home_team, g.away_team " +
               "FROM Coach_In_Game cig " +
               "JOIN Coaches c ON cig.cid = c.cid " +
               "JOIN Games g ON cig.gid = g.gid",
               "cig.gid, cig.cid",
               Arrays.asList(
                  pk("cid", "cig.cid", ColType.INT),
                  pk("gid", "cig.gid", ColType.INT)
               )
            );
         case 19:
            return new SearchTableConfig(
               "Team_Plays",
               "Team_Plays (joined with Team_Players & Games)",
               "SELECT tpl.*, tp.fname, tp.lname, g.date_of_game, g.home_team, g.away_team " +
               "FROM Team_Plays tpl " +
               "JOIN Team_Players tp ON tpl.pid = tp.pid " +
               "JOIN Games g ON tpl.gid = g.gid",
               "tpl.gid, tpl.pid",
               Arrays.asList(
                  pk("pid", "tpl.pid", ColType.INT),
                  pk("gid", "tpl.gid", ColType.INT),
                  col("list_of_plays", "tpl.list_of_plays", ColType.STRING_ARRAY),
                  col("list_of_stats", "tpl.list_of_stats", ColType.STRING_ARRAY)
               )
            );
         case 20:
            return new SearchTableConfig(
               "Opposing_Plays",
               "Opposing_Plays (joined with Opposing_Players & Games)",
               "SELECT opl.*, op.fname, op.lname, op.team, g.date_of_game, g.home_team, g.away_team " +
               "FROM Opposing_Plays opl " +
               "JOIN Opposing_Players op ON opl.opid = op.opid " +
               "JOIN Games g ON opl.gid = g.gid",
               "opl.gid, opl.opid",
               Arrays.asList(
                  pk("opid", "opl.opid", ColType.INT),
                  pk("gid", "opl.gid", ColType.INT),
                  col("list_of_plays", "opl.list_of_plays", ColType.STRING_ARRAY),
                  col("list_of_stats", "opl.list_of_stats", ColType.STRING_ARRAY)
               )
            );
         case 21:
            return new SearchTableConfig(
               "Analyze_Team",
               "Analyze_Team (joined with Statistical_Team & Team_Players)",
               "SELECT at.*, st.fname AS analyst_fname, st.lname AS analyst_lname, " +
               "tp.fname AS player_fname, tp.lname AS player_lname " +
               "FROM Analyze_Team at " +
               "JOIN Statistical_Team st ON at.stid = st.stid " +
               "JOIN Team_Players tp ON at.pid = tp.pid",
               "at.date DESC, at.stid, at.pid",
               Arrays.asList(
                  pk("stid", "at.stid", ColType.INT),
                  pk("pid", "at.pid", ColType.INT),
                  col("analysis", "at.analysis", ColType.STRING),
                  pk("date", "at.date", ColType.DATE)
               )
            );
         case 22:
            return new SearchTableConfig(
               "Analyze_Opposing",
               "Analyze_Opposing (joined with Statistical_Team & Opposing_Players)",
               "SELECT ao.*, st.fname AS analyst_fname, st.lname AS analyst_lname, " +
               "op.fname AS player_fname, op.lname AS player_lname, op.team " +
               "FROM Analyze_Opposing ao " +
               "JOIN Statistical_Team st ON ao.stid = st.stid " +
               "JOIN Opposing_Players op ON ao.opid = op.opid",
               "ao.date DESC, ao.stid, ao.opid",
               Arrays.asList(
                  pk("stid", "ao.stid", ColType.INT),
                  pk("opid", "ao.opid", ColType.INT),
                  col("analysis", "ao.analysis", ColType.STRING),
                  pk("date", "ao.date", ColType.DATE)
               )
            );
         case 23:
            return new SearchTableConfig(
               "Practices_In",
               "Practices_In (joined with Team_Players)",
               "SELECT pi.*, tp.fname, tp.lname, tp.positions " +
               "FROM Practices_In pi " +
               "JOIN Team_Players tp ON pi.pid = tp.pid",
               "pi.date_of_practice DESC, pi.morning_afternoon, pi.pid",
               Arrays.asList(
                  pk("pid", "pi.pid", ColType.INT),
                  pk("date_of_practice", "pi.date_of_practice", ColType.DATE),
                  pk("morning_afternoon", "pi.morning_afternoon", ColType.STRING),
                  col("did_practice", "pi.did_practice", ColType.BOOLEAN),
                  col("excused_unexcused", "pi.excused_unexcused", ColType.STRING)
               )
            );
         case 24:
            return new SearchTableConfig(
               "Manages_reg_staff",
               "Manages_reg_staff (joined with Managers & Regular_Staff)",
               "SELECT mrs.*, m.fname AS manager_fname, m.lname AS manager_lname, " +
               "rs.fname AS staff_fname, rs.lname AS staff_lname, rs.job_title " +
               "FROM Manages_reg_staff mrs " +
               "JOIN Managers m ON mrs.mid = m.mid " +
               "JOIN Regular_Staff rs ON mrs.jib = rs.jib",
               "mrs.mid, mrs.jib",
               Arrays.asList(
                  pk("mid", "mrs.mid", ColType.INT),
                  pk("jib", "mrs.jib", ColType.INT),
                  col("days", "mrs.days", ColType.INT),
                  col("schedule", "mrs.schedule", ColType.STRING_ARRAY)
               )
            );
         case 25:
            return new SearchTableConfig(
               "Inspects",
               "Inspects (joined with Team_Doctors & Team_Players)",
               "SELECT i.*, td.fname AS doctor_fname, td.lname AS doctor_lname, td.speciality, " +
               "tp.fname AS player_fname, tp.lname AS player_lname " +
               "FROM Inspects i " +
               "JOIN Team_Doctors td ON i.did = td.did " +
               "JOIN Team_Players tp ON i.pid = tp.pid",
               "i.date DESC, i.did, i.pid",
               Arrays.asList(
                  pk("did", "i.did", ColType.INT),
                  pk("pid", "i.pid", ColType.INT),
                  col("diagnosis", "i.diagnosis", ColType.STRING),
                  col("treatment_length", "i.treatment_length", ColType.INT),
                  pk("date", "i.date", ColType.DATE)
               )
            );
         case 26:
            return new SearchTableConfig(
               "Works_on",
               "Works_on (joined with Trainers & Team_Players)",
               "SELECT w.*, tr.fname AS trainer_fname, tr.lname AS trainer_lname, " +
               "tp.fname AS player_fname, tp.lname AS player_lname " +
               "FROM Works_on w " +
               "JOIN Trainers tr ON w.tid = tr.tid " +
               "JOIN Team_Players tp ON w.pid = tp.pid",
               "w.date DESC, w.tid, w.pid",
               Arrays.asList(
                  pk("tid", "w.tid", ColType.INT),
                  pk("pid", "w.pid", ColType.INT),
                  col("condition_of_player", "w.condition_of_player", ColType.STRING),
                  col("recommends", "w.recommends", ColType.BOOLEAN),
                  pk("date", "w.date", ColType.DATE)
               )
            );
         default:
            return null;
      }
   }

   private static Object readTypedScalar(String prompt, ColType type, boolean required) throws IOException {
      switch (type) {
         case INT:
            return readInt(prompt, required, null, null);
         case DOUBLE:
            return readDouble(prompt, required, null, false);
         case DATE:
            return readDate(prompt, required);
         case BOOLEAN:
            return readBoolean(prompt, required);
         case STRING:
         default:
            return required ? readRequiredString(prompt) : readOptionalString(prompt);
      }
   }

   private static void bindQueryParams(Connection conn, PreparedStatement ps, List<QueryParam> params) throws SQLException {
      for (int i = 0; i < params.size(); i++) {
         int idx = i + 1;
         QueryParam qp = params.get(i);
         if (qp.value == null) {
            switch (qp.type) {
               case INT:          ps.setNull(idx, Types.INTEGER); break;
               case DOUBLE:       ps.setNull(idx, Types.DOUBLE); break;
               case DATE:         ps.setNull(idx, Types.DATE); break;
               case BOOLEAN:      ps.setNull(idx, Types.BOOLEAN); break;
               case INT_ARRAY:
               case STRING_ARRAY: ps.setNull(idx, Types.ARRAY); break;
               case STRING:
               default:           ps.setNull(idx, Types.VARCHAR); break;
            }
            continue;
         }
         switch (qp.type) {
            case INT:
               ps.setInt(idx, (Integer) qp.value);
               break;
            case DOUBLE:
               ps.setDouble(idx, (Double) qp.value);
               break;
            case DATE:
               ps.setDate(idx, (Date) qp.value);
               break;
            case BOOLEAN:
               ps.setBoolean(idx, (Boolean) qp.value);
               break;
            case INT_ARRAY:
               setNullableIntArray(conn, ps, idx, (Integer[]) qp.value);
               break;
            case STRING_ARRAY:
               setNullableStringArray(conn, ps, idx, (String[]) qp.value, "text");
               break;
            case STRING:
            default:
               ps.setString(idx, (String) qp.value);
               break;
         }
      }
   }

   // =========================================================================
   // QUERY 1: SEARCH & RETRIEVE RECORDS ACROSS ALL 26 TABLES (WITH JOINS)
   // =========================================================================

   public static void Query1(EmbeddedSQL esql) {
      printAllTablesMenu("SEARCH AND RETRIEVE RECORDS");

      int choice = readChoice();
      if (choice == 0) return;

      SearchTableConfig config = getTableConfig(choice);
      if (config == null) {
         System.out.println("Invalid table selection.");
         return;
      }

      Connection conn = esql.getConnection();
      try {
         System.out.println("\nSelected Table: " + config.tableName);
         System.out.println("Search Mode:");
         System.out.println("  1. View all records (no filters)");
         System.out.println("  2. Filter by specific attributes (equality or range)");
         int mode = readInt("Choose mode (1 or 2): ", true, 1, 2);

         List<String> whereClauses = new ArrayList<>();
         List<QueryParam> params = new ArrayList<>();

         if (mode == 2) {
            boolean addMore = true;
            while (addMore) {
               System.out.println("\nAvailable attributes in " + config.tableName + ":");
               for (int i = 0; i < config.columns.size(); i++) {
                  SearchColumn sc = config.columns.get(i);
                  System.out.println("  " + (i + 1) + ". " + sc.label + " (" + sc.type + ")");
               }
               int colIdx = readInt("Select attribute number to filter on: ", true, 1, config.columns.size()) - 1;
               SearchColumn selectedCol = config.columns.get(colIdx);

               if (selectedCol.type == ColType.INT || selectedCol.type == ColType.DOUBLE || selectedCol.type == ColType.DATE) {
                  System.out.println("Filter type for '" + selectedCol.label + "':");
                  System.out.println("  1. Exact match (=)");
                  System.out.println("  2. Range (min and/or max)");
                  int opType = readInt("Choose filter type (1 or 2): ", true, 1, 2);

                  if (opType == 1) {
                     Object val = readTypedScalar("Enter exact value for " + selectedCol.label + ": ", selectedCol.type, true);
                     whereClauses.add(selectedCol.sqlExpr + " = ?");
                     params.add(new QueryParam(selectedCol.type, val));
                  } else {
                     System.out.println("Enter range bounds (press Enter to leave a bound open):");
                     Object minVal = readTypedScalar("  Minimum " + selectedCol.label + " (>=): ", selectedCol.type, false);
                     Object maxVal = readTypedScalar("  Maximum " + selectedCol.label + " (<=): ", selectedCol.type, false);
                     if (minVal == null && maxVal == null) {
                        System.out.println("  [Notice] Both bounds left empty; skipping filter on " + selectedCol.label + ".");
                     } else {
                        if (minVal != null) {
                           whereClauses.add(selectedCol.sqlExpr + " >= ?");
                           params.add(new QueryParam(selectedCol.type, minVal));
                        }
                        if (maxVal != null) {
                           whereClauses.add(selectedCol.sqlExpr + " <= ?");
                           params.add(new QueryParam(selectedCol.type, maxVal));
                        }
                     }
                  }
               } else if (selectedCol.type == ColType.STRING) {
                  System.out.println("Filter type for '" + selectedCol.label + "':");
                  System.out.println("  1. Exact match (=)");
                  System.out.println("  2. Partial / substring match (case-insensitive ILIKE)");
                  int opType = readInt("Choose filter type (1 or 2): ", true, 1, 2);

                  String val = readRequiredString("Enter text value for " + selectedCol.label + ": ");
                  if (opType == 1) {
                     whereClauses.add("LOWER(" + selectedCol.sqlExpr + ") = LOWER(?)");
                     params.add(new QueryParam(ColType.STRING, val));
                  } else {
                     whereClauses.add(selectedCol.sqlExpr + " ILIKE ?");
                     params.add(new QueryParam(ColType.STRING, "%" + val + "%"));
                  }
               } else if (selectedCol.type == ColType.BOOLEAN) {
                  Boolean val = readBoolean("Enter boolean value for " + selectedCol.label + " (true/false): ", true);
                  whereClauses.add(selectedCol.sqlExpr + " = ?");
                  params.add(new QueryParam(ColType.BOOLEAN, val));
               } else if (selectedCol.type == ColType.INT_ARRAY) {
                  Integer elem = readInt("Enter integer element to search inside array " + selectedCol.label + ": ", true, null, null);
                  whereClauses.add("? = ANY(" + selectedCol.sqlExpr + ")");
                  params.add(new QueryParam(ColType.INT, elem));
               } else if (selectedCol.type == ColType.STRING_ARRAY) {
                  String elem = readRequiredString("Enter text element to search inside array " + selectedCol.label + ": ");
                  whereClauses.add("EXISTS (SELECT 1 FROM unnest(" + selectedCol.sqlExpr + ") AS elem WHERE LOWER(elem) = LOWER(?))");
                  params.add(new QueryParam(ColType.STRING, elem));
               }

               addMore = readBoolean("Add another filter condition (AND)? (yes/no): ", true);
            }
         }

         StringBuilder sql = new StringBuilder(config.baseSelectFrom);
         if (!whereClauses.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", whereClauses));
         }
         if (config.defaultOrderBy != null && !config.defaultOrderBy.isEmpty()) {
            sql.append(" ORDER BY ").append(config.defaultOrderBy);
         }

         System.out.println("\nExecuting query...\n");
         try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bindQueryParams(conn, ps, params);
            try (ResultSet rs = ps.executeQuery()) {
               int rowCount = printSmartResultSet(rs);
               System.out.println("\ntotal row(s): " + rowCount);
            }
         }
      } catch (SQLException e) {
         System.err.println("\n[Database Error] Search failed (SQLState: " + e.getSQLState() + "):");
         System.err.println("  " + e.getMessage());
      } catch (IOException e) {
         System.err.println("\n[Input Error] Failed reading input: " + e.getMessage());
      }
   }

   // =========================================================================
   // QUERY 2: UPDATE EXISTING RECORDS IN ANY OF THE 26 TABLES (BEFORE & AFTER)
   // =========================================================================

   private static Object readUpdateValue(String tableName, SearchColumn col) throws IOException {
      String colName = col.label;

      // Table- and column-specific domain validation to catch invalid updates early
      if (colName.equals("morning_afternoon")) {
         return readEnum("New value for " + colName + " ('Morning' or 'Afternoon'): ",
                         true, Arrays.asList("Morning", "Afternoon"));
      }
      if (colName.equals("regular_practice_squad")) {
         return readEnum("New value for " + colName + " ('Regular' or 'Practice Squad'): ",
                         true, Arrays.asList("Regular", "Practice Squad"));
      }
      if (colName.equals("defensive_offensive")) {
         return readEnum("New value for " + colName + " ('Offensive', 'Defensive', 'Special Teams', 'Both', or Enter for NULL): ",
                         false, Arrays.asList("Offensive", "Defensive", "Special Teams", "Both"));
      }
      if (colName.equals("focus")) {
         return readEnum("New value for " + colName + " ('Offensive', 'Defensive', 'Both'): ",
                         true, Arrays.asList("Offensive", "Defensive", "Both"));
      }
      if (colName.equals("excused_unexcused")) {
         return readEnum("New value for " + colName + " ('Excused', 'Unexcused', or Enter for NULL): ",
                         false, Arrays.asList("Excused", "Unexcused"));
      }
      if (colName.equals("jersey_number")) {
         return readInt("New value for jersey_number (0-99, or Enter for NULL): ", false, 0, 99);
      }
      if (colName.equals("days")) {
         return readInt("New value for days (1-7, or Enter for NULL): ", false, 1, 7);
      }
      if (colName.equals("age")) {
         int minAge = tableName.equals("Regular_Staff") ? 16 : 18;
         return readInt("New value for age (>= " + minAge + ", or Enter for NULL): ", false, minAge, null);
      }
      if (colName.equals("height") || colName.equals("weight")) {
         return readDouble("New value for " + colName + " (> 0, or Enter for NULL): ", false, 0.0, true);
      }
      if (colName.equals("salary")) {
         return readDouble("New value for salary (>= 0, or Enter for NULL): ", false, 0.0, false);
      }
      if (colName.equals("experience") || colName.equals("treatment_length")) {
         return readInt("New value for " + colName + " (>= 0, or Enter for NULL): ", false, 0, null);
      }
      if (colName.equals("positions")) {
         return readRequiredString("New value for positions (required, e.g., 'QB' or 'WR, KR'): ").toLowerCase();
      }
      if (colName.equals("fname") || colName.equals("lname") || colName.equals("team")
          || colName.equals("position") || colName.equals("job_title")
          || colName.equals("home_team") || colName.equals("away_team")
          || colName.equals("health_status")) {
         return readRequiredString("New value for " + colName + " (required, cannot be empty): ");
      }
      if (colName.equals("mid") && (tableName.equals("Team_Players") || tableName.equals("Coaches"))) {
         return readInt("New value for mid (required existing Manager ID): ", true, null, null);
      }
      if (col.isPk) {
         return readTypedScalar("New primary key value for " + colName + " (required): ", col.type, true);
      }

      // Generic handling for remaining columns (press Enter to set NULL where allowed)
      switch (col.type) {
         case INT:
            return readInt("New integer value for " + colName + " (or Enter for NULL): ", false, null, null);
         case DOUBLE:
            return readDouble("New numeric value for " + colName + " (or Enter for NULL): ", false, null, false);
         case DATE:
            return readDate("New date for " + colName + " (YYYY-MM-DD, or Enter for NULL): ", false);
         case BOOLEAN:
            return readBoolean("New boolean for " + colName + " (true/false, or Enter for NULL): ", false);
         case INT_ARRAY:
            return readIntArray("New comma-separated integers for " + colName + " (e.g., 42, 35, 51, or Enter for NULL): ");
         case STRING_ARRAY:
            return readStringArray("New comma-separated items for " + colName + " (or Enter for NULL): ");
         case STRING:
         default:
            return readOptionalString("New text for " + colName + " (or Enter for NULL): ");
      }
   }

   public static void Query2(EmbeddedSQL esql) {
      printAllTablesMenu("UPDATE EXISTING RECORD (WITH BEFORE & AFTER VIEW)");

      int choice = readChoice();
      if (choice == 0) return;

      SearchTableConfig config = getTableConfig(choice);
      if (config == null) {
         System.out.println("Invalid table selection.");
         return;
      }

      Connection conn = esql.getConnection();
      try {
         System.out.println("\nSelected Table: " + config.tableName);

         // Optional quick lookup so user can see existing primary keys
         boolean showCurrent = readBoolean("Would you like to view current records in " + config.rawTableName + " first? (yes/no): ", true);
         if (showCurrent) {
            String listSql = config.baseSelectFrom +
                             (config.defaultOrderBy != null ? " ORDER BY " + config.defaultOrderBy : "");
            try (PreparedStatement listPs = conn.prepareStatement(listSql);
                 ResultSet listRs = listPs.executeQuery()) {
               printSmartResultSet(listRs);
            }
         }

         // Step 1: Identify target record by Primary Key(s)
         List<SearchColumn> pkCols = config.getPrimaryKeys();
         System.out.println("\nStep 1: Enter the Primary Key value(s) of the record to update:");
         List<QueryParam> pkParams = new ArrayList<>();
         List<String> selectWhere = new ArrayList<>();
         List<String> updateWhere = new ArrayList<>();

         for (SearchColumn pkCol : pkCols) {
            Object pkVal = readTypedScalar("  " + pkCol.label + " (" + pkCol.type + ", required): ", pkCol.type, true);
            pkParams.add(new QueryParam(pkCol.type, pkVal));
            if (pkCol.type == ColType.STRING) {
               selectWhere.add("LOWER(" + pkCol.sqlExpr + ") = LOWER(?)");
               updateWhere.add("LOWER(" + pkCol.label + ") = LOWER(?)");
            } else {
               selectWhere.add(pkCol.sqlExpr + " = ?");
               updateWhere.add(pkCol.label + " = ?");
            }
         }

         // Step 2: Fetch and display BEFORE state
         String fetchSql = config.baseSelectFrom + " WHERE " + String.join(" AND ", selectWhere);
         System.out.println("\n--- BEFORE UPDATE ---");
         int foundRows;
         try (PreparedStatement beforePs = conn.prepareStatement(fetchSql)) {
            bindQueryParams(conn, beforePs, pkParams);
            try (ResultSet beforeRs = beforePs.executeQuery()) {
               foundRows = printSmartResultSet(beforeRs);
            }
         }

         if (foundRows == 0) {
            System.out.println("[Notice] No matching record found with the given primary key(s). Update aborted.");
            return;
         }

         // Step 3: Choose columns to modify and validate new values
         System.out.println("\nStep 2: Choose attribute(s) to modify:");
         List<String> setClauses = new ArrayList<>();
         List<QueryParam> setParams = new ArrayList<>();
         List<QueryParam> updatedPkParams = new ArrayList<>(pkParams);

         boolean moreCols = true;
         while (moreCols) {
            System.out.println("\nEditable attributes in " + config.rawTableName + ":");
            for (int i = 0; i < config.columns.size(); i++) {
               SearchColumn sc = config.columns.get(i);
               String tag = sc.isPk ? " [PRIMARY KEY]" : "";
               System.out.println("  " + (i + 1) + ". " + sc.label + " (" + sc.type + ")" + tag);
            }
            int colIdx = readInt("Select attribute number to update: ", true, 1, config.columns.size()) - 1;
            SearchColumn targetCol = config.columns.get(colIdx);

            Object newVal = readUpdateValue(config.rawTableName, targetCol);
            setClauses.add(targetCol.label + " = ?");
            setParams.add(new QueryParam(targetCol.type, newVal));

            // If user updates a primary key column, track its new value so we can fetch the AFTER row
            if (targetCol.isPk) {
               for (int k = 0; k < pkCols.size(); k++) {
                  if (pkCols.get(k).label.equals(targetCol.label)) {
                     updatedPkParams.set(k, new QueryParam(targetCol.type, newVal));
                  }
               }
            }

            moreCols = readBoolean("Modify another attribute on this record? (yes/no): ", true);
         }

         // Step 4: Execute UPDATE inside a transaction to preserve database integrity
         String updateSql = "UPDATE " + config.rawTableName +
                            " SET " + String.join(", ", setClauses) +
                            " WHERE " + String.join(" AND ", updateWhere);

         boolean prevAutoCommit = conn.getAutoCommit();
         conn.setAutoCommit(false);
         try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
            List<QueryParam> allParams = new ArrayList<>(setParams);
            allParams.addAll(pkParams);
            bindQueryParams(conn, updatePs, allParams);

            int affected = updatePs.executeUpdate();
            conn.commit();
            System.out.println("\nSuccess: " + affected + " record(s) updated in " + config.rawTableName + ".");
         } catch (SQLException sqlEx) {
            conn.rollback();
            System.err.println("\n[Database Integrity Error] Update rejected and rolled back (SQLState: " + sqlEx.getSQLState() + "):");
            System.err.println("  " + sqlEx.getMessage());
            System.out.println("\n--- RECORD UNCHANGED (ROLLBACK VERIFIED) ---");
            try (PreparedStatement checkPs = conn.prepareStatement(fetchSql)) {
               bindQueryParams(conn, checkPs, pkParams);
               try (ResultSet checkRs = checkPs.executeQuery()) {
                  printSmartResultSet(checkRs);
               }
            }
            return;
         } finally {
            conn.setAutoCommit(prevAutoCommit);
         }

         // Step 5: Fetch and display AFTER state
         System.out.println("\n--- AFTER UPDATE ---");
         try (PreparedStatement afterPs = conn.prepareStatement(fetchSql)) {
            bindQueryParams(conn, afterPs, updatedPkParams);
            try (ResultSet afterRs = afterPs.executeQuery()) {
               printSmartResultSet(afterRs);
            }
         }

      } catch (SQLException e) {
         System.err.println("\n[Database Error] Operation failed (SQLState: " + e.getSQLState() + "):");
         System.err.println("  " + e.getMessage());
      } catch (IOException e) {
         System.err.println("\n[Input Error] Failed reading input: " + e.getMessage());
      }
   }

   // =========================================================================
   // REMAINING QUERIES
   // =========================================================================

   public static void Query3(EmbeddedSQL esql){
      try{
         String query = "SELECT * FROM vw_franchise_payroll;";

         int rowCount = esql.executeQuery(query);
         System.out.println("total row(s): " + rowCount);
      }catch(Exception e){
         System.err.println(e.getMessage());
      }
   }

   public static void Query4(EmbeddedSQL esql){
      try{
         String query =
            "SELECT s.sname, MAX(c.cost) " +
            "FROM suppliers s " +
            "JOIN catalog c ON c.sid = s.sid " +
            "WHERE s.sid IN ( " +
            "    SELECT c.sid " +
            "    FROM catalog c " +
            "    JOIN parts p ON p.pid = c.pid " +
            "    WHERE p.color = 'Green' " +
            "    INTERSECT " +
            "    SELECT c.sid " +
            "    FROM catalog c " +
            "    JOIN parts p ON p.pid = c.pid " +
            "    WHERE p.color = 'Red' " +
            ") " +
            "GROUP BY s.sname";

         int rowCount = esql.executeQuery(query);
         System.out.println("total row(s): " + rowCount);
      }catch(Exception e){
         System.err.println(e.getMessage());
      }
   }

   public static void Query5(EmbeddedSQL esql){
      try{
         String query = 
            "SELECT p.pname " +
            "FROM catalog c " +
            "JOIN parts p ON p.pid = c.pid " +
            "WHERE c.cost < ";

         System.out.print("\tEnter cost: $");
         String input = in.readLine();
         query += input;

         int rowCount = esql.executeQuery(query);
         System.out.println ("total row(s): " + rowCount);
      }catch(Exception e){
         System.err.println (e.getMessage());
      }
   }

   public static void Query6(EmbeddedSQL esql){
      try{
         String query = 
            "SELECT s.address " +
            "FROM suppliers s " +
            "JOIN catalog c ON c.sid = s.sid " +
            "JOIN parts p ON p.pid = c.pid " +
            "WHERE LOWER(p.pname) = LOWER(";

         System.out.print("\tEnter parts name: $");
         String input = in.readLine().trim().toLowerCase();
         query += "'" + input.replace("'", "''") + "')";

         int rowCount = esql.executeQuery(query);
         System.out.println ("total row(s): " + rowCount);
      }catch(Exception e){
         System.err.println (e.getMessage());
      }
   }

}
