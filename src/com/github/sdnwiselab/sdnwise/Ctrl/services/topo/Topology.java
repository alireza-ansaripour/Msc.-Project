package com.github.sdnwiselab.sdnwise.Ctrl.services.topo;


import com.github.sdnwiselab.sdnwise.Ctrl.Controller;
import com.github.sdnwiselab.sdnwise.Ctrl.apps.spaningTree.SpanningTreeService;
import com.github.sdnwiselab.sdnwise.Ctrl.interfaces.IDummyCtrlModule;
import com.github.sdnwiselab.sdnwise.Ctrl.interfaces.IPacketListener;
import com.github.sdnwiselab.sdnwise.flowtable.*;
import com.github.sdnwiselab.sdnwise.packet.*;
import com.github.sdnwiselab.sdnwise.util.NodeAddress;

import java.util.*;
import org.contikios.cooja.*;
import org.contikios.cooja.interfaces.*;
import org.contikios.cooja.motes.AbstractApplicationMote;


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


    private void addNewNode(RequestPacket requestPacket){
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
        System.out.println(graph);

        ctrl.notifyNodeAdd(vertex);
        ArrayList<Integer> path = TopologyService.getPath(1, address);
        int nxHop = path.get(1);
        int tunID = SpanningTreeService.getTunnelID(address);
        System.out.println(tunID);
        FlowTableEntry entry = new FlowTableEntry();
        int src = 0;
        int dst = 0;

        entry.addWindow(Window.fromString("P.SRC == " + src));
        entry.addWindow(Window.fromString("P.DST == " + dst));
        entry.addWindow(Window.fromString("P.13 == " + neibour));
        entry.addAction(new SetAction("SET P.13 == " + address));
        entry.addAction(new ForwardBroadcastAction());
        ResponsePacket responsePacket = new ResponsePacket(1,new NodeAddress(1),new NodeAddress(address), entry, (byte) tunID);
        responsePacket.setNxh(new NodeAddress(nxHop));
        System.out.println("broadcast " + address + "-" + tunID);
        System.out.println("packet: " + responsePacket);
        ctrl.sendResponse(responsePacket);
    }

    ArrayList<Integer>addedNodes = new ArrayList<>();

    private void handleRequestPacket(RequestPacket requestPacket){

        if (requestPacket.getSrc().intValue() == 1)
            return;

        NetworkPacket networkPacket = new NetworkPacket(requestPacket.getData());
        int addr = requestPacket.getSrc().intValue();
        if (networkPacket.getDst().intValue() == 0 && networkPacket.getTyp() == NetworkPacket.DATA){
            if(!graph.keySet().contains(addr)){
                System.out.println("new Node " + addr);

                addNewNode(requestPacket);
            }else{
                checkUpdate(requestPacket);
            }
        }

    }

    private void checkUpdate(RequestPacket requestPacket) {
        DataPacket dataPacket = new DataPacket(requestPacket.getData());
        int firstNodeAddr = requestPacket.getSrc().intValue();
        int secondNodeAddr = dataPacket.getData()[0];
        if (!addedNodes.contains(secondNodeAddr)){
            addedNodes.add(secondNodeAddr);
        }
        Vertex firstNode = graph.get(firstNodeAddr);
        Vertex secondeNode = graph.get(secondNodeAddr);
        if(firstNode.isAdj(secondeNode) && secondeNode.isAdj(firstNode))
            return;
        if (!firstNode.isAdj(secondeNode)){
            firstNode.addAdj(secondeNode);
        }
        if(secondeNode.isAdj(firstNode)){
            secondeNode.addAdj(firstNode);
        }
    }

    boolean flag = false;
    Simulation simulation = null;
    HashMap <Integer, ArrayList<Integer>> topo = new HashMap<>();
    private void handleReportPacket(ReportPacket reportPacket){
        NodeAddress src = reportPacket.getSrc();
        ArrayList<Integer> nodes = new ArrayList<>();
        for (NodeAddress address: reportPacket.getNeighbors().keySet())
            nodes.add(address.intValue());
        topo.put(src.intValue(), nodes);
        System.out.println(topo);
//        int srcID = src.intValue();
//        if (!flag) {
//            Vertex vertex = new Vertex(Integer.toString(srcID));
//            graph.put(srcID, vertex);
//            simulation = ctrl.getSimulation();
//            for (NodeAddress address: reportPacket.getNeighbors().keySet()){
//                int addr = address.intValue();
//                if(graph.containsKey(addr)){
//                    vertex.addAdj(graph.get(addr));
//                }
//            }
//
//            DataPacket dataPacket = new DataPacket(1, new NodeAddress(0), new NodeAddress(0), new byte[]{1});
//            dataPacket.setNxh(NodeAddress.BROADCAST_ADDR);
//            ctrl.sendResponse(dataPacket);
//            sendPacket(dataPacket);
//
//            ctrl.notifyNodeAdd(vertex);
//        }
//        flag = true;
    }

    private void sendPacket(NetworkPacket packet){
        System.out.println("sending ");
        simulation.scheduleEvent(
                new MoteTimeEvent((AbstractApplicationMote)ctrl.getSink(), 0) {
                    @Override
                    public void execute(long t) {
                        ctrl.sendResponse(packet);
                        sendPacket(packet);
                    }
                }, simulation.getSimulationTime() + (3000) * Simulation.MILLISECOND
        );

    }

    private Controller ctrl;
    @Override
    public void startUp(Controller controller) {
        controller.addPacketListener(this);
        ctrl = controller;
        TopologyService.setTopo(this);
    }
}
