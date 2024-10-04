import java.io.*;
import java.nio.file.*;
import java.util.*;

public class CsvToSqlScript {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // User inputs
        System.out.print("Enter the CSV file path: ");
        String csvFilePath = scanner.nextLine();  // Path to the input CSV file

        System.out.print("Enter the SQL output file path: ");
        String sqlOutputFilePath = scanner.nextLine();  // Path to the output SQL file

        System.out.print("Enter the table name: ");
        String tableName = scanner.nextLine();

        System.out.print("Enter the column names (comma-separated): ");
        String columnNamesInput = scanner.nextLine();
        List<String> columnNames = new ArrayList<>(Arrays.asList(columnNamesInput.split(",")));

        System.out.print("Enter the column numbers to be enclosed in single quotes (comma-separated, 1-based index): ");
        String quoteColumnsInput = scanner.nextLine();
        Set<Integer> quoteColumns = parseIndexes(quoteColumnsInput);

        System.out.print("Enter the column number for which to apply mod function (comma-separated, format: column_number:mod_value): ");
        String modColumnsInput = scanner.nextLine();
        Map<Integer, Integer> modColumns = parseModColumns(modColumnsInput);

        // Check if there are any new columns to add with default values
        List<String> newColumns = new ArrayList<>();
        List<String> newColumnDefaults = new ArrayList<>();
        List<String> newColumnTypes = new ArrayList<>();

        System.out.print("Do you want to add any new columns to the table? (yes/no): ");
        String addNewColumns = scanner.nextLine().trim().toLowerCase();

        if (addNewColumns.equals("yes")) {
            System.out.print("Enter new column names with data types (comma-separated, format: column_name:data_type): ");
            String newColumnsInput = scanner.nextLine();
            for (String columnWithType : newColumnsInput.split(",")) {
                String[] columnDetails = columnWithType.split(":");
                String columnName = columnDetails[0].trim();
                String dataType = columnDetails[1].trim().toLowerCase();
                newColumns.add(columnName);
                newColumnTypes.add(dataType);

                System.out.print("Enter the default value for column '" + columnName + "' (or 'null' for NULL value): ");
                String defaultValue = scanner.nextLine().trim();

                if (defaultValue.equalsIgnoreCase("null")) {
                    newColumnDefaults.add("NULL");
                } else if (dataType.equals("string")) {
                    newColumnDefaults.add("'" + defaultValue + "'");
                } else {
                    newColumnDefaults.add(defaultValue);
                }
            }
        }

        try {
            List<String> lines = Files.readAllLines(Paths.get(csvFilePath));
            StringBuilder sqlScript = new StringBuilder();

            // Iterate over each row in the CSV file
            for (String line : lines) {
                String[] columns = line.split(",");

                // Build the SQL insert statement
                StringBuilder valuesBuilder = new StringBuilder();
                List<String> modResults = new ArrayList<>();

                // Process existing columns with quotes and mods
                for (int i = 0; i < columns.length; i++) {
                    String columnValue = columns[i].trim();

                    // Handle dates starting from 2024
                    if (columnValue.startsWith("2024")) {
                        columnValue = "LOCALTIMESTAMP"; // Replace date with LOCALTIMESTAMP
                    }

                    // Apply quoting logic
                    if (quoteColumns.contains(i + 1)) {
                        columnValue = "'" + columnValue + "'";
                    }

                    // If column is marked for mod calculation, store mod result in modResults list
                    if (modColumns.containsKey(i + 1)) {
                        int columnValInt = Integer.parseInt(columns[i].trim());
                        int modValue = columnValInt % modColumns.get(i + 1);
                        modResults.add(String.valueOf(modValue)); // Store mod result
                    }

                    valuesBuilder.append(columnValue);
                    if (i < columns.length - 1) {
                        valuesBuilder.append(", ");
                    }
                }

                // Append mod results first (before new columns)
                for (String modResult : modResults) {
                    valuesBuilder.append(", ").append(modResult);
                }

                // Add default values for new columns with correct data type
                for (int i = 0; i < newColumnDefaults.size(); i++) {
                    String defaultValue = newColumnDefaults.get(i);
                    valuesBuilder.append(", ").append(defaultValue);
                }

                // Build the final list of column names, including new columns
                List<String> fullColumnNames = new ArrayList<>(columnNames);
                fullColumnNames.addAll(newColumns);

                // Build and append the complete SQL insert statement
                String sqlInsert = String.format(
                        "INSERT INTO %s (%s) VALUES (%s);",
                        tableName.trim(),
                        String.join(", ", fullColumnNames),
                        valuesBuilder.toString()
                );
                sqlScript.append(sqlInsert).append("\n");
            }

            // Write the script to a file
            Files.write(Paths.get(sqlOutputFilePath), sqlScript.toString().getBytes());
            System.out.println("SQL script generated successfully!");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Parse column indexes from user input for quoting
    private static Set<Integer> parseIndexes(String input) {
        Set<Integer> indexes = new HashSet<>();
        if (input != null && !input.trim().isEmpty()) {
            String[] parts = input.split(",");
            for (String part : parts) {
                indexes.add(Integer.parseInt(part.trim()));
            }
        }
        return indexes;
    }

    // Parse mod column details from user input (e.g., "3:11" -> mod column 3 with divisor 11)
    private static Map<Integer, Integer> parseModColumns(String input) {
        Map<Integer, Integer> modColumns = new HashMap<>();
        if (input != null && !input.trim().isEmpty()) {
            String[] parts = input.split(",");
            for (String part : parts) {
                String[] modDetails = part.split(":");
                int columnIndex = Integer.parseInt(modDetails[0].trim());
                int modValue = Integer.parseInt(modDetails[1].trim());
                modColumns.put(columnIndex, modValue);
            }
        }
        return modColumns;
    }
}
