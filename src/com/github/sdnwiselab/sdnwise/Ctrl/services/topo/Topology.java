package com.github.sdnwiselab.sdnwise.Ctrl.services.topo;


import com.github.sdnwiselab.sdnwise.Ctrl.Controller;
import com.github.sdnwiselab.sdnwise.Ctrl.interfaces.IDummyCtrlModule;
import com.github.sdnwiselab.sdnwise.Ctrl.interfaces.IPacketListener;
import com.github.sdnwiselab.sdnwise.flowtable.Window;
import com.github.sdnwiselab.sdnwise.packet.NetworkPacket;
import com.github.sdnwiselab.sdnwise.packet.OpenPathPacket;
import com.github.sdnwiselab.sdnwise.packet.ReportPacket;
import com.github.sdnwiselab.sdnwise.util.NodeAddress;

import java.util.*;

public class Topology implements IPacketListener, IDummyCtrlModule {
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

    @Override
    public String toString() {
        return printGraph();
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
        System.out.println("new node added ");
    }

    @Override
    public void receive(NetworkPacket packet) {
        if(packet.getTyp() == NetworkPacket.REPORT){
            updateGraph((ReportPacket) packet);
        }
    }

    private void updateGraph(ReportPacket reportPacket){
        NodeAddress src = reportPacket.getSrc();
        int srcID = src.intValue();
        ArrayList<Integer> neiburs = new ArrayList<>();
        for (NodeAddress address: reportPacket.getNeighbors().keySet()){
            neiburs.add(address.intValue());
        }
        addNode(srcID, neiburs);
        if(graph.size() == 30 && !flag){
            flag = true;
            System.out.println(printGraph());
            System.out.println("its show time");
            for(int id : graph.keySet()){
                ArrayList<Integer> path = getPathFromCtrl(id);
                if(path.size() == 1)
                    continue;
                System.out.println("path to " + id + " is : "+ path);
                List<Window> windows = new ArrayList<>();
                windows.add(Window.fromString("P.TYP == 4"));
                ArrayList<NodeAddress> pathNA = new ArrayList<>();
                for(Integer nId : path)
                    pathNA.add(new NodeAddress(nId));
                OpenPathPacket pathPacket = new OpenPathPacket(1,new NodeAddress(1), new NodeAddress(srcID),pathNA);
                pathPacket.setNxh(new NodeAddress(path.get(1)));
                pathPacket.setWindows(windows);
                ctrl.sendResponse(pathPacket);
            }
        }
    }
    private boolean flag = false;
    private Controller ctrl;
    @Override
    public void startUp(Controller controller) {
        controller.addPacketListener(this);
        ctrl = controller;
    }
}
