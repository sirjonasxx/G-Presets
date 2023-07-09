package extension.tools.presetconfig.wired.incoming;

import extension.tools.presetconfig.wired.PresetWiredSelector;
import gearth.protocol.HPacket;
import utils.Utils;

import java.util.List;

public class RetrievedWiredSelector extends PresetWiredSelector implements RetrievedWired {

    private final int typeId;

    public RetrievedWiredSelector(int wiredId, List<Integer> options, String stringConfig, List<Integer> items, int typeId, boolean filter, boolean invert, List<Integer> pickedFurniSources, List<Integer> pickedUserSources) {
        super(wiredId, options, stringConfig, items, filter, invert, pickedFurniSources, pickedUserSources);
        this.typeId = typeId;
    }

    @Override
    public int getTypeId() {
        return typeId;
    }

    public static RetrievedWiredSelector fromPacket(HPacket packet) {
        packet.readInteger(); // selection limit

        List<Integer> items = Utils.readIntList(packet);

        int typeId = packet.readInteger(); // typeid


        int wiredId = packet.readInteger(); // furni id
        String configString = packet.readString();

        List<Integer> options = Utils.readIntList(packet);

        List<Integer> furniSources = Utils.readIntList(packet);
        List<Integer> userSources = Utils.readIntList(packet);

        packet.readInteger();
        boolean filter = packet.readBoolean();
        boolean invert = packet.readBoolean();

        // more irrelevant stuff here
        return new RetrievedWiredSelector(wiredId, options, configString, items, typeId, filter, invert, furniSources, userSources);
    }
}
