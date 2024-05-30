package extension.parsers;

import gearth.protocol.HPacket;

public class HVariableInfoAndValue {
    private final int value;
    public final HWiredVariable variable;

    public HVariableInfoAndValue(HPacket packet) {
        this.variable = new HWiredVariable(packet);
        this.value = packet.readInteger();
    }
}
