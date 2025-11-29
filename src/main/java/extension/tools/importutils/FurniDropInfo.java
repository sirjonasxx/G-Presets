package extension.tools.importutils;

import extension.tools.postconfig.ItemSource;

public class FurniDropInfo {
    private int x;
    private int y;
    private int typeId;
    private ItemSource itemSource; // may be customizable per furni in the future
    private int rotation;

    public FurniDropInfo(int x, int y, int typeId, ItemSource itemSource, int rotation) {
        this.x = x;
        this.y = y;
        this.typeId = typeId;
        this.itemSource = itemSource;
        this.rotation = rotation;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getTypeId() {
        return typeId;
    }

    public ItemSource getItemSource() {
        return itemSource;
    }

    public int getRotation() {
        return rotation;
    }
}
