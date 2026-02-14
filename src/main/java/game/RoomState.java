package game;

import extension.logger.Logger;
import furnidata.FurniDataTools;
import game.itemstate.FloorItemState;
import game.itemstate.WallItemState;
import gearth.extensions.ExtensionBase;
import gearth.extensions.IExtension;
import gearth.extensions.parsers.HFloorItem;
import gearth.extensions.parsers.HWallItem;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import utils.Callback;

import java.util.*;

public class RoomState {

    private final Object lock = new Object();

    private final Callback onRoomStateChange;
    private final Callback onRoomLeave;
    private final Logger logger;

    private long latestRequestTimestamp = -1;

    private volatile int roomId;
    private volatile int[][] heightmap = null; // 256 * 256

    private final FloorItemState floorItemState;
    private final WallItemState wallItemState;

    private volatile char[][] floorplan = null;

    public RoomState(IExtension extension, Logger logger, Callback onRoomStateChange, Callback onRoomLeave) {
        this.logger = logger;
        this.onRoomStateChange = onRoomStateChange;
        this.onRoomLeave = onRoomLeave;
        this.floorItemState = new FloorItemState(extension, logger, onRoomStateChange);
        this.wallItemState = new WallItemState(extension, logger, onRoomStateChange);

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
        synchronized (lock) {
            roomId = hMessage.getPacket().readInteger();
        }

        if (latestRequestTimestamp > System.currentTimeMillis() - 400) {
            hMessage.setBlocked(true); // request wasnt made by user
            latestRequestTimestamp = -1;
        }

        onRoomStateChange.call();
    }

    public boolean inRoom() {
        return floorItemState.inRoom() && wallItemState.inRoom() && floorplan != null && heightmap != null && roomId != 0;
    }

    public void reset() {
        if (heightmap != null || floorItemState.inRoom() || wallItemState.inRoom() || floorplan != null) {
            synchronized (lock) {
                heightmap = null;
                floorItemState.reset();
                wallItemState.reset();
                floorplan = null;
                roomId = 0;
            }
            onRoomLeave.call();
            onRoomStateChange.call();
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

    public void requestRoom(ExtensionBase ext) {
        latestRequestTimestamp = System.currentTimeMillis();
        ext.sendToServer(new HPacket("GetHeightMap", HMessage.Direction.TOSERVER));
    }

    public HFloorItem floorFurniFromId(int id) {
        return floorItemState.furniFromId(id);
    }

    public HWallItem wallItemFromId(int id) {
        return wallItemState.furniFromId(id);
    }

    public List<HFloorItem> getFloorFurniOnTile(int x, int y) {
        return floorItemState.getFurniOnTile(x, y);
    }

    public List<HWallItem> getWallFurniOnTile(int x, int y) {
        return wallItemState.getFurniOnTile(x, y);
    }

    public double getTileHeight(int x, int y) {
        synchronized (lock) {
            return ((double)(heightmap[x][y] & 16383)) / 256;
        }
    }

    public List<HFloorItem> getFloorItemsFromType(FurniDataTools furniDataTools, String furniName) {
        if (furniDataTools.isReady()) {
            int typeId = furniDataTools.getFloorTypeId(furniName);
            return floorItemState.getItemsByType(typeId);
        }
        return new ArrayList<>();
    }

    public List<HWallItem> getWallItemsFromType(FurniDataTools furniDataTools, String furniName) {
        if (furniDataTools.isReady()) {
            int typeId = furniDataTools.getFloorTypeId(furniName);
            return wallItemState.getItemsByType(typeId);
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

    public int getRoomId() {
        return roomId;
    }
}
