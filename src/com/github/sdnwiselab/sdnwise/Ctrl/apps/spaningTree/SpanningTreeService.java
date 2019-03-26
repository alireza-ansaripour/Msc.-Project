package com.github.sdnwiselab.sdnwise.Ctrl.apps.spaningTree;

import java.util.ArrayList;

public class SpanningTreeService {
    private static SpaningTreeGenerator spaningTreeGenerator = null;

    public static void setSpaningTreeGenerator(SpaningTreeGenerator spg){
        if(spaningTreeGenerator == null)
            spaningTreeGenerator = spg;
    }

    public static int getTunnelID(int n){
        if(n == 1)
            return 1;
        int tunnel = spaningTreeGenerator.getTunnels().get(n);
        Node node = spaningTreeGenerator.nodes.get(1);
        int toSend = 0;
        for (Range range: node.routingTable.values()){
            if (range.start <= tunnel && range.end >= tunnel){
                toSend = tunnel-range.offset;
            }
        }
        return toSend;
    }

    public static ArrayList<Integer> getpath(int nodeID){
        return spaningTreeGenerator.getPaths().get(nodeID);
    }
}
