package com.github.sdnwiselab.sdnwise.Ctrl.interfaces;

import com.github.sdnwiselab.sdnwise.Ctrl.services.topo.Topology;

public interface ITopoUpdateListener {
    void onTopoUpdate(Topology topology);
}
