package extension.tools.presetconfig.wired.incoming;

import extension.tools.presetconfig.wired.PresetWiredEffect;
import gearth.protocol.HPacket;
import utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class RetrievedWiredEffect extends PresetWiredEffect implements RetrievedWired {

    private final int typeId;

    public RetrievedWiredEffect(int wiredId, List<Integer> options, String stringConfig, List<Integer> items, int delay, int typeId, List<Integer> pickedFurniSources, List<Integer> pickedUserSources) {
        super(wiredId, options, stringConfig, items, delay, pickedFurniSources, pickedUserSources);
        this.typeId = typeId;
    }

    @Override
    public int getTypeId() {
        return typeId;
    }

    public static RetrievedWiredEffect fromPacket(HPacket packet) {
        packet.readInteger(); // selection limit

        List<Integer> items = Utils.readIntList(packet);

        int typeId = packet.readInteger(); // typeid
        int wiredId = packet.readInteger(); // furni id

        String configString = packet.readString();
        List<Integer> options = Utils.readIntList(packet);

        List<Integer> furniSources = Utils.readIntList(packet);
        List<Integer> userSources = Utils.readIntList(packet);

        packet.readInteger(); // wired code
        int delay = packet.readInteger(); // delay

        // more irrelevant stuff here
        return new RetrievedWiredEffect(wiredId, options, configString, items, delay, typeId, furniSources, userSources);
    }
}
