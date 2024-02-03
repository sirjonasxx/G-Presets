package game;

import extension.logger.Logger;
import furnidata.FurniDataTools;
import gearth.extensions.ExtensionBase;
import gearth.extensions.IExtension;
import gearth.extensions.parsers.HFloorItem;
import gearth.extensions.parsers.HPoint;
import gearth.extensions.parsers.stuffdata.IStuffData;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import utils.Callback;

import java.util.*;
import java.util.function.Consumer;

public class FloorState {

    private final Object lock = new Object();

    private Callback onFurnisChange;
    private Callback onRoomLeave;
    private Logger logger;

    private long latestRequestTimestamp = -1;

    private volatile int[][] heightmap = null; // 256 * 256
    private volatile Map<Integer, HFloorItem> furniIdToItem = null;
    private volatile Map<Integer, Set<HFloorItem>> typeIdToItems = null;
    private volatile List<List<Map<Integer, HFloorItem>>> furnimap = null;
    private volatile char[][] floorplan = null;


    private volatile Map<Integer, Set<Consumer<HFloorItem>>> stateUpdateListeners = new HashMap<>();


    public FloorState(IExtension extension, Logger logger, Callback onFloorItemsChange, Callback onRoomLeave) {
        this.logger = logger;
        this.onFurnisChange = onFloorItemsChange;
        this.onRoomLeave = onRoomLeave;
        extension.intercept(HMessage.Direction.TOCLIENT, "Objects", this::parseFloorItems);

        extension.intercept(HMessage.Direction.TOCLIENT, "ObjectAdd", this::onObjectAdd);
        extension.intercept(HMessage.Direction.TOCLIENT, "ObjectRemove", this::onObjectRemove);
        extension.intercept(HMessage.Direction.TOCLIENT, "ObjectUpdate", this::onObjectUpdate);
        extension.intercept(HMessage.Direction.TOCLIENT, "SlideObjectBundle", this::onSlide);
        extension.intercept(HMessage.Direction.TOCLIENT, "WiredFurniMove", this::onFurniMove);
        extension.intercept(HMessage.Direction.TOCLIENT, "WiredMovements", this::onWiredMovements);

        extension.intercept(HMessage.Direction.TOCLIENT, "ObjectDataUpdate", this::onDataUpdate);
        extension.intercept(HMessage.Direction.TOCLIENT, "ObjectsDataUpdate", this::onDataUpdates);

        extension.intercept(HMessage.Direction.TOCLIENT, "HeightMap", this::parseHeightmap);
        extension.intercept(HMessage.Direction.TOCLIENT, "HeightMapUpdate", this::heightmapUpdate);

        extension.intercept(HMessage.Direction.TOCLIENT, "FloorHeightMap", this::parseFloorPlan);

        extension.intercept(HMessage.Direction.TOCLIENT, "RoomEntryInfo", this::roomEntryInfo);


        extension.intercept(HMessage.Direction.TOCLIENT, "CloseConnection", m -> reset());
        extension.intercept(HMessage.Direction.TOSERVER, "Quit", m -> reset());
        extension.intercept(HMessage.Direction.TOCLIENT, "RoomReady", m -> reset());
    }

    private void parseFloorPlan(HMessage hMessage) {
        synchronized (lock) {
            HPacket packet = hMessage.getPacket();
            packet.readByte();
            packet.readInteger();
            String raw = packet.readString();
            String[] split = raw.split("\r");
            floorplan = new char[split[0].length()][];
            for (int x = 0; x < split[0].length(); x++) {
                floorplan[x] = new char[split.length];
                for (int y = 0; y < split.length; y++) {
                    floorplan[x][y] = split[y].charAt(x);
                }
            }
        }

        logger.log("Parsed floorplan", "blue");
    }

    private void roomEntryInfo(HMessage hMessage) {
        if (latestRequestTimestamp > System.currentTimeMillis() - 400) {
            hMessage.setBlocked(true); // request wasnt made by user
            latestRequestTimestamp = -1;
        }
    }

    public boolean inRoom() {
        return furnimap != null && floorplan != null && heightmap != null;
    }
    public void reset() {
        if (heightmap != null || furnimap != null || floorplan != null) {
            synchronized (lock) {
                heightmap = null;
                furniIdToItem = null;
                furnimap = null;
                floorplan = null;
                typeIdToItems = null;
            }
            onRoomLeave.call();
            onFurnisChange.call();
        }
    }

    private void parseHeightmap(HMessage hMessage) {
        HPacket packet = hMessage.getPacket();

        int columns = packet.readInteger();
        int tiles = packet.readInteger();
        int rows = tiles/columns;

        int[][] heightmap = new int[columns][];
        for (int col = 0; col < columns; col++) {
            heightmap[col] = new int[rows];
        }

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                heightmap[col][row] = packet.readUshort();
            }
        }

        synchronized (lock) {
            this.heightmap = heightmap;
        }

        logger.log("Parsed heightmap", "blue");
    }
    private void heightmapUpdate(HMessage hMessage) {
        if (heightmap != null) {
            HPacket packet = hMessage.getPacket();
            int updates = packet.readByte();

            for (int i = 0; i < updates; i++) {
                int x = packet.readByte();
                int y = packet.readByte();
                int height = packet.readUshort();
                heightmap[x][y] = height;
            }
        }
    }

    private void parseFloorItems(HMessage hMessage) {
        HFloorItem[] floorItems = HFloorItem.parse(hMessage.getPacket());

        synchronized (lock) {
            if (furnimap == null) {
                furniIdToItem = new HashMap<>();
                furnimap = new ArrayList<>();
                typeIdToItems = new HashMap<>();

                for (int i = 0; i < 130; i++) {
                    furnimap.add(new ArrayList<>());
                    for (int j = 0; j < 130; j++) {
                        furnimap.get(i).add(new HashMap<>());
                    }
                }
            }

            Arrays.stream(floorItems).forEach(this::addObject);
        }

        onFurnisChange.call();
        logger.log("Parsed floor items", "blue");
    }

    private void onObjectRemove(HMessage hMessage) {
        if (inRoom()) {
            HPacket packet = hMessage.getPacket();
            int furniid = Integer.parseInt(packet.readString());
            removeObject(furniid);
            onFurnisChange.call();
        }
    }
    private void removeObject(int furniId) {
        synchronized (lock) {
            HFloorItem item = furniIdToItem.remove(furniId);
            if (item != null) {
                furnimap.get(item.getTile().getX()).get(item.getTile().getY()).remove(item.getId());
                typeIdToItems.get(item.getTypeId()).remove(item);
            }
        }
    }
    private void onObjectAdd(HMessage hMessage) {
        if (inRoom()) {
            addObject(hMessage.getPacket(), null);
            onFurnisChange.call();
        }

    }
    private void addObject(HPacket packet, String ownerName) {
        synchronized (lock) {
            HFloorItem item = new HFloorItem(packet);
            if (ownerName == null) {
                ownerName = packet.readString();
            }
            item.setOwnerName(ownerName);

            addObject(item);
        }
    }

    private void addObject(HFloorItem item) {
        furnimap.get(item.getTile().getX()).get(item.getTile().getY()).put(item.getId(), item);
        furniIdToItem.put(item.getId(), item);
        if (!typeIdToItems.containsKey(item.getTypeId())) {
            typeIdToItems.put(item.getTypeId(), new HashSet<>());
        }
        typeIdToItems.get(item.getTypeId()).add(item);

    }

    private void onObjectUpdate(HMessage hMessage) {
        if (inRoom()) {
            HFloorItem newItem = new HFloorItem(hMessage.getPacket());

            HFloorItem old = furniIdToItem.get(newItem.getId());
            String owner = "";
            if (old != null) {
                owner = old.getOwnerName();
            }

            removeObject(newItem.getId());
            hMessage.getPacket().resetReadIndex();
            addObject(hMessage.getPacket(), owner);
        }
    }

    private void onSlide(HMessage hMessage) {
        if (inRoom()) {
            HPacket packet = hMessage.getPacket();
            int oldX = packet.readInteger();
            int oldY = packet.readInteger();
            int newX = packet.readInteger();
            int newY = packet.readInteger();

            int amount = packet.readInteger();

            synchronized (lock) {
                for (int i = 0; i < amount; i++) {
                    int furniId = packet.readInteger();
                    String oldZ = packet.readString();
                    String newZ = packet.readString();

                    updateFurniPosition(furniId, newX, newY, newZ);
                }
            }

//            int roller = packet.readInteger();
        }
    }

    private void onFurniMove(HMessage hMessage) {
        if (inRoom()) {
            HPacket packet = hMessage.getPacket();
            synchronized (lock) {
                int oldX = packet.readInteger();
                int oldY = packet.readInteger();
                int newX = packet.readInteger();
                int newY = packet.readInteger();

                String oldZ = packet.readString();
                String newZ = packet.readString();

                int furniId = packet.readInteger();

                updateFurniPosition(furniId, newX, newY, newZ);
            }
        }
    }

    private void updateFurniPosition(int furniId, int newX, int newY, String newZ) {
        HFloorItem item = furniIdToItem.get(furniId);
        if (item != null) {
            furnimap.get(item.getTile().getX()).get(item.getTile().getY()).remove(item.getId());
            item.setTile(new HPoint(newX, newY, Double.parseDouble(newZ)));
            furnimap.get(newX).get(newY).put(item.getId(), item);
        }
    }

    private void onWiredMovements(HMessage hMessage) {
        if (inRoom()) {
            HPacket packet = hMessage.getPacket();
            synchronized (lock) {
                int count = packet.readInteger();
                for (int i = 0; i < count; i++) {
                    int type = packet.readInteger();
                    if (type == 0) { // user
                        packet.readInteger();
                        packet.readInteger();
                        packet.readInteger();
                        packet.readInteger();
                        packet.readString();
                        packet.readString();

                        packet.readInteger();
                        packet.readInteger();
                        packet.readInteger();
                        packet.readInteger();
                        packet.readInteger();
                    }
                    else if (type == 1) { // furni
                        int oldX = packet.readInteger();
                        int oldY = packet.readInteger();
                        int newX = packet.readInteger();
                        int newY = packet.readInteger();

                        String oldZ = packet.readString();
                        String newZ = packet.readString();

                        int furniId = packet.readInteger();
                        int animationTime = packet.readInteger();
                        int direction = packet.readInteger();

                        updateFurniPosition(furniId, newX, newY, newZ);
                    }
                    else { // wall item
                        packet.readInteger();
                        packet.readBoolean();

                        packet.readInteger();
                        packet.readInteger();
                        packet.readInteger();
                        packet.readInteger();
                        packet.readInteger();
                        packet.readInteger();
                        packet.readInteger();
                        packet.readInteger();
                        packet.readInteger();
                    }
                }
            }
        }
    }


    private void onDataUpdate(HPacket hPacket, int id) {
        IStuffData stuff = IStuffData.read(hPacket);

        HFloorItem item = null;
        List<Consumer<HFloorItem>> listeners = null;

        synchronized (lock) {
            if (inRoom() && furniIdToItem.containsKey(id)) {
                item = furniIdToItem.get(id);
                item.setStuff(stuff);

                if (stateUpdateListeners.containsKey(id)) {
                    listeners = new ArrayList<>(stateUpdateListeners.get(id));
                }
            }
        }

        if (listeners != null) {
            for (Consumer<HFloorItem> l : listeners) {
                l.accept(item);
            }
        }

    }

    private void onDataUpdates(HMessage hMessage) {
        HPacket packet = hMessage.getPacket();
        int updatesCount = packet.readInteger();
        for (int i = 0; i < updatesCount; i++) {
            int id = packet.readInteger();
            onDataUpdate(packet, id);
        }
    }

    private void onDataUpdate(HMessage hMessage) {
        HPacket packet = hMessage.getPacket();
        onDataUpdate(hMessage.getPacket(), Integer.parseInt(packet.readString()));
    }

    public void requestRoom(ExtensionBase ext) {
        latestRequestTimestamp = System.currentTimeMillis();
        ext.sendToServer(new HPacket("GetHeightMap", HMessage.Direction.TOSERVER));
    }

    public HFloorItem furniFromId(int id) {
        synchronized (lock) {
            return furniIdToItem.get(id);
        }
    }

    public List<HFloorItem> getFurniOnTile(int x, int y) {
        synchronized (lock) {
            if (inRoom()) {
                return new ArrayList<>(furnimap.get(x).get(y).values());
            }
        }
        return new ArrayList<>();
    }

    public double getTileHeight(int x, int y) {
        synchronized (lock) {
            return ((double)(heightmap[x][y] & 16383)) / 256;
        }
    }

    public List<HFloorItem> getItemsFromType(FurniDataTools furniDataTools, String furniName) {
        if (furniDataTools.isReady()) {
            int typeId = furniDataTools.getFloorTypeId(furniName);
            return getItemsFromType(typeId);
        }
        return new ArrayList<>();
    }

    public List<HFloorItem> getItemsFromType(int typeId) {
        synchronized (lock) {
            if (inRoom()) {
                Set<HFloorItem> result = typeIdToItems.get(typeId);
                return result == null ? new ArrayList<>() : new ArrayList<>(result);
            }
        }
        return new ArrayList<>();
    }

    public char floorHeight(int x, int y) {
        char result;
        synchronized (lock) {
            result = (floorplan != null && x >= 0 && y >= 0 &&
                    x < floorplan.length && y < floorplan[x].length) ? floorplan[x][y] : 'x';
        }
        return result;
    }

    public void addItemStateListener(int furniId, Consumer<HFloorItem> listener) {
        synchronized (lock) {
            if (!stateUpdateListeners.containsKey(furniId)) {
                stateUpdateListeners.put(furniId, new HashSet<>());
            }
            stateUpdateListeners.get(furniId).add(listener);
        }
    }

    public void removeItemStateListener(int furniId, Consumer<HFloorItem> listener) {
        synchronized (lock) {
            if (stateUpdateListeners.containsKey(furniId)) {
                stateUpdateListeners.get(furniId).remove(listener);
                if (stateUpdateListeners.get(furniId).size() == 0) {
                    stateUpdateListeners.remove(furniId);
                }
            }
        }
    }
}
