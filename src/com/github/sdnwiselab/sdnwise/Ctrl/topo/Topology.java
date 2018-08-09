package com.github.sdnwiselab.sdnwise.Ctrl.topo;


import java.util.*;

public class Topology {
    public HashMap<Integer, Vertex> graph = new HashMap<>();

    public String printGraph(){
        String out = "";
        for (int node : graph.keySet()){
            out += "Node: " + node + " : ";
            for (Edge edge : graph.get(node).adjacencies){
                out += edge.target + ", ";
            }
            out += "\n";
        }
        return out;
    }

    public Topology(){

    }

    public ArrayList<Integer> findPath(int src, int dst){
        try {
            for (Vertex v : graph.values())
                v.minDistance = Double.POSITIVE_INFINITY;
            Dijkstra.computePaths(graph.get(src));
            List<Vertex> path = Dijkstra.getShortestPathTo(graph.get(dst));
            ArrayList<Integer> p = new ArrayList<>();
            for (Vertex vertex : path) {
                p.add(Integer.parseInt(vertex.name));
            }
            return p;
        }catch (Exception e){
            System.out.println("exp: " + e.getMessage());
            return null;
        }
    }


    public ArrayList<Integer> getPathFromCtrl(int nodeId){
        ArrayList<Integer> p = findPath(1, nodeId);
//        Collections.reverse(p);
        return p;
    }

    public boolean isThereAPathToCtrl(ArrayList<Integer> neighbours){
        for (int node : neighbours){
            if ( graph.keySet().contains(node))
                return true;
        }
        return false;
    }

    public void addNode (int nodeID, ArrayList<Integer> neibours)  {
        Vertex v = graph.get(nodeID);
        if(v == null){
            v = new Vertex("" + nodeID);
            graph.put(nodeID, v);
        }
        for (Integer neibour : neibours) {
            Vertex adj = graph.get(neibour);
            if (adj == null) {
                adj = new Vertex("" + neibour);
                graph.put(neibour, adj);
            }
            adj.addAdj(v);
            v.addAdj(adj);
        }

        System.out.println(graph);
        System.out.println("new node added ");
    }

}
