package extension.tools.importutils;

import extension.tools.presetconfig.wired.PresetWiredBase;
import java.util.List;
import java.util.Map;

public class WiredSaveInfo {

    private PresetWiredBase wiredBase;

    private Map<Integer, String> furniStateSets;
    private List<FurniMoveInfo> furniMoveInfos;

    public WiredSaveInfo(
            PresetWiredBase wiredBase,
            Map<Integer, String> furniStateSets,
            List<FurniMoveInfo> furniMoveInfos) {
        this.wiredBase = wiredBase;
        this.furniStateSets = furniStateSets;
        this.furniMoveInfos = furniMoveInfos;
    }
}
