package cs321.search;

import cs321.btree.BTree;
import cs321.common.ParseArgumentException;
import cs321.common.ParseArgumentUtils;



/**
 * The driver class for searching a Database of a B-Tree.
 * 
 * This class is used to search a B-Tree database for a specific key.
 * It takes the following command line arguments:
 * 
 *  java -jar build/libs/SSHSearchDatabase.jar \
 * --type=<tree-type> \
 * --database=<path-to-SQLite-db> \
 * --top-frequency=<10|25|50>
 * 
 * Where <tree-type> is one of the following:
 * accepted-ip
 * accepted-time
 * failed-ip
 * failed-time
 * invalid-ip
 * invalid-time
 * reverseaddress-ip
 * reverseaddress-time
 * user-ip
 * 
 * <path-to-SQLite-db> is the path to the SQLite database file.
 * --database=SSHLogDB.db
 * 
 * --top-frequency=<10|25|50> is the number of top results to return.
 * --top-frequency=10
 * --top-frequency=25
 * --top-frequency=50
 * 
 * ALternative Mode: --type=test
 * Testing without a real BTree/database.
 * 
 * Standard Output:
 * <Key> <Frequency>
 * 
 * Example: 
 * Accepted-111.222.107.90 25
 * Accepted-119.137.63.195 14
 * Accepted-137.189.88.215 12
 */
public class SSHSearchDatabase
{
	
    public static void main(String[] args) throws Exception
    {
        System.out.println("Hello world from cs321.search.SSHSearchDatabase.main");
    }

}
