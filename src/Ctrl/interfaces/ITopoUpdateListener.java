package Ctrl.interfaces;

import Ctrl.services.topo.Topology;
import Ctrl.services.topo.Vertex;

public interface ITopoUpdateListener {
    void onTopoUpdate(Topology topology);
    void onNodeAdd(Vertex node);
    void onNodeRemove(Vertex node);
}
