package cs321.search;

import cs321.btree.BTree;
import cs321.btree.BTreeException;
import cs321.btree.TreeObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

public class SSHSearchBTree {
    public static void main(String[] args) throws BTreeException {
        try {
            SSHSearchBTreeArguments arguments = new SSHSearchBTreeArguments(args);

			BTree btree;
			if(arguments.isCacheEnabled())
			{
				btree = new BTree(arguments.getDegree(), arguments.getBtreeFile(), true, arguments.getCacheSize());
			}
			else
			{
				btree = new BTree(arguments.getDegree(), arguments.getBtreeFile());
			}

            List<SearchResult> results = new ArrayList<>();
            BufferedReader reader = new BufferedReader(new FileReader(arguments.getQueryFile()));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                TreeObject result = btree.search(line);
                if (result != null) {
                    results.add(new SearchResult(result.getKey(), result.getCount()));
                }
            }
            reader.close();

            if (arguments.getTopFrequency() > 0) {
                results.sort(Comparator
                        .comparingLong(SearchResult::getCount).reversed()
                        .thenComparing(SearchResult::getKey));
                results = results.subList(0, Math.min(arguments.getTopFrequency(), results.size()));
            }

            for (SearchResult r : results) {
                System.out.println(r.key + " " + r.count);
            }

        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static class SearchResult {
        String key;
        long count;

        public SearchResult(String key, long count) {
            this.key = key;
            this.count = count;
        }

        public String getKey() {
            return key;
        }

        public long getCount() {
            return count;
        }
    }
}

