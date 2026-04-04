package com.self.help.graph.core;

import lombok.Data;
import org.roaringbitmap.IntIterator;
import org.roaringbitmap.RoaringBitmap;

import java.util.*;

@Data
public class GraphEngine {
    private BiDirectionalDictionary nodeDict;
    private BiDirectionalDictionary relDict;

    private InvertedIndexColumn fromInverted;
    private InvertedIndexColumn toInverted;
    private InvertedIndexColumn relationInverted;

    private RowStore rowStore;

    public GraphEngine() {
        nodeDict = new BiDirectionalDictionary();
        relDict = new BiDirectionalDictionary();

        fromInverted = new InvertedIndexColumn();
        toInverted = new InvertedIndexColumn();
        relationInverted = new InvertedIndexColumn();

        rowStore = new RowStore();
    }

    public void addRow(String from, String to, String relation) {
        int fromId = nodeDict.getOrCreateId(from);
        int toId = nodeDict.getOrCreateId(to);
        int relId = relDict.getOrCreateId(relation);

        int rowId = rowStore.addRow(fromId, toId, relId);

        fromInverted.addRowToValue(fromId, rowId);
        toInverted.addRowToValue(toId, rowId);
        relationInverted.addRowToValue(relId, rowId);
    }

    public Set<Row> getMappedRows() {
        int rowCount = rowStore.getRowCount();
        Set<Row> results = new LinkedHashSet<>();

        for (int i = 0; i < rowCount; i++) {
            // 1. Get the Dict IDs from the RowStore (Forward Index)
            int fromId = rowStore.getFromId(i);
            int toId = rowStore.getToId(i);
            int relId = rowStore.getRelId(i);

            // 2. Translate Dict IDs back to Strings using Dictionaries
            String fromValue = nodeDict.getValue(fromId);
            String toValue = nodeDict.getValue(toId);
            String relValue = relDict.getValue(relId);

            // 3. Create the Record and add to list
            results.add(new Row(fromValue, toValue, relValue));
        }

        return results;
    }

    public List<Row> concentrateByRelation(List<String> relations) {
        // 1. Create a container for the combined Row IDs
        RoaringBitmap combinedResult = new RoaringBitmap();

        for (String relation : relations) {
            // 2. Get the Dict ID for this specific relation
            // We use a safe lookup (not getOrCreate) to avoid polluting the dict
            int relId = relDict.getOrCreateId(relation);

            if (relId != -1) {
                // 3. Perform Bitwise OR: O(N) where N is number of matching rows
                // This is vectorized and incredibly fast
                combinedResult.or(relationInverted.getRowsForValue(relId));
            }
        }

        // 4. Hydrate only the matching rows
        List<Row> results = new ArrayList<>(combinedResult.getCardinality());
        IntIterator it = combinedResult.getIntIterator();

        while (it.hasNext()) {
            int rowId = it.next();
            results.add(new Row(
                    nodeDict.getValue(rowStore.getFromId(rowId)),
                    nodeDict.getValue(rowStore.getToId(rowId)),
                    relDict.getValue(rowStore.getRelId(rowId))
            ));
        }

        return results;
    }

    public Set<String> getUniqueRelations() {
        Set<String> results = new LinkedHashSet<>();
        for(int i=0;i<relDict.size();i++) {
            results.add(relDict.getValue(i));
        }
        return results;
    }

    public Set<String> getUniqueNodes() {
        Set<String> results = new LinkedHashSet<>();
        for(int i=0;i<nodeDict.size();i++) {
            results.add(nodeDict.getValue(i));
        }
        return results;
    }

    /**
     * Returns all rows representing the full upstream (ancestry) and
     * downstream (descendant) graph connected to the given node names.
     */
    public List<Row> concentrateOnNodes(List<String> nodeNames) {
        // 1. Resolve Seed Node IDs
        RoaringBitmap seedNodeIds = new RoaringBitmap();
        for (String name : nodeNames) {
            int id = nodeDict.getIdIfExists(name); // Assuming you added getIdIfExists
            if (id != -1) {
                seedNodeIds.add(id);
            }
        }

        if (seedNodeIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. The Global Collector (Automatically deduplicates Row IDs)
        RoaringBitmap collectedRows = new RoaringBitmap();

        // 3. Traverse Upward (Find all Ancestors)
        traverseDAG(seedNodeIds, collectedRows, true);

        // 4. Traverse Downward (Find all Descendants)
        traverseDAG(seedNodeIds, collectedRows, false);

        // 5. Hydrate the final unique set of edges back into Objects
        return hydrateRows(collectedRows);
    }

    /**
     * High-performance Level-Synchronous BFS for Directed Acyclic Graphs (DAGs).
     * * @param initialFrontier The starting Node IDs
     * @param collectedRows   The global bitmap where matching Row IDs are accumulated
     * @param isUpward        True for Ancestry (Parents), False for Descendants (Children)
     */
    private void traverseDAG(RoaringBitmap initialFrontier, RoaringBitmap collectedRows, boolean isUpward) {
        RoaringBitmap currentFrontier = initialFrontier.clone();

        // Tracks nodes we've already processed to prevent duplicate work on Diamond patterns
        RoaringBitmap visitedNodes = initialFrontier.clone();

        while (!currentFrontier.isEmpty()) {
            RoaringBitmap nextFrontier = new RoaringBitmap();
            IntIterator nodeIt = currentFrontier.getIntIterator();

            while (nodeIt.hasNext()) {
                int nodeId = nodeIt.next();

                // Step A: Find matching edges for this Node
                // Upward = Who points to me? (I am the 'To' node)
                // Downward = Who do I point to? (I am the 'From' node)
                RoaringBitmap rows = isUpward ? toInverted.getRowsForValue(nodeId)
                        : fromInverted.getRowsForValue(nodeId);

                // Step B: Accumulate these Row IDs into the global output
                collectedRows.or(rows);

                // Step C: Look up the next nodes using the Forward Index (RowStore)
                IntIterator rowIt = rows.getIntIterator();
                while (rowIt.hasNext()) {
                    int rowId = rowIt.next();

                    // If moving Upward, the next logical node is the 'From' node.
                    // If moving Downward, the next logical node is the 'To' node.
                    int nextNodeId = isUpward ? rowStore.getFromId(rowId)
                            : rowStore.getToId(rowId);

                    // Step D: Diamond Pattern check
                    // Only add to the next frontier if we haven't expanded it yet
                    if (!visitedNodes.contains(nextNodeId)) {
                        visitedNodes.add(nextNodeId);
                        nextFrontier.add(nextNodeId);
                    }
                }
            }

            // Move to the next degree of separation
            currentFrontier = nextFrontier;
        }
    }

    /**
     * Hydrates an optimized RoaringBitmap of Row IDs back into a List of Row objects.
     */
    private List<Row> hydrateRows(RoaringBitmap rowIds) {
        int expectedSize = rowIds.getCardinality();
        if (expectedSize == 0) {
            return Collections.emptyList();
        }

        List<Row> results = new ArrayList<>(expectedSize);
        IntIterator it = rowIds.getIntIterator();

        while (it.hasNext()) {
            int rowId = it.next();
            results.add(new Row(
                    nodeDict.getValue(rowStore.getFromId(rowId)),
                    nodeDict.getValue(rowStore.getToId(rowId)),
                    relDict.getValue(rowStore.getRelId(rowId))
            ));
        }

        return results;
    }
}