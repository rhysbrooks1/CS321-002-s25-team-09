package cs321.btree;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Map.Entry;

import cs321.common.Cache;

/**
 * Disk-backed B-Tree implementation with optional caching.
 */
public class BTree implements BTreeInterface {

    // Constants
    private final int METADATA_SIZE = Long.BYTES;

    // Metadata
    private long nextDiskAddress = METADATA_SIZE;
    private int nodeSize;
    private long size;
    private int degree;
    private int nodes;
    private int height;
    private String filename;

    // Core structures
    private BTreeNode root;
    private Cache<Long, BTreeNode> BTreeCache;
    private FileChannel fileChannel;
    private ByteBuffer buffer;

    // Used for dumping sorted data
    private String[] sortedStringValues;
    private TreeObject[] sortedTreeObjects;

    // === Constructors ===

    public BTree(int degree, String filename, boolean usingCache, int cacheSize) {
        this.filename = filename;
        this.degree = degree <= 0 ? BTreeNode.getOptimalDegree() : degree;
        this.size = 0;
        this.nodes = 1;
        this.height = 0;
        this.root = new BTreeNode(this.degree);
        this.root.diskAddress = nextDiskAddress;

        if (usingCache) {
            BTreeCache = new Cache<>(cacheSize);
        }

        nodeSize = estimateNodeDiskSize();
        nextDiskAddress += nodeSize;
        buffer = ByteBuffer.allocateDirect(nodeSize);

        try {
            // [Change 0: always truncate file on open, removed if-exists/new-file branching]
            RandomAccessFile dataFile = new RandomAccessFile(filename, "rw");
            fileChannel = dataFile.getChannel();
            fileChannel.truncate(0);                       // [Change 1: truncate file on open]
            writeMetaData();                              // [Change 2: write only root pointer metadata]
            writeNode(root);                              // [Change 3: use writeNode helper for consistency]
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    public BTree(String filename) {
        this(0, filename, false, -1);
    }

    public BTree(int degree, String filename) {
        this(degree, filename, false, -1);
    }

    public BTree(String filename, boolean usingCache, int cacheSize) {
        this(0, filename, usingCache, cacheSize);
    }

    // === Accessors ===

    @Override
    public long getSize() { return size; }

    @Override
    public int getDegree() { return degree; }

    @Override
    public long getNumberOfNodes() { return nodes; }

    @Override
    public int getHeight() { return height; }

    public Cache<Long, BTreeNode> getCache() { return BTreeCache; }

    // === Insertion ===

    @Override
    public void insert(TreeObject obj) throws IOException {
        if (root.keys.size() == 2 * degree - 1) {
            BTreeNode newRoot = new BTreeNode(degree);
            newRoot.isLeaf = false;
            newRoot.children.add(root.diskAddress);
            newRoot.diskAddress = nextDiskAddress;
            nextDiskAddress += nodeSize;
            nodes++;
            height++;
            splitChild(newRoot, 0);
            root = newRoot;
            writeNode(root);
        }
        insertNonFull(root, obj);
    }

    private void insertNonFull(BTreeNode node, TreeObject obj) {
        int i = node.keys.size() - 1;
        if (node.isLeaf) {
            while (i >= 0 && obj.compareTo(node.keys.get(i)) < 0) {
                i--; }
            if (i >= 0 && obj.compareTo(node.keys.get(i)) == 0) {
                node.keys.get(i).setCount(node.keys.get(i).getCount() + 1);
            } else {
                node.keys.add(i + 1, obj);
                size++;
            }
            writeNode(node);
        } else {
            while (i >= 0 && obj.compareTo(node.keys.get(i)) < 0) {
                i--; }
            if (i >= 0 && obj.compareTo(node.keys.get(i)) == 0) {
                node.keys.get(i).setCount(node.keys.get(i).getCount() + 1);
                writeNode(node);
                return;
            }
            i++;
            long childAddress = node.children.get(i);
            BTreeNode child = readNode(childAddress);
            if (child.keys.size() == 2 * degree - 1) {
                splitChild(node, i);
                if (obj.compareTo(node.keys.get(i)) == 0) {
                    node.keys.get(i).setCount(node.keys.get(i).getCount() + 1);
                    writeNode(node);
                    return;
                }
                if (obj.compareTo(node.keys.get(i)) > 0) { i++; }
                child = readNode(node.children.get(i));
            }
            insertNonFull(child, obj);
        }
    }

    private void splitChild(BTreeNode parent, int index) {
        BTreeNode fullChild = readNode(parent.children.get(index));
        BTreeNode newChild = new BTreeNode(degree);
        newChild.isLeaf = fullChild.isLeaf;
        newChild.diskAddress = nextDiskAddress;
        nextDiskAddress += nodeSize;
        nodes++;
        TreeObject middleKey = fullChild.keys.get(degree - 1);
        for (int j = degree; j < 2 * degree - 1; j++) {
            newChild.keys.add(fullChild.keys.get(j));
        }
        if (!fullChild.isLeaf) {
            for (int j = degree; j <= 2 * degree - 1; j++) {
                newChild.children.add(fullChild.children.get(j));
            }
        }
        while (fullChild.keys.size() > degree - 1) {
            fullChild.keys.remove(fullChild.keys.size() - 1);
        }
        if (!fullChild.isLeaf) {
            while (fullChild.children.size() > degree) {
                fullChild.children.remove(fullChild.children.size() - 1);
            }
        }
        parent.keys.add(index, middleKey);
        parent.children.add(index + 1, newChild.diskAddress);
        writeNode(fullChild);
        writeNode(newChild);
        writeNode(parent);
    }

    @Override
    public TreeObject search(String key) throws IOException {
        return search(root, key);
    }

    private TreeObject search(BTreeNode node, String key) {
        int i = 0;
        while (i < node.keys.size() && key.compareTo(node.keys.get(i).getKey()) > 0) { i++; }
        if (i < node.keys.size() && key.compareTo(node.keys.get(i).getKey()) == 0) {
            return node.keys.get(i);
        } else if (node.isLeaf) {
            return null;
        } else {
            BTreeNode child = readNode(node.children.get(i));
            return search(child, key);
        }
    }

    @Override
    public void delete(String key) {
        // Optional
    }

    @Override
    public void dumpToFile(PrintWriter out) throws IOException {
        TreeObject[] sorted = getSortedTreeObjects();
        for (TreeObject obj : sorted) {
            out.println(obj.getKey() + " " + obj.getCount());
        }
        out.close();
    }

    @Override
    public void dumpToDatabase(String dbName, String tableName) throws IOException {
        // Optional
    }

    public String[] getSortedKeyArray() {
        sortedStringValues = new String[(int) size];
        sortedTreeObjects = new TreeObject[(int) size];
        inorderTraversal(root, sortedStringValues, sortedTreeObjects, 0);
        return sortedStringValues;
    }

    public TreeObject[] getSortedTreeObjects() {
        sortedStringValues = new String[(int) size];
        sortedTreeObjects = new TreeObject[(int) size];
        inorderTraversal(root, sortedStringValues, sortedTreeObjects, 0);
        return sortedTreeObjects;
    }

    private int inorderTraversal(BTreeNode node, String[] values, TreeObject[] objects, int index) {
        if (node.isLeaf) {
            for (TreeObject key : node.keys) {
                values[index] = key.getKey();
                objects[index++] = key;
            }
            return index;
        } else {
            int i = 0;
            for (; i < node.keys.size(); i++) {
                BTreeNode child = readNode(node.children.get(i));
                index = inorderTraversal(child, values, objects, index);
                values[index] = node.keys.get(i).getKey();
                objects[index++] = node.keys.get(i);
            }
            BTreeNode child = readNode(node.children.get(i));
            return inorderTraversal(child, values, objects, index);
        }
    }

    public void finishUp() throws IOException {
        writeMetaData();
        if (BTreeCache != null) {
            for (Entry<Long, BTreeNode> entry : BTreeCache.getCachedNodes().entrySet()) {
                diskWrite(entry.getValue());
            }
        }
        fileChannel.close();
    }

    private void writeNode(BTreeNode node) {
        if (BTreeCache != null) {
            BTreeNode evicted = BTreeCache.add(node);
            if (evicted != null) {
                diskWrite(evicted);
            }
        } else {
            diskWrite(node);
        }
    }

    private BTreeNode readNode(long diskAddress) {
        if (diskAddress == 0) return null;
        if (BTreeCache != null) {
            BTreeNode cached = BTreeCache.get(diskAddress);
            if (cached != null) return cached;
        }
        return diskRead(diskAddress);
    }

    private BTreeNode diskRead(long diskAddress) {
        try {
            fileChannel.position(diskAddress);
            buffer.clear();
            // [Change 4: full-buffer read loop to avoid partial I/O]
            while (buffer.hasRemaining()) {
                int r = fileChannel.read(buffer);
                if (r < 0) throw new IOException("EOF at " + diskAddress);
            }
            ((Buffer)buffer).flip();             // [Change 5: cast to Buffer before flip]
            return BTreeNode.fromByteBuffer(buffer, degree, diskAddress);
        } catch (IOException e) {
            throw new RuntimeException("Error reading node from disk: " + e.getMessage());
        }
    }

    private void diskWrite(BTreeNode node) {
        try {
            fileChannel.position(node.diskAddress);
            buffer.clear();
            node.toByteBuffer(buffer);
            ((Buffer)buffer).flip();           // [Change 6: cast to Buffer before flip]
            // [Change 7: full-buffer write loop to avoid partial I/O]
            while (buffer.hasRemaining()) {
                fileChannel.write(buffer);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error writing node to disk: " + e.getMessage());
        }
    }

    private void readMetaData() throws IOException {
        fileChannel.position(0);
        ByteBuffer tmp = ByteBuffer.allocateDirect(METADATA_SIZE);
        // [Change 8: full-buffer read loop for metadata]
        while (tmp.hasRemaining()) {
            if (fileChannel.read(tmp) < 0) break;
        }
        ((Buffer)tmp).flip();                  // [Change 9: cast before flip]
        root.diskAddress = tmp.getLong();
    }

    private void writeMetaData() throws IOException {
        fileChannel.position(0);
        ByteBuffer tmp = ByteBuffer.allocateDirect(METADATA_SIZE);
        tmp.putLong(root.diskAddress);
        ((Buffer)tmp).flip();                 // [Change 10: cast before flip]
        // [Change 11: full-buffer write loop for metadata]
        while (tmp.hasRemaining()) {
            fileChannel.write(tmp);
        }
    }

    private int estimateNodeDiskSize() {
        return (2 * degree - 1) * TreeObject.getDiskSize()
             + (2 * degree) * Long.BYTES
             + Long.BYTES + Integer.BYTES + 1;
    }
}
