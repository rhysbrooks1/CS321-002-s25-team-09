package cs321.btree;

import java.io.EOFException;
import java.io.File;
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

    // === Constants ===

    /**
     * Metadata layout (in bytes):
     *   root.diskAddress      : Long.BYTES
     *   nextDiskAddress       : Long.BYTES
     *   nodes count           : Integer.BYTES
     *   height                : Integer.BYTES
     */
    private final int METADATA_SIZE =
        Long.BYTES    /* root.diskAddress */
      + Long.BYTES    /* nextDiskAddress */
      + Integer.BYTES /* nodes */
      + Integer.BYTES /* height */;

    // === Metadata ===

    private long nextDiskAddress = METADATA_SIZE;
    private int nodeSize;
    private long size;
    private int degree;
    private int nodes;
    private int height;
    private String filename;

    // === Core structures ===

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

        File file = new File(filename);
        try {
            RandomAccessFile dataFile = new RandomAccessFile(filename, "rw");
            fileChannel = dataFile.getChannel();
            // Always start fresh: truncate any existing file
            fileChannel.truncate(0);
            // Write empty-tree metadata and root
            writeMetaData();
            writeNode(root);
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
            while (i >= 0 && obj.compareTo(node.keys.get(i)) < 0) i--;
            if (i >= 0 && obj.compareTo(node.keys.get(i)) == 0) {
                node.keys.get(i).setCount(node.keys.get(i).getCount() + 1);
            } else {
                node.keys.add(i + 1, obj);
                size++;
            }
            writeNode(node);
        } else {
            while (i >= 0 && obj.compareTo(node.keys.get(i)) < 0) i--;
            if (i >= 0 && obj.compareTo(node.keys.get(i)) == 0) {
                node.keys.get(i).setCount(node.keys.get(i).getCount() + 1);
                writeNode(node);
                return;
            }
            i++;
            long childAddr = node.children.get(i);
            BTreeNode child = readNode(childAddr);
            if (child.keys.size() == 2 * degree - 1) {
                splitChild(node, i);
                if (obj.compareTo(node.keys.get(i)) == 0) {
                    node.keys.get(i).setCount(node.keys.get(i).getCount() + 1);
                    writeNode(node);
                    return;
                }
                if (obj.compareTo(node.keys.get(i)) > 0) i++;
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

        TreeObject mid = fullChild.keys.get(degree - 1);
        for (int j = degree; j < 2 * degree - 1; j++) newChild.keys.add(fullChild.keys.get(j));
        if (!fullChild.isLeaf) {
            for (int j = degree; j <= 2 * degree - 1; j++)
                newChild.children.add(fullChild.children.get(j));
        }
        while (fullChild.keys.size() > degree - 1) fullChild.keys.remove(fullChild.keys.size() - 1);
        if (!fullChild.isLeaf) while (fullChild.children.size() > degree)
            fullChild.children.remove(fullChild.children.size() - 1);

        parent.keys.add(index, mid);
        parent.children.add(index + 1, newChild.diskAddress);

        writeNode(fullChild);
        writeNode(newChild);
        writeNode(parent);
    }

    // === Search ===

    @Override
    public TreeObject search(String key) throws IOException {
        return search(root, key);
    }

    private TreeObject search(BTreeNode node, String key) {
        int i = 0;
        while (i < node.keys.size() && key.compareTo(node.keys.get(i).getKey()) > 0) i++;
        if (i < node.keys.size() && key.compareTo(node.keys.get(i).getKey()) == 0)
            return node.keys.get(i);
        if (node.isLeaf) return null;
        return search(readNode(node.children.get(i)), key);
    }

    @Override
    public void delete(String key) {
        // Optional
    }

    // === Dumping ===

    @Override
    public void dumpToFile(PrintWriter out) throws IOException {
        for (TreeObject obj : getSortedTreeObjects()) out.println(obj.getKey() + " " + obj.getCount());
        out.close();
    }

    @Override
    public void dumpToDatabase(String dbName, String tableName) throws IOException {
        // Optional
    }

    // === Inorder traversal helpers ===

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

    private int inorderTraversal(BTreeNode node, String[] vals, TreeObject[] objs, int idx) {
        if (node.isLeaf) {
            for (TreeObject k : node.keys) {
                vals[idx] = k.getKey(); objs[idx] = k; idx++;
            }
            return idx;
        }
        int i = 0;
        for (; i < node.keys.size(); i++) {
            idx = inorderTraversal(readNode(node.children.get(i)), vals, objs, idx);
            vals[idx] = node.keys.get(i).getKey(); objs[idx] = node.keys.get(i); idx++;
        }
        return inorderTraversal(readNode(node.children.get(i)), vals, objs, idx);
    }

    // === Metadata ===

    public void finishUp() throws IOException {
        writeMetaData();
        if (BTreeCache != null) {
            for (Entry<Long, BTreeNode> e : BTreeCache.getCachedNodes().entrySet())
                diskWrite(e.getValue());
        }
        fileChannel.close();
    }

    private void writeMetaData() throws IOException {
        fileChannel.position(0);
        ByteBuffer m = ByteBuffer.allocateDirect(METADATA_SIZE);
        m.putLong(root.diskAddress);
        m.putLong(nextDiskAddress);
        m.putInt(nodes);
        m.putInt(height);
        ((Buffer) m).flip();
        while (m.hasRemaining()) fileChannel.write(m);
    }

    private int estimateNodeDiskSize() {
        return (2 * degree - 1) * TreeObject.getDiskSize()
             + (2 * degree) * Long.BYTES
             + Long.BYTES
             + Integer.BYTES
             + 1;
    }

    // === Disk IO ===

    private void writeNode(BTreeNode node) {
        if (BTreeCache != null) {
            BTreeNode ev = BTreeCache.add(node);
            if (ev != null) diskWrite(ev);
        } else {
            diskWrite(node);
        }
    }

    private BTreeNode readNode(long addr) {
        if (addr == 0) return null;
        if (BTreeCache != null) {
            BTreeNode c = BTreeCache.get(addr);
            if (c != null) return c;
        }
        return diskRead(addr);
    }

    private BTreeNode diskRead(long addr) {
        try {
            fileChannel.position(addr);
            buffer.clear();
            while (buffer.hasRemaining()) {
                int r = fileChannel.read(buffer);
                if (r < 0) throw new IOException("EOF at " + addr);
            }
            ((Buffer) buffer).flip();
            return BTreeNode.fromByteBuffer(buffer, degree, addr);
        } catch (IOException e) {
            throw new RuntimeException("Read error: " + e.getMessage(), e);
        }
    }

    private void diskWrite(BTreeNode node) {
        try {
            fileChannel.position(node.diskAddress);
            buffer.clear();
            node.toByteBuffer(buffer);
            ((Buffer) buffer).flip();
            while (buffer.hasRemaining()) fileChannel.write(buffer);
        } catch (IOException e) {
            throw new RuntimeException("Write error: " + e.getMessage(), e);
        }
    }
}
