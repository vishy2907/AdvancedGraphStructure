package com.self.help.graph;

import com.self.help.graph.core.Row;

import java.util.List;

public class StaticDataLoader {

    public static List<Row> getStaticData() {
        Row row1 = new Row("Nasik", "Pune", "byRoad");
        Row row2 = new Row("Mumbai", "Pune", "byRoad");
        Row row3 = new Row("Pune", "Solapur", "byRoad");
        Row row4 = new Row("Solapur", "Vijapur", "byRoad");
        Row row5 = new Row("Solapur", "Vijapur", "byRail");


        return List.of(row1, row2, row3, row4, row5);
    }
}
