package utils.parsers;

import gearth.protocol.HPacket;

public class WiredAllVariableHolder {
    private int unused;
    private VariableInfoAndHolders variableInfoAndHolders;

    public WiredAllVariableHolder(HPacket packet) {
        packet.readInteger();
        variableInfoAndHolders = new VariableInfoAndHolders(packet);
    }
}
