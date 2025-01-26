package extension.parsers;

import gearth.protocol.HPacket;

public class HSharedGlobalPlaceholder {
    public final int roomId;
    public final String roomName;
    public final String placeholderName;

    public HSharedGlobalPlaceholder(HPacket packet) {
        this.roomId = packet.readInteger();
        this.roomName = packet.readString();
        this.placeholderName = packet.readString();
    }
}
