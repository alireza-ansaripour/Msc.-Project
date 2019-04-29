package Ctrl.apps.spaningTree;

import java.util.HashMap;

public class Node {
    public int start, end;
    public int id;
    public HashMap<Integer, Range> routingTable = new HashMap<>();
    public HashMap<Integer, Integer> ruleMap = new HashMap<>();
    public HashMap<Range, Integer> offsets = new HashMap<>();

    public Node(int id, int start, int end) {
        this.start = start;
        this.end = end;
        this.id = id;
    }

    public int addTunnel(int id){
        this.addRange(id, new Range(this.end + 1, this.end + 1));
        this.end++;
        return this.end;
    }

    public void addRange(int key, Range range) {
        routingTable.put(key, range);
        offsets.put(range, 0);
    }

    public int registerTunnel(Node previous, int id){
        System.out.println("reg for node " + previous + " - " + id);
        if (id > end){
            end = id;
        }

        Range range = routingTable.get(previous.id);
        if ((id + range.offset) > range.end){
            range.end = id + range.offset;
            if(end < range.end)
                end = range.end;
        }

        for (int key : routingTable.keySet()){
            if(key == previous.id)
                continue;

            Range r = routingTable.get(key);
            if (r.start >= range.end){
                r.start ++;
                r.end ++;
                if(r.end > end)
                    end = r.end;
                offsets.put(r, offsets.get(r)+1);
                r.offset++;
            }
        }
        return id + range.offset;

    }

}

class Range{
    public Range(int start, int end) {
        this.start = start;
        this.end = end;
        this.offset = 0;
    }

    public int start, end;
    public int offset;


    @Override
    public String toString() {
        return offset + ":" + start + "-" + end;
    }
}
