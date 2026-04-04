package com.self.help.graph;

import com.self.help.graph.core.GraphEngine;
import com.self.help.graph.core.Row;
import org.roaringbitmap.RoaringBitmap;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class GraphService {
    GraphEngine graphEngine;

    public GraphService() {
        System.out.println("Initializing GraphService");
        graphEngine = new GraphEngine();
        StaticDataLoader.getStaticData().forEach(record -> graphEngine.addRow(record.from(), record.to(), record.relation()));
    }

    Set<Row> getRows() {
        return graphEngine.getMappedRows();
    }

    public void addRow(Row row) {
        graphEngine.addRow(row.from(), row.to(), row.relation());
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
}
