package com.github.sdnwiselab.sdnwise.Ctrl.interfaces;

import com.github.sdnwiselab.sdnwise.Ctrl.services.topo.Topology;
import com.github.sdnwiselab.sdnwise.Ctrl.services.topo.Vertex;

public interface ITopoUpdateListener {
    void onTopoUpdate(Topology topology);
    void onNodeAdd(Vertex node);
    void onNodeRemove(Vertex node);
}
