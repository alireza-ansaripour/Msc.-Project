package com.github.sdnwiselab.sdnwise.Ctrl.apps.spaningTree;

import java.util.HashMap;

public class Node {
    public int start, end;
    public int id;
    public HashMap<Integer, Range> routingTable = new HashMap<>();
    public HashMap<Range, Integer> offsets = new HashMap<>();

    public Node(int id, int start, int end) {
        this.start = start;
        this.end = end;
        this.id = id;
    }

    public int addTunnel(int id){
        this.addRange(id, new Range(this.end + 1, this.end + 1));
        return this.end + 1;
    }

    public void addRange(int key, Range range) {
        routingTable.put(key, range);
        offsets.put(range, 0);
    }

    public void registerTunnel(int id){
        if (id > end){
            end = id;
        }
        boolean increaseOffset = false;
        for (int key : routingTable.keySet()){
            Range r = routingTable.get(key);
            if (increaseOffset && r.start >= id){
                r.start ++;
                r.end ++;
                offsets.put(r, offsets.get(r)+1);
                r.offset++;
            }else {

                if (r.end == id-1){
                    r.end++;
                    increaseOffset = true;
                    end++;
                }
            }
        }

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
