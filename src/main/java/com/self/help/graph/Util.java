package com.self.help.graph;

public class Util {

    /*
    // --- Phase 1: Pre-computed DataCube Primitive Indices ---
    private final int idCubeIndex;
    private final int labelCubeIndex;
    private final int[] attrCubeIndices;
    private final int[] relationCubeIndices;

    // --- Phase 2: Deterministic Array Offsets ---
    private final int idDictOffset;
    private final int labelDictOffset;
    private final int attrDictBaseOffset;
    private final int relDictBaseOffset;

    // --- Phase 3 & 4: Dictionaries and Buffers ---
    private final BiDictionary[] dictionaryRegistry;
    private final int[] attrBuffer;
    private final int[] relationBuffer;

    GraphIngestionEngine(DataCube dataCube, GraphMappingSpec spec) {

        // ==========================================
        // 1. FAIL-FAST VALIDATION GATE
        // ==========================================
        validateSpec(dataCube, spec);

        // ==========================================
        // 2. DATACUBE RESOLUTION & ALIASING
        // ==========================================
        NodeSpec fromSpec = spec.getFromNodeSpec();
        NodeSpec toSpec = spec.getToNodeSpec();
        boolean idIsNull = fromSpec.getIdColumnName() == null;

        this.labelCubeIndex = dataCube.getColumnIndex(fromSpec.getLabelColumnName());

        // POINTER ALIASING
        this.idCubeIndex = idIsNull ? this.labelCubeIndex : dataCube.getColumnIndex(fromSpec.getIdColumnName());

        List<String> attrs = fromSpec.getNodeAttributeNames() == null ? Collections.emptyList() : fromSpec.getNodeAttributeNames();
        int attrSize = attrs.size();

        this.attrCubeIndices = new int[attrSize];
        for (int i = 0; i < attrSize; i++) {
            this.attrCubeIndices[i] = dataCube.getColumnIndex(attrs.get(i));
        }

        List<String> relations = spec.getRelationColumnNames();
        int relSize = relations.size();

        this.relationCubeIndices = new int[relSize];
        for (int i = 0; i < relSize; i++) {
            this.relationCubeIndices[i] = dataCube.getColumnIndex(relations.get(i));
        }

        // ==========================================
        // 3. DETERMINISTIC OFFSETS & ALLOCATION
        // ==========================================
        int currentOffset = 0;

        if (!idIsNull) {
            this.idDictOffset = currentOffset++;
            this.labelDictOffset = currentOffset++;
        } else {
            // POINTER ALIASING
            this.labelDictOffset = currentOffset++;
            this.idDictOffset = this.labelDictOffset;
        }

        this.attrDictBaseOffset = currentOffset;
        currentOffset += attrSize;

        this.relDictBaseOffset = currentOffset;
        currentOffset += relSize;

        this.dictionaryRegistry = new BiDictionary[currentOffset];
        for (int i = 0; i < currentOffset; i++) {
            this.dictionaryRegistry[i] = new BiDictionaryImpl();
        }

        this.attrBuffer = new int[attrSize];
        this.relationBuffer = new int[relSize];
    }

    // ==========================================
    // ISOLATED VALIDATION ENGINE
    // ==========================================

    /**
     * Completely validates the mapping spec against logical rules and physical DataCube presence.
     * Throws an IllegalArgumentException immediately upon any violation.
     * /
    private static void validateSpec(DataCube dataCube, GraphMappingSpec spec) {
        NodeSpec fromSpec = spec.getFromNodeSpec();
        NodeSpec toSpec = spec.getToNodeSpec();

        // 1. Validate ID Parity
        boolean idIsNull = fromSpec.getIdColumnName() == null;
        if (idIsNull != (toSpec.getIdColumnName() == null)) {
            throw new IllegalArgumentException("ID Column parity mismatch: idColumnName must be null in both or provided in both.");
        }

        // 2. Validate Attribute Size Parity
        List<String> fromAttrs = fromSpec.getNodeAttributeNames() == null ? Collections.emptyList() : fromSpec.getNodeAttributeNames();
        List<String> toAttrs = toSpec.getNodeAttributeNames() == null ? Collections.emptyList() : toSpec.getNodeAttributeNames();

        if (fromAttrs.size() != toAttrs.size()) {
            throw new IllegalArgumentException("Attribute size mismatch: fromNodeSpec has " + fromAttrs.size() + " attributes, but toNodeSpec has " + toAttrs.size());
        }

        // 3. Validate Intra-node Overlap (From)
        Set<String> fromCore = new HashSet<>();
        if (!idIsNull) fromCore.add(fromSpec.getIdColumnName());
        fromCore.add(fromSpec.getLabelColumnName());

        Set<String> fromAttrSet = new HashSet<>(fromAttrs);
        Set<String> fromIntraOverlap = new HashSet<>(fromCore);
        fromIntraOverlap.retainAll(fromAttrSet);
        if (!fromIntraOverlap.isEmpty()) {
            throw new IllegalArgumentException("Intra-node violation in fromNodeSpec. Overlap: " + fromIntraOverlap);
        }

        // 4. Validate Intra-node Overlap (To)
        Set<String> toCore = new HashSet<>();
        if (!idIsNull) toCore.add(toSpec.getIdColumnName());
        toCore.add(toSpec.getLabelColumnName());

        Set<String> toAttrSet = new HashSet<>(toAttrs);
        Set<String> toIntraOverlap = new HashSet<>(toCore);
        toIntraOverlap.retainAll(toAttrSet);
        if (!toIntraOverlap.isEmpty()) {
            throw new IllegalArgumentException("Intra-node violation in toNodeSpec. Overlap: " + toIntraOverlap);
        }

        // 5. Validate Cross-Node Disjoint
        Set<String> fromAllCols = new HashSet<>(fromCore);
        fromAllCols.addAll(fromAttrSet);
        Set<String> toAllCols = new HashSet<>(toCore);
        toAllCols.addAll(toAttrSet);

        Set<String> crossOverlap = new HashSet<>(fromAllCols);
        crossOverlap.retainAll(toAllCols);
        if (!crossOverlap.isEmpty()) {
            throw new IllegalArgumentException("Disjoint violation: fromNodeSpec and toNodeSpec share columns: " + crossOverlap);
        }

        // 6. Validate DataCube Presence
        // We verify every single extracted column actually exists in the DataCube
        verifyColumnExists(dataCube, fromSpec.getLabelColumnName());
        verifyColumnExists(dataCube, toSpec.getLabelColumnName());
        if (!idIsNull) {
            verifyColumnExists(dataCube, fromSpec.getIdColumnName());
            verifyColumnExists(dataCube, toSpec.getIdColumnName());
        }
        for (String attr : fromAttrs) verifyColumnExists(dataCube, attr);
        for (String attr : toAttrs) verifyColumnExists(dataCube, attr);
        for (String rel : spec.getRelationColumnNames()) verifyColumnExists(dataCube, rel);
    }

    private static void verifyColumnExists(DataCube dataCube, String columnName) {
        if (columnName == null || columnName.trim().isEmpty() || dataCube.getColumnIndex(columnName) == -1) {
            throw new IllegalArgumentException("Mapping violation: Column '" + columnName + "' is missing or does not exist in the DataCube.");
        }
    }
    */

    // ... (The synchronized ingest method remains identical and ultra-fast) ...
}