package extension.tools.presetconfig.wired.incoming;

import extension.tools.presetconfig.wired.PresetWiredAddon;
import gearth.protocol.HPacket;

import java.util.ArrayList;
import java.util.List;

public class RetrievedWiredAddon extends PresetWiredAddon implements RetrievedWired {

    private final int typeId;

    public RetrievedWiredAddon(int wiredId, List<Integer> options, String stringConfig, List<Integer> items, int typeId) {
        super(wiredId, options, stringConfig, items);
        this.typeId = typeId;
    }

    @Override
    public int getTypeId() {
        return typeId;
    }

    public static RetrievedWiredAddon fromPacket(HPacket packet) {
        packet.readInteger(); // selection limit

        int itemCount = packet.readInteger(); // only includes items that are in room
        List<Integer> items = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            items.add(packet.readInteger());
        }

        int typeId = packet.readInteger(); // typeid


        int wiredId = packet.readInteger(); // furni id
        String configString = packet.readString();

        int optionsCount = packet.readInteger();
        List<Integer> options = new ArrayList<>();
        for (int i = 0; i < optionsCount; i++) {
            options.add(packet.readInteger());
        }

        // more irrelevant stuff here
        // todo
        return new RetrievedWiredAddon(wiredId, options, configString, items, typeId);
    }
}
