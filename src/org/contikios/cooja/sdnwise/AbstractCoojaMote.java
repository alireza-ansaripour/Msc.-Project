/*
 * Copyright (c) 2010, Swedish Institute of Computer Science.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 *
 */

package org.contikios.cooja.sdnwise;

import com.github.sdnwiselab.sdnwise.mote.core.*;
import com.github.sdnwiselab.sdnwise.mote.logger.MoteFormatter;
import com.github.sdnwiselab.sdnwise.packet.NetworkPacket;
import static com.github.sdnwiselab.sdnwise.packet.NetworkPacket.*;
import java.io.*;
import java.util.*;
import java.util.logging.*;

import com.github.sdnwiselab.sdnwise.util.NodeAddress;
import org.contikios.cooja.*;
import org.contikios.cooja.interfaces.*;
import org.contikios.cooja.motes.AbstractApplicationMote;

/**
 * Example SdnWise mote.
 *
 * This mote is simulated in COOJA via the Imported App Mote Type.
 *
 * @author Sebastiano Milardo
 */
public abstract class AbstractCoojaMote extends AbstractApplicationMote {

    // Statistics
    private int sentBytes;
    private int receivedBytes;
    private int sentDataBytes;
    private int receivedDataBytes;
    private long lastTx;
    private long doneTx;

    // Cooja
    private Simulation simulation;
    private Random random;
    private ApplicationRadio radio;
    private ApplicationLED leds;
    private Button btn;
    private final Level defaultLogLevel = Level.FINEST;
    private Logger measureLogger;
    protected com.github.sdnwiselab.sdnwise.mote.battery.Battery battery;

    AbstractCore core;

    public AbstractCoojaMote() {
        super();
    }

    public AbstractCoojaMote(MoteType moteType, Simulation simulation) {
        super(moteType, simulation);
    }

    public void writeString(String s){
        runCommand(s);
    }

    public abstract void init();

    public void logger() {
        measureLogger.log(Level.FINEST, // NODE;BATTERY LVL(mC);BATTERY LVL(%);NO. RULES INSTALLED; B SENT; B RECEIVED;
                "{0};{1};{2};{3};{4};{5};{6};{7};",
                new Object[]{core.getMyAddress(),
                    String.valueOf(battery.getLevel()),
                    String.valueOf(battery.getByteLevel() / 2.55),
                    core.getFlowTableSize(),
                    sentBytes, receivedBytes,
                    sentDataBytes, receivedDataBytes});
    }

    @Override
    public void execute(long time) {
        if (radio == null) {
            simulation = getSimulation();
            simulation.setRandomSeedGenerated(true);
            random = simulation.getRandomGenerator();
            radio = (ApplicationRadio) getInterfaces().getRadio();
            System.out.println(radio.getPosition().getXCoordinate()+","+radio.getPosition().getYCoordinate());
            leds = (ApplicationLED) getInterfaces().getLED();
            btn = (Button) getInterfaces().getButton();
            init();
            measureLogger = initLogger(Level.FINEST, core.getMyAddress()
                    + ".log", new MoteFormatter());
        }

        // The nodes do not start all at the same time
        int delay = random.nextInt(10);

        // This event simulates a clock running every 1s
        simulation.scheduleEvent(
                new MoteTimeEvent(this, 0) {
            @Override
            public void execute(long t) {
                if (battery.getByteLevel() > 0) {
                    core.timer();
                }
                logger();
                requestImmediateWakeup();
            }
        },
                simulation.getSimulationTime()
                + (1000 + delay) * Simulation.MILLISECOND
        );
    }

    public void receivedPacket(NetworkPacket networkPacket){
        core.rxRadioPacket(networkPacket);
    }

    @Override
    public void receivedPacket(RadioPacket p) {
	    String a = new String(p.getPacketData());
        if (battery.getByteLevel() > 0) {

            byte[] networkPacket;

            if (NetworkPacket.isSdnWise(p.getPacketData())) {
                networkPacket = Arrays.copyOfRange(p.getPacketData(), 0, p.getPacketData().length);
            } else {
                networkPacket = Arrays.copyOfRange(p.getPacketData(), 15, p.getPacketData().length - 2);
            }

            NetworkPacket np = new NetworkPacket(networkPacket);
            if (np.isSdnWise()) {
                receivedBytes += np.getLen();
                if (DATA == np.getTyp()) {
                    receivedDataBytes += np.getPayloadSize();
                }
            }

            core.rxRadioPacket(np);
        }
    }

    @Override
    public void sentPacket(RadioPacket p) {
    }

    @Override
    public String toString() {
        return "SDN-WISE Mote " + getID();
    }

    private Logger initLogger(Level level, String file, java.util.logging.Formatter formatter) {
        Logger LOGGER = java.util.logging.Logger.getLogger(file);
        LOGGER.setLevel(level);
        try {
            FileHandler fh;
            File dir = new File("logs");
            dir.mkdir();
            fh = new FileHandler("logs/" + file);
            fh.setFormatter(formatter);
            LOGGER.addHandler(fh);
            LOGGER.setUseParentHandlers(false);
        } catch (IOException | SecurityException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        return LOGGER;
    }


    private void checkSend(NetworkPacket networkPacket){
        int index = networkPacket.getMsgIndex();
        NetworkPacket toSend = core.getPacket(index);
        if (toSend != null && toSend.equals(networkPacket) ) {
            core.removeSendPacket(networkPacket);
            radioTX(networkPacket);

        }
    }

    public void radioTX(final NetworkPacket np) {
        if (battery.getByteLevel() > 0) {
            long actualTime = simulation.getSimulationTimeMillis();


            if (radio.isTransmitting() || radio.isReceiving() || radio.isInterfered()|| doneTx > actualTime) {
                lastTx = Math.max(lastTx, simulation.getSimulationTime()) + 1 * Simulation.MILLISECOND;
                simulation.scheduleEvent(new MoteTimeEvent(this, 0) {
                    @Override
                    public void execute(long t) {
                        radioTX(np);
                    }
                },
                        lastTx
                );

            } else {
                // send the NetworkPacket over the radio


                if(np.getTyp() != ACK){
                    if (np.isSdnWise()) {
                        sentBytes += np.getLen();
                        if (DATA == np.getTyp()) {
                            sentDataBytes += np.getPayloadSize();
                        }
                    }
                    Random random = new Random();
                    random.nextInt(100);
                    // simulates the battery consumption
                    int index = random.nextInt(100);
//                    if(np.getMsgIndex() == 0)
                    np.setMSGIndex((byte)index);

                    if(np.getTyp() != BEACON) {
                        core.addSendPackets(np);
                        simulation.scheduleEvent(
                                new MoteTimeEvent(this, 0) {
                                    @Override
                                    public void execute(long t) {
                                        checkSend(np);
                                    }
                                },
                                simulation.getSimulationTime() + 1000 * Simulation.MILLISECOND
                        );
                    }
                }
                doneTx = actualTime + 1 ;
                np.setCUR(core.getMyAddress().getArray()[0], core.getMyAddress().getArray()[1]);
                log("sending: " + np );
                RadioPacket pk = new COOJARadioPacket(np.toByteArray());
                //battery.transmitRadio(pk.getPacketData().length);

                simulation.scheduleEvent(
                        new MoteTimeEvent(this, 0) {
                            @Override
                            public void execute(long t) {

                                radio.startTransmittingPacket(pk,1/2 * simulation.MILLISECOND);
                            }
                        },
                        simulation.getSimulationTime() + 1 * Simulation.MILLISECOND
                );

            }
        }
    }

    void startThreads() {
        new Thread(new SenderRunnable()).start();
        new Thread(new LoggerRunnable()).start();
    }

    protected void runCommand(String input){}
    private class SenderRunnable implements Runnable {

        @Override
        public void run() {
            try {
                while (true) {
                    radioTX(core.getNetworkPacketToBeSend());
                }
            } catch (InterruptedException ex) {
                log(ex.getLocalizedMessage());
            }
        }
    }


    private class LoggerRunnable implements Runnable {

        @Override
        public void run() {
            try {
                while (true) {
                    Pair<Level, String> tmp = core.getLogToBePrinted();
                    if (tmp.getKey().intValue() >= Level.INFO.intValue()) {
                        log(tmp.getValue());
                    }
                }
            } catch (Exception ex) {
                log(ex.getLocalizedMessage());
            }
        }
    }

}
