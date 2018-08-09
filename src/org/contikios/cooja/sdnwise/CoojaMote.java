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

import com.github.sdnwiselab.sdnwise.mote.battery.Battery;
import com.github.sdnwiselab.sdnwise.mote.core.*;
import com.github.sdnwiselab.sdnwise.packet.DataPacket;
import com.github.sdnwiselab.sdnwise.packet.NetworkPacket;
import com.github.sdnwiselab.sdnwise.util.NodeAddress;
import org.contikios.cooja.*;

/**
 * @author Sebastiano Milardo
 */
public class CoojaMote extends AbstractCoojaMote {

    public CoojaMote() {
        super();
    }


    public CoojaMote(MoteType moteType, Simulation simulation) {
        super(moteType, simulation);
    }
    @Override
    public final void init() {
        battery = new Battery();
        core = new MoteCore((byte) 1, new NodeAddress(this.getID()), battery);
        core.start();
        startThreads();

    }

    @Override
    public void execute(long time) {
        super.execute(time);
        CoojaMote mote = this;
        Simulation simulation = getSimulation();

        simulation.scheduleEvent(
                new MoteTimeEvent(this, 200000 * simulation.MILLISECOND) {
                    @Override
                    public void execute(long t) {
                        NodeAddress addr = new NodeAddress("0.10");
                        NetworkPacket np = new DataPacket(1, new NodeAddress(mote.getID()), addr,new byte[]{1});
                        if(mote.getID()==6 ) {
                            log("sending data");
//                            mote.core.send(np);

                        }
                    }
                },
                simulation.getSimulationTime()
                        + 44000 * Simulation.MILLISECOND
        );
    }
}
