package extension.tools.importutils;

public class FurniMoveInfo {

    private int furniId;
    private int x;
    private int y;
    private int rotation;
    private boolean useStackTile;

    public FurniMoveInfo(int furniId, int x, int y, int rotation, boolean useStackTile) {
        this.furniId = furniId;
        this.x = x;
        this.y = y;
        this.rotation = rotation;
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

    public boolean useStackTile() {
        return useStackTile;
    }
}
