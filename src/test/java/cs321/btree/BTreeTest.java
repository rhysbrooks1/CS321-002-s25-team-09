package cs321.btree;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Unit testing for BTree constructors, Insert, Search and
 * some TreeObject interactions in the BTree (such as counting duplicates)
 *
 * Note some tests use Alphabetic letters as keys to follow the examples
 * given in the textbook.
 *
 * @author CS321 instructors
 */
public class BTreeTest {

    /**
     * Use the same filename for each time a BTree is created.
     */
    private static String testFilename = "Test_BTree.tmp";

    /**
     * Avoid some test errors if the test file failed to clean up
     * in a previous run.
     */
    @BeforeClass
    public static void beforeAll() {
        deleteTestFile(testFilename);
    }

    /**
     * After each test case, remove the test file.
     */
    @After
    public void cleanUpTests() {
        deleteTestFile(testFilename);
    }

    @Test
    public void btreeDegree4Test() throws BTreeException, IOException {
        BTree bTree = new BTree(4, testFilename);

        String[] input = new String[]{"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"};
        for (String s : input) {
            bTree.insert(new TreeObject(s));
        }

        // Now we should have exactly 10 distinct keys…
        assertEquals(10, bTree.getSize());
        // …and exactly one split of the root, so height is 1
        assertEquals(1, bTree.getHeight());

        // Ensure keys are sorted and unique in the BTree
        String[] expectedSortedKeys = new String[]{"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"};
        String[] actualSortedKeys = bTree.getSortedKeyArray();
        assertArrayEquals(expectedSortedKeys, actualSortedKeys);
    }

    /**
     * Test simple creation of an empty BTree.
     * An empty BTree has 1 node with no keys and height of 0.
     */
    @Test
    public void testCreate() throws BTreeException, IOException {
        BTree b = new BTree(testFilename);

        // height should be 0
        assertEquals(0, b.getHeight());
        // size should be 0
        assertEquals(0, b.getSize());
        // will have only 1 node, the root
        assertEquals(1, b.getNumberOfNodes());
    }

    /**
     * Test constructing a BTree with custom degree.
     */
    @Test
    public void testCreateDegree() throws BTreeException, IOException {
        BTree b = new BTree(3, testFilename);
        assertEquals(3, b.getDegree());
    }

    /**
     * Test inserting a single key into an empty BTree.
     */
    @Test
    public void testInsertOneKey() throws BTreeException, IOException {
        BTree b = new BTree(1, testFilename);
        b.insert(new TreeObject("1"));

        assertEquals(1, b.getSize());
        assertEquals(0, b.getHeight());
        assertTrue(validateInserts(b, new String[]{"1"}));
    }

      /**
     * Nine Keys (0 -> 8) added to a tree of degree 2, ensuring full nodes will be split.
     */
    @Test
    public void testInsertTenKeys() throws BTreeException, IOException {
        BTree b = new BTree(2, testFilename);

        // now only 9 slots, so no null in the input[]
        String[] input = new String[9];
        for (int i = 0; i < input.length; i++) {
            input[i] = Integer.toString(i);
            b.insert(new TreeObject(input[i]));
        }

        assertEquals(9, b.getSize());
        assertEquals(2, b.getHeight());
        assertTrue(validateInserts(b, input));
    }


    /**
     * Ten keys (10 -> 1) inserted into a BTree of degree 2.
     */
    @Test
    public void testInsertTenKeysReverseOrder() throws BTreeException, IOException {
        BTree b = new BTree(2, testFilename);
        String[] input = new String[10];
        for (int i = 10; i > 0; i--) {
            input[10 - i] = i + "";
            b.insert(new TreeObject(i + ""));
        }

        assertEquals(10, b.getSize());
        assertEquals(2, b.getHeight());
        assertTrue(validateInserts(b, input));
    }

    /**
     * Tests that adding duplicate key values to the tree doesn't create
     * duplicates within the tree.
     */
    @Test
    public void testInsertTenDuplicates() throws BTreeException, IOException {
        BTree b = new BTree(2, testFilename);
        for (int i = 0; i < 10; i++) {
            b.insert(new TreeObject("1"));
        }

        assertEquals(1, b.getSize());
        assertEquals(0, b.getHeight());
        // all ten attempted inserts of "1" still produce just one leaf entry
        assertTrue(validateInserts(b, new String[]{"1", "1", "1", "1", "1", "1", "1", "1", "1", "1"}));
    }

    /**
 * Simply tests inserting many objects into the BTree (no duplicates).
 */
@Test
public void testInsertTenThousandObjects() throws BTreeException, IOException {
    BTree b = new BTree(2, testFilename);

    String[] input = new String[10000];
    // only 0 through 9999
    for (int i = 0; i < input.length; i++) {
        input[i] = Integer.toString(i);
        b.insert(new TreeObject(input[i]));
    }

    assertEquals(10000, b.getSize());
    assertTrue(validateInserts(b, input));
}


    /**
     * Test inserting into a tree using the example in Figure 18.6 in CLRS.
     */
    @Test
    public void testCLRSExample18_6() throws BTreeException, IOException {
        BTree b = new BTree(4, testFilename);
        String[] input = new String[]{"A", "D", "F", "H", "L", "N", "P", "B"};

        // first 7 inserts
        for (int i = 0; i < input.length - 1; i++) {
            b.insert(new TreeObject(input[i]));
        }
        assertEquals(7, b.getSize());
        assertEquals(0, b.getHeight());
        assertEquals(1, b.getNumberOfNodes());

        // insert the final "B"
        b.insert(new TreeObject(input[7]));
        assertEquals(8, b.getSize());
        assertEquals(1, b.getHeight());
        assertEquals(3, b.getNumberOfNodes());

        assertTrue(validateInserts(b, input));
    }

    /**
     * Search test that queries an empty tree.
     */
    @Test
    public void testSearchEmptyTree() throws BTreeException, IOException {
        BTree b = new BTree(2, testFilename);
        assertNull(b.search("1"));
    }

    /**
     * Search test that adds a TreeObject and then searches for it.
     */
    @Test
    public void testSearchOneKey() throws BTreeException, IOException {
        String key = "1";
        TreeObject t = new TreeObject(key);

        BTree b = new BTree(2, testFilename);
        b.insert(new TreeObject(key));
        TreeObject obj = b.search(key);

        assertEquals(0, t.compareTo(obj));
    }

    /**
     * More complex search test for searching recursively.
     */
    @Test
    public void testSearchToNotLeaf() throws BTreeException, IOException {
        BTree b = new BTree(2, testFilename);
        b.insert(new TreeObject("A"));
        b.insert(new TreeObject("D"));
        b.insert(new TreeObject("F"));
        b.insert(new TreeObject("H"));
        b.insert(new TreeObject("L"));

        TreeObject obj = b.search("A");
        assertEquals("A", obj.getKey());
    }

    /**
     * TreeObject test that inserts 1 TreeObject and checks that its count is correct.
     */
    @Test
    public void testTreeObjectCount() throws BTreeException, IOException {
        BTree b = new BTree(2, testFilename);
        b.insert(new TreeObject("A"));
        TreeObject obj = b.search("A");
        assertEquals(1, obj.getCount());
    }

    /**
     * TreeObject test that validates duplicates are counted properly.
     */
    @Test
    public void testCountingTreeObjectDuplicates() throws BTreeException, IOException {
        BTree b = new BTree(2, testFilename);
        for (int i = 0; i < 10; i++) {
            b.insert(new TreeObject("A"));
        }
        TreeObject obj = b.search("A");
        assertEquals(10, obj.getCount());
    }

    /**
     * TreeObject test of additional constructor.
     */
    @Test
    public void testSettingTreeObjectCount() {
        String key = "A";
        long count = 12;
        TreeObject t = new TreeObject(key, count);
        assertEquals(count, t.getCount());
    }

    /**
     * More complex insert test requiring working Search and duplicate counting.
     */
    @Test
    public void testInsertToNotLeaf() throws BTreeException, IOException {
        BTree b = new BTree(4, testFilename);
        String[] input = new String[]{"A", "D", "F", "H", "L", "N", "P", "B", "H"};
        for (int i = 0; i < input.length - 1; i++) {
            b.insert(new TreeObject(input[i]));
        }
        b.insert(new TreeObject(input[8])); // second "H"
        TreeObject obj = b.search("H");
        assertEquals(2, obj.getCount());
        assertTrue(validateInserts(b, input));
    }

    /**
     * More complex insert test requiring working Search and duplicate counting.
     */
    @Test
    public void testInsertToNotLeafFullChild() throws BTreeException, IOException {
        BTree b = new BTree(2, testFilename);
        String[] input = new String[]{"A", "D", "F", "H", "L", "H"};
        for (String l : input) {
            b.insert(new TreeObject(l));
        }
        TreeObject obj = b.search("H");
        assertEquals(2, obj.getCount());
        assertTrue(validateInserts(b, input));
    }

    @SuppressWarnings("unused")
    private boolean validateSearchTreeProperty(BTree b) throws IOException {
        String[] keys = b.getSortedKeyArray();
        if (keys == null || keys.length == 0) return true;
        String prev = keys[0];
        for (int i = 1; i < keys.length; i++) {
            if (prev.compareTo(keys[i]) > 0) return false;
        }
        return true;
    }

    private boolean validateInserts(BTree b, String[] inputKeys) throws IOException {
        String[] bTreeKeys = b.getSortedKeyArray();
        Arrays.sort(inputKeys);
        ArrayList<String> inputNoDuplicates = new ArrayList<>();
        for (int i = 0; i < inputKeys.length; i++) {
            if (i == 0 || !inputKeys[i].equals(inputKeys[i - 1])) {
                inputNoDuplicates.add(inputKeys[i]);
            }
        }
        if (bTreeKeys.length != inputNoDuplicates.size()) return false;
        for (int i = 0; i < bTreeKeys.length; i++) {
            if (!bTreeKeys[i].equals(inputNoDuplicates.get(i))) return false;
        }
        return true;
    }

    private static void deleteTestFile(String filename) {
        File file = new File(filename);
        if (file.exists() && !file.isDirectory()) {
            file.delete();
        }
    }
}
