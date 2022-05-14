package extension.tools.postconfig;

import gearth.extensions.parsers.HPoint;

import java.util.HashMap;
import java.util.Map;

public class FurniPostConfig {

    // identifier created by BuildingPreset
    private final String furniIdentifier;

    // use existing furni in room instead of placing new one
    // standard rotation/state/location will not be changed in this case
    private final boolean useExistingFurni;
    private final int existingFurniId;

    // following rules only apply if useExistingFurni = false
    private final String overrideClassName;
    private final HPoint overrideLocation;
    private final Integer overrideRotation;
    private final String overrideState;

    private final Map<Integer, Integer> wiredRotationMapping;
    private final Map<String, String> wiredStateMapping;
//    private final Map<HPoint, HPoint> wiredLocationMapping;

    public FurniPostConfig(String furniIdentifier, boolean useExistingFurni, int existingFurniId, String overrideClassName, HPoint overrideLocation, Integer overrideRotation, String overrideState, Map<Integer, Integer> wiredRotationMapping, Map<String, String> wiredStateMapping/*, Map<HPoint, HPoint> wiredLocationMapping*/) {
        this.furniIdentifier = furniIdentifier;
        this.useExistingFurni = useExistingFurni;
        this.existingFurniId = existingFurniId;
        this.overrideClassName = overrideClassName;
        this.overrideLocation = overrideLocation;
        this.overrideRotation = overrideRotation;
        this.overrideState = overrideState;
        this.wiredRotationMapping = wiredRotationMapping;
        this.wiredStateMapping = wiredStateMapping;
//        this.wiredLocationMapping = wiredLocationMapping;
    }

    public FurniPostConfig(String furniIdentifier) {
        // initializes empty config
        this.furniIdentifier = furniIdentifier;
        useExistingFurni = false;
        existingFurniId = -1;

        overrideClassName = null;
        overrideLocation = null;
        overrideRotation = null;
        overrideState = null;

        wiredRotationMapping = new HashMap<>();
        wiredStateMapping = new HashMap<>();
//        wiredLocationMapping = new HashMap<>();
    }


    public String getFurniIdentifier() {
        return furniIdentifier;
    }

    public boolean isUseExistingFurni() {
        return useExistingFurni;
    }

    public int getExistingFurniId() {
        return existingFurniId;
    }

    public String getOverrideClassName() {
        return overrideClassName;
    }

    public HPoint getOverrideLocation() {
        return overrideLocation;
    }

    public Integer getOverrideRotation() {
        return overrideRotation;
    }

    public String getOverrideState() {
        return overrideState;
    }

    public Map<Integer, Integer> getWiredRotationMapping() {
        return wiredRotationMapping;
    }

    public Map<String, String> getWiredStateMapping() {
        return wiredStateMapping;
    }

//    public Map<HPoint, HPoint> getWiredLocationMapping() {
//        return wiredLocationMapping;
//    }
}
