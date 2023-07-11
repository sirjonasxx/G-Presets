package extension.tools.importutils;

public class FurniMoveInfo {

    private int furniId;
    private int x;
    private int y;
    private int rotation;
    private boolean useStackTile;
    private Integer altitude;

    public FurniMoveInfo(int furniId, int x, int y, int rotation, Integer altitude, boolean useStackTile) {
        this.furniId = furniId;
        this.x = x;
        this.y = y;
        this.rotation = rotation;
        this.altitude = altitude;
        this.useStackTile = useStackTile;
    }

    public int getFurniId() {
        return furniId;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getRotation() {
        return rotation;
    }

    public Integer getAltitude() {
        return altitude;
    }

    public boolean useStackTile() {
        return useStackTile;
    }
}
