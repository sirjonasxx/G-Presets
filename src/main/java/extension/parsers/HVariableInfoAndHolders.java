package extension.parsers;

import gearth.protocol.HPacket;

import java.util.HashMap;

public class HVariableInfoAndHolders {
    private final HashMap<Integer, Integer> holders;
    public final HWiredVariable variable;

    public HVariableInfoAndHolders(HPacket packet) {
        this.variable = new HWiredVariable(packet);
        this.holders = new HashMap<>();
        int count = packet.readInteger();
        for(int i = 0; i < count; i++) {
            this.holders.put(packet.readInteger(), packet.readInteger());
        }
    }
}
