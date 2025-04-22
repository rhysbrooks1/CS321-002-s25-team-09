package cs321.create;

import cs321.common.ParseArgumentException;

import java.util.HashMap;
import java.util.Map;

/**
 * The driver class for wrangling a raw SSH log file into a useful form.
 * Converts raw logs into stripped format for downstream BTree creation.
 */
public class SSHDataWrangler {

    private static String rawSSHFile;
    private static String SSHFile;

    /**
     * Main driver of the program.
     * @param args Command-line arguments
     */
    public static void main(String[] args) throws Exception {
        System.out.println("Starting SSHDataWrangler...");

        try {
            parseArguments(args);
        } catch (ParseArgumentException e) {
            printUsageAndExit(e.getMessage());
        }

        // TODO: Perform actual wrangling logic here
        System.out.println("Raw file: " + rawSSHFile);
        System.out.println("Output file: " + SSHFile);
    }

    /**
     * Processes command line arguments.
     * Expected format:
     * --rawSshFile=<inputFile>
     * --sshFile=<outputFile>
     */
    public static void parseArguments(String[] args) throws ParseArgumentException {
        Map<String, String> argMap = new HashMap<>();

        for (String arg : args) {
            if (arg.startsWith("--") && arg.contains("=")) {
                String[] parts = arg.substring(2).split("=", 2);
                if (parts.length == 2) {
                    argMap.put(parts[0], parts[1]);
                }
            }
        }

        rawSSHFile = argMap.get("rawSshFile");
        SSHFile = argMap.get("sshFile");

        if (rawSSHFile == null || SSHFile == null) {
            throw new ParseArgumentException("Both --rawSshFile and --sshFile arguments are required.");
        }
    }

    /**
     * Prints usage message and exits the program.
     * @param errorMessage Description of what went wrong
     */
    private static void printUsageAndExit(String errorMessage) {
        System.err.println("Error: " + errorMessage);
        System.err.println("Usage: java -jar SSHDataWrangler.jar --rawSshFile=<rawLog> --sshFile=<strippedLog>");
        System.exit(1);
    }
}
