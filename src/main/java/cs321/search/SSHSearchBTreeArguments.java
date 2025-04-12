package cs321.search;

/**
 * Parses and validates command-line arguments for the SSHSearchBTree program.
 *
 * This class is responsible for interpreting arguments passed to:
 *
 *   java -jar build/libs/SSHSearchBTree.jar
 *
 * Required command-line arguments:
 *
 *   --cache=<0|1>
 *     Enables (1) or disables (0) the use of an in-memory cache for BTree nodes.
 *
 *   --degree=<btree-degree>
 *     Specifies the degree of the BTree.
 *     Use 0 to auto-calculate the optimal degree based on 4096-byte node size.
 *
 *   --btree-file=<btree-filename>
 *     The path to the binary file containing the serialized BTree.
 *
 *   --query-file=<query-filename>
 *     A file containing one query key per line to search within the BTree.
 *
 * Optional command-line arguments:
 *
 *   --top-frequency=<10|25|50>
 *     If provided, only the top N results will be returned based on frequency.
 *     Defaults to no frequency filtering if omitted.
 *
 *   --cache-size=<n>
 *     The maximum number of BTreeNode objects that can be stored in memory.
 *     Required if --cache=1. Must be between 100 and 10000.
 *
 *   --debug=<0|1>
 *     Enables verbose debug mode (1) or runs normally (0).
 *     Default is 0.
 *
 * Complete Format:
 * java -jar build/libs/SSHSearchBTree.jar --cache=<0/1> --degree=<btree-degree> \
          --btree-file=<btree-filename> --query-file=<query-fileaname> \
          [--top-frequency=<10/25/50>] [--cache-size=<n>]  [--debug=<0|1>]
 */

public class SSHSearchBTreeArguments
{

}
