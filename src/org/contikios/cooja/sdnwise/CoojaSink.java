/*
 * Copyright (C) 2015 SDN-WISE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.contikios.cooja.sdnwise;;

import com.github.sdnwiselab.sdnwise.Ctrl.Controller;
import com.github.sdnwiselab.sdnwise.mote.battery.SinkBattery;
import com.github.sdnwiselab.sdnwise.mote.core.*;
import com.github.sdnwiselab.sdnwise.packet.NetworkPacket;
import com.github.sdnwiselab.sdnwise.util.NodeAddress;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.logging.*;
import javax.swing.JOptionPane;
import org.contikios.cooja.*;

/**
 * @author Sebastiano Milardo
 */
public class CoojaSink extends AbstractCoojaMote {

    private Socket tcpSocket;
    private DataInputStream riceviOBJ;
    private DataOutputStream inviaOBJ;
    private InetAddress addrController;
    private int portController;

    public CoojaSink() {
        super();
    }

    public CoojaSink(MoteType moteType, Simulation simulation) {
        super(moteType, simulation);
        System.out.println(moteType);
        System.out.println(moteType.getMoteInterfaceClasses());
    }
    public void logI(String msg){
        core.log(Level.INFO, msg);
    }

    @Override
    public final void init() {
        battery = new SinkBattery();
        Controller.getInstance(this);


        core = new SinkCore((byte) 1,
                new NodeAddress(this.getID()),
                battery,
                "00000001",
                "00:01:02:03:04:05",
                1,
                null);
        core.start();
        startThreads();
    }

    @Override
    public void execute(long time) {
        super.execute(time);
    }
    //    private class TcpListener implements Runnable {
//
//        @Override
//        public void run() {
//            try {
//                while (true) {
//                    NetworkPacket np = new NetworkPacket(riceviOBJ);
//                    core.rxRadioPacket(np, 255);
//                }
//            } catch (IOException ex) {
//                Logger.getLogger(CoojaSink.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//    }

    private class TcpSender implements Runnable {

        @Override
        public void run() {
            Controller controller = Controller.getInstance();
            try {
                while (true) {
                    NetworkPacket np = ((SinkCore) core).getControllerPacketTobeSend();
                    controller.txControllerQueue.put(np);
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(CoojaSink.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    void startThreads() {
        super.startThreads();
        new Thread(new TcpSender()).start();

    }

    private String[] getControllerIpPort() {
        String s = "localhost:8081";

        String[] tmp = s.split(":");

        if (tmp.length != 2) {
            return getControllerIpPort();
        } else {
            return tmp;
        }
    }
}