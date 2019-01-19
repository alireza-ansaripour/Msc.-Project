package com.github.sdnwiselab.sdnwise.Ctrl.SampleApp;
import com.github.sdnwiselab.sdnwise.Ctrl.Controller;
import com.github.sdnwiselab.sdnwise.Ctrl.interfaces.IDummyCtrlModule;
import com.github.sdnwiselab.sdnwise.Ctrl.interfaces.IPacketListener;
import com.github.sdnwiselab.sdnwise.packet.NetworkPacket;

public class Router implements IDummyCtrlModule, IPacketListener {
    @Override
    public void startUp(Controller context) {
        context.addPacketListener(this);
    }

    @Override
    public void receive(NetworkPacket packet) {
        System.out.println("Network Packet Received " + packet.toString());
    }
}
