package game.itemstate;

import extension.logger.Logger;
import gearth.extensions.parsers.HPoint;
import gearth.extensions.parsers.IFurni;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import utils.Callback;

import java.util.*;

public abstract class ItemState<Item extends IFurni> {
    protected final Object lock = new Object();

    protected final Callback onRoomStateChange;
    protected final Logger logger;

    protected volatile Map<Integer, Item> idToItem = null;
    protected volatile Map<Integer, Set<Item>> typeIdToItems = null;
    protected volatile List<List<Map<Integer, Item>>> itemMap = null;

    public ItemState(Logger logger, Callback onRoomStateChange) {
        this.onRoomStateChange = onRoomStateChange;
        this.logger = logger;
    }

    protected abstract Item[] parseItems(HMessage message);

    protected abstract Item parseItem(HPacket packet);

    protected abstract HPoint getItemPosition(Item item);

    protected abstract void setOwnerName(Item item, String ownerName);

    protected abstract String getParsedMessage();

    public boolean inRoom() {
        return itemMap != null;
    }

    public void reset() {
        if (itemMap != null) {
            synchronized (lock) {
                idToItem = null;
                typeIdToItems = null;
                itemMap = null;
            }
        }
    }

    protected void onItems(HMessage message) {
        Item[] items = parseItems(message);

        synchronized (lock) {
            if (itemMap == null) {
                idToItem = new HashMap<>();
                itemMap = new ArrayList<>();
                typeIdToItems = new HashMap<>();

                for (int i = 0; i < 130; i++) {
                    itemMap.add(new ArrayList<>());
                    for (int j = 0; j < 130; j++) {
                        itemMap.get(i).add(new HashMap<>());
                    }
                }
            }

            Arrays.stream(items).forEach(this::addItem);
        }

        onRoomStateChange.call();
        logger.log(getParsedMessage(), "blue");
    }

    protected void onItemRemove(HMessage message) {
        if (inRoom()) {
            HPacket packet = message.getPacket();
            int itemId = Integer.parseInt(packet.readString());
            removeItem(itemId);
            onRoomStateChange.call();
        }
    }

    private void removeItem(int itemId) {
        synchronized (lock) {
            Item item = idToItem.remove(itemId);
            if (item != null) {
                HPoint position = getItemPosition(item);
                itemMap.get(position.getX()).get(position.getY()).remove(item.getId());
                typeIdToItems.get(item.getTypeId()).remove(item);
            }
        }
    }

    protected void onItemAdd(HMessage message) {
        if (inRoom()) {
            addItem(message.getPacket(), null);
            onRoomStateChange.call();
        }
    }

    private void addItem(HPacket packet, String ownerName) {
        synchronized (lock) {
            Item item = parseItem(packet);
            if (ownerName == null) {
                ownerName = packet.readString();
            }
            setOwnerName(item, ownerName);

            addItem(item);
        }
    }

    private void addItem(Item item) {
        HPoint position = getItemPosition(item);
        itemMap.get(position.getX()).get(position.getY()).put(item.getId(), item);
        idToItem.put(item.getId(), item);
        typeIdToItems.putIfAbsent(item.getTypeId(), new HashSet<>());
        typeIdToItems.get(item.getTypeId()).add(item);
    }

    protected void onItemUpdate(HMessage message) {
        if (inRoom()) {
            Item item = parseItem(message.getPacket());

            Item old =  idToItem.get(item.getId());
            String owner = "";
            if (old != null) {
                owner = old.getOwnerName();
            }

            removeItem(item.getId());
            message.getPacket().resetReadIndex();
            addItem(message.getPacket(), owner);
        }
    }

    public Item furniFromId(int id) {
        synchronized (lock) {
            return idToItem.get(id);
        }
    }

    public List<Item> getFurniOnTile(int x, int y) {
        synchronized (lock) {
            if (inRoom()) {
                return new ArrayList<>(itemMap.get(x).get(y).values());
            }
        }
        return new ArrayList<>();
    }

    public List<Item> getItemsByType(int typeId) {
        synchronized (lock) {
            if (inRoom()) {
                Set<Item> result = typeIdToItems.get(typeId);
                return result == null ? new ArrayList<>() : new ArrayList<>(result);
            }
        }
        return new ArrayList<>();
    }
}
