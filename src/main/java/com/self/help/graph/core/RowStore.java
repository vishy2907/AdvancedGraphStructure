package com.self.help.graph.core;

class RowStore {
    // Array index = Row ID
    // Values = Dict IDs from the respective Dictionaries
    private int[] fromIds;
    private int[] toIds;
    private int[] relationIds;

    private int currentRowCount = 0;

    public RowStore() {
        // Initializing with 64 elements as per our geometric expansion strategy
        this.fromIds = new int[64];
        this.toIds = new int[64];
        this.relationIds = new int[64];
    }

    public int addRow(int fromId, int toId, int relId) {
        int rowId = currentRowCount++;
        if (rowId >= fromIds.length) {
            expand(rowId);
        }

        fromIds[rowId] = fromId;
        toIds[rowId] = toId;
        relationIds[rowId] = relId;

        if (rowId >= currentRowCount) {
            currentRowCount = rowId + 1;
        }
        return rowId;
    }

    private void expand(int requiredIndex) {
        int newCapacity = fromIds.length;
        while (newCapacity <= requiredIndex) {
            newCapacity *= 2;
        }

        // Expand all three arrays to maintain alignment
        fromIds = grow(fromIds, newCapacity);
        toIds = grow(toIds, newCapacity);
        relationIds = grow(relationIds, newCapacity);
    }

    private int[] grow(int[] oldArray, int newCapacity) {
        int[] newArray = new int[newCapacity];
        System.arraycopy(oldArray, 0, newArray, 0, oldArray.length);
        return newArray;
    }

    // Getters for Hydration/UI
    public int getFromId(int rowId) { return fromIds[rowId]; }
    public int getToId(int rowId) { return toIds[rowId]; }
    public int getRelId(int rowId) { return relationIds[rowId]; }

    public int getRowCount() {
        return currentRowCount;
    }
}
