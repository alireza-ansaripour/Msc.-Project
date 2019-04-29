package Ctrl;

import Ctrl.apps.spaningTree.SpaningTreeGenerator;
import Ctrl.interfaces.IDummyCtrlModule;
import Ctrl.interfaces.IPacketListener;
import Ctrl.interfaces.ITopoUpdateListener;

import Ctrl.services.topo.Topology;
import Ctrl.services.topo.Vertex;
import com.github.sdnwiselab.sdnwise.packet.NetworkPacket;
import org.contikios.cooja.sdnwise.CoojaSink;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;


public class Controller {
    private CoojaSink sink;
    private ArrayList<IPacketListener> packetListeners = new ArrayList<>();

    public CoojaSink getSink() {
        return sink;
    }

    private static Controller instance = null;
    private ArrayList<ITopoUpdateListener> topoUpdateListeners = new ArrayList<>();
    private Class [] modules = new Class[]{
            Topology.class,
            SpaningTreeGenerator.class,
//            Router.class
    };

    public Controller() {
        instance = this;
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


    public static NetworkPacket p;

    public  static Semaphore semaphore = new Semaphore(1);
    private void startListening(){

        while (true){
            try {
                NetworkPacket packet = txControllerQueue.take();
                p = packet;
                handleIncommingPacket(packet);
            } catch (InterruptedException e) {
                sink.logI("error in getting message");
            }finally {

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

    public void notifyNodeAdd(Vertex node){
        for (ITopoUpdateListener listener:topoUpdateListeners)
            listener.onNodeAdd(node);
    }

    public void notifyNodeRemove(Vertex node){
        for (ITopoUpdateListener listener:topoUpdateListeners)
            listener.onNodeRemove(node);
    }


    public void addPacketListener(IPacketListener listener){
        packetListeners.add(listener);
    }

    public void addTopoChangeListener(ITopoUpdateListener listener){topoUpdateListeners.add(listener);}



    private static Controller controller;

    public void sendResponse(NetworkPacket networkPacket){
        this.sink.receivedPacket(networkPacket);
    }


    public static Controller getController(){
        return instance;
    }

    public static Controller getInstance(CoojaSink sink){
        if(controller == null){
            controller = new Controller();
            controller.sink = sink;
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {

                    controller.startListening();
                }
            });
            t.start();

        }

        return controller;
    }
    public static Controller getInstance(){
        return controller;
    }


}
