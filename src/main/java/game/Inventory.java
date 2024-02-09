package game;

import extension.logger.Logger;
import gearth.extensions.IExtension;
import gearth.extensions.parsers.HInventoryItem;
import gearth.extensions.parsers.HProductType;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import utils.Callback;

import java.util.*;

public class Inventory {

    public enum InventoryState {
        UNAVAILABLE,
        LOADING,
        LOADED
    }

    private final Callback onInventoryStateChange;
    private final IExtension extension;
    private final Logger logger;

    private List<HInventoryItem> buffer = null;

    private Map<Integer, HInventoryItem> itemPlacements = new HashMap<>();
    private Map<Integer, Map<Integer, HInventoryItem>> wallItemsByType = new HashMap<>();
    private Map<Integer, Map<Integer, HInventoryItem>> floorItemsByType = new HashMap<>();

    private InventoryState state = InventoryState.UNAVAILABLE;
    private volatile boolean virtualRequest = false;

    public Inventory(IExtension extension, Logger logger, Callback onInventoryStateChange) {
        this.extension = extension;
        this.logger = logger;
        this.onInventoryStateChange = onInventoryStateChange;

        extension.intercept(HMessage.Direction.TOCLIENT, "FurniList", this::loadItems);
        extension.intercept(HMessage.Direction.TOCLIENT, "FurniListAddOrUpdate", (m) -> {
            HInventoryItem item = new HInventoryItem(m.getPacket());
            updateOrAddItem(item);
        });
        extension.intercept(HMessage.Direction.TOCLIENT, "FurniListRemove", (m) ->
                removeItem(m.getPacket().readInteger()));
    }

    public InventoryState getState() {
        return state;
    }

    private void loadItems(HMessage hMessage) {
        if (virtualRequest) {
            hMessage.setBlocked(true);
        }
        
        boolean stateChanged = false;

        HPacket inventoryLoadPacket = hMessage.getPacket();
        int total = inventoryLoadPacket.readInteger();
        int i = inventoryLoadPacket.readInteger();

        if (i == 0) {
            clear();
            logger.log((itemPlacements.isEmpty() ? "Loading" : "Updating") + " inventory...", "blue");
            buffer = new ArrayList<>();
            stateChanged = true;
            state = InventoryState.LOADING;
        }

        inventoryLoadPacket.resetReadIndex();
        HInventoryItem[] items = HInventoryItem.parse(inventoryLoadPacket);
        buffer.addAll(Arrays.asList(items));

        boolean inventoryComplete = i == total - 1;
        if (inventoryComplete) {
            buffer.forEach(this::updateOrAddItem);
            logger.log(String.format("Inventory loaded: found %d items.", itemPlacements.size()), "blue");
            buffer = null;
            stateChanged = true;
            virtualRequest = false;
            state = InventoryState.LOADED;
        }

        if (stateChanged) {
            onInventoryStateChange.call();
        }
    }

    private void updateOrAddItem(HInventoryItem item) {
        if (state != InventoryState.UNAVAILABLE) {
            itemPlacements.put(item.getPlacementId(), item);
            Map<Integer, Map<Integer, HInventoryItem>> map = item.getType() == HProductType.FloorItem ? floorItemsByType : wallItemsByType;
            if (!map.containsKey(item.getTypeId()))
                map.put(item.getTypeId(), new HashMap<>());
            map.get(item.getTypeId()).put(item.getId(), item);
        }
    }


    private void removeItem(int id) {
        if (state == InventoryState.LOADED) {
            HInventoryItem item = itemPlacements.remove(id);
            if (item != null) {
                if (item.getType() == HProductType.FloorItem) {
                    if (floorItemsByType.containsKey(item.getTypeId()))
                        floorItemsByType.get(item.getTypeId()).remove(item.getId());
                }
                else {
                    if (wallItemsByType.containsKey(item.getTypeId()))
                        wallItemsByType.get(item.getTypeId()).remove(item.getId());
                }
            }
        }
    }

    public List<HInventoryItem> getInventoryItems() {
        return new ArrayList<>(itemPlacements.values());
    }
    public List<HInventoryItem> getFloorItemsByType(int typeId) {
        Map<Integer, HInventoryItem> items = floorItemsByType.get(typeId);
        if (items == null)
            return Collections.emptyList();
        return new ArrayList<>(items.values());
    }

    public List<HInventoryItem> getWallItemsByType(int typeId) {
        Map<Integer, HInventoryItem> items = wallItemsByType.get(typeId);
        if (items == null)
            return Collections.emptyList();
        return new ArrayList<>(items.values());
    }

    public void clear() {
        buffer = null;
        itemPlacements.clear();
        floorItemsByType.clear();
        wallItemsByType.clear();

        state = InventoryState.UNAVAILABLE;
        onInventoryStateChange.call();
    }

    public void requestInventory() {
        clear();
        virtualRequest = true;
        extension.sendToServer(new HPacket("RequestFurniInventory", HMessage.Direction.TOSERVER));
    }
}
