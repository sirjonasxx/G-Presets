package extension.parsers;

import gearth.protocol.HPacket;
import java.util.ArrayList;
import java.util.List;

public class HSharedGlobalPlaceholderList {
    public final List<HSharedGlobalPlaceholder> sharedPlaceholders = new ArrayList<>();

    public HSharedGlobalPlaceholderList(HPacket packet) {
        int count = packet.readInteger();
        for (int i = 0; i < count; i++) {
            HSharedGlobalPlaceholder placeholder = new HSharedGlobalPlaceholder(packet);
            this.sharedPlaceholders.add(placeholder);
        }
    }
}
