package com.github.sdnwiselab.sdnwise.Ctrl.services.topo;

import java.util.ArrayList;

public class Node {
    private int id;
    private ArrayList<Link> links = new ArrayList<>();

    public Node(int id, ArrayList<Link> links) {
        this.id = id;
        this.links = links;
    }

    public int getId() {
        return id;
    }

    public ArrayList<Link> getLinks() {
        return links;
    }
    public void addLink(Link link){
        this.links.add(link);
    }

}
