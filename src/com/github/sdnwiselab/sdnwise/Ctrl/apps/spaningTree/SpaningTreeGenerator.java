package com.github.sdnwiselab.sdnwise.Ctrl.apps.spaningTree;

import com.github.sdnwiselab.sdnwise.Ctrl.Controller;
import com.github.sdnwiselab.sdnwise.Ctrl.apps.spaningTree.ruleManager.FlowTableManager;
import com.github.sdnwiselab.sdnwise.Ctrl.interfaces.IDummyCtrlModule;
import com.github.sdnwiselab.sdnwise.Ctrl.interfaces.ITopoUpdateListener;
import com.github.sdnwiselab.sdnwise.Ctrl.services.topo.Topology;
import com.github.sdnwiselab.sdnwise.Ctrl.services.topo.TopologyService;
import com.github.sdnwiselab.sdnwise.Ctrl.services.topo.Vertex;
import com.github.sdnwiselab.sdnwise.flowtable.*;
import com.github.sdnwiselab.sdnwise.packet.ResponsePacket;
import com.github.sdnwiselab.sdnwise.util.NodeAddress;


import java.util.*;

public class SpaningTreeGenerator implements IDummyCtrlModule,ITopoUpdateListener {
    private Set<Integer> pathSets = new HashSet<>();
    HashMap<Integer, ArrayList<Integer>>[] nextHopList = new HashMap[100];
    HashMap<Integer, Node> nodes = new HashMap<>();
    private HashMap<Integer, ArrayList<Integer>>paths = new HashMap<>();
    private HashMap<Integer, Byte> tunnels = new HashMap<>();
    Controller controller;

    public HashMap<Integer, ArrayList<Integer>> getPaths() {
        return paths;
    }

    public HashMap<Integer, Byte> getTunnels() {
        return tunnels;
    }

    @Override
    public void startUp(Controller context) {
        context.addTopoChangeListener(this);
        controller = context;
        SpanningTreeService.setSpaningTreeGenerator(this);
    }

    public HashMap<Integer, Node> getNodes() {
        return nodes;
    }

    private void updatePathNxtHop(ArrayList<Integer> path, HashMap<Integer, ArrayList<Integer>>[] nextHopList){
        int id = path.get(path.size()-1);
        for (int i = 0; i < path.size()-1; i++) {
            int n1 = path.get(i);
            int nxh;
            if(i != path.size())
                nxh= path.get(i+1);
            else
                nxh = n1;
            if(nextHopList[n1] == null)
                nextHopList[n1] = new HashMap<>();
            ArrayList<Integer> dsts = nextHopList[n1].get(nxh);
            if(dsts == null)
                dsts = new ArrayList<>();
            dsts.add(id);
            nextHopList[n1].put(nxh, dsts);
        }
    }

    private void updateNxhopList(Topology topology){

        for (int id : topology.graph.keySet()) {
            // if there is a path to node then continue
            if (pathSets.contains(id))
                continue;

            // else setup a path from node to ctrl
            ArrayList<Integer> path = topology.getPathFromCtrl(id);
            paths.put(id, path);
            System.out.println(id + " the path: " + path);

            // updates the routing table of other nodes
            updatePathNxtHop(path, nextHopList);

            pathSets.add(id);
            if (path.size() == 1)
                continue;

        }
    }




    boolean init = false;

    @Override
    public void onTopoUpdate(Topology topology) {

    }

    public void addNewNode(int srcId){
        ArrayList<Integer> path = TopologyService.getPath(1, srcId);
        paths.put(srcId, path);
        if(path.size() == 1){
            nodes.put(srcId, new Node(srcId, 1, 1));
            return;
        }
        int lastNode = path.get(path.size()-2);
        int tunId = nodes.get(lastNode).addTunnel(srcId);
        System.out.println("last node " + lastNode + "-" + tunId);
        nodes.put(srcId, new Node(srcId, tunId, tunId));
        path = new ArrayList<>();
        for(int i = paths.get(lastNode).size()-1;i>= 0; i--){
            path.add(paths.get(lastNode).get(i));
        }
        Node previous = null;
        for (int key : path) {
            Node n = nodes.get(key);
            if(n == null)
                continue;
            System.out.println(n.id + "_" + n.routingTable);

            if(n.id != lastNode)
                tunId = n.registerTunnel(previous, tunId);
            previous = n;
            System.out.println(n.id + "_" + n.routingTable);
            for (int k : n.routingTable.keySet()){
                Node child = nodes.get(k);
                Range range = n.routingTable.get(k);

                FlowTableEntry entry = createResponse(k, range);
                int ruleID = -1;
                if(n.ruleMap.get(k) == null){
                    ruleID = FlowTableManager.addRule(n.id, entry);
                }else {
                    FlowTableEntry removeEntry = FlowTableManager.getRule(n.id, n.ruleMap.get(k));
                    ruleID = FlowTableManager.replaceRule(n.id, entry, removeEntry);
                }
                n.ruleMap.put(k, ruleID);
            }

        }

        for (int node: tunnels.keySet()) {
            if (tunnels.get(node) >= tunId) {
                tunnels.put(node,(byte)(tunnels.get(node) + 1));
            }
        }
        tunnels.put(srcId, (byte) tunId);
        System.out.println(tunnels);
    }

    @Override
    public void onNodeAdd(Vertex node) {
        int id = Integer.parseInt(node.name);
        System.out.println("the added node is " + id);
        addNewNode(id);
    }

    @Override
    public void onNodeRemove(Vertex node) {

    }




    private FlowTableEntry createResponse(int nodeID, Range range){
        FlowTableEntry entry = new FlowTableEntry();
        entry.addWindow(Window.fromString("P.TYP == 4"));
        entry.addWindow(Window.fromString("P.13 >= " + range.start));
        entry.addWindow(Window.fromString("P.13 <= " + range.end));
        entry.addAction(new ForwardUnicastAction(new NodeAddress(nodeID)));
        entry.addAction(new SetAction("SET P.13 = P.13 - " + range.offset));
        entry.getStats().setPermanent();
        return entry;
    }

}
