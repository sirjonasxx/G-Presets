package extension.tools.presetconfig.wired.incoming;

import extension.parsers.HWiredContext;
import extension.tools.presetconfig.wired.PresetWiredSelector;
import gearth.protocol.HPacket;
import java.util.List;

public class RetrievedWiredSelector extends PresetWiredSelector implements RetrievedWired {

    private final int typeId;

    public RetrievedWiredSelector(
            int wiredId,
            List<Integer> options,
            String stringConfig,
            List<Integer> items,
            List<Integer> secondItems,
            int typeId,
            boolean filter,
            boolean invert,
            List<Integer> pickedFurniSources,
            List<Integer> pickedUserSources,
            List<String> variableIds,
            HWiredContext wiredContext) {
        super(
                wiredId,
                options,
                stringConfig,
                items,
                secondItems,
                filter,
                invert,
                pickedFurniSources,
                pickedUserSources,
                variableIds);
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
