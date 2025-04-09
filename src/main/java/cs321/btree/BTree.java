package cs321.btree;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class BTree implements BTreeInterface 

{

    // Node Class Begin//////////////////////////////////////////////////////////////////
    private class BTreeNode{
        List<TreeObject> keys;         // keys in this node
        List<BTreeNode> children;      // children of this node
        boolean isLeaf;
        long address;


        public BTreeNode(boolean isLeaf) {
            this.isLeaf = isLeaf;
            this.keys = new ArrayList<>();
            this.children = new ArrayList<>();
        }
    }
    // Node Class End////////////////////////////////////////////////////////////////////

    
    private int degree = 0;
    private long size; // total number of keys
    private long numberOfNodes;
    private BTreeNode root; // pointer to root node
    private RandomAccessFile file; // file to be read from 
    private int nodeSize;
    private long nextDiskOffset = 8; // Reserve first 8 bytes for metadata (e.g., root address)
    private byte[] ioBuffer; // Used for read/write ops






    //Constructor
    public BTree(int degree, String filename) throws IOException {
        if (degree < 2) {
            throw new IllegalArgumentException("Degree must be >= 2");
        }
        this.degree = degree;
        this.size = 0;
        this.numberOfNodes = 0;
        this.root = new BTreeNode(true);
        this.file = new RandomAccessFile(filename, "rw");

        // Compute size of one BTreeNode on disk
        this.nodeSize = 1 + Integer.BYTES +
        (2 * degree - 1) * TreeObject.BYTES +
        (2 * degree) * Long.BYTES;

        this.ioBuffer = new byte[nodeSize];
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
        return numberOfNodes;
    }

    @Override
    public int getHeight() {
        return getHeight(root);
    }

    private int getHeight(BTreeNode node) {
        if (node.isLeaf) {
            return 1;
        } else {
            return 1 + getHeight(node.children.get(0));
        }
    }

    /////////Searching Methods/////////////////////////////////////////////////////

    // Searches from root
    @Override
    public TreeObject search(String key) {
        return searchRecursive(this.root, key);
    }


    // Searches from a given node
    private TreeObject searchRecursive(BTreeNode node, String key) {
        int i = 0;
        while (i < node.keys.size() && key.compareTo(node.keys.get(i).getKey()) > 0) {
            i++;
        }

        if (i < node.keys.size() && key.equals(node.keys.get(i).getKey())) {
            return node.keys.get(i);
        }

        if (node.isLeaf) {
            return null;
        } else {
            return searchRecursive(node.children.get(i), key);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////
    

    /////BTree Manipulation/////////////////////////////////////////////////////////////

    @Override
    public void delete(String key){
    }

    // Delete helper methods

    //
    @Override
    public void insert(TreeObject obj) throws IOException {
        BTreeNode r = root;
        TreeObject existing = search(obj.getKey());

        if (existing != null) {
            existing.incCount();
            return;
        }

        if (r.keys.size() == (2 * degree - 1)) {
            BTreeNode s = new BTreeNode(false);
            s.children.add(r);
            root = s;
            splitChild(s, 0);
            insertNonFull(s, obj);
        } else {
            insertNonFull(r, obj);
        }

        size++;
        // diskWrite(r); for making insert write to disk
    }



    // Insert helper methods
    private void insertNonFull(BTreeNode node, TreeObject obj) {
        int i = node.keys.size() - 1;
    
        if (node.isLeaf) {
            while (i >= 0 && obj.compareTo(node.keys.get(i)) < 0) {
                i--;
            }
            node.keys.add(i + 1, obj);
        } else {
            while (i >= 0 && obj.compareTo(node.keys.get(i)) < 0) {
                i--;
            }
            i++;
            if (node.children.get(i).keys.size() == (2 * degree - 1)) {
                splitChild(node, i);
                if (obj.compareTo(node.keys.get(i)) > 0) {
                    i++;
                }
            }
            insertNonFull(node.children.get(i), obj);
        }
    }

    private void splitChild(BTreeNode parent, int index) {
        BTreeNode y = parent.children.get(index);
        BTreeNode z = new BTreeNode(y.isLeaf);
    
        for (int j = 0; j < degree - 1; j++) {
            z.keys.add(y.keys.remove(degree));
        }
    
        if (!y.isLeaf) {
            for (int j = 0; j < degree; j++) {
                z.children.add(y.children.remove(degree));
            }
        }
    
        parent.children.add(index + 1, z);
        parent.keys.add(index, y.keys.remove(degree - 1));
        numberOfNodes++;
    }
    //


    
    ///Disk methods//////////////////////////////////////////////////////////////////////////////////////////

    public void diskWrite(BTreeNode x) throws IOException {
    if (x.address == 0) {
        x.address = nextDiskOffset;
        nextDiskOffset += nodeSize;
    }

    file.seek(x.address);
    int pos = 0;

    ioBuffer[pos++] = (byte) (x.isLeaf ? 1 : 0);

    // Number of keys
    ByteBuffer.wrap(ioBuffer, pos, Integer.BYTES).putInt(x.keys.size());
    pos += Integer.BYTES;

    // Write keys
    for (int i = 0; i < 2 * degree - 1; i++) {
        if (i < x.keys.size()) {
            TreeObject obj = x.keys.get(i);
            byte[] keyBytes = new byte[64];
            byte[] rawKey = obj.getKey().getBytes();
            System.arraycopy(rawKey, 0, keyBytes, 0, Math.min(64, rawKey.length));
            System.arraycopy(keyBytes, 0, ioBuffer, pos, 64);
            pos += 64;
            ByteBuffer.wrap(ioBuffer, pos, Long.BYTES).putLong(obj.getCount());
            pos += Long.BYTES;
        } else {
            pos += 64 + Long.BYTES;
        }
    }

    // Write child pointers
    for (int i = 0; i < 2 * degree; i++) {
        long addr = (i < x.children.size()) ? x.children.get(i).address : 0;
        ByteBuffer.wrap(ioBuffer, pos, Long.BYTES).putLong(addr);
        pos += Long.BYTES;
    }

    file.write(ioBuffer);
}

    
    public BTreeNode diskRead(long offset) throws IOException {
    file.seek(offset);
    file.readFully(ioBuffer);
    int pos = 0;

    boolean isLeaf = ioBuffer[pos++] == 1;
    int keyCount = ByteBuffer.wrap(ioBuffer, pos, Integer.BYTES).getInt();
    pos += Integer.BYTES;

    BTreeNode node = new BTreeNode(isLeaf);
    node.address = offset;

    for (int i = 0; i < keyCount; i++) {
        byte[] keyBytes = new byte[64];
        System.arraycopy(ioBuffer, pos, keyBytes, 0, 64);
        pos += 64;
        String key = new String(keyBytes).trim();
        long count = ByteBuffer.wrap(ioBuffer, pos, Long.BYTES).getLong();
        pos += Long.BYTES;
        node.keys.add(new TreeObject(key, count));
    }

    // Skip remaining key slots
    pos += (2 * degree - 1 - keyCount) * TreeObject.BYTES;

    for (int i = 0; i < 2 * degree; i++) {
        long addr = ByteBuffer.wrap(ioBuffer, pos, Long.BYTES).getLong();
        pos += Long.BYTES;
        if (addr != 0) {
            BTreeNode child = new BTreeNode(true);
            child.address = addr;
            node.children.add(child);
        }
    }

    return node;
}


    ////////////////////////////////////////////////////////////////////////////////////////////////
    




    @Override
    public void dumpToFile(PrintWriter out) throws IOException{
        //todo
    }

    @Override
    public void dumpToDatabase(String dbName, String tableName) throws IOException{
        //todo
    }

   







}
