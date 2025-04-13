package cs321.btree;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class BTree implements BTreeInterface {

    // Inner class to represent nodes in the B-Tree.
    private class Node {
        boolean leaf;
        ArrayList<TreeObject> keys;
        ArrayList<Node> children;

        Node(boolean leaf) {
            this.leaf = leaf;
            keys = new ArrayList<>();
            children = new ArrayList<>();
        }
    }

    private Node root;
    private int degree;          // minimum degree (t)
    private long size;           // number of distinct keys in the tree
    private long numberOfNodes;  // total number of nodes (root included)
    private int height;          // height of the tree (single node tree has height 0)
    private String filename;     // filename (used for disk I/O in a more complete implementation)

    /**
     * One-argument constructor that creates a BTree with default degree 2.
     *
     * @param filename the file name used for disk I/O (unused in this in-memory version)
     * @throws BTreeException if the tree cannot be created
     */
    public BTree(String filename) throws BTreeException {
        this(2, filename);
    }

    /**
     * Constructs a BTree with the specified degree.
     *
     * @param degree   the minimum degree of the BTree (must be at least 2)
     * @param filename the file name used for disk I/O (unused in this implementation)
     * @throws BTreeException if the degree is less than 2
     */
    public BTree(int degree, String filename) throws BTreeException {
        if (degree < 2) {
            throw new BTreeException("Degree must be at least 2");
        }
        this.degree = degree;
        this.filename = filename;
        root = new Node(true);   // start with an empty leaf node as root
        size = 0;
        numberOfNodes = 1;       // only the root node exists
        height = 0;
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
        return height;
    }

    @Override
    public void insert(TreeObject obj) throws IOException {
        // Check for duplicate key. If duplicate exists, update count.
        TreeObject existing = search(obj.getKey());
        if (existing != null) {
            existing.setCount(existing.getCount() + 1);
            return;
        }
        // If root is full, split it and increase the tree height.
        if (root.keys.size() == 2 * degree - 1) {
            Node s = new Node(false);
            s.children.add(root);
            splitChild(s, 0, root);
            root = s;
            numberOfNodes++; // new root is created
            height++;        // height increases when the root splits
        }
        insertNonFull(root, obj);
        size++;  // increase count of distinct keys
    }

    private void insertNonFull(Node x, TreeObject k) {
        int i = x.keys.size() - 1;
        if (x.leaf) {
            // Insert the key into the leaf node in the correct order.
            while (i >= 0 && k.compareTo(x.keys.get(i)) < 0) {
                i--;
            }
            x.keys.add(i + 1, k);
        } else {
            // Find the child to descend into.
            while (i >= 0 && k.compareTo(x.keys.get(i)) < 0) {
                i--;
            }
            i++;
            // If the found child is full, split it.
            if (x.children.get(i).keys.size() == 2 * degree - 1) {
                splitChild(x, i, x.children.get(i));
                // Determine which of the two children we should use.
                if (k.compareTo(x.keys.get(i)) > 0) {
                    i++;
                }
            }
            insertNonFull(x.children.get(i), k);
        }
    }

    private void splitChild(Node parent, int index, Node fullChild) {
        Node newChild = new Node(fullChild.leaf);

        // Move the latter (degree-1) keys to the new node.
        for (int j = 0; j < degree - 1; j++) {
            newChild.keys.add(fullChild.keys.get(j + degree));
        }
        // If not a leaf, move the corresponding children.
        if (!fullChild.leaf) {
            for (int j = 0; j < degree; j++) {
                newChild.children.add(fullChild.children.get(j + degree));
            }
        }
        // Remove the keys and children that were moved.
        for (int j = fullChild.keys.size() - 1; j >= degree; j--) {
            fullChild.keys.remove(j);
        }
        if (!fullChild.leaf) {
            for (int j = fullChild.children.size() - 1; j >= degree; j--) {
                fullChild.children.remove(j);
            }
        }
        // The median key is moved up to the parent.
        TreeObject median = fullChild.keys.remove(degree - 1);
        parent.keys.add(index, median);
        parent.children.add(index + 1, newChild);
        numberOfNodes++; // account for the new node
    }

    @Override
    public TreeObject search(String key) throws IOException {
        return search(root, key);
    }

    private TreeObject search(Node x, String key) {
        int i = 0;
        while (i < x.keys.size() && key.compareTo(x.keys.get(i).getKey()) > 0) {
            i++;
        }
        if (i < x.keys.size() && key.equals(x.keys.get(i).getKey())) {
            return x.keys.get(i);
        }
        if (x.leaf) {
            return null;
        } else {
            return search(x.children.get(i), key);
        }
    }

    /**
     * Returns a sorted array of key strings from an inorder traversal of the BTree.
     */
    public String[] getSortedKeyArray() throws IOException {
        List<String> keysList = new ArrayList<>();
        inorder(root, keysList);
        return keysList.toArray(new String[0]);
    }

    private void inorder(Node x, List<String> list) {
        if (x.leaf) {
            for (TreeObject key : x.keys) {
                list.add(key.getKey());
            }
        } else {
            int i;
            for (i = 0; i < x.keys.size(); i++) {
                inorder(x.children.get(i), list);
                list.add(x.keys.get(i).getKey());
            }
            inorder(x.children.get(i), list);
        }
    }

    @Override
    public void dumpToFile(PrintWriter out) throws IOException {
        // Not implemented
    }

    @Override
    public void dumpToDatabase(String dbName, String tableName) throws IOException {
        // Not implemented
    }

    @Override
    public void delete(String key) {
        // Not implemented
    }

    private void diskRead() throws IOException {
        // Not implemented
    }

    private void diskWrite() throws IOException {
        // Not implemented
    }
}
