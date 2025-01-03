package extension.parsers;

import gearth.protocol.HPacket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HAllVariablesInRoom {
    private final List<Integer> variableList = new ArrayList<Integer>();
    public HAllVariablesInRoom(HPacket packet) {
        variableList.add(packet.readInteger());
    }
    public List<Integer> getVariableList() {
        return variableList;
    }
}