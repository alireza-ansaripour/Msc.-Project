package Ctrl.services.topo;

public class Link {
    private Node src,dst;

    public Link(Node src, Node dst) {
        this.src = src;
        this.dst = dst;
    }

    public Node getSrc() {
        return src;
    }

    public void setSrc(Node src) {
        this.src = src;
    }

    public Node getDst() {
        return dst;
    }

    public void setDst(Node dst) {
        this.dst = dst;
    }

    @Override
    public boolean equals(Object obj) {
        Link o = (Link) obj;
        return o.dst == dst && o.src == src;
    }
}
