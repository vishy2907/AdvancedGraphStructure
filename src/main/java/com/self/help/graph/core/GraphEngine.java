package com.self.help.graph.core;

import com.self.help.graph.GraphAggregateStats;
import lombok.Data;
import org.roaringbitmap.IntIterator;
import org.roaringbitmap.RoaringBitmap;

import java.util.*;

@Data
public class GraphEngine {
    private BiDirectionalDictionary nodeDict;
    private BiDirectionalDictionary attribute1Dict;

    private BiDirectionalDictionary relDict;

    private InvertedIndexColumn fromInverted;
    private InvertedIndexColumn fromAttribute1Inverted;
    private InvertedIndexColumn toInverted;
    private InvertedIndexColumn toAttribute1Inverted;

    private InvertedIndexColumn relationInverted;

    private RoaringBitmap deletedFromRows;
    private RoaringBitmap deletedToRows;

    private RowStore rowStore;

    public GraphEngine() {
        nodeDict = new BiDirectionalDictionary();
        attribute1Dict = new BiDirectionalDictionary();
        relDict = new BiDirectionalDictionary();

        fromInverted = new InvertedIndexColumn();
        fromAttribute1Inverted = new  InvertedIndexColumn();
        toInverted = new InvertedIndexColumn();
        toAttribute1Inverted = new  InvertedIndexColumn();
        relationInverted = new InvertedIndexColumn();

        deletedFromRows = new RoaringBitmap();
        deletedToRows = new RoaringBitmap();

        rowStore = new RowStore();
    }

    public void addRow(String from, String fromAttribute1, String to, String toAttribute1, String relation) {
        int fromId = nodeDict.getOrCreateId(from);
        int fromAttrId = attribute1Dict.getOrCreateId(fromAttribute1);
        int toId = nodeDict.getOrCreateId(to);
        int toAttrId = attribute1Dict.getOrCreateId(toAttribute1);
        int relId = relDict.getOrCreateId(relation);

        markDuplicateRowsAsDeleted(fromId, fromAttrId, toId, toAttrId, relId);

        int rowId = rowStore.addRow(fromId, fromAttrId, toId, toAttrId, relId);

        fromInverted.addRowToValue(fromId, rowId);
        fromAttribute1Inverted.addRowToValue(fromAttrId, rowId);
        toInverted.addRowToValue(toId, rowId);
        toAttribute1Inverted.addRowToValue(toAttrId, rowId);
        relationInverted.addRowToValue(relId, rowId);
    }

    public Set<Row> getMappedRows() {
        RoaringBitmap activeRows = getActiveRowIds();
        return new LinkedHashSet<>(hydrateRows(activeRows));
    }

    public List<Row> concentrateByRelation(List<String> relations) {
        // 1. Create a container for the combined Row IDs
        RoaringBitmap combinedResult = new RoaringBitmap();

        for (String relation : relations) {
            // 2. Get the Dict ID for this specific relation
            // We use a safe lookup (not getOrCreate) to avoid polluting the dict
            int relId = relDict.getIdIfExists(relation);

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
                    attribute1Dict.getValue(rowStore.getFromAttr1Id(rowId)),
                    nodeDict.getValue(rowStore.getToId(rowId)),
                    attribute1Dict.getValue(rowStore.getToAttr1Id(rowId)),
                    relDict.getValue(rowStore.getRelId(rowId))
            ));
        }

        return results;
    }

    public GraphAggregateStats getAggregateStats() {
        int activeRowCount = getActiveRowIds().getCardinality();
        return new GraphAggregateStats("all", List.of(), activeRowCount, activeRowCount, activeRowCount);
    }

    public GraphAggregateStats getAggregateStats(List<String> nodeNames) {
        RoaringBitmap selectedNodeIds = resolveNodeIds(nodeNames);
        if (selectedNodeIds.isEmpty()) {
            return new GraphAggregateStats("selected", List.copyOf(nodeNames), 0, 0, 0);
        }

        RoaringBitmap outgoingRows = collectRowsForNodes(selectedNodeIds, false);
        RoaringBitmap incomingRows = collectRowsForNodes(selectedNodeIds, true);
        RoaringBitmap uniqueRows = outgoingRows.clone();
        uniqueRows.or(incomingRows);

        return new GraphAggregateStats(
                "selected",
                List.copyOf(nodeNames),
                outgoingRows.getCardinality(),
                incomingRows.getCardinality(),
                uniqueRows.getCardinality()
        );
    }

    public Set<String> getUniqueRelations() {
        Set<String> results = new LinkedHashSet<>();
        for (int i = 0; i < relDict.size(); i++) {
            results.add(relDict.getValue(i));
        }
        return results;
    }

    public Set<String> getUniqueNodes() {
        Set<String> results = new LinkedHashSet<>();
        for (int i = 0; i < nodeDict.size(); i++) {
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
        RoaringBitmap seedNodeIds = resolveNodeIds(nodeNames);

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

    private RoaringBitmap resolveNodeIds(List<String> nodeNames) {
        RoaringBitmap nodeIds = new RoaringBitmap();
        for (String name : nodeNames) {
            int id = nodeDict.getIdIfExists(name);
            if (id != -1) {
                nodeIds.add(id);
            }
        }
        return nodeIds;
    }

    private RoaringBitmap collectRowsForNodes(RoaringBitmap nodeIds, boolean incoming) {
        RoaringBitmap rows = new RoaringBitmap();
        IntIterator it = nodeIds.getIntIterator();

        while (it.hasNext()) {
            int nodeId = it.next();
            rows.or(incoming ? toInverted.getRowsForValue(nodeId) : fromInverted.getRowsForValue(nodeId));
        }

        rows.andNot(getDeletedRowIds());
        return rows;
    }

    private void markDuplicateRowsAsDeleted(int fromId, int fromAttrId, int toId, int toAttrId, int relId) {
        RoaringBitmap duplicateRows = fromInverted.getRowsForValue(fromId).clone();
        duplicateRows.and(fromAttribute1Inverted.getRowsForValue(fromAttrId));
        duplicateRows.and(toInverted.getRowsForValue(toId));
        duplicateRows.and(toAttribute1Inverted.getRowsForValue(toAttrId));
        duplicateRows.and(relationInverted.getRowsForValue(relId));
        duplicateRows.andNot(getDeletedRowIds());

        if (!duplicateRows.isEmpty()) {
            deletedFromRows.or(duplicateRows);
            deletedToRows.or(duplicateRows);
        }
    }

    private RoaringBitmap getDeletedRowIds() {
        RoaringBitmap deletedRows = deletedFromRows.clone();
        deletedRows.and(deletedToRows);
        return deletedRows;
    }

    private RoaringBitmap getActiveRowIds() {
        RoaringBitmap activeRows = new RoaringBitmap();
        int rowCount = rowStore.getRowCount();
        if (rowCount > 0) {
            activeRows.add(0L, rowCount);
            activeRows.andNot(getDeletedRowIds());
        }
        return activeRows;
    }

    /**
     * High-performance Level-Synchronous BFS for Directed Acyclic Graphs (DAGs).
     * * @param initialFrontier The starting Node IDs
     *
     * @param collectedRows The global bitmap where matching Row IDs are accumulated
     * @param isUpward      True for Ancestry (Parents), False for Descendants (Children)
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
                RoaringBitmap rows = (isUpward ? toInverted.getRowsForValue(nodeId)
                        : fromInverted.getRowsForValue(nodeId)).clone();
                rows.andNot(getDeletedRowIds());

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
                    attribute1Dict.getValue(rowStore.getFromAttr1Id(rowId)),
                    nodeDict.getValue(rowStore.getToId(rowId)),
                    attribute1Dict.getValue(rowStore.getToAttr1Id(rowId)),
                    relDict.getValue(rowStore.getRelId(rowId))
            ));
        }

        return results;
    }
}
