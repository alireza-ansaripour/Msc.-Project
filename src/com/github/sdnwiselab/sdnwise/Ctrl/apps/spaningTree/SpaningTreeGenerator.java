package com.github.sdnwiselab.sdnwise.Ctrl.apps.spaningTree;

import com.github.sdnwiselab.sdnwise.Ctrl.Controller;
import com.github.sdnwiselab.sdnwise.Ctrl.interfaces.IDummyCtrlModule;
import com.github.sdnwiselab.sdnwise.Ctrl.interfaces.ITopoUpdateListener;
import com.github.sdnwiselab.sdnwise.Ctrl.services.topo.Topology;
import com.github.sdnwiselab.sdnwise.flowtable.FlowTableEntry;
import com.github.sdnwiselab.sdnwise.flowtable.ForwardUnicastAction;
import com.github.sdnwiselab.sdnwise.flowtable.Window;
import com.github.sdnwiselab.sdnwise.packet.OpenPathPacket;
import com.github.sdnwiselab.sdnwise.packet.ResponsePacket;
import com.github.sdnwiselab.sdnwise.util.NodeAddress;


import java.util.*;

public class SpaningTreeGenerator implements IDummyCtrlModule,ITopoUpdateListener {
    private Set<Integer> pathSets = new HashSet<>();
    Controller controller;
    private  HashMap<Integer, ArrayList<Integer>>[] list ;
    @Override
    public void startUp(Controller context) {
        context.addTopoChangeListener(this);
        controller = context;
    }




    @Override
    public void onTopoUpdate(Topology topology) {
        System.out.println("here");
        HashMap<Integer, ArrayList<Integer>>[] list = new HashMap[30];
        for (int id : topology.graph.keySet()) {
            if (pathSets.contains(id))
                continue;
            ArrayList<Integer> path = topology.getPathFromCtrl(id);
            for (int i = 0; i < path.size()-1; i++) {
                int n1 = path.get(i);
                int nxh;
                if(i != path.size())
                     nxh= path.get(i+1);
                else
                    nxh = n1;
                if(list[n1] == null)
                    list[n1] = new HashMap<>();
                ArrayList<Integer> dsts = list[n1].get(nxh);
                if(dsts == null)
                    dsts = new ArrayList<>();
                dsts.add(id);
                list[n1].put(nxh, dsts);
            }



            if (path.size() == 1)
                continue;
            System.out.println("path to " + id + " is : " + path);
            pathSets.add(id);
            List<Window> windows = new ArrayList<>();
            windows.add(Window.fromString("P.TYP == 4"));
            ArrayList<NodeAddress> pathNA = new ArrayList<>();
            for (Integer nId : path)
                pathNA.add(new NodeAddress(nId));
            OpenPathPacket pathPacket = new OpenPathPacket(1, new NodeAddress(1), new NodeAddress(id), pathNA);
            pathPacket.setNxh(new NodeAddress(path.get(1)));
            pathPacket.setWindows(windows);
//            controller.sendResponse(pathPacket);
        }
        for (int i = 1; i <list.length ; i++) {
            System.out.println(i + " : " + list[i]);
        }
        ArrayDeque<Node> nodes = new ArrayDeque<>();
        nodes.push(new Node(1, 1, 100));
        while (nodes.size() != 0){
            Node node = nodes.removeFirst();
            System.out.println("Node #" + node.id + "-" + node.start);
            HashMap<Integer, ArrayList<Integer>> data = list[node.id];
            if(data == null)
                continue;
            int pivot = node.start;
            for(int key: data.keySet()){
                Node child = new Node(key, pivot, pivot + data.get(key).size()-1);
                nodes.add(child);
                System.out.println("    " + key + ":" + pivot + "-" + (pivot + data.get(key).size()-1));
                pivot += data.get(key).size();
                if(node.id == 1)
                    continue;
                FlowTableEntry entry = createResponse(child);
                ResponsePacket responsePacket = new ResponsePacket(1, new NodeAddress(1), new NodeAddress(node.id), entry, (byte) node.start );
                responsePacket.setTunnelIndex((byte) node.start);
                int nextHop = topology.getPathFromCtrl(node.id).get(1);
                responsePacket.setNxh("0." + nextHop);
                this.controller.sendResponse(responsePacket);
            }
        }
    }
    private FlowTableEntry createResponse(Node node){
        FlowTableEntry entry = new FlowTableEntry();
        entry.addWindow(Window.fromString("P.TYP == 4"));
        entry.addWindow(Window.fromString("P.13 >= " + node.start));
        entry.addWindow(Window.fromString("P.13 <= " + node.end));
        entry.addAction(new ForwardUnicastAction(new NodeAddress(node.id)));
        return entry;
    }

}
