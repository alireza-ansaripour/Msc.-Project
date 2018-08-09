package com.github.sdnwiselab.sdnwise.packet;

import com.github.sdnwiselab.sdnwise.util.NodeAddress;

public class AckPacket extends NetworkPacket {
    public AckPacket(int net, NodeAddress src, NodeAddress dst) {
        super(net, src, dst);
        setTyp(ACK);
    }
}
