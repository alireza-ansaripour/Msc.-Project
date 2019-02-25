package com.github.sdnwiselab.sdnwise.Ctrl.apps.spaningTree;

public class SpanningTreeService {
    private static SpaningTreeGenerator spaningTreeGenerator = null;

    public static void setSpaningTreeGenerator(SpaningTreeGenerator spg){
        if(spaningTreeGenerator == null)
            spaningTreeGenerator = spg;
    }

    public static int getTunnelID(int n){
        Node node = spaningTreeGenerator.getNodes().get(n);
        return node.start;
    }

}
