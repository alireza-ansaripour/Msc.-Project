package com.github.sdnwiselab.sdnwise.Ctrl.services.topo;


import com.github.sdnwiselab.sdnwise.Ctrl.Controller;
import com.github.sdnwiselab.sdnwise.Ctrl.apps.spaningTree.SpanningTreeService;
import com.github.sdnwiselab.sdnwise.Ctrl.interfaces.IDummyCtrlModule;
import com.github.sdnwiselab.sdnwise.Ctrl.interfaces.IPacketListener;
import com.github.sdnwiselab.sdnwise.flowtable.FlowTableEntry;
import com.github.sdnwiselab.sdnwise.flowtable.Window;
import com.github.sdnwiselab.sdnwise.packet.*;
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
            for (Vertex v : graph.values()){
                v.minDistance = Double.POSITIVE_INFINITY;
                v.previous = null;
            }
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
        switch (packet.getTyp()){
            case NetworkPacket.REPORT:
                handleReportPacket((ReportPacket) packet);
                break;
            case NetworkPacket.REQUEST:
                handleRequestPacket((RequestPacket) packet);
                break;
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

    private HashMap<Integer, ArrayList<Integer>> nodeStatus = new HashMap<>();

    private boolean checkArrayEquality(ArrayList<Integer> arr1, ArrayList<Integer> arr2){
        boolean equal = true;
        for (Integer i1: arr1) {
            if (!arr2.contains(i1))
                equal = false;
        }
        for (Integer i2:arr2){
            if(!arr1.contains(i2))
                equal = false;
        }
        return equal;
    }


    private void handleReponseTYP1(RequestPacket requestPacket){
        DataPacket dataPacket = new DataPacket(requestPacket.getData());
        Vertex vertex = graph.get(requestPacket.getSrc().intValue());
        int neibour = dataPacket.getData()[0];
        int address = requestPacket.getSrc().intValue();
        if(vertex == null){
            vertex = new Vertex(""+ address);
            graph.put(address, vertex);
        }

        Vertex srcNode = graph.get(neibour);
        srcNode.addAdj(vertex);
        vertex.addAdj(srcNode);
        System.out.println("have to handle "+ vertex);
        System.out.println(graph);
        ctrl.notifyTopologyChange(this);
        ArrayList<Integer> path = TopologyService.getPath(1, address);
        System.out.println("the path is " + path);
        int nxHop = path.get(1);
        int tunID = SpanningTreeService.getTunnelID(address);
        System.out.println(tunID);
        ResponsePacket responsePacket = new ResponsePacket(1,new NodeAddress(1),new NodeAddress(0), new FlowTableEntry(), (byte) tunID);
        responsePacket.setNxh(new NodeAddress(nxHop));
        ctrl.sendResponse(responsePacket);
    }

    private void handleRequestPacket(RequestPacket requestPacket){
        NetworkPacket networkPacket = new NetworkPacket(requestPacket.getData());
        System.out.println("the request packet is " + requestPacket);
        System.out.println("the network packet is " + networkPacket);
        System.out.println("the packet type " + networkPacket.getTyp());
        if (networkPacket.getDst().intValue() == 0){
            switch (networkPacket.getTyp()){
                case NetworkPacket.DATA:
                    handleReponseTYP1(requestPacket);
                    break;
                case NetworkPacket.RESPONSE:
                    handleResponseTYP2(requestPacket);
                    break;
            }
        }

    }

    private void handleResponseTYP2(RequestPacket requestPacket) {

    }

    boolean flag = false;
    private void handleReportPacket(ReportPacket reportPacket){
        if (!flag) {
            NodeAddress src = reportPacket.getSrc();
            int srcID = src.intValue();
            Vertex vertex = new Vertex(Integer.toString(srcID));
            graph.put(srcID, vertex);
            for (NodeAddress address: reportPacket.getNeighbors().keySet()){
                int addr = address.intValue();
                if(graph.containsKey(addr)){
                    vertex.addAdj(graph.get(addr));
                }

                DataPacket dataPacket = new DataPacket(1, new NodeAddress(1), new NodeAddress(0), new byte[]{1});
                dataPacket.setNxh(address);
                ctrl.sendResponse(dataPacket);
            }
            ctrl.notifyTopologyChange(this);
        }
        flag = true;


//        if(!checkedNodes.contains(srcID))
//            System.out.println("report packet for src " + srcID);
//        checkedNodes.add(srcID);
//        unCheckedNodes.remove(srcID);
//
//
//
//        ArrayList<Integer> neighbours = new ArrayList<>();
//        for (NodeAddress address: reportPacket.getNeighbors().keySet()){
//            int addr = address.intValue();
//            neighbours.add(addr);
//            if (!checkedNodes.contains(addr)){
//                unCheckedNodes.add(addr);
//                updateFlag = true;
//            }
//        }
//        Vertex vertex = graph.get(srcID) == null? new Vertex(""+srcID): graph.get(srcID);
//        ArrayList<Integer> list = new ArrayList<>();
//        for (Edge v : vertex.adjacencies){
//            list.add(Integer.parseInt(v.target.name));
//        }
//        Boolean diff = checkUpdate(list, neighbours);
//
//        updateFlag = diff ? true : updateFlag;
//        if(diff) {
//            updateGraph(srcID, neighbours);
//        }
//
//
//        if(updateFlag && unCheckedNodes.size() == 0){
//            ctrl.notifyTopologyChange(this);
//            updateFlag = false;
//        }

    }
    private Controller ctrl;
    @Override
    public void startUp(Controller controller) {
        controller.addPacketListener(this);
        ctrl = controller;
        TopologyService.setTopo(this);
    }
}
