package extension.parsers;

import gearth.protocol.HPacket;

import java.util.HashMap;

public class HAllVariablesInRoom {
    public final HashMap<String, HWiredVariable> variables = new HashMap<>();
    public final int hash;

    public HAllVariablesInRoom(HPacket packet) {
        this.hash = packet.readInteger();
    }
}
