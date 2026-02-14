package game.itemstate;

import extension.logger.Logger;
import gearth.extensions.IExtension;
import gearth.extensions.parsers.HFloorItem;
import gearth.extensions.parsers.HPoint;
import gearth.extensions.parsers.stuffdata.IStuffData;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import utils.Callback;

public class FloorItemState extends ItemState<HFloorItem> {
    public FloorItemState(IExtension extension, Logger logger, Callback onRoomStateChange) {
        super(logger, onRoomStateChange);

        extension.intercept(HMessage.Direction.TOCLIENT, "Objects", this::onItems);
        extension.intercept(HMessage.Direction.TOCLIENT, "ObjectAdd", this::onItemAdd);
        extension.intercept(HMessage.Direction.TOCLIENT, "ObjectRemove", this::onItemRemove);
        extension.intercept(HMessage.Direction.TOCLIENT, "ObjectUpdate", this::onItemUpdate);

        extension.intercept(HMessage.Direction.TOCLIENT, "SlideObjectBundle", this::onSlide);
        extension.intercept(HMessage.Direction.TOCLIENT, "WiredFurniMove", this::onFurniMove);
        extension.intercept(HMessage.Direction.TOCLIENT, "WiredMovements", this::onWiredMovements);

        extension.intercept(HMessage.Direction.TOCLIENT, "ObjectDataUpdate", this::onDataUpdate);
        extension.intercept(HMessage.Direction.TOCLIENT, "ObjectsDataUpdate", this::onDataUpdates);
    }

    @Override
    protected HFloorItem[] parseItems(HMessage message) {
        return HFloorItem.parse(message.getPacket());
    }

    @Override
    protected HFloorItem parseItem(HPacket packet) {
        return new HFloorItem(packet);
    }

    @Override
    protected HPoint getItemPosition(HFloorItem item) {
        return item.getTile();
    }

    @Override
    protected void setOwnerName(HFloorItem item, String ownerName) {
        item.setOwnerName(ownerName);
    }

    @Override
    protected String getParsedMessage() {
        return "Parsed floor items";
    }

    private void updateFurniPosition(int furniId, int newX, int newY, String newZ) {
        HFloorItem item = idToItem.get(furniId);
        if (item == null) return;

        itemMap.get(item.getTile().getX()).get(item.getTile().getY()).remove(item.getId());
        item.setTile(new HPoint(newX, newY, Double.parseDouble(newZ)));
        itemMap.get(newX).get(newY).put(item.getId(), item);
    }

    private void onSlide(HMessage message) {
        if (inRoom()) {
            HPacket packet = message.getPacket();
            int _oldX = packet.readInteger();
            int _oldY = packet.readInteger();
            int newX = packet.readInteger();
            int newY = packet.readInteger();

            int count = packet.readInteger();
            synchronized (lock) {
                for (int i = 0; i < count; i++) {
                    int furniId = packet.readInteger();
                    String _oldZ = packet.readString();
                    String newZ = packet.readString();

                    updateFurniPosition(furniId, newX, newY, newZ);
                }
            }
        }
    }

    private void onFurniMove(HMessage message) {
        if (inRoom()) {
            HPacket packet = message.getPacket();
            synchronized (lock) {
                int _oldX = packet.readInteger();
                int _oldY = packet.readInteger();
                int newX = packet.readInteger();
                int newY = packet.readInteger();

                String _oldZ = packet.readString();
                String newZ = packet.readString();

                int furniId = packet.readInteger();

                updateFurniPosition(furniId, newX, newY, newZ);
            }
        }
    }

    private void onWiredMovements(HMessage message) {
        if (inRoom()) {
            HPacket packet = message.getPacket();
            int count = packet.readInteger();
            synchronized (lock) {
                for (int i = 0; i < count; i++) {
                    switch (packet.readInteger()) {
                        case 0: // User
                            packet.skip("iiiissiiiii");
                            break;
                        case 1: // Floor furni
                            packet.skip("ii");
                            int newX = packet.readInteger();
                            int newY = packet.readInteger();
                            packet.skip("s");
                            String newZ = packet.readString();
                            int furniId = packet.readInteger();
                            packet.skip("ii");

                            updateFurniPosition(furniId, newX, newY, newZ);
                            break;
                        case 2: // Wall furni
                            packet.skip("iBiiiiiiiii");
                            break;
                        case 3: // User direction
                            packet.skip("iii");
                            break;
                    }
                }
            }
        }
    }

    private void onDataUpdate(HPacket packet, int itemId) {
        IStuffData stuff = IStuffData.read(packet);

        synchronized (lock) {
            if (inRoom() && idToItem.containsKey(itemId)) {
                HFloorItem item = idToItem.get(itemId);
                item.setStuff(stuff);
            }
        }
    }

    private void onDataUpdates(HMessage message) {
        HPacket packet = message.getPacket();
        int count = packet.readInteger();
        for (int i = 0; i < count; i++) {
            int id = packet.readInteger();
            onDataUpdate(packet, id);
        }
    }

    private void onDataUpdate(HMessage message) {
        HPacket packet = message.getPacket();
        onDataUpdate(packet, Integer.parseInt(packet.readString()));
    }
}
