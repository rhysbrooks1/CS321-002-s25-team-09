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

    public BTree(int degree, String type, boolean useCache, int cacheSize) throws IOException {
        this.degree = degree;
        this.tableType = type;
        this.useCache = useCache;
        this.cacheSize = cacheSize;
        this.btreeFile = new File("SSH_log.txt.ssh.btree." + type + "." + degree);
        boolean exists = btreeFile.exists();
        this.file = new RandomAccessFile(btreeFile, "rw");
        this.cache = useCache ? new LinkedHashMap<Long, BTreeNode>() {
            protected boolean removeEldestEntry(Map.Entry<Long, BTreeNode> eldest) {
                return size() > cacheSize;
            }
        } : null;

        if (exists && btreeFile.length() >= METADATA_SIZE) {
            readMetadata();
        } else {
            this.rootOffset = createNewNode(true);
            writeMetadata();
        }
    }

    // New constructor: (int degree, String filename)
    public BTree(int degree, String filename) throws IOException {
        this.degree = degree;
        this.tableType = "btree";
        this.useCache = false;
        this.cacheSize = 0;
        this.btreeFile = new File(filename);
        boolean exists = btreeFile.exists();
        this.file = new RandomAccessFile(btreeFile, "rw");
        this.cache = null;

        if (exists && btreeFile.length() >= METADATA_SIZE) {
            readMetadata();
        } else {
            this.rootOffset = createNewNode(true);
            writeMetadata();
        }
    }

    // New constructor: (String filename)
    public BTree(String filename) throws IOException {
        this(2, filename); // default degree of 2
    }

    private void writeMetadata() throws IOException {
        file.seek(0);
        ByteBuffer buffer = ByteBuffer.allocate(METADATA_SIZE);
        buffer.putLong(rootOffset);
        buffer.putInt(degree);
        buffer.putLong(size);
        buffer.putLong(nodeCount);
        file.write(buffer.array());
    }

    private void readMetadata() throws IOException {
        file.seek(0);
        byte[] metadata = new byte[METADATA_SIZE];
        file.readFully(metadata);
        ByteBuffer buffer = ByteBuffer.wrap(metadata);
        rootOffset = buffer.getLong();
        int storedDegree = buffer.getInt();
        if (storedDegree != degree) {
            file.setLength(0);
            this.rootOffset = createNewNode(true);
            size = 0;
            nodeCount = 1;
            writeMetadata();
        } else {
            size = buffer.getLong();
            nodeCount = buffer.getLong();
        }
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public int getDegree() {
        return degree;
    }

    @Override
    public long getNumberOfNodes() {
        return nodeCount;
    }

    @Override
    public int getHeight() {
        return calculateHeight(rootOffset);
    }

    private int calculateHeight(long offset) {
        try {
            BTreeNode node = readNode(offset);
            if (node.isLeaf) return 0;
            return 1 + calculateHeight(node.children[0]);
        } catch (IOException e) {
            return -1;
        }
    }

    @Override
    public void insert(TreeObject obj) throws IOException {
        BTreeNode root = readNode(rootOffset);
        if (root.isFull(degree)) {
            long newRootOffset = createNewNode(false);
            BTreeNode newRoot = new BTreeNode(false, degree);
            newRoot.children[0] = rootOffset;
            writeNode(newRootOffset, newRoot);
            splitChild(newRoot, newRootOffset, 0);
            rootOffset = newRootOffset;
            writeMetadata();
            insertNonFull(newRoot, newRootOffset, obj);
        } else {
            insertNonFull(root, rootOffset, obj);
        }
    }

    private void insertNonFull(BTreeNode node, long nodeOffset, TreeObject obj) throws IOException {
        int i = node.n - 1;
        if (node.isLeaf) {
            while (i >= 0 && obj.compareTo(node.keys[i]) < 0) i--;
            if (i >= 0 && node.keys[i].getKey().equals(obj.getKey())) {
                node.keys[i].incCount();
            } else {
                for (int j = node.n; j > i + 1; j--) node.keys[j] = node.keys[j - 1];
                node.keys[i + 1] = obj;
                node.n++;
                size++;
            }
            writeNode(nodeOffset, node);
        }  else {
            while (i >= 0 && obj.compareTo(node.keys[i]) < 0) i--;
            i++;
            BTreeNode child = readNode(node.children[i]);
            if (child.isFull(degree)) {
                splitChild(node, nodeOffset, i);
                node = readNode(nodeOffset);
                if (obj.compareTo(node.keys[i]) > 0) i++;
                child = readNode(node.children[i]);
            }
            insertNonFull(child, node.children[i], obj);
        }
    }

    private void splitChild(BTreeNode parent, long parentOffset, int i) throws IOException {
        BTreeNode full = readNode(parent.children[i]);
        long newOffset = createNewNode(full.isLeaf);
        BTreeNode sibling = new BTreeNode(full.isLeaf, degree);

        for (int j = 0; j < degree - 1; j++) sibling.keys[j] = full.keys[j + degree];
        if (!full.isLeaf) System.arraycopy(full.children, degree, sibling.children, 0, degree);
        sibling.n = degree - 1;
        full.n = degree - 1;

        for (int j = parent.n; j > i; j--) parent.children[j + 1] = parent.children[j];
        parent.children[i + 1] = newOffset;
        for (int j = parent.n - 1; j >= i; j--) parent.keys[j + 1] = parent.keys[j];
        parent.keys[i] = full.keys[degree - 1];
        parent.n++;

        writeNode(parentOffset, parent);
        writeNode(parent.children[i], full);
        writeNode(newOffset, sibling);
        nodeCount++;
    }

    @Override
    public TreeObject search(String key) throws IOException {
        return searchRecursive(rootOffset, key);
    }

    private TreeObject searchRecursive(long offset, String key) throws IOException {
        BTreeNode node = readNode(offset);
        int i = 0;
        while (i < node.n && key.compareTo(node.keys[i].getKey()) > 0) i++;
        if (i < node.n && key.equals(node.keys[i].getKey())) return node.keys[i];
        if (node.isLeaf) return null;
        return searchRecursive(node.children[i], key);
    }

    @Override
    public void dumpToFile(PrintWriter out) throws IOException {
        dumpToFileRecursive(rootOffset, out);
    }

    private void dumpToFileRecursive(long offset, PrintWriter out) throws IOException {
        BTreeNode node = readNode(offset);
        for (int i = 0; i < node.n; i++) {
            if (!node.isLeaf) dumpToFileRecursive(node.children[i], out);
            out.println(node.keys[i]);
        }
        if (!node.isLeaf) dumpToFileRecursive(node.children[node.n], out);
    }

    @Override
    public void dumpToDatabase(String dbName, String tableName) throws IOException {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbName)) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("DROP TABLE IF EXISTS " + tableName);
                stmt.executeUpdate("CREATE TABLE " + tableName + " (key TEXT, frequency INTEGER)");
            }
            String sql = "INSERT INTO " + tableName + " (key, frequency) VALUES (?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                inorderDatabaseDump(rootOffset, ps);
            }
        } catch (SQLException e) {
            throw new IOException("Database error: " + e.getMessage());
        }
    }

    private void inorderDatabaseDump(long offset, PreparedStatement ps) throws IOException, SQLException {
        BTreeNode node = readNode(offset);
        for (int i = 0; i < node.n; i++) {
            if (!node.isLeaf) inorderDatabaseDump(node.children[i], ps);
            ps.setString(1, node.keys[i].getKey());
            ps.setLong(2, node.keys[i].getCount());
            ps.executeUpdate();
        }
        if (!node.isLeaf) inorderDatabaseDump(node.children[node.n], ps);
    }

    @Override
    public void delete(String key) {
        throw new UnsupportedOperationException("Delete not implemented");
    }

    private long createNewNode(boolean isLeaf) throws IOException {
        BTreeNode node = new BTreeNode(isLeaf, degree);
        long offset = file.length();
        writeNode(offset, node);
        if (useCache) cache.put(offset, node);
        nodeCount++;
        return offset;
    }

    private BTreeNode readNode(long offset) throws IOException {
        if (useCache && cache.containsKey(offset)) return cache.get(offset);
        file.seek(offset);
        byte[] buffer = new byte[BLOCK_SIZE];
        file.readFully(buffer);
        BTreeNode node = BTreeNode.fromBytes(buffer, degree);
        if (useCache) cache.put(offset, node);
        return node;
    }

    private void writeNode(long offset, BTreeNode node) throws IOException {
        file.seek(offset);
        file.write(node.toBytes());
        if (useCache) cache.put(offset, node);
    }

    public String[] getSortedKeyArray() throws IOException {
        List<String> keys = new ArrayList<>();
        inorderKeyCollection(rootOffset, keys);
        return keys.toArray(new String[0]);
    }

    private void inorderKeyCollection(long offset, List<String> keys) throws IOException {
        BTreeNode node = readNode(offset);
        for (int i = 0; i < node.n; i++) {
            if (!node.isLeaf) inorderKeyCollection(node.children[i], keys);
            keys.add(node.keys[i].getKey());
        }
        if (!node.isLeaf) inorderKeyCollection(node.children[node.n], keys);
    }

    static class BTreeNode {
        boolean isLeaf;
        int n;
        TreeObject[] keys;
        long[] children;

        BTreeNode(boolean isLeaf, int degree) {
            this.isLeaf = isLeaf;
            this.n = 0;
            this.keys = new TreeObject[2 * degree - 1];
            this.children = new long[2 * degree];
        }

        boolean isFull(int degree) {
            return n == 2 * degree - 1;
        }

        byte[] toBytes() throws IOException {
            ByteBuffer buffer = ByteBuffer.allocate(BLOCK_SIZE);
            buffer.put((byte) (isLeaf ? 1 : 0));
            buffer.putInt(n);
            for (int i = 0; i < keys.length; i++) {
                if (i < n && keys[i] != null) {
                    byte[] keyBytes = Arrays.copyOf(keys[i].getKey().getBytes(), 64);
                    buffer.put(keyBytes);
                    buffer.putLong(keys[i].getCount());
                } else {
                    buffer.put(new byte[64]);
                    buffer.putLong(0);
                }
            }
            for (int i = 0; i < children.length; i++) buffer.putLong(children[i]);
            return buffer.array();
        }

        static BTreeNode fromBytes(byte[] data, int degree) {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            BTreeNode node = new BTreeNode(buffer.get() == 1, degree);
            node.n = buffer.getInt();
            node.keys = new TreeObject[2 * degree - 1];
            for (int i = 0; i < node.keys.length; i++) {
                byte[] keyBytes = new byte[64];
                buffer.get(keyBytes);
                long count = buffer.getLong();
                String key = new String(keyBytes).trim();
                if (!key.isEmpty()) node.keys[i] = new TreeObject(key, count);
            }
            node.children = new long[2 * degree];
            for (int i = 0; i < node.children.length; i++) node.children[i] = buffer.getLong();
            return node;
        }
    }
}
