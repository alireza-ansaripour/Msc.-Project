package Ctrl.services.topo;

import java.util.ArrayList;

public class Vertex implements Comparable<Vertex> {
    public final String name;
    public ArrayList<Edge> adjacencies = new ArrayList<>();
    private ArrayList<Vertex> vertices = new ArrayList<>();
    public void addAdj(Vertex v){
        if (vertices.contains(v))
            return;
        vertices.add(v);
        v.addAdj(this);
        Edge edge =  new Edge(v, 1);
        adjacencies.add(edge);
    }
    public double minDistance = Double.POSITIVE_INFINITY;
    public Vertex previous;
    public Vertex(String argName) { name = argName; }
    public String toString() { return name + adjacencies; }

    public boolean isAdj(Vertex v){
        if (vertices.contains(v))
            return true;
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        Vertex v = (Vertex) obj;
        return v.name.equals(name);
    }

    public int compareTo(Vertex other)
        {
        return Double.compare(minDistance, other.minDistance);
        }
}