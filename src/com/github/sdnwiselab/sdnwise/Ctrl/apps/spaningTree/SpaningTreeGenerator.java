package com.github.sdnwiselab.sdnwise.Ctrl.apps.spaningTree;

import com.github.sdnwiselab.sdnwise.Ctrl.Controller;
import com.github.sdnwiselab.sdnwise.Ctrl.interfaces.IDummyCtrlModule;
import com.github.sdnwiselab.sdnwise.Ctrl.interfaces.ITopoUpdateListener;
import com.github.sdnwiselab.sdnwise.Ctrl.services.topo.Dijkstra;
import com.github.sdnwiselab.sdnwise.Ctrl.services.topo.Topology;
import com.github.sdnwiselab.sdnwise.Ctrl.services.topo.Vertex;
import com.github.sdnwiselab.sdnwise.flowtable.*;
import com.github.sdnwiselab.sdnwise.packet.ResponsePacket;
import com.github.sdnwiselab.sdnwise.util.NodeAddress;


import java.util.*;

public class SpaningTreeGenerator implements IDummyCtrlModule,ITopoUpdateListener {
    private Set<Integer> pathSets = new HashSet<>();
    HashMap<Integer, ArrayList<Integer>>[] nextHopList = new HashMap[30];
    HashMap<Integer, Node> nodes = new HashMap<>();
    private HashMap<Integer, ArrayList<Integer>>paths = new HashMap<>();
    Controller controller;

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

            // updates the routing table of other nodes
            updatePathNxtHop(path, nextHopList);

            pathSets.add(id);
            if (path.size() == 1)
                continue;

        }
    }


    private void assignTunnel(Topology topology){
        ArrayDeque<Node> nodes = new ArrayDeque<>();
        Node first = new Node(1, 1, 100);

        nodes.push(first);

        while (nodes.size() != 0){
            Node node = nodes.removeFirst();
            this.nodes.put(node.id, node);
            System.out.println("Node #" + node.id + "-" + node.start);
            HashMap<Integer, ArrayList<Integer>> data = nextHopList[node.id];
            if(data == null)
                continue;
            int pivot = node.start  +1;
            for(int key: data.keySet()){
                Node child = new Node(key, pivot, pivot + data.get(key).size()-1);
                node.addRange(key, new Range(child.start, child.end));
                nodes.add(child);
                System.out.println("    " + key + ":" + pivot + "-" + (pivot + data.get(key).size()-1));
                pivot += data.get(key).size();
                if(node.id == 1)
                    continue;

                // create packet for node
                FlowTableEntry entry = createResponse(child);
                ResponsePacket responsePacket = new ResponsePacket(1, new NodeAddress(1), new NodeAddress(node.id), entry, (byte) node.start );
                responsePacket.setTunnelIndex((byte) node.start);
                int nextHop = topology.getPathFromCtrl(node.id).get(1);
                responsePacket.setNxh("0." + nextHop);

                this.controller.sendResponse(responsePacket);
            }
        }
    }



    private void initSpanningTree(Topology topology){
        updateNxhopList(topology);

        for (int i = 1; i <nextHopList.length ; i++) {
            System.out.println(i + " : " + nextHopList[i]);
        }

        assignTunnel(topology);
    }

    boolean init = false;

    private void addBranch(Topology topology){
        for (int id : topology.graph.keySet()){
            if(pathSets.contains(id))
                continue;
            pathSets.add(id);
            ArrayList<Integer> path = topology.getPathFromCtrl(id);
            int lastNode = path.get(path.size()-2);
            System.out.println(paths.get(lastNode));
            int tunId = nodes.get(lastNode).addTunnel(id);
            System.out.println(tunId);
            nodes.put(id, new Node(id, tunId, tunId));
            for (int key : paths.get(lastNode)) {
                Node n = nodes.get(key);
                if(n == null)
                    continue;
                System.out.println(n.id + "-" + n.routingTable);
                if(n.id == lastNode)
                    continue;
                n.registerTunnel(tunId);
                System.out.println(n.id + "-" + n.routingTable);
                for (int k : n.routingTable.keySet()){
                    Node child = nodes.get(k);
                    Range range = n.routingTable.get(k);
                    if(n.id == 1)
                        continue;
                    FlowTableEntry entry = createResponse(child, range.offset);
                    ResponsePacket responsePacket = new ResponsePacket(1, new NodeAddress(1), new NodeAddress(n.id), entry, (byte) n.start );
                    responsePacket.setTtl((byte) Stats.SDN_WISE_RL_TTL_MAX);
                    responsePacket.setTunnelIndex((byte) n.start);
                    int nextHop = paths.get(n.id).get(1);
                    responsePacket.setNxh("0." + nextHop);
                    this.controller.sendResponse(responsePacket);
                }

            }
        }


    }

    @Override
    public void onTopoUpdate(Topology topology) {
        if (!init) {
            initSpanningTree(topology);
            init = true;
        }
        else{
            addBranch(topology);
        }
    }


    private FlowTableEntry createResponse(Node node){
        return createResponse(node, 0);
    }

    private FlowTableEntry createResponse(Node node, int offset){
        FlowTableEntry entry = new FlowTableEntry();
        entry.addWindow(Window.fromString("P.TYP == 4"));
        entry.addWindow(Window.fromString("P.13 >= " + node.start));
        entry.addWindow(Window.fromString("P.13 <= " + node.end));
        entry.addAction(new ForwardUnicastAction(new NodeAddress(node.id)));
        entry.addAction(new SetAction("SET P.13 = P.13 - " + offset));
        entry.getStats().setPermanent();
        System.out.println(entry.getStats());
        return entry;
    }

}
