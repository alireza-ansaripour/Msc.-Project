package org.contikios.cooja.sdnwise;

import com.github.sdnwiselab.sdnwise.packet.AckPacket;
import com.github.sdnwiselab.sdnwise.packet.NetworkPacket;
import com.github.sdnwiselab.sdnwise.util.NodeAddress;
import org.contikios.cooja.*;
import org.contikios.cooja.interfaces.*;
import org.contikios.cooja.motes.AbstractApplicationMote;
import java.util.ArrayDeque;
import org.contikios.cooja.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;


public class OrderManager {
    private ArrayBlockingQueue<NetworkPacket> packetQueue = new ArrayBlockingQueue<>(1000);
    private static HashMap<Integer, OrderManager> managers = new HashMap<>();
    private ArrayList<NetworkPacket> pendingPackets = new ArrayList<>();
    private ArrayList<NetworkPacket> list = new ArrayList<>();
    private HashMap<Integer, Byte>sendingAck = new HashMap<>();
    private HashMap<Integer, Integer>recvAck = new HashMap<>();
    private static final int WINDOW_SIZE = 1;
    private static int timeout = 3;
    private int lastAck = 0,currentAck = 0;
    private static Simulation simulation = null;
    private AbstractCoojaMote mote = null;
    private SendAction recvAction = null;

    private boolean isTrackable(NetworkPacket networkPacket){
        return !(
                networkPacket.getTyp() == NetworkPacket.ACK ||
                networkPacket.getDst().isBroadcast() ||
                networkPacket.getNxh().isBroadcast()
        );
    }

    public  OrderManager(Simulation s, AbstractCoojaMote m){
        simulation = s;
        mote = m;
        managers.put(m.getID(), this);
        new Sender().start();
    }

    public static OrderManager getManager(int id){
        return managers.get(id);
    }

    private AckPacket prepareAck(NetworkPacket packet){
        AckPacket packet1 =  new AckPacket(0,new NodeAddress(mote.getID()),packet.getCUR());
        packet1.setMSGIndex(packet.getMsgIndex());
        packet1.setNxh(packet.getCUR());
        packet1.setDst(packet.getCUR());
        return packet1;
    }
    public void packetReceived(NetworkPacket networkPacket){
        mote.logI(" recv packet " + networkPacket);
        if(isTrackable(networkPacket)){
            int src = networkPacket.getCUR().intValue();
            int waiting = recvAck.computeIfAbsent(src, integer -> 0);
            mote.logI("waiting for " + waiting + " got " + networkPacket.getMsgIndex()+ " for  src " + src);
            AckPacket packet = prepareAck(networkPacket);
            addPacketToSend(packet);
//            if (waiting != networkPacket.getMsgIndex()) {
//                return;
//            }
            mote.logI("waiting for right packet");
            recvAck.put(src, waiting+1);
            recvAction.run(networkPacket);
        }else
            recvAction.run(networkPacket);
    }
    Semaphore se = new Semaphore(1);
    private void resendMessages(){
        ArrayList<NetworkPacket> packets = new ArrayList<>(pendingPackets);
        for (NetworkPacket packet: packets) {
            mote.logI("resending message " + packet);
//            if (!packetQueue.contains(packet))
//                packetQueue.add(packet);
        }
    }

    private void handleTimeout(final int number){
        simulation.scheduleEvent(
                new MoteTimeEvent((AbstractApplicationMote)mote, 0) {
                    @Override
                    public void execute(long t) {
                        if(pendingPackets.size() != 0)
                            handleTimeout(number + 1);
                        else
                            handleTimeout(number);
                        if (number == timeout){
                            resendMessages();
                            handleTimeout(0);
                        }

                    }
                },
                simulation.getSimulationTime()
                        + (1000) * Simulation.MILLISECOND
        );
    }

    private class Sender extends Thread{
        @Override
        public void run() {
            super.run();
            handleTimeout(0);
            long doneTx = simulation.getSimulationTimeMillis();
            while (true){
                try {
                    long now = simulation.getSimulationTimeMillis();
                    if (!mote.getRadio().isTransmitting() && !mote.getRadio().isReceiving() && !mote.getRadio().isInterfered() &&now >= doneTx){
                        doneTx = now + (100);
                        NetworkPacket packet = packetQueue.take();
                        mote.logI("took a packet " + packet);
                        if (!(mote.getID() == packet.getDst().intValue() || packet.getDst().isBroadcast() || packet.getNxh().isBroadcast() || packet.getTyp() == NetworkPacket.ACK)){
                            Byte i = sendingAck.get(packet.getNxh().intValue());
                            byte index = (i == null ? 0: (byte)i);
                            if (i == null){
                                sendingAck.put(packet.getNxh().intValue(), (byte) 0);
                                index = 0;
                            }
                            mote.logI("sending ack for " + packet.getNxh().intValue() + " value " + index);
                            if (packet.getMsgIndex() == 100){
                                packet.setMSGIndex(index);
                                index ++;
                                index = (byte)(index % 90);
                                sendingAck.put(packet.getNxh().intValue(), index);
                            }


                            if (!pendingPackets.contains(packet))
                                pendingPackets.add(packet);
                        }
                        packet.setCUR((byte)0, (byte)mote.getID());


                        if(action != null){
                            simulation.scheduleEvent(
                                new MoteTimeEvent((AbstractApplicationMote)mote, 0) {
                                    @Override
                                    public void execute(long t) {
                                        action.run(packet);
                                    }
                                },
                                    simulation.getSimulationTime()
                                            + (mote.getID()) * Simulation.MILLISECOND
                            );



                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }
    }
    private SendAction action = null;
    public  void runSend(SendAction a){action = a;}


    public void addPacketToSend(NetworkPacket networkPacket){
        if (networkPacket.getTyp() == NetworkPacket.ACK)
            packetQueue.add(networkPacket);
        if (networkPacket.getTyp() != NetworkPacket.ACK)
            networkPacket.setMSGIndex((byte)100);
        if (pendingPackets.size() < WINDOW_SIZE)
            packetQueue.add(networkPacket);
        else
            list.add(networkPacket);
    }


    public void AckPacket(int src, byte id){
        mote.logI(" received ack for " + id + " for src " + src);

        if (pendingPackets.size() == 0) {
            return;
        }

        for (int i = 0; i < pendingPackets.size(); ) {
            NetworkPacket packet = pendingPackets.get(i);
            if(packet.getNxh().intValue() == src){
                if (id == packet.getMsgIndex()) {
                    pendingPackets.remove(i);
                    mote.logI("packet removed " + pendingPackets);
                    if (list.size() >0 ){
                        packetQueue.add(list.get(0));
                        list.remove(0);
                    }
                }
                else
                    i++;
            }else{
                i++;
            }
        }

    }
    public void setOnRecvAction(SendAction action){
        recvAction = action;
    }
}


interface SendAction{
    void run(NetworkPacket networkPacket);
}

