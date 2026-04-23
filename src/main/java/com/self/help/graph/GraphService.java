package com.self.help.graph;

import com.self.help.graph.core.GraphEngine;
import com.self.help.graph.core.Row;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class GraphService {
    GraphEngine graphEngine;

    public GraphService() {
        System.out.println("Initializing GraphService");
        graphEngine = new GraphEngine();
        StaticDataLoader.getStaticData().forEach(record -> graphEngine.addRow(record.from(), record.fromAttr1(), record.to(), record.toAttr1(), record.relation()));
    }

    Set<Row> getRows() {
        return graphEngine.getMappedRows();
    }

    public void addRow(Row row) {
        graphEngine.addRow(row.from(), row.fromAttr1(), row.to(), row.toAttr1(), row.relation());
    }

    public List<Row> concentrateByRelation(List<String> relations) {
        return graphEngine.concentrateByRelation(relations);
    }

    public Set<String> getUniqueRelations() {
        return graphEngine.getUniqueRelations();
    }

    public Set<String> getUniqueNode() {
        return graphEngine.getUniqueNodes();
    }

    public List<Row> concentrateOnNodes(List<String> nodeNames) {
        return graphEngine.concentrateOnNodes(nodeNames);
    }

    public GraphAggregateStats getAggregateStats() {
        return graphEngine.getAggregateStats();
    }

    public GraphAggregateStats getAggregateStats(List<String> nodeNames) {
        return graphEngine.getAggregateStats(nodeNames);
    }
}
