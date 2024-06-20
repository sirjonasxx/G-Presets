package extension.parsers;

import gearth.protocol.HPacket;

public class HSharedVariable {
    public final int roomId;
    public final String roomName;
    public final HWiredVariable wiredVariable;

    public HSharedVariable(HPacket packet) {
        this.roomId = packet.readInteger();
        this.roomName = packet.readString();
        this.wiredVariable = new HWiredVariable(packet);
    }
}
