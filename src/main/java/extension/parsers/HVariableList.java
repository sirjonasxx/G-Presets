package extension.parsers;

import gearth.protocol.HPacket;

import java.util.HashMap;

public class HVariableList {
    public final HashMap<String, HWiredVariable> variables = new HashMap<>();

    public HVariableList(HPacket packet) {
        int count = packet.readInteger();
        for(int i = 0; i < count; i++) {
            HWiredVariable info = new HWiredVariable(packet);
            this.variables.put(info.id, info);
        }
    }
}
