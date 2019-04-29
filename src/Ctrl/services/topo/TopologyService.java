package Ctrl.services.topo;

import java.util.ArrayList;

public class TopologyService {
    private static Topology topology;

    public static void setTopo(Topology t){
        if (topology == null)
            topology = t;
    }

    public static ArrayList<Integer> getPath(int from, int to){
        return topology.findPath(from, to);
    }





}
