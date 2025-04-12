package cs321.search;

public class SSHSearchBTree {
   /**
     * Main method to parse CLI args and construct the program.
     *
     * @param argv Command-line arguments
     */
    public static void main(String[] argv) {
        try {
            SSHSearchBTreeArguments parsedArgs = new SSHSearchBTreeArguments(argv);
            new SSHSearchBTree(parsedArgs); 
        } catch (IllegalArgumentException e) {
            System.err.println("Argument error: " + e.getMessage());
            System.exit(1);
        }
    }
	/**
     * Constructs and runs the BTree search using validated arguments.
     *
     * @param args Parsed command-line arguments
     */
    public SSHSearchBTree(SSHSearchBTreeArguments args) {
        // Execution logic placed directly inside constructor
        // TODO: Insert actual BTree loading and querying logic here
    }
}

