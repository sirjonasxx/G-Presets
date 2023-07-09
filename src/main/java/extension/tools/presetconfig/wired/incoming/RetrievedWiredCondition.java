package extension.tools.presetconfig.wired.incoming;

import extension.tools.presetconfig.wired.PresetWiredCondition;
import gearth.protocol.HPacket;
import utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class RetrievedWiredCondition extends PresetWiredCondition implements RetrievedWired {

    private final int typeId;

    public RetrievedWiredCondition(int wiredId, List<Integer> options, String stringConfig, List<Integer> items, int typeId, int quantifier, List<Integer> pickedFurniSources, List<Integer> pickedUserSources) {
        super(wiredId, options, stringConfig, items, quantifier, pickedFurniSources, pickedUserSources);
        this.typeId = typeId;
    }

    @Override
    public int getTypeId() {
        return typeId;
    }

    public static RetrievedWiredCondition fromPacket(HPacket packet) {
        packet.readInteger(); // selection limit

        List<Integer> items = Utils.readIntList(packet);

        int typeId = packet.readInteger(); // typeid


        int wiredId = packet.readInteger(); // furni id
        String configString = packet.readString();

        List<Integer> options = Utils.readIntList(packet);

        List<Integer> furniSources = Utils.readIntList(packet);
        List<Integer> userSources = Utils.readIntList(packet);

        packet.readInteger();
        int quantifier = packet.readInteger();


        // more irrelevant stuff here
        return new RetrievedWiredCondition(wiredId, options, configString, items, typeId, quantifier, furniSources, userSources);
    }
}
