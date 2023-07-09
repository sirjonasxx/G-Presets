package extension.tools.presetconfig.wired.incoming;

import extension.tools.presetconfig.wired.PresetWiredTrigger;
import gearth.protocol.HPacket;
import utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class RetrievedWiredTrigger extends PresetWiredTrigger implements RetrievedWired {

    private final int typeId;

    public RetrievedWiredTrigger(int wiredId, List<Integer> options, String stringConfig, List<Integer> items, int typeId, List<Integer> pickedFurniSources, List<Integer> pickedUserSources) {
        super(wiredId, options, stringConfig, items, pickedFurniSources, pickedUserSources);
        this.typeId = typeId;
    }

    @Override
    public int getTypeId() {
        return typeId;
    }

    public static RetrievedWiredTrigger fromPacket(HPacket packet) {
        packet.readInteger(); // selection limit

        List<Integer> items = Utils.readIntList(packet);

        int typeId = packet.readInteger(); // typeid


        int wiredId = packet.readInteger(); // furni id
        String configString = packet.readString();

        List<Integer> options = Utils.readIntList(packet);

        List<Integer> furniSources = Utils.readIntList(packet);
        List<Integer> userSources = Utils.readIntList(packet);

        // more irrelevant stuff here
        return new RetrievedWiredTrigger(wiredId, options, configString, items, typeId, furniSources, userSources);
    }
}
