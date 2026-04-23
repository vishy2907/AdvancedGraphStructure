package com.self.help.graph.core;

import com.self.help.graph.GraphAggregateStats;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GraphEngineAggregateStatsTest {

    @Test
    void returnsAllRowCountsForWholeGraph() {
        GraphEngine engine = new GraphEngine();
        engine.addRow("A", "a1", "B", "b1", "r1");
        engine.addRow("B", "b1", "C", "c1", "r2");
        engine.addRow("C", "c1", "A", "a1", "r3");

        GraphAggregateStats stats = engine.getAggregateStats();

        assertEquals("all", stats.scope());
        assertEquals(List.of(), stats.selectedNodes());
        assertEquals(3, stats.outgoingEdgeCount());
        assertEquals(3, stats.incomingEdgeCount());
        assertEquals(3, stats.uniqueEdgeCount());
    }

    @Test
    void countsIncomingOutgoingAndUniqueEdgesForSelectedNodes() {
        GraphEngine engine = new GraphEngine();
        engine.addRow("A", "a1", "B", "b1", "r1");
        engine.addRow("B", "b1", "C", "c1", "r2");
        engine.addRow("C", "c1", "A", "a1", "r3");
        engine.addRow("B", "b1", "B", "b1", "self");

        GraphAggregateStats stats = engine.getAggregateStats(List.of("B"));

        assertEquals("selected", stats.scope());
        assertEquals(List.of("B"), stats.selectedNodes());
        assertEquals(2, stats.outgoingEdgeCount());
        assertEquals(2, stats.incomingEdgeCount());
        assertEquals(3, stats.uniqueEdgeCount());
    }

    @Test
    void unionsCountsAcrossMultipleSelectedNodesWithoutDoubleCountingUniqueRows() {
        GraphEngine engine = new GraphEngine();
        engine.addRow("A", "a1", "B", "b1", "r1");
        engine.addRow("B", "b1", "C", "c1", "r2");
        engine.addRow("C", "c1", "A", "a1", "r3");
        engine.addRow("B", "b1", "A", "a1", "r4");

        GraphAggregateStats stats = engine.getAggregateStats(List.of("A", "B"));

        assertEquals(3, stats.outgoingEdgeCount());
        assertEquals(3, stats.incomingEdgeCount());
        assertEquals(4, stats.uniqueEdgeCount());
    }

    @Test
    void returnsZeroCountsWhenNoSelectedNodesExistInGraph() {
        GraphEngine engine = new GraphEngine();
        engine.addRow("A", "a1", "B", "b1", "r1");

        GraphAggregateStats stats = engine.getAggregateStats(List.of("Z"));

        assertEquals("selected", stats.scope());
        assertEquals(List.of("Z"), stats.selectedNodes());
        assertEquals(0, stats.outgoingEdgeCount());
        assertEquals(0, stats.incomingEdgeCount());
        assertEquals(0, stats.uniqueEdgeCount());
    }

    @Test
    void duplicateIngestReplacesOlderRowInsteadOfCountingItTwice() {
        GraphEngine engine = new GraphEngine();
        Row duplicate = new Row("A", "a1", "B", "b1", "r1");

        engine.addRow(duplicate.from(), duplicate.fromAttr1(), duplicate.to(), duplicate.toAttr1(), duplicate.relation());
        engine.addRow(duplicate.from(), duplicate.fromAttr1(), duplicate.to(), duplicate.toAttr1(), duplicate.relation());

        Set<Row> activeRows = engine.getMappedRows();
        GraphAggregateStats stats = engine.getAggregateStats(List.of("A", "B"));

        assertEquals(Set.of(duplicate), activeRows);
        assertEquals(1, stats.outgoingEdgeCount());
        assertEquals(1, stats.incomingEdgeCount());
        assertEquals(1, stats.uniqueEdgeCount());
    }
}
