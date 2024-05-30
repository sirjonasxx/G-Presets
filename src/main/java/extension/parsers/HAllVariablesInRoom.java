package extension.parsers;

import gearth.protocol.HPacket;

import java.util.HashMap;

public class HAllVariablesInRoom {
    public final HashMap<Long, HWiredVariable> variables = new HashMap<>();

    public HAllVariablesInRoom(HPacket packet) {
        int count = packet.readInteger();
        for(int ii = 0; ii < count; ii++) {
            HWiredVariable info = new HWiredVariable(packet);
            this.variables.put(info.id, info);
        }
    }
}
