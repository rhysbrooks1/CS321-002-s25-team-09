package cs321.btree;

public class TreeObject implements Comparable<TreeObject> {

    private String key;
    private long count;

    public TreeObject(String key) {
        this.key = key;
        this.count = 1;
    }

    public TreeObject(String key, long count) {
        this.key = key;
        this.count = count;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    /**
     * @param o the object to be compared.
     * @return
     */
    @Override
    public int compareTo(TreeObject o) {
        return this.key.compareTo(o.getKey());
    }
}
