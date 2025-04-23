package cs321.btree;

import java.io.*;
import java.nio.ByteBuffer;
import java.sql.*;
import java.util.*;

public class BTree implements BTreeInterface {
    private static final int BLOCK_SIZE    = 4096;
    private static final int METADATA_SIZE = 256;

    private final int degree;
    private final boolean useCache;
    private final int cacheSize;
    private final String tableType;

    private long size       = 0;
    private long nodeCount  = 0;
    private long rootOffset = -1;
    private RandomAccessFile file;
    private final Map<Long, BTreeNode> cache;
    private final File btreeFile;

    /** Full constructor **/
    public BTree(int degree, String type, boolean useCache, int cacheSize) throws IOException {
        this.degree    = degree;
        this.tableType = type;
        this.useCache  = useCache;
        this.cacheSize = cacheSize;
        this.btreeFile = new File("SSH_log.txt.ssh.btree." + type + "." + degree);

        boolean exists = btreeFile.exists();
        this.file = new RandomAccessFile(btreeFile, "rw");
        this.cache = useCache
          ? new LinkedHashMap<Long,BTreeNode>() {
               protected boolean removeEldestEntry(Map.Entry<Long,BTreeNode> e) {
                   return size() > cacheSize;
               }
            }
          : null;

        if (exists && file.length() >= METADATA_SIZE) {
            readMetadata();
        } else {
            // brand‐new tree
            size      = 0;
            nodeCount = 0;
            writeMetadata();               // reserve metadata space
            rootOffset = createNewNode(true);
            writeMetadata();               // now write real rootOffset & nodeCount
        }
    }

    /** degree + filename **/
    public BTree(int degree, String filename) throws IOException {
        this.degree    = degree;
        this.tableType = "btree";
        this.useCache  = false;
        this.cacheSize = 0;
        this.btreeFile = new File(filename);

        boolean exists = btreeFile.exists();
        this.file = new RandomAccessFile(btreeFile, "rw");
        this.cache = null;

        if (exists && file.length() >= METADATA_SIZE) {
            readMetadata();
        } else {
            size      = 0;
            nodeCount = 0;
            writeMetadata();
            rootOffset = createNewNode(true);
            writeMetadata();
        }
    }

    /** default‐degree=2 + filename **/
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
        byte[] m = new byte[METADATA_SIZE];
        file.readFully(m);
        ByteBuffer buf = ByteBuffer.wrap(m);

        long storedRoot   = buf.getLong();
        int  storedDegree = buf.getInt();
        long storedSize   = buf.getLong();
        long storedCount  = buf.getLong();

        if (storedDegree != degree) {
            file.setLength(0);
            size      = 0;
            nodeCount = 0;
            writeMetadata();
            rootOffset = createNewNode(true);
            writeMetadata();
        } else {
            rootOffset = storedRoot;
            size       = storedSize;
            nodeCount  = storedCount;
        }
    }

    @Override public long getSize()          { return size; }
    @Override public int  getDegree()        { return degree; }
    @Override public long getNumberOfNodes() { return nodeCount; }
    @Override public int  getHeight()        { return calcHeight(rootOffset); }

    private int calcHeight(long off) {
        try {
            BTreeNode n = readNode(off);
            return n.isLeaf ? 0 : 1 + calcHeight(n.children[0]);
        } catch (IOException e) {
            return -1;
        }
    }

    @Override
    public void insert(TreeObject obj) throws IOException {
        // --- up‐front duplicate check ---
        TreeObject dup = search(obj.getKey());
        if (dup != null) {
            dup.incCount();
            // count bump only in memory; ensure metadata rewrite so file isn't stale
            writeMetadata();
            return;
        }

        // no duplicate → true B‐tree insert
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
        if (node.isLeaf) {
            int i = node.n - 1;
            while (i >= 0 && obj.compareTo(node.keys[i]) < 0) i--;
            for (int j = node.n; j > i + 1; j--) node.keys[j] = node.keys[j - 1];
            node.keys[i + 1] = obj;
            node.n++;
            size++;
            writeNode(off, node);
        } else {
            int i = 0;
            while (i < node.n && obj.compareTo(node.keys[i]) > 0) i++;
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

    private void splitChild(BTreeNode parent, long pOff, int idx) throws IOException {
        BTreeNode full = readNode(parent.children[idx]);
        long sibOff = createNewNode(full.isLeaf);
        BTreeNode sib = new BTreeNode(full.isLeaf, degree);

        // move keys and children
        for (int j = 0; j < degree - 1; j++) {
            sib.keys[j] = full.keys[j + degree];
        }
        if (!full.isLeaf) {
            System.arraycopy(full.children, degree, sib.children, 0, degree);
        }
        sib.n  = degree - 1;
        full.n = degree - 1;

        // insert sibling into parent
        for (int j = parent.n; j > idx; j--) {
            parent.children[j + 1] = parent.children[j];
        }
        parent.children[idx + 1] = sibOff;

        for (int j = parent.n - 1; j >= idx; j--) {
            parent.keys[j + 1] = parent.keys[j];
        }
        parent.keys[idx] = full.keys[degree - 1];
        parent.n++;

        writeNode(pOff, parent);
        writeNode(parent.children[idx], full);
        writeNode(sibOff, sib);
    }

    @Override
    public TreeObject search(String key) throws IOException {
        return searchRec(rootOffset, key);
    }
    private TreeObject searchRec(long off, String key) throws IOException {
        BTreeNode n = readNode(off);
        int i = 0;
        while (i < n.n && key.compareTo(n.keys[i].getKey()) > 0) i++;
        if (i < n.n && key.equals(n.keys[i].getKey())) return n.keys[i];
        return n.isLeaf ? null : searchRec(n.children[i], key);
    }

    @Override
    public void dumpToFile(PrintWriter out) throws IOException {
        dumpRec(rootOffset, out);
    }
    private void dumpRec(long off, PrintWriter out) throws IOException {
        BTreeNode n = readNode(off);
        for (int i = 0; i < n.n; i++) {
            if (!n.isLeaf) dumpRec(n.children[i], out);
            out.println(n.keys[i]);
        }
        if (!n.isLeaf) dumpRec(n.children[n.n], out);
    }

    @Override
    public void dumpToDatabase(String db, String tbl) throws IOException {
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + db)) {
            try (Statement s = c.createStatement()) {
                s.executeUpdate("DROP TABLE IF EXISTS " + tbl);
                s.executeUpdate("CREATE TABLE " + tbl + " (key TEXT, frequency INTEGER)");
            }
            String sql = "INSERT INTO " + tbl + " (key, frequency) VALUES (?, ?)";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                dumpDB(rootOffset, ps);
            }
        } catch (SQLException ex) {
            throw new IOException(ex);
        }
    }
    private void dumpDB(long off, PreparedStatement ps) throws IOException, SQLException {
        BTreeNode n = readNode(off);
        for (int i = 0; i < n.n; i++) {
            if (!n.isLeaf) dumpDB(n.children[i], ps);
            ps.setString(1, n.keys[i].getKey());
            ps.setLong(2, n.keys[i].getCount());
            ps.executeUpdate();
        }
        if (!n.isLeaf) dumpDB(n.children[n.n], ps);
    }

    @Override public void delete(String key) {
        throw new UnsupportedOperationException("Delete not implemented");
    }

    private long createNewNode(boolean leaf) throws IOException {
        BTreeNode node = new BTreeNode(leaf, degree);
        long off = file.length();
        writeNode(off, node);
        if (useCache) cache.put(off, node);
        nodeCount++;
        return off;
    }

    private BTreeNode readNode(long off) throws IOException {
        if (useCache && cache.containsKey(off)) return cache.get(off);
        file.seek(off);
        byte[] buf = new byte[BLOCK_SIZE];
        file.readFully(buf);
        BTreeNode node = BTreeNode.fromBytes(buf, degree);
        if (useCache) cache.put(off, node);
        return node;
    }

    private void writeNode(long off, BTreeNode node) throws IOException {
        file.seek(off);
        file.write(node.toBytes());
        if (useCache) cache.put(off, node);
    }

    public String[] getSortedKeyArray() throws IOException {
        List<String> keys = new ArrayList<>();
        collect(rootOffset, keys);
        return keys.toArray(new String[0]);
    }
    private void collect(long off, List<String> keys) throws IOException {
        BTreeNode n = readNode(off);
        for (int i = 0; i < n.n; i++) {
            if (!n.isLeaf) collect(n.children[i], keys);
            keys.add(n.keys[i].getKey());
        }
        if (!n.isLeaf) collect(n.children[n.n], keys);
    }

    static class BTreeNode {
        boolean        isLeaf;
        int            n;
        TreeObject[]   keys;
        long[]         children;

        BTreeNode(boolean isLeaf, int degree) {
            this.isLeaf   = isLeaf;
            this.n        = 0;
            this.keys     = new TreeObject[2 * degree - 1];
            this.children = new long[2 * degree];
        }

        boolean isFull(int degree) {
            return n == 2 * degree - 1;
        }

        byte[] toBytes() throws IOException {
            ByteBuffer buf = ByteBuffer.allocate(BLOCK_SIZE);
            buf.put((byte)(isLeaf ? 1 : 0));
            buf.putInt(n);
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
            for (long c : children) buf.putLong(c);
            return buf.array();
        }

        static BTreeNode fromBytes(byte[] data, int degree) {
            ByteBuffer buf = ByteBuffer.wrap(data);
            BTreeNode node = new BTreeNode(buf.get()==1, degree);
            node.n = buf.getInt();
            node.keys = new TreeObject[2 * degree - 1];
            for (int i = 0; i < node.keys.length; i++) {
                byte[] kb = new byte[64];
                buf.get(kb);
                long cnt = buf.getLong();
                String k = new String(kb).trim();
                if (!k.isEmpty()) node.keys[i] = new TreeObject(k, cnt);
            }
            node.children = new long[2 * degree];
            for (int i = 0; i < node.children.length; i++) {
                node.children[i] = buf.getLong();
            }
            return node;
        }
    }
}
