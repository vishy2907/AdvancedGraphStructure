package com.self.help.graph;

import java.util.List;

public record GraphAggregateStats(
        String scope,
        List<String> selectedNodes,
        int outgoingEdgeCount,
        int incomingEdgeCount,
        int uniqueEdgeCount
) {
}
