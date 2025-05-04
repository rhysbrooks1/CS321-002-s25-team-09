package cs321.search;

import java.sql.*;

/**
 * The driver class for searching a Database of a B-Tree.
 *
 * @author Devyn Korber
 */
public class SSHSearchDatabase {
    private Connection connection;

    /**
     * Constructor for initializing the database connection
     * 
     * @param params created from the arguments class
     */
    public SSHSearchDatabase(SSHSearchDatabaseArguments params) {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + params.getDatabase());
            System.out.println("Successfully connected to the database: " + connection);
            search(params.getTopFrequency(), params.getTreeType());
            close();
        } catch (SQLException e) {
            System.err.println("Connection error: " + e.getMessage());
        }
    }

    /**
     * Method to search the database
     * 
     * @param topFrequency - the top frequency command line argument
     * @param treeType     - the type of tree from the command line argument
     */
    public void search(int topFrequency, String treeType) {
        treeType = treeType.replaceAll("-", "_");
        String selectSQL = "SELECT key, frequency FROM " + treeType +
                " GROUP BY key ORDER BY frequency DESC LIMIT ?";

        try (PreparedStatement ps = connection.prepareStatement(selectSQL)) {
            ps.setInt(1, topFrequency);
            ResultSet rs = ps.executeQuery();

            // Iterate through select SQL results
            System.out.println("Key\t\t\t\t|\tFrequency");
            while (rs.next()) {
                String key = rs.getString("key");
                int freq = rs.getInt("frequency");
                System.out.println(key + "\t|\t" + freq);
            }
        } catch (SQLException e) {
            System.err.println("Search error: " + e.getMessage());
        }

    }

    /**
     * Method to close the db connection
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Successfully closed connection." + "\n");
            }
        } catch (SQLException e) {
            System.err.println("Closing error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            new SSHSearchDatabase(new SSHSearchDatabaseArguments(args));
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
