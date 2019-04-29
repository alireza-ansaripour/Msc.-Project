package Ctrl.apps.spaningTree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

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
        return tunnel;
    }

    public static HashMap<Integer, Set<Integer>> getSpanningTree(){
        HashMap<Integer, Set<Integer>> tree = new HashMap<>();
        for (Node node : spaningTreeGenerator.nodes.values()){
            System.out.println(node.id + ":" + node.routingTable);
            tree.put(node.id, node.routingTable.keySet());
        }
        return tree;
    }

    public static void printTun(){
        for (Node node : spaningTreeGenerator.nodes.values()){
            System.out.println(node.id + ":" + node.routingTable);
        }
    }

    public static ArrayList<Integer> getpath(int nodeID){
        return spaningTreeGenerator.getPaths().get(nodeID);
    }
}
