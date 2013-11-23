package org.codehaus.groovy.control;

import java.util.LinkedList;
import java.util.List;

public class LineMap {

    private int[] map;

    private int[] toIntArray(List<Integer> list)  {
        int[] ret = new int[list.size()];
        int i = 0;
        for (Integer e : list)
            ret[i++] = e.intValue();
        return ret;
    }

    public LineMap(char[] chars) {
        List<Integer> _map = new LinkedList<Integer>();
        int i = 0;
        while(i < chars.length) {
            if(chars[i] == '\r') {
                if(i+1 < chars.length) {
                    if(chars[i+1] == '\n') {
                        i = i + 2;
                        _map.add(i);
                        continue;
                    }
                }
                i = i + 1;
                _map.add(i);
                continue;
            } else if(chars[i] == '\n') {
                i = i + 1;
                _map.add(i);
                continue;
            } else {
                i++;
            }
        }
        this.setMap(toIntArray(_map));
    }

    public int lineOf(int charIndex) {
        for(int i = 0; i < map.length-1; i++) {
            if(charIndex > map[i] && charIndex <= map[i+1]) {
                return i+1;
            }
        }
        return -1;
    }

    public int[] getMap() {
        return map;
    }

    public void setMap(int[] map) {
        this.map = map;
    }
}
