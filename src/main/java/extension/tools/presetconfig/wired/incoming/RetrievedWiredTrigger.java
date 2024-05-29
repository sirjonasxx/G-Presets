package extension.tools.presetconfig.wired.incoming;

import extension.tools.presetconfig.wired.PresetWiredTrigger;
import gearth.protocol.HPacket;

import java.util.List;

public class RetrievedWiredTrigger extends PresetWiredTrigger implements RetrievedWired {

    private final int typeId;

    public RetrievedWiredTrigger(int wiredId, List<Integer> options, String stringConfig, List<Integer> items, int typeId, List<Integer> pickedFurniSources, List<Integer> pickedUserSources, List<Long> variableIds) {
        super(wiredId, options, stringConfig, items, pickedFurniSources, pickedUserSources, variableIds);
        this.typeId = typeId;
    }

    @Override
    public int getTypeId() {
        return typeId;
    }

    public static RetrievedWiredTrigger fromPacket(HPacket packet) {
        return RetrievedWired.fromPacket(packet, RetrievedWiredTrigger.class);
    }
}
