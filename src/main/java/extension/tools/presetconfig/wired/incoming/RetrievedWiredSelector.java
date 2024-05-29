package extension.tools.presetconfig.wired.incoming;

import extension.tools.presetconfig.wired.PresetWiredSelector;
import gearth.protocol.HPacket;

import java.util.List;

public class RetrievedWiredSelector extends PresetWiredSelector implements RetrievedWired {

    private final int typeId;

    public RetrievedWiredSelector(int wiredId, List<Integer> options, String stringConfig, List<Integer> items, int typeId, boolean filter, boolean invert, List<Integer> pickedFurniSources, List<Integer> pickedUserSources, List<Long> variableIds) {
        super(wiredId, options, stringConfig, items, filter, invert, pickedFurniSources, pickedUserSources, variableIds);
        this.typeId = typeId;
    }

    @Override
    public int getTypeId() {
        return typeId;
    }

    public static RetrievedWiredSelector fromPacket(HPacket packet) {
        return RetrievedWired.fromPacket(packet, RetrievedWiredSelector.class);
    }
}
