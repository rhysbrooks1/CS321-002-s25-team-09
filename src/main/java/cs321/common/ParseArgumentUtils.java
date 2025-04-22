package cs321.common;

/**
 * Utility class for validating and converting argument values in a standardized way.
 */
public class ParseArgumentUtils {

    /**
     * Verifies that the given integer falls within an inclusive range.
     * Throws ParseArgumentException if it does not.
     *
     * @param argument the integer value to check
     * @param lowRangeInclusive minimum allowed value (inclusive)
     * @param highRangeInclusive maximum allowed value (inclusive)
     * @throws ParseArgumentException if the value is out of bounds
     */
    public static void verifyRanges(int argument, int lowRangeInclusive, int highRangeInclusive) throws ParseArgumentException {
        if (argument < lowRangeInclusive || argument > highRangeInclusive) {
            throw new ParseArgumentException(
                "Value " + argument + " is out of range. Expected between " +
                lowRangeInclusive + " and " + highRangeInclusive + ".");
        }
    }

    /**
     * Converts a string to an integer and throws a friendly exception if parsing fails.
     *
     * @param argument the string to parse
     * @return the parsed integer
     * @throws ParseArgumentException if the string is not a valid integer
     */
    public static int convertStringToInt(String argument) throws ParseArgumentException {
        try {
            return Integer.parseInt(argument);
        } catch (NumberFormatException e) {
            throw new ParseArgumentException("Invalid integer value: '" + argument + "'");
        }
    }
}
