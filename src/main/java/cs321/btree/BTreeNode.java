package cs321.btree;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import cs321.common.KeyInterface;

public class BTreeNode implements KeyInterface<Long> {

    public ArrayList<TreeObject> keys;
    public ArrayList<Long> children;
    public boolean isLeaf;
    public long diskAddress;
    private final int maxKeys;

    public BTreeNode(int degree) {
        this.maxKeys = (2 * degree) - 1;
        this.keys = new ArrayList<>(maxKeys);
        this.children = new ArrayList<>(maxKeys + 1);
        this.isLeaf = true;
        this.diskAddress = 0;
    }

    @Override
    public Long getKey() {
        return this.diskAddress;
    }

    public boolean isLeaf() {
        return this.isLeaf;
    }

    public boolean isFull() {
        return keys.size() == maxKeys;
    }

    public int getCount() {
        return keys.size();
    }

    public int getDiskSize() {
        return Integer.BYTES + 1 + Long.BYTES +
                (maxKeys * TreeObject.getDiskSize()) +
                ((maxKeys + 1) * Long.BYTES);
    }

    public static int getOptimalDegree() {
        int blockSize = 4096;
        int overhead = Integer.BYTES + 1 + Long.BYTES;
        int treeObjectSize = TreeObject.getDiskSize();
        int pointerSize = Long.BYTES;
        return (blockSize - overhead) / (2 * (treeObjectSize + pointerSize));
    }

    public void toByteBuffer(ByteBuffer buffer) {
        buffer.putInt(keys.size()); // number of keys
        buffer.put((byte) (isLeaf ? 1 : 0)); // 1 if leaf else 0
        buffer.putLong(diskAddress); // current disk address

        // Write all keys
        for (int i = 0; i < maxKeys; i++) {
            if (i < keys.size()) {
                writeTreeObject(buffer, keys.get(i));
            } else {
                writeEmptyTreeObject(buffer);
            }
        }

        // Write all child pointers
        for (int i = 0; i < maxKeys + 1; i++) {
            if (i < children.size()) {
                buffer.putLong(children.get(i));
            } else {
                buffer.putLong(0L); // 0 means null pointer
            }
        }
    }

    public static BTreeNode fromByteBuffer(ByteBuffer buffer, int degree, long diskAddress) {
        BTreeNode node = new BTreeNode(degree);
        node.diskAddress = diskAddress;
    
        int count = buffer.getInt();
        node.isLeaf = (buffer.get() == 1);
        node.diskAddress = buffer.getLong(); // disk address
    
        // Read all TreeObjects
        for (int i = 0; i < (2 * degree - 1); i++) {
            byte[] keyBytes = new byte[64];
            buffer.get(keyBytes);
            String keyString = new String(keyBytes, StandardCharsets.UTF_8).trim();
            long countValue = buffer.getLong();
            if (i < count) {
                node.keys.add(new TreeObject(keyString, countValue));
            }
            // otherwise skip the empty ones
        }
    
        // Read all child pointers
        for (int i = 0; i < (2 * degree); i++) {
            long childAddr = buffer.getLong();
            if (!node.isLeaf && i <= count) {
                node.children.add(childAddr);
            }
            // otherwise skip or just read to maintain correct positioning
        }
    
        return node;
    }
    

    private static void writeTreeObject(ByteBuffer buffer, TreeObject obj) {
        byte[] keyBytes = obj.getKey().getBytes(StandardCharsets.UTF_8);
        byte[] padded = new byte[64];
        for (int i = 0; i < Math.min(64, keyBytes.length); i++) {
            padded[i] = keyBytes[i];
        }
        buffer.put(padded);
        buffer.putLong(obj.getCount());
    }

    private static void writeEmptyTreeObject(ByteBuffer buffer) {
        buffer.put(new byte[64]); // empty key
        buffer.putLong(0L); // empty count
    }

    private static TreeObject readTreeObject(ByteBuffer buffer) {
        byte[] keyBytes = new byte[64];
        buffer.get(keyBytes);
        String key = new String(keyBytes, StandardCharsets.UTF_8).trim();
        long count = buffer.getLong();
        return new TreeObject(key, count);
    }
}
