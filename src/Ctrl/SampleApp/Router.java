package Ctrl.SampleApp;
import Ctrl.Controller;
import Ctrl.apps.spaningTree.SpanningTreeService;
import Ctrl.interfaces.IDummyCtrlModule;
import Ctrl.interfaces.IPacketListener;
import Ctrl.services.topo.TopologyService;
import com.github.sdnwiselab.sdnwise.flowtable.FlowTableEntry;
import com.github.sdnwiselab.sdnwise.flowtable.ForwardUnicastAction;
import com.github.sdnwiselab.sdnwise.flowtable.Window;
import com.github.sdnwiselab.sdnwise.packet.DataPacket;
import com.github.sdnwiselab.sdnwise.packet.NetworkPacket;
import com.github.sdnwiselab.sdnwise.packet.RequestPacket;
import com.github.sdnwiselab.sdnwise.packet.ResponsePacket;
import com.github.sdnwiselab.sdnwise.util.NodeAddress;

import java.util.ArrayList;

public class Router implements IDummyCtrlModule, IPacketListener {
    Controller ctrl;
    @Override
    public void startUp(Controller context) {
        context.addPacketListener(this);
        ctrl = context;
    }

    @Override
    public void receive(NetworkPacket packet) {

        try{
            RequestPacket rp = (RequestPacket) packet;

            NetworkPacket p = new NetworkPacket(rp.getData());

            int src = p.getSrc().intValue();
            int dst = p.getDst().intValue();
            System.out.println("rout from " + src + " to " + dst + ":" + p.toString());
            ArrayList<Integer> path = TopologyService.getPath(rp.getSrc().intValue(), dst);
            System.out.println("The path is: "  +  TopologyService.getPath(src, dst));
            if (path.size() == 1)
                return;
            int nxtH = path.get(1);
            System.out.println("nxtHop is " + nxtH);
            int tunID = SpanningTreeService.getTunnelID(rp.getSrc().intValue());
            System.out.println("Tunnel ID is " + tunID);

            FlowTableEntry entry = new FlowTableEntry();
            entry.addWindow(Window.fromString("P.SRC == " + src));
            entry.addWindow(Window.fromString("P.DST == " + dst));
            entry.addAction(new ForwardUnicastAction(new NodeAddress(nxtH)));

            ResponsePacket responsePacket = new ResponsePacket(1,rp.getDst(),rp.getSrc(), entry, (byte) tunID);

            int nextHop = TopologyService.getPath(rp.getDst().intValue(), rp.getSrc().intValue()).get(1);
            responsePacket.setNxh(new NodeAddress(nextHop));

            ctrl.sendResponse(responsePacket);


        }catch (ClassCastException exp){

        }
    }
}
