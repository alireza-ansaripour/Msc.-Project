package Ctrl.interfaces;

import com.github.sdnwiselab.sdnwise.packet.NetworkPacket;

public interface IPacketListener {
    void receive(NetworkPacket packet);
}
