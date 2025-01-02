package extension.tools.presetconfig.wired.incoming;

import extension.parsers.HWiredContext;
import extension.tools.presetconfig.wired.PresetWiredAddon;
import gearth.protocol.HPacket;

import java.util.List;

public class RetrievedWiredAddon extends PresetWiredAddon implements RetrievedWired {

    private final int typeId;

    public RetrievedWiredAddon(int wiredId, List<Integer> options, String stringConfig, List<Integer> items, int typeId, List<Integer> pickedFurniSources, List<Integer> pickedUserSources, List<String> variableIds, HWiredContext wiredContext) {
        super(wiredId, options, stringConfig, items, pickedFurniSources, pickedUserSources, variableIds);
        this.typeId = typeId;
    }

    @Override
    public int getTypeId() {
        return typeId;
    }

    public static RetrievedWiredAddon fromPacket(HPacket packet) {
        return RetrievedWired.fromPacket(packet, RetrievedWiredAddon.class);
    }
}
