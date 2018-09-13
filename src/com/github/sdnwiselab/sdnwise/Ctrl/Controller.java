package com.github.sdnwiselab.sdnwise.Ctrl;

import com.github.sdnwiselab.sdnwise.Ctrl.apps.spaningTree.SpaningTreeGenerator;
import com.github.sdnwiselab.sdnwise.Ctrl.interfaces.IDummyCtrlModule;
import com.github.sdnwiselab.sdnwise.Ctrl.interfaces.IPacketListener;
import com.github.sdnwiselab.sdnwise.Ctrl.interfaces.ITopoUpdateListener;
import com.github.sdnwiselab.sdnwise.Ctrl.services.topo.Topology;
import com.github.sdnwiselab.sdnwise.packet.*;
import org.contikios.cooja.sdnwise.CoojaSink;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;


public class Controller {
    private CoojaSink sink;
    private ArrayList<IPacketListener> packetListeners = new ArrayList<>();
    private ArrayList<ITopoUpdateListener> topoUpdateListeners = new ArrayList<>();
    private Class [] modules = new Class[]{
            Topology.class,
            SpaningTreeGenerator.class
    };

    public Controller() {
        for (Class c: modules) {
            try {
                IDummyCtrlModule obj = (IDummyCtrlModule) c.newInstance();
                obj.startUp(this);
            } catch (IllegalAccessException | InstantiationException e) {
                e.printStackTrace();
            }
        }
    }

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
                handleIncommingPacket(packet);
            } catch (InterruptedException e) {
                sink.logI("error in getting message");
            }finally {
                semaphore.release();
            }
        }
    }

    public void handleIncommingPacket(NetworkPacket networkPacket){
        sink.logI("ctrl packet " + networkPacket.toString());
        for (IPacketListener listener  : packetListeners) {
            listener.receive(networkPacket);
        }
    }

    public void notifyTopologyChange(Topology topology){
        for (ITopoUpdateListener listener : topoUpdateListeners){
            listener.onTopoUpdate(topology);
        }
    }


    public void addPacketListener(IPacketListener listener){
        packetListeners.add(listener);
    }

    public void addTopoChangeListener(ITopoUpdateListener listener){topoUpdateListeners.add(listener);}

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
