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


    private boolean containsLink(int src, int dst){
        Vertex v1 = graph.get(src);
        Vertex v2 = graph.get(dst);
        if (v2 == null || v1 == null)
            return false;
        return v1.adjacencies.contains(v2) && v2.adjacencies.contains(v1);
    }


    public void updateGraph(int nodeID, ArrayList<Integer> neibours)  {
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
            v.addAdj(adj);
        }

        for (int i = 0; i < v.adjacencies.size(); ) {
            Edge n = v.adjacencies.get(i);
            if(!neibours.contains(Integer.parseInt(n.target.name))){
                v.adjacencies.remove(n);
                Vertex toRemove = n.target;
                for(Edge e : toRemove.adjacencies)
                    unCheckedNodes.add(Integer.parseInt(e.target.name));
            }else
                i++;
        }
    }

    @Override
    public void receive(NetworkPacket packet) {
        if(packet.getTyp() == NetworkPacket.REPORT){
            handleReportPacket((ReportPacket) packet);
        }
    }


    private Set<Integer> checkedNodes = new HashSet<>();
    private Set<Integer> unCheckedNodes = new HashSet<>();
    private boolean updateFlag = false;

    private boolean checkUpdate(ArrayList<Integer> list1, ArrayList<Integer> list2){
        boolean flag = false;
        for (int i1 : list1)
            if (!list2.contains(i1))
                flag = true;

        for (int i2 : list2)
            if (!list1.contains(i2))
                flag = true;
        return flag;
    }
    private void handleReportPacket(ReportPacket reportPacket){
        NodeAddress src = reportPacket.getSrc();
        int srcID = src.intValue();
        checkedNodes.add(srcID);
        unCheckedNodes.remove(srcID);
        ArrayList<Integer> neighbours = new ArrayList<>();
        for (NodeAddress address: reportPacket.getNeighbors().keySet()){
            int addr = address.intValue();
            neighbours.add(addr);
            if (!checkedNodes.contains(addr)){
                unCheckedNodes.add(addr);
                updateFlag = true;
            }
        }
        Vertex vertex = graph.get(srcID) == null? new Vertex(""+srcID): graph.get(srcID);
        ArrayList<Integer> list = new ArrayList<>();
        for (Edge v : vertex.adjacencies){
            list.add(Integer.parseInt(v.target.name));
        }
        Boolean diff = checkUpdate(list, neighbours);
        if (diff)
            System.out.println("diff "+ srcID + " - " + diff+ " - " + neighbours + "-" + list);

        updateFlag = diff ? true : updateFlag;
        if(diff) {
            updateGraph(srcID, neighbours);
            System.out.println("update node " + srcID + "neighbours " + neighbours);
        }


        if(updateFlag && unCheckedNodes.size() == 0){
            ctrl.notifyTopologyChange(this);
            updateFlag = false;
        }

    }
    private Controller ctrl;
    @Override
    public void startUp(Controller controller) {
        controller.addPacketListener(this);
        ctrl = controller;
    }
}
