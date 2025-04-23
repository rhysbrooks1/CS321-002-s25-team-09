package cs321.btree;

import java.io.*;
import java.nio.ByteBuffer;
import java.sql.*;
import java.util.*;

public class BTree implements BTreeInterface {
    private static final int BLOCK_SIZE = 4096;
    private static final int METADATA_SIZE = 256;

    private final int degree;
    private final boolean useCache;
    private final int cacheSize;
    private final String tableType;

    private long size = 0;
    private long nodeCount = 0;
    private long rootOffset = -1;
    private RandomAccessFile file;
    private final Map<Long, BTreeNode> cache;
    private final File btreeFile;

    /** Full constructor (with cache options) **/
    public BTree(int degree, String type, boolean useCache, int cacheSize) throws IOException {
        this.degree = degree;
        this.tableType = type;
        this.useCache = useCache;
        this.cacheSize = cacheSize;
        this.btreeFile = new File("SSH_log.txt.ssh.btree." + type + "." + degree);
        boolean exists = btreeFile.exists();
        this.file = new RandomAccessFile(btreeFile, "rw");
        this.cache = useCache
            ? new LinkedHashMap<Long, BTreeNode>() {
                protected boolean removeEldestEntry(Map.Entry<Long, BTreeNode> e) {
                    return size() > cacheSize;
                }
            }
            : null;

        if (exists && file.length() >= METADATA_SIZE) {
            readMetadata();
        } else {
            // brand‐new file: reserve metadata, then create root
            size = 0;
            nodeCount = 0;
            writeMetadata();                 // now file.length() == METADATA_SIZE
            rootOffset = createNewNode(true); // writes at offset METADATA_SIZE
            writeMetadata();                 // update metadata with real rootOffset/nodeCount
        }
    }

    /** Constructor with degree + filename **/
    public BTree(int degree, String filename) throws IOException {
        this.degree = degree;
        this.tableType = "btree";
        this.useCache = false;
        this.cacheSize = 0;
        this.btreeFile = new File(filename);
        boolean exists = btreeFile.exists();
        this.file = new RandomAccessFile(btreeFile, "rw");
        this.cache = null;

        if (exists && file.length() >= METADATA_SIZE) {
            readMetadata();
        } else {
            size = 0;
            nodeCount = 0;
            writeMetadata();
            rootOffset = createNewNode(true);
            writeMetadata();
        }
    }

    /** Convenience constructor with default degree=2 **/
    public BTree(String filename) throws IOException {
        this(2, filename);
    }

    private void writeMetadata() throws IOException {
        file.seek(0);
        ByteBuffer buf = ByteBuffer.allocate(METADATA_SIZE);
        buf.putLong(rootOffset);
        buf.putInt(degree);
        buf.putLong(size);
        buf.putLong(nodeCount);
        file.write(buf.array());
    }

    private void readMetadata() throws IOException {
        file.seek(0);
        byte[] meta = new byte[METADATA_SIZE];
        file.readFully(meta);
        ByteBuffer buf = ByteBuffer.wrap(meta);

        long storedRoot = buf.getLong();
        int storedDeg  = buf.getInt();
        long storedSize = buf.getLong();
        long storedCount = buf.getLong();

        if (storedDeg != degree) {
            // reinitialize file
            file.setLength(0);
            size = 0;
            nodeCount = 0;
            writeMetadata();
            rootOffset = createNewNode(true);
            writeMetadata();
        } else {
            rootOffset = storedRoot;
            size = storedSize;
            nodeCount = storedCount;
        }
    }

    @Override public long getSize()            { return size; }
    @Override public int  getDegree()          { return degree; }
    @Override public long getNumberOfNodes()   { return nodeCount; }
    @Override public int  getHeight()          { return calcHeight(rootOffset); }

    private int calcHeight(long off) {
        try {
            BTreeNode n = readNode(off);
            if (n.isLeaf) return 0;
            return 1 + calcHeight(n.children[0]);
        } catch (IOException e) {
            return -1;
        }
    }

    @Override
    public void insert(TreeObject obj) throws IOException {
        BTreeNode root = readNode(rootOffset);
        if (root.isFull(degree)) {
            long newRootOff = createNewNode(false);
            BTreeNode newRoot = new BTreeNode(false, degree);
            newRoot.children[0] = rootOffset;
            writeNode(newRootOff, newRoot);

            splitChild(newRoot, newRootOff, 0);
            rootOffset = newRootOff;
            writeMetadata();

            insertNonFull(newRoot, newRootOff, obj);
        } else {
            insertNonFull(root, rootOffset, obj);
        }
    }

    private void insertNonFull(BTreeNode node, long off, TreeObject obj) throws IOException {
        int i = node.n - 1;
        if (node.isLeaf) {
            while (i >= 0 && obj.compareTo(node.keys[i]) < 0) i--;
            if (i >= 0 && node.keys[i].getKey().equals(obj.getKey())) {
                node.keys[i].incCount();
            } else {
                for (int j = node.n; j > i + 1; j--) node.keys[j] = node.keys[j-1];
                node.keys[i+1] = obj;
                node.n++;
                size++;
            }
            writeNode(off, node);
        } else {
            while (i >= 0 && obj.compareTo(node.keys[i]) < 0) i--;
            i++;
            BTreeNode child = readNode(node.children[i]);
            if (child.isFull(degree)) {
                splitChild(node, off, i);
                node = readNode(off);
                if (obj.compareTo(node.keys[i]) > 0) i++;
                child = readNode(node.children[i]);
            }
            insertNonFull(child, node.children[i], obj);
        }
    }

    private void splitChild(BTreeNode parent, long pOff, int i) throws IOException {
        BTreeNode full = readNode(parent.children[i]);
        long sibOff = createNewNode(full.isLeaf);
        BTreeNode sib = new BTreeNode(full.isLeaf, degree);

        // move keys & children
        for (int j = 0; j < degree - 1; j++)
            sib.keys[j] = full.keys[j + degree];
        if (!full.isLeaf)
            System.arraycopy(full.children, degree, sib.children, 0, degree);

        sib.n = degree - 1;
        full.n = degree - 1;

        // shift parent pointers & insert sibling
        for (int j = parent.n; j > i; j--) parent.children[j+1] = parent.children[j];
        parent.children[i+1] = sibOff;

        // shift parent keys & insert median
        for (int j = parent.n-1; j >= i; j--) parent.keys[j+1] = parent.keys[j];
        parent.keys[i] = full.keys[degree-1];
        parent.n++;

        writeNode(pOff, parent);
        writeNode(parent.children[i], full);
        writeNode(sibOff, sib);
        nodeCount++;
    }

    @Override
    public TreeObject search(String key) throws IOException {
        return searchRec(rootOffset, key);
    }

    private TreeObject searchRec(long off, String key) throws IOException {
        BTreeNode node = readNode(off);
        int i = 0;
        while (i < node.n && key.compareTo(node.keys[i].getKey()) > 0) i++;
        if (i < node.n && key.equals(node.keys[i].getKey())) return node.keys[i];
        if (node.isLeaf) return null;
        return searchRec(node.children[i], key);
    }

    @Override
    public void dumpToFile(PrintWriter out) throws IOException {
        dumpRec(rootOffset, out);
    }

    private void dumpRec(long off, PrintWriter out) throws IOException {
        BTreeNode node = readNode(off);
        for (int i = 0; i < node.n; i++) {
            if (!node.isLeaf) dumpRec(node.children[i], out);
            out.println(node.keys[i]);
        }
        if (!node.isLeaf)
            dumpRec(node.children[node.n], out);
    }

    @Override
    public void dumpToDatabase(String db, String tbl) throws IOException {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + db)) {
            try (Statement s = conn.createStatement()) {
                s.executeUpdate("DROP TABLE IF EXISTS " + tbl);
                s.executeUpdate("CREATE TABLE " + tbl + " (key TEXT, frequency INTEGER)");
            }
            String sql = "INSERT INTO " + tbl + " (key, frequency) VALUES (?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                inorderDB(rootOffset, ps);
            }
        } catch (SQLException ex) {
            throw new IOException(ex);
        }
    }

    private void inorderDB(long off, PreparedStatement ps) throws IOException, SQLException {
        BTreeNode node = readNode(off);
        for (int i = 0; i < node.n; i++) {
            if (!node.isLeaf) inorderDB(node.children[i], ps);
            ps.setString(1, node.keys[i].getKey());
            ps.setLong(2, node.keys[i].getCount());
            ps.executeUpdate();
        }
        if (!node.isLeaf)
            inorderDB(node.children[node.n], ps);
    }

    @Override
    public void delete(String key) {
        throw new UnsupportedOperationException("Delete not implemented");
    }

    private long createNewNode(boolean leaf) throws IOException {
        BTreeNode n = new BTreeNode(leaf, degree);
        long off = file.length();
        file.seek(off);
        file.write(n.toBytes());
        if (useCache) cache.put(off, n);
        nodeCount++;
        return off;
    }

    private BTreeNode readNode(long off) throws IOException {
        if (useCache && cache.containsKey(off)) return cache.get(off);
        file.seek(off);
        byte[] buf = new byte[BLOCK_SIZE];
        file.readFully(buf);
        BTreeNode n = BTreeNode.fromBytes(buf, degree);
        if (useCache) cache.put(off, n);
        return n;
    }

    private void writeNode(long off, BTreeNode node) throws IOException {
        file.seek(off);
        file.write(node.toBytes());
        if (useCache) cache.put(off, node);
    }

    public String[] getSortedKeyArray() throws IOException {
        List<String> list = new ArrayList<>();
        collectKeys(rootOffset, list);
        return list.toArray(new String[0]);
    }
    private void collectKeys(long off, List<String> list) throws IOException {
        BTreeNode n = readNode(off);
        for (int i = 0; i < n.n; i++) {
            if (!n.isLeaf) collectKeys(n.children[i], list);
            list.add(n.keys[i].getKey());
        }
        if (!n.isLeaf) collectKeys(n.children[n.n], list);
    }

    /** Node‐structure **/
    static class BTreeNode {
        boolean isLeaf;
        int n;
        TreeObject[] keys;
        long[] children;

        BTreeNode(boolean isLeaf, int degree) {
            this.isLeaf = isLeaf;
            this.n = 0;
            this.keys = new TreeObject[2*degree - 1];
            this.children = new long[2*degree];
        }

        boolean isFull(int degree) {
            return n == 2*degree - 1;
        }

        byte[] toBytes() throws IOException {
            ByteBuffer buf = ByteBuffer.allocate(BLOCK_SIZE);
            buf.put((byte)(isLeaf ? 1 : 0));
            buf.putInt(n);
            // keys & counts
            for (int i = 0; i < keys.length; i++) {
                if (i < n && keys[i] != null) {
                    byte[] kb = Arrays.copyOf(keys[i].getKey().getBytes(), 64);
                    buf.put(kb);
                    buf.putLong(keys[i].getCount());
                } else {
                    buf.put(new byte[64]);
                    buf.putLong(0L);
                }
            }
            // children
            for (long c : children) buf.putLong(c);
            return buf.array();
        }

        static BTreeNode fromBytes(byte[] data, int degree) {
            ByteBuffer buf = ByteBuffer.wrap(data);
            BTreeNode node = new BTreeNode(buf.get()==1, degree);
            node.n = buf.getInt();
            node.keys = new TreeObject[2*degree - 1];
            for (int i = 0; i < node.keys.length; i++) {
                byte[] kb = new byte[64];
                buf.get(kb);
                long cnt = buf.getLong();
                String k = new String(kb).trim();
                if (!k.isEmpty()) node.keys[i] = new TreeObject(k, cnt);
            }
            node.children = new long[2*degree];
            for (int i = 0; i < node.children.length; i++) {
                node.children[i] = buf.getLong();
            }
            return node;
        }
    }
}
