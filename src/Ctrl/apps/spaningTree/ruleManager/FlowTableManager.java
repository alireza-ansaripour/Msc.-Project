package Ctrl.apps.spaningTree.ruleManager;

import Ctrl.Controller;
import Ctrl.apps.spaningTree.SpanningTreeService;
import com.github.sdnwiselab.sdnwise.flowtable.FlowTableEntry;
import com.github.sdnwiselab.sdnwise.flowtable.Stats;
import com.github.sdnwiselab.sdnwise.flowtable.Window;
import com.github.sdnwiselab.sdnwise.packet.ResponsePacket;
import com.github.sdnwiselab.sdnwise.util.NodeAddress;

import javax.naming.ldap.Control;
import java.util.ArrayList;
import java.util.HashMap;

public class FlowTableManager {
    private FlowTableManager(){}

    private static FlowTableManager instance = new FlowTableManager();
    private HashMap<Integer, ArrayList<FlowTableEntry>> nodesRules = new HashMap<>();
    private static int counter = 0;

    public static int addRule(int node, FlowTableEntry entry){
        ArrayList<FlowTableEntry> flowTable = instance.nodesRules.computeIfAbsent(node, k -> new ArrayList<>());
        int id = counter;
        counter ++;
        entry.setId(id);
        flowTable.add(entry);
        sendPacket(node, entry);
        return id;
    }


    private static void sendPacket(int node, FlowTableEntry entry){
        int tunID  = SpanningTreeService.getTunnelID(node);
        ResponsePacket responsePacket = new ResponsePacket(1, new NodeAddress(1), new NodeAddress(node), entry, (byte) tunID);
        responsePacket.setTtl((byte) Stats.SDN_WISE_RL_TTL_MAX);
        int nextHop;
        if(node != 1)
            nextHop = SpanningTreeService.getpath(node).get(1);
        else
            nextHop = 1;
        responsePacket.setNxh("0." + 1);
        Controller.getInstance().sendResponse(responsePacket);
    }

    public static ArrayList<FlowTableEntry> getFlowTable(int node){
        return instance.nodesRules.get(node);
    }

    public static void removeRule(int node, FlowTableEntry entry){
        instance.nodesRules.get(node).remove(entry);
        FlowTableEntry toSend = new FlowTableEntry();
        for (Window w: entry.getWindows()) {
            toSend.addWindow(w);
        }
        sendPacket(node, toSend);
    }

    public static FlowTableEntry getRule(int node, int id){
        FlowTableEntry ent = null;
        for(FlowTableEntry entry : instance.nodesRules.get(node)){
            if (entry.getId() == id)
                ent = entry;
        }
        return ent;
    }

    public static void removeRule(int node, int id){
        FlowTableEntry ent = getRule(node, id);
        if(ent != null){
            removeRule(node, ent);
        }
    }

    public static int replaceRule(int node, FlowTableEntry toInsert, FlowTableEntry toExit){
        removeRule(node, toExit);
        return addRule(node, toInsert);
    }


}
