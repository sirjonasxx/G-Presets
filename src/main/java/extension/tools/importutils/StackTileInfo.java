package extension.tools.importutils;

import gearth.extensions.parsers.HPoint;

public class StackTileInfo {

    private int furniId;

    private HPoint location;
    private int rotation;

    private int dimension;  // -1 means 1x2

    public StackTileInfo(int furniId, HPoint location, int rotation, int dimension) {
        this.furniId = furniId;
        this.location = location;
        this.rotation = rotation;
        this.dimension = dimension;
    }

    public int getFurniId() {
        return furniId;
    }

    public HPoint getLocation() {
        return location;
    }

    public int getRotation() {
        return rotation;
    }

    public int getDimension() {
        return dimension;
    }
}
