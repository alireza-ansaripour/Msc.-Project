package com.github.sdnwiselab.sdnwise.Ctrl;

import com.github.sdnwiselab.sdnwise.Ctrl.topo.Topology;
import com.github.sdnwiselab.sdnwise.flowtable.FlowTableEntry;
import com.github.sdnwiselab.sdnwise.flowtable.ForwardUnicastAction;
import com.github.sdnwiselab.sdnwise.flowtable.Window;
import com.github.sdnwiselab.sdnwise.mote.core.AbstractCore;
import com.github.sdnwiselab.sdnwise.mote.core.SinkCore;
import com.github.sdnwiselab.sdnwise.mote.standalone.AbstractMote;
import com.github.sdnwiselab.sdnwise.packet.*;
import com.github.sdnwiselab.sdnwise.util.NodeAddress;
import org.contikios.cooja.sdnwise.CoojaSink;

import javax.xml.crypto.Data;
import javax.xml.soap.Node;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.logging.*;

public class Controller {
    private CoojaSink sink;
    private Topology topology = new Topology();

    public final ArrayBlockingQueue<NetworkPacket> txControllerQueue
            = new ArrayBlockingQueue<>(1000);

    private class Listener implements Runnable{
        @Override
        public void run() {
            startListening();
        }
    }

    private void startListening(){
        Semaphore semaphore = new Semaphore(1);
        while (true){
            try {
                semaphore.acquire();
                NetworkPacket packet = txControllerQueue.take();
                sink.logI("taking");
                handleIncommingPacket(packet);
            } catch (InterruptedException e) {
                sink.logI("error in getting message");
            }finally {
                sink.logI("release");
                semaphore.release();
            }
        }
    }
    public void handleIncommingPacket(NetworkPacket networkPacket){
        sink.logI("ctrl packet " + networkPacket.toString());
        switch (networkPacket.getTyp()){
            case NetworkPacket.DATA:
                handleData((DataPacket) networkPacket);
                break;
            case NetworkPacket.REQUEST:
                handleRequest((RequestPacket) networkPacket);
                break;
            case NetworkPacket.REPORT:
                ReportPacket reportPacket = (ReportPacket) networkPacket;
                handleReport(reportPacket);
                break;
        }
    }

    private void handleRequest(RequestPacket requestPacket){
        sink.logI("new Request packet from " + requestPacket.getSrc());
        byte[] payload = requestPacket.getData();
        NetworkPacket np = new NetworkPacket(payload);
        sink.logI(requestPacket.getSrc() + "->" + np.getDst());
        HashMap<Integer, Integer> path = new HashMap<>();
        path.put(6,2);
        path.put(2,7);
        path.put(7,10);
        FlowTableEntry entry = new FlowTableEntry();
        entry.addWindow(Window.fromString("P.DST == " + np.getDst().intValue()));
        int nxh = path.get(requestPacket.getSrc().intValue());
        sink.logI("next hop is " + nxh);
        entry.addAction(new ForwardUnicastAction(new NodeAddress(nxh)));
        ResponsePacket responsePacket = new ResponsePacket(1, new NodeAddress(1), requestPacket.getSrc(), entry );
        int nextHop = topology.getPathFromCtrl(requestPacket.getSrc().intValue()).get(1);

        responsePacket.setNxh("0." + nextHop);
        sink.logI("from ctrl " + responsePacket);
        this.sendResponse(responsePacket);
    }

    private void handleData(DataPacket dataPacket){
        System.out.println("new Data Packet from " + dataPacket.getSrc());
    }


    private ArrayList<Integer> q = new ArrayList<>();

    private ArrayList<Integer> paths = new ArrayList<>();
    private void handleReport(ReportPacket reportPacket){
        NodeAddress src = reportPacket.getSrc();
        sink.logI("new Rep packet from " + src);
        int srcID = src.intValue();

        ArrayList<Integer> neiburs = new ArrayList<>();
        for (NodeAddress address: reportPacket.getNeighbors().keySet()){
            neiburs.add(address.intValue());
        }
        if(!topology.isThereAPathToCtrl(neiburs) && srcID != 1){
            sink.logI("no path to ctrl");
            return;
        }
        topology.addNode(srcID, neiburs);
        sink.logI(topology.printGraph());
        if (srcID == 1) {
            return;
        }
        ArrayList<Integer> path = null;
        path = topology.getPathFromCtrl(srcID);
        sink.logI("the path is " + path);

        if(path.size() <= 1){
            q.add(srcID);
            sink.logI("here it is " + srcID);
            return;
        }
        if(this.paths.contains(srcID))
            return;
        this.paths.add(srcID);
        List<Window> windows = new ArrayList<>();
        windows.add(Window.fromString("P.TYP == 4"));
        ArrayList<NodeAddress> pathNA = new ArrayList<>();
        for(Integer id : path)
            pathNA.add(new NodeAddress(id));
        OpenPathPacket pathPacket = new OpenPathPacket(1,new NodeAddress(1), new NodeAddress(srcID),pathNA);
        pathPacket.setNxh(new NodeAddress(path.get(1)));
        pathPacket.setWindows(windows);
        sink.logI(pathPacket.toString());
        sink.radioTX(pathPacket);
    }

    private static Controller controller;

    public void sendResponse(NetworkPacket networkPacket){
        this.sink.radioTX(networkPacket);
    }
    public static Controller getInstance(CoojaSink sink){
        if(controller == null){
            controller = new Controller();
            controller.sink = sink;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    controller.startListening();
                }
            }).start();
        }

        return controller;
    }
    public static Controller getInstance(){
        return controller;
    }

}
