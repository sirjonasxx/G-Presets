package game.itemstate;

import extension.logger.Logger;
import gearth.extensions.IExtension;
import gearth.extensions.parsers.HPoint;
import gearth.extensions.parsers.HWallItem;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import utils.Callback;
import utils.WallPosition;

public class WallItemState extends ItemState<HWallItem> {
    public WallItemState(IExtension extension, Logger logger, Callback onRoomStateChange) {
        super(logger, onRoomStateChange);

        extension.intercept(HMessage.Direction.TOCLIENT, "Items", this::onItems);
        extension.intercept(HMessage.Direction.TOCLIENT, "ItemAdd", this::onItemAdd);
        extension.intercept(HMessage.Direction.TOCLIENT, "ItemRemove", this::onItemRemove);
        extension.intercept(HMessage.Direction.TOCLIENT, "ItemUpdate", this::onItemUpdate);

        extension.intercept(HMessage.Direction.TOCLIENT, "WiredWallItemMove", this::onWiredWallItemMove);
        extension.intercept(HMessage.Direction.TOCLIENT, "WiredMovements", this::onWiredMovements);

        extension.intercept(HMessage.Direction.TOCLIENT, "ItemDataUpdate", this::onDataUpdate);
    }

    @Override
    protected HWallItem[] parseItems(HMessage message) {
        return HWallItem.parse(message.getPacket());
    }

    @Override
    protected HWallItem parseItem(HPacket packet) {
        return new HWallItem(packet);
    }

    @Override
    protected HPoint getItemPosition(HWallItem item) {
        WallPosition wallPosition = new WallPosition(item.getLocation());
        return new HPoint(wallPosition.getX(), wallPosition.getY());
    }

    @Override
    protected void setOwnerName(HWallItem item, String ownerName) {
        item.setOwnerName(ownerName);
    }

    @Override
    protected String getParsedMessage() {
        return "Parsed wall items";
    }

    private void updateFurniPosition(int furniId, int newWallX, int newWallY, int newOffsetX, int newOffsetY) {
        HWallItem item = idToItem.get(furniId);
        if (item == null) return;

        WallPosition oldPositon = new WallPosition(item.getLocation());
        itemMap.get(oldPositon.getX()).get(oldPositon.getY()).remove(item.getId());
        WallPosition newPosition = new WallPosition(newWallX, newWallY, newOffsetX, newOffsetY, oldPositon.getDirection(), 0);
        item.setLocation(newPosition.toString());
    }

    private void onWiredWallItemMove(HMessage message) {
        if (inRoom()) {
            HPacket packet = message.getPacket();
            synchronized (lock) {
                int furniId = Integer.parseInt(packet.readString());
                packet.skip("iiii");
                int newWallX = packet.readInteger();
                int newWallY = packet.readInteger();
                int newOffsetX = packet.readInteger();
                int newOffsetY = packet.readInteger();
                updateFurniPosition(furniId, newWallX, newWallY, newOffsetX, newOffsetY);
            }
        }
    }

    private void onWiredMovements(HMessage message) {
        if (inRoom()) {
            HPacket packet = message.getPacket();
            int count = packet.readInteger();
            synchronized (lock) {
                for (int i = 0; i < count; i ++) {
                    switch(packet.readInteger()) {
                        case 0: // User
                            packet.skip("iiiissiiiii");
                            break;
                        case 1: // Floor furni
                            packet.skip("iiiissiii");
                            break;
                        case 2: // Wall furni
                            int furniId = packet.readInteger();
                            packet.skip("biiii");
                            int newWallX = packet.readInteger();
                            int newWallY = packet.readInteger();
                            int newOffsetX = packet.readInteger();
                            int newOffsetY = packet.readInteger();
                            packet.skip("i");

                            updateFurniPosition(furniId, newWallX, newWallY, newOffsetX, newOffsetY);
                            break;
                        case 3:
                            packet.skip("iii");
                            break;
                    }
                }
            }
        }
    }

    private void onDataUpdate(HMessage message) {
        HPacket packet = message.getPacket();
        int id = Integer.parseInt(packet.readString());
        synchronized (lock) {
            HWallItem item = idToItem.get(id);
            if (item == null) return;

            item.setState(packet.readString());
        }
    }
}
