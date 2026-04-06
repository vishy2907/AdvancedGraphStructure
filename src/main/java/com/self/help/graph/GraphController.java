package com.self.help.graph;

import com.self.help.graph.core.Row;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RestController
public class GraphController {
    private final GraphService graphService;

    public GraphController(GraphService graphService) {
        this.graphService = graphService;
    }

    @GetMapping("/graph")
    public Set<Row> getGraph() {
        return graphService.getRows();
    }

    @GetMapping("/getUniqueRelations")
    public Set<String> getUniqueRelations() {
        return graphService.getUniqueRelations();
    }

    @GetMapping("/getUniqueNodes")
    public Set<String> getUniqueNodes() {
        return graphService.getUniqueNode();
    }

    @PostMapping("/addRow")
    public ResponseEntity<Set<Row>> addRow(@RequestBody Row row) {
        graphService.addRow(row);
        return ResponseEntity.ok(graphService.getRows());
    }

    @PostMapping("/concentrateOnRelations")
    public ResponseEntity<Set<Row>> concentrateOnRelations(@RequestBody List<String> relations) {
        if (relations == null || relations.isEmpty()) {
            return ResponseEntity.ok(Collections.emptySet());
        }

        List<Row> filteredRows = graphService.concentrateByRelation(relations);
        return ResponseEntity.ok(new LinkedHashSet<>(filteredRows));
    }

    @PostMapping("/concentrateOnNodes")
    public ResponseEntity<Set<Row>> concentrateOnNodes(@RequestBody List<String> nodeNames) {
        if (nodeNames == null || nodeNames.isEmpty()) {
            return ResponseEntity.ok(Collections.emptySet());
        }

        List<Row> filteredRows = graphService.concentrateOnNodes(nodeNames);
        return ResponseEntity.ok(new LinkedHashSet<>(filteredRows));
    }
}
