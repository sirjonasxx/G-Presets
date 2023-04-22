package extension.tools.presetconfig.wired.incoming;

import extension.tools.presetconfig.wired.PresetWiredEffect;
import gearth.protocol.HPacket;

import java.util.ArrayList;
import java.util.List;

public class RetrievedWiredEffect extends PresetWiredEffect implements RetrievedWired {

    private final int typeId;

    public RetrievedWiredEffect(int wiredId, List<Integer> options, String stringConfig, List<Integer> items, int delay, int typeId) {
        super(wiredId, options, stringConfig, items, delay);
        this.typeId = typeId;
    }

    @Override
    public int getTypeId() {
        return typeId;
    }

    public static RetrievedWiredEffect fromPacket(HPacket packet) {
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

        int list1length = packet.readInteger();
        for (int i = 0; i < list1length; i++) {packet.readInteger();}

        int list2length = packet.readInteger();
        for (int i = 0; i < list2length; i++) {packet.readInteger();}

        packet.readInteger(); // wired code
        int delay = packet.readInteger(); // delay

        // more irrelevant stuff here
        // todo
        return new RetrievedWiredEffect(wiredId, options, configString, items, delay, typeId);
    }
}
