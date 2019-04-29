package Ctrl.services.topo;

import java.util.ArrayList;

public class Node {
    private int id;

    private ArrayList<Node> neighbours;

    public Node(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public ArrayList<Node> getNeighbours() {
        return neighbours;
    }

    public void addNeighbours(Node node) {
        this.neighbours.add(node);
    }


}
