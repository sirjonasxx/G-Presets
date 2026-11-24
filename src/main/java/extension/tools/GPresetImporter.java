package extension.tools;

import extension.GPresets;
import extension.parsers.HWiredVariable;
import extension.tools.importutils.*;
import extension.tools.postconfig.ItemSource;
import extension.tools.postconfig.PostConfig;
import extension.tools.presetconfig.PresetConfig;
import extension.tools.presetconfig.ads_bg.PresetAdsBackground;
import extension.tools.presetconfig.binding.PresetWiredFurniBinding;
import extension.tools.presetconfig.furni.PresetFurni;
import extension.tools.presetconfig.label.PresetFurniLabel;
import extension.tools.presetconfig.recolorable.PresetRecolorable;
import extension.tools.presetconfig.wired.*;
import furnidata.FurniDataTools;
import game.FloorState;
import game.Inventory;
import gearth.extensions.parsers.HFloorItem;
import gearth.extensions.parsers.HInventoryItem;
import gearth.extensions.parsers.HPoint;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import utils.StateExtractor;
import utils.Utils;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class GPresetImporter {

    private final Object lock = new Object();

    private GPresets extension;
    private List<String> needVariableIds = new ArrayList<>();
    private int variablesProcessed;
    private int variablesToProcess;
    private List<Integer> referenceVariables = new ArrayList<>();


    public enum BuildingImportState {
        NONE,

        // select a location ingame that will be used to move furniture to that need state adjustments
        // preferably your character stands next to it (for manipulating states for furni like switches)
        // the location may not contain any furniture
        AWAITING_UNOCCUPIED_SPACE,

        AWAITING_ROOT_LOCATION,

        ADD_UNSTACKABLES,   // add directly to correct location

        // find a position for the magic stack tile

        ADD_FURNITURE,      // add to placeholder location (magic stack tile)

        // check if all furniture were placed

        SETUP_ADS,
        SETUP_WIRED,
        MOVE_FURNITURE      // move to correct location
    }

    private PresetConfig presetConfig = null;
    private PostConfig postConfig = new PostConfig();


    private PresetConfig workingPresetConfig = null;
//    private PostConfig workingPostConfig = null;


    private Map<Integer, Integer> realFurniIdMap = null;
    private final Map<String, String> realVariableIdMap = new HashMap<>();

    // expect furni drops on location described by key(string) -> "x|y|typeId"
    private Map<String, LinkedList<Integer>> expectFurniDrops = null;
    private BuildingImportState state = BuildingImportState.NONE;


    private volatile int mainStackTile = -1;
    private volatile int mainStackDimensions = 0;
    private HPoint reservedSpace = null;
    private HPoint rootLocation = null;
    private HPoint stackTileLocation = null;

    private List<StackTileInfo> allAvailableStackTiles = null;
//    private HPoint originalStackTileLocation = new HPoint(0, 0); // move back after movements

    private int heightOffset = 0;

    public GPresetImporter(GPresets extension) {
        this.extension = extension;

        extension.intercept(HMessage.Direction.TOSERVER, "Chat", this::onChat);
        extension.intercept(HMessage.Direction.TOSERVER, "MoveAvatar", this::moveAvatar);

        extension.intercept(HMessage.Direction.TOCLIENT, "ObjectAdd", this::onObjectAdd);
        extension.intercept(HMessage.Direction.TOCLIENT, "WiredAllVariablesDiffs", this::onWiredAllVariables);

        extension.intercept(HMessage.Direction.TOSERVER, "PlaceObject", this::maybeBlockPlacements);
        extension.intercept(HMessage.Direction.TOSERVER, "BuildersClubPlaceRoomItem", this::maybeBlockPlacements);

        extension.intercept(HMessage.Direction.TOSERVER, "MoveObject", this::blockFurniAdjustments);
        extension.intercept(HMessage.Direction.TOSERVER, "UseFurniture", this::blockFurniAdjustments);
        extension.intercept(HMessage.Direction.TOSERVER, "SetCustomStackingHeight", this::blockFurniAdjustments);
        extension.intercept(HMessage.Direction.TOSERVER, "UpdateCondition", this::blockFurniAdjustments);
        extension.intercept(HMessage.Direction.TOSERVER, "UpdateTrigger", this::blockFurniAdjustments);
        extension.intercept(HMessage.Direction.TOSERVER, "UpdateAction", this::blockFurniAdjustments);
        extension.intercept(HMessage.Direction.TOSERVER, "UpdateAddon", this::blockFurniAdjustments);
        extension.intercept(HMessage.Direction.TOSERVER, "UpdateSelector", this::blockFurniAdjustments);

        extension.intercept(HMessage.Direction.TOCLIENT, "WiredSaveSuccess", this::wiredSaved);
        extension.intercept(HMessage.Direction.TOCLIENT, "FurnitureLabelsAndRegionsUpdate", this::labelUpdated);
    }

    private void labelUpdated(HMessage hMessage) {
        synchronized (lock) {
            LabelConfirmation awaited = awaitedLabelConfirmation;
            if (awaited == null || state != BuildingImportState.SETUP_WIRED) return;

            for (PresetFurniLabel updated : PresetFurniLabel.readFromPacket(hMessage.getPacket())) {
                if (updated.getFurniId() != awaited.realFurniId) continue;

                // the update confirms the packet that is currently awaited, and only that one: an
                // update carrying the settings of a label does not confirm a pending text change
                boolean confirmed = awaited.part == LabelPart.SETTINGS
                        ? updated.hasSameSettingsAs(awaited.label)
                        : updated.hasSameTextAs(awaited.label);

                if (confirmed) {
                    awaited.latch.countDown();
                    return;
                }
            }
        }
    }

    private void onWiredAllVariables(HMessage hMessage) {
        if (state == BuildingImportState.SETUP_WIRED) {
            HPacket packet = hMessage.getPacket();
            int _allVariablesHash = packet.readInteger();
            boolean isLastChunk = packet.readBoolean();

            int removedVariablesLength = packet.readInteger();
            for(int i = 0; i < removedVariablesLength; i++) {
                packet.readLong();
            }

            HashSet<HWiredVariable> variables = new HashSet<>();

            int count = packet.readInteger();
            for(int i = 0; i < count; i++) {
                int addedOrUpdated = packet.readInteger();
                variables.add(new HWiredVariable(packet));
            }

            if(workingPresetConfig != null && workingPresetConfig.getPresetWireds() != null) {
                HashMap<String, String> map = workingPresetConfig.getPresetWireds().getVariablesMap();

                for(HWiredVariable newVar : variables) {
                    Optional<Map.Entry<String, String>> op = map.entrySet().stream().filter(k -> k.getKey().equals(newVar.name)).findFirst();
                    op.ifPresent(stringStringEntry -> realVariableIdMap.put(stringStringEntry.getValue(), newVar.id));
                }

                for (PresetWiredVariable origVar : workingPresetConfig.getPresetWireds().getVariables()) {
                    for(HWiredVariable newVar : variables) {
                        if(newVar.name.equals(origVar.getStringConfig())) {
                            realVariableIdMap.put(origVar.variableId, newVar.id);
                        }
                    }
                }
            }

            for(String id : realVariableIdMap.keySet()) {
                needVariableIds.remove(id);
            }

            if(needVariableIds.isEmpty()) {
                wiredVariableConfirmation.release();
            }

        }
    }

    private void blockFurniAdjustments(HMessage message) {
        synchronized (lock) {
            if (state == BuildingImportState.SETUP_WIRED || state == BuildingImportState.MOVE_FURNITURE) {
                message.setBlocked(true);
                extension.sendVisualChatInfo("Don't adjust furniture while the importer is active!");
            }
        }
    }

    private void maybeBlockPlacements(HMessage hMessage) {
        synchronized (lock) {
            if (state != BuildingImportState.NONE) {
                hMessage.setBlocked(true);
                extension.sendVisualChatInfo("Can't drop furniture while the importer is active, please await the procedure or abort");
            }
        }
    }

    private boolean isPlacing() {
        return state == BuildingImportState.ADD_UNSTACKABLES || state == BuildingImportState.ADD_FURNITURE;
    }

    private void onObjectAdd(HMessage hMessage) {
        synchronized (lock) {
            if (isPlacing()) {
                HFloorItem item = new HFloorItem(hMessage.getPacket());
//                String classname = extension.getFurniDataTools().getFloorItemName(item.getTypeId());
                String dropKey = String.format("%d|%d|%d",
                        item.getTile().getX(), item.getTile().getY(), item.getTypeId());

                LinkedList<Integer> awaitMatchingDropIds = expectFurniDrops.get(dropKey);
                if (awaitMatchingDropIds != null && !awaitMatchingDropIds.isEmpty()) {
                    int assignedFurniId = awaitMatchingDropIds.pollFirst();
                    realFurniIdMap.put(assignedFurniId, item.getId());

//                    if (classname != null && classname.equals("ads_background")) {
//                        PresetAdsBackground ads = workingPresetConfig.getAdsBackgrounds().stream().filter(a -> a.getFurniId() == assignedFurniId).findFirst().orElse(null);
//                        if (ads != null) {
//
//                        }
//                    }

                    if (awaitMatchingDropIds.isEmpty()) {
                        expectFurniDrops.remove(dropKey);
                    }
                }
            }
        }
    }

    public boolean isReady() {
        return extension.getFloorState().inRoom() && extension.furniDataReady() &&
                extension.getInventory().getState() == Inventory.InventoryState.LOADED && extension.stackTile() != null &&
                presetConfig != null &&
                extension.getPermissions().canMoveFurni() && (!extension.shouldExportWired() || extension.getPermissions().canModifyWired());
    }

    private void onChat(HMessage hMessage) {
        synchronized (lock) {
            String text = hMessage.getPacket().readString();
            if (text.startsWith(":ip") || text.startsWith(":importpreset")) {
                hMessage.setBlocked(true);

                if (state != BuildingImportState.NONE) {
                    extension.sendVisualChatInfo("Already importing preset.. finish up or abort first");
                }
                else if (!isReady()) {
                    if (presetConfig == null) {
                        extension.sendVisualChatInfo("ERROR: select the preset first");
                    }
                    else {
                        extension.sendVisualChatInfo("ERROR: extension not fully initialized yet");
                    }
                }
                else {
                    prepare();
                    try {
                        String[] split = text.split(" ", 2);
                        String[] loc = split[1].split(", ?");
                        int rootX = Integer.parseInt(loc[0]);
                        int rootY = Integer.parseInt(loc[1]);
                        rootLocation = new HPoint(rootX, rootY);
                    }
                    catch (Exception ignore) {}
                }

            }

            if (text.equals(":abort") || text.equals(":a")) {
                hMessage.setBlocked(true);

                if (state != BuildingImportState.NONE) {
                    state = BuildingImportState.NONE;
                    extension.sendVisualChatInfo("Successfully aborted importing");
                }
            }

        }
    }

    private void prepare() {
        if (presetConfig != null) {
//            workingPostConfig = postConfig;
            workingPresetConfig = new PresetConfig(presetConfig.toJsonObject());

            realFurniIdMap = workingPresetConfig.applyPostConfig(postConfig);
            expectFurniDrops = new HashMap<>();
            inventoryCache.clear();
            rootLocation = null;
            referenceVariables = new ArrayList<>();

            // check if user has enough furniture / BC items available (depending on settings)
            FurniDataTools furniData = extension.getFurniDataTools();
            List<FurniDropInfo> fakeDropInfo = new ArrayList<>();
            for (PresetFurni f : workingPresetConfig.getFurniture()) {
                fakeDropInfo.add(new FurniDropInfo(-1, -1, furniData.getFloorTypeId(f.getClassName()), postConfig.getItemSource(), -1, ""));
                if (f.getClassName().equals("wf_var_reference")) {
                    referenceVariables.add(f.getFurniId());
                }
            }
            Map<String, Integer> missing = AvailabilityChecker.missingItems(fakeDropInfo, extension.getInventory(), furniData);
            if (missing == null) {
                extension.sendVisualChatInfo("ERROR: Inventory, catalog or furnidata is unavailable");
            }
            else {
                if (!missing.isEmpty()) {
                    if (extension.allowIncompleteBuilds()) {
                        extension.sendVisualChatInfo("Some items were not available, building anyways..");
                    }
                    else {
                        extension.sendVisualChatInfo("ERROR: Some items were not available, check the availability first!");
                        return;
                    }
                }
                mainStackTile = extension.stackTile().getId();

                allAvailableStackTiles = new ArrayList<>();
                Set<String> allStacktileClasses = Arrays.stream(StackTileSetting.values()).map(StackTileSetting::getClassName).collect(Collectors.toSet());
                allStacktileClasses.forEach(c -> {
                    List<HFloorItem> stackTiles = extension.getFloorState().getItemsFromType(furniData, c);
                    if (!stackTiles.isEmpty()) {
                        HFloorItem stackTile = stackTiles.getFirst();
                        allAvailableStackTiles.add(new StackTileInfo(
                                stackTile.getId(),
                                stackTile.getTile(),
                                stackTile.getFacing().ordinal(),
                                StackTileSetting.fromClassName(c).getDimension()
                        ));
                    }
                });

                if (!allAvailableStackTiles.isEmpty()) {
                    extension.getLogger().log(String.format("Detected %d available types of stacktiles", allAvailableStackTiles.size()), "green");
                }

//                originalStackTileLocation = extension.stackTile().getTile();
                mainStackDimensions = extension.getStackTileSetting().getDimension();

                state = BuildingImportState.AWAITING_UNOCCUPIED_SPACE;
                extension.sendVisualChatInfo("Select unoccupied space in the room");
            }

        }
        else {
            extension.sendVisualChatInfo("ERROR: No preset selected!");
        }
    }

    private void moveAvatar(HMessage hMessage) {
        synchronized (lock) {
            boolean startAddingFurni = false;

            if (state == BuildingImportState.AWAITING_UNOCCUPIED_SPACE) {
                hMessage.setBlocked(true);
                reservedSpace = new HPoint(
                        hMessage.getPacket().readInteger(),
                        hMessage.getPacket().readInteger()
                );

                if (rootLocation == null) {
                    state = BuildingImportState.AWAITING_ROOT_LOCATION;
                    extension.sendVisualChatInfo("Select where the preset should be imported");
                }
                else {
                    startAddingFurni = true;
                }
            }
            else if (state == BuildingImportState.AWAITING_ROOT_LOCATION) {
                hMessage.setBlocked(true);
                int x = hMessage.getPacket().readInteger();
                int y = hMessage.getPacket().readInteger();
                rootLocation = new HPoint(
                        x,
                        y//,
//                        PresetUtils.lowestFloorPoint(extension.getFloorState(), workingPresetConfig, new HPoint(x, y))
                );

                startAddingFurni = true;
            }

            if (startAddingFurni) {
                heightOffset = PresetUtils.lowestFloorPoint(extension.getFloorState(), workingPresetConfig, rootLocation);

                state = BuildingImportState.ADD_UNSTACKABLES;
                new Thread(this::addUnstackables).start();
                extension.sendVisualChatInfo("Adding furniture...");
            }
        }
    }

    Map<Integer, LinkedList<HInventoryItem>> inventoryCache = new HashMap<>();

    private void dropFurni(FurniDropInfo dropInfo) {
        FurniDataTools furniData = extension.getFurniDataTools();

        int typeId = dropInfo.getTypeId();
        String className = furniData.getFloorItemName(typeId);

        ItemSource source = dropInfo.getItemSource();

        Inventory inventory = extension.getInventory();

        int offerId = furniData.getFloorItemDetails(className).bcOfferId;
        if (!inventoryCache.containsKey(typeId)) {
            inventoryCache.put(typeId, new LinkedList<>(inventory.getFloorItemsByType(typeId)));
        }
        LinkedList<HInventoryItem> inventoryItems = inventoryCache.get(typeId);

        boolean useBC = source == ItemSource.ONLY_BC || (source == ItemSource.PREFER_BC && offerId != -1)
                || ( source == ItemSource.PREFER_INVENTORY && inventoryItems.isEmpty());

        if (useBC) {
            if (offerId == -1) {
                if (!extension.allowIncompleteBuilds()) {
                    state = BuildingImportState.NONE;
                    extension.sendVisualChatInfo(String.format("ERROR: Couldn't find the item '%s' in BC warehouse.. aborting", className));
                }
                else {
                    extension.sendVisualChatInfo(String.format("Couldn't find the item '%s' in BC warehouse.. continuing", className));
                }
                return;
            }
            extension.sendToServer(new HPacket(
                    "BuildersClubPlaceRoomItem",
                    HMessage.Direction.TOSERVER,
                    -1,
                    offerId,
                    dropInfo.getExtraParam(),
                    dropInfo.getX(),
                    dropInfo.getY(),
                    dropInfo.getRotation(),
                    true
            ));

            Utils.sleep(230);
        }
        else {
            if (inventoryItems.isEmpty()) {
                if (!extension.allowIncompleteBuilds()) {
                    state = BuildingImportState.NONE;
                    extension.sendVisualChatInfo(String.format("ERROR: Couldn't find '%s' in inventory.. aborting", className));
                }
                else {
                    extension.sendVisualChatInfo(String.format("Couldn't find '%s' in inventory.. continuing", className));
                }
                return;
            }
            HInventoryItem item = inventoryItems.pollFirst();
            extension.sendToServer(new HPacket(
                    "PlaceObject",
                    HMessage.Direction.TOSERVER,
                    String.format("-%d %d %d %d",
                            item.getId(),
                            dropInfo.getX(),
                            dropInfo.getY(),
                            dropInfo.getRotation()
                    )
            ));

//            Utils.sleep(600);
            Utils.sleep(150);
        }
    }

    private void moveFurni(int furniId, int x, int y, int rot, boolean moveStackTile, double height) {
        if (moveStackTile) {
            StackTileInfo stackInfo = StackTileUtils.findBestDropLocation(x, y, allAvailableStackTiles, extension.getFloorState());
            if (stackInfo != null) {
                moveFurni(stackInfo.getFurniId(), stackInfo.getLocation().getX(), stackInfo.getLocation().getY(),
                        stackInfo.getRotation(), false, -1);
                if (height != -1) {
                    extension.sendToServer(new HPacket(
                            "SetCustomStackingHeight",
                            HMessage.Direction.TOSERVER,
                            stackInfo.getFurniId(),
                            ((int) (height * 100)) + heightOffset * 100
                    ));

                    Utils.sleep(40);
                }
            }
        }

        extension.sendToServer(new HPacket(
                "MoveObject",
                HMessage.Direction.TOSERVER,
                furniId,
                x, y, rot
        ));

        Utils.sleep(60);
    }

    private void attemptSetState(int furniId, String targetState) {
        FloorState floor = extension.getFloorState();
        FurniDataTools furniData = extension.getFurniDataTools();
        if (floor == null || furniData == null || floor.furniFromId(furniId) == null) return;

        HFloorItem item = floor.furniFromId(furniId);
        boolean isStackable = furniData.isStackable(furniData.getFloorItemName(item.getTypeId()));
        HPoint originalLocation = item.getTile();
        int originalRotation = item.getFacing().ordinal();

        if (extension.getPermissions().canModifyWired()) {
            // Set state using the @state variable
            extension.sendToServer(new HPacket("WiredSetObjectVariableValue", HMessage.Direction.TOSERVER, 0, furniId, "-110", Integer.parseInt(targetState)));
            Utils.sleep(Math.max(extension.getSafeFeedbackTimeout(), 60));
        } else {
            String currentState = StateExtractor.stateFromItem(item);
            if (currentState == null || currentState.equals(targetState)) {
                return;
            }

            synchronized (lock) {
                if (state != BuildingImportState.NONE) {
                    moveFurni(furniId, reservedSpace.getX(), reservedSpace.getY(), 0, false, -1);
                }
            }

            String newState = "-1";

            int i = 0;
            while (state != BuildingImportState.NONE && newState != null && !currentState.equals(newState) && !newState.equals(targetState) && i < 20) {
                currentState = newState;

                extension.sendToServer(new HPacket("UseFurniture", HMessage.Direction.TOSERVER, furniId, 0));
                Utils.sleep(Math.max(extension.getSafeFeedbackTimeout(), 60));

                HFloorItem itemNew = floor.furniFromId(furniId);
                newState = StateExtractor.stateFromItem(itemNew);

                i++;
            }
        }

        moveFurni(furniId, originalLocation.getX(), originalLocation.getY(), originalRotation, isStackable, -1);
    }


    // every wired packet, wired label updates included, shares the same server rate limit
    private long latestWiredSave = -1;

    private final Semaphore wiredSaveConfirmation = new Semaphore(0);
    private final Semaphore wiredVariableConfirmation = new Semaphore(0);

    private static final int SAVE_DELAY_MS = 300;
    private static final int MAX_LABEL_SAVE_ATTEMPTS = 3;

    private enum LabelPart {
        SETTINGS,
        TEXT
    }

    /**
     * SetFurnitureLabel (text) and SetFurnitureLabelSettings (settings) are independent packets
     * that are confirmed by separate FurnitureLabelsAndRegionsUpdate packets, so every sent packet
     * gets its own confirmation object.
     */
    private static class LabelConfirmation {
        private final PresetFurniLabel label;
        private final int realFurniId; // the id used in the packet, not the preset id
        private final LabelPart part;
        private final CountDownLatch latch = new CountDownLatch(1);

        private LabelConfirmation(PresetFurniLabel label, int realFurniId, LabelPart part) {
            this.label = label;
            this.realFurniId = realFurniId;
            this.part = part;
        }
    }

    private volatile LabelConfirmation awaitedLabelConfirmation = null;

    private void wiredSaved(HMessage hMessage) {
        if (state == BuildingImportState.SETUP_WIRED) {
            wiredSaveConfirmation.release();
        }
    }

    private void saveWired(PresetWiredBase presetWired, int attempt) {
        if (attempt > 2) return;

        waitForSaveSlot(latestWiredSave);

        wiredSaveConfirmation.drainPermits();
        wiredVariableConfirmation.drainPermits();

        if (presetWired.getVariableIds() != null && !presetWired.getVariableIds().isEmpty()) {
            needVariableIds.clear();
            needVariableIds.addAll(presetWired.getVariableIds());
            needVariableIds = needVariableIds.stream()
                    .map(id -> id.startsWith("s") ? id.substring(1).split("-")[0] : id)
                    .filter(id -> realVariableIdMap.keySet().stream().noneMatch(x -> x.equals(id)))
                    .toList();
            if(!needVariableIds.isEmpty()) {
                extension.sendToServer(new HPacket("WiredGetAllVariablesDiffs", HMessage.Direction.TOSERVER, 0));
                boolean gotVariableConfirmation = false;
                try { gotVariableConfirmation = wiredVariableConfirmation.tryAcquire(5, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {}
            }
        }

        PresetWiredBase presetWiredBase;
        synchronized (lock) {
            if (referenceVariables.contains(presetWired.getWiredId())) {
                String defaultId = presetWired.getVariableIds().getFirst();
                realVariableIdMap.put(defaultId, defaultId);
            }
            presetWiredBase = presetWired.applyWiredConfig(extension, realFurniIdMap, realVariableIdMap);
        }

        latestWiredSave = System.currentTimeMillis();

        boolean gotConfirmation = false;
        try { gotConfirmation = wiredSaveConfirmation.tryAcquire(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}

        if (presetWired instanceof PresetWiredVariable) {
            variablesProcessed++;
            
            if(variablesProcessed >= variablesToProcess) {
                extension.sendToServer(new HPacket("WiredGetAllVariablesDiffs", HMessage.Direction.TOSERVER, 0));
                boolean gotVariableConfirmation = false;
                try { gotVariableConfirmation = wiredVariableConfirmation.tryAcquire(5, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {}
            }
        }

        if (gotConfirmation) {
            if (presetWiredBase != null) {
                extension.getExporter().cacheWiredConfig(presetWiredBase);
            }
        }
        else {
            saveWired(presetWired, attempt + 1);
        }
    }

    private void saveLabel(PresetFurniLabel label) {
        if (!realFurniIdMap.containsKey(label.getFurniId())) return;

        int realId = realFurniIdMap.get(label.getFurniId());
        HFloorItem item = extension.getFloorState().furniFromId(realId);
        if (item != null && !"wf_label".equals(extension.getFurniDataTools().getFloorItemName(item.getTypeId()))) {
            return;
        }

        // The server only answers with a FurnitureLabelsAndRegionsUpdate when something really
        // changed. A redundant SetFurnitureLabel or SetFurnitureLabelSettings stays unanswered.
        // The exporter tracks the state of every label in the room, including the state a label
        // gets when it is dropped, so compare each part against that state and only send the
        // packets that actually change something.
        for (int attempt = 0; attempt < MAX_LABEL_SAVE_ATTEMPTS; attempt++) {
            PresetFurniLabel current = extension.getExporter().getLabelConfig(realId);
            boolean settingsDiffer = current == null || !current.hasSameSettingsAs(label);
            boolean textDiffer = current == null || !current.hasSameTextAs(label);

            if (!settingsDiffer && !textDiffer) {
                return;
            }

            // each packet is confirmed on its own, and only if that packet really changed its part
            boolean settingsSaved = !settingsDiffer || sendLabelSettings(label, realId);
            boolean textSaved = !textDiffer || sendLabelText(label, realId);

            if (settingsSaved && textSaved) {
                return;
            }
            // a late confirmation may have updated the exporter's cache in the meantime,
            // in which case the retry recomputes what still differs and resends only that
        }
    }

    private boolean sendLabelSettings(PresetFurniLabel label, int realId) {
        return sendUpdateWiredLabelPacket(new HPacket(
                "SetFurnitureLabelSettings",
                HMessage.Direction.TOSERVER,
                realId,
                label.getColor(),
                label.getAccess(),
                label.isAlwaysOpen()
        ), label, realId, LabelPart.SETTINGS);
    }

    private boolean sendLabelText(PresetFurniLabel label, int realId) {
        return sendUpdateWiredLabelPacket(new HPacket(
                "SetFurnitureLabel",
                HMessage.Direction.TOSERVER,
                realId,
                label.getText()
        ), label, realId, LabelPart.TEXT);
    }

    private boolean sendUpdateWiredLabelPacket(HPacket packet, PresetFurniLabel label, int realId, LabelPart part) {
        // every packet needs its own delay, a packet sent too fast is ignored by the server
        waitForSaveSlot(latestWiredSave);

        LabelConfirmation confirmation = new LabelConfirmation(label, realId, part);
        awaitedLabelConfirmation = confirmation;

        extension.sendToServer(packet);
        latestWiredSave = System.currentTimeMillis();

        try {
            return confirmation.latch.await(5, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            return false;
        }
        finally {
            awaitedLabelConfirmation = null;
        }
    }

    private void waitForSaveSlot(long lastSave) {
        long elapsed = System.currentTimeMillis() - lastSave;
        if (elapsed < SAVE_DELAY_MS) {
            Utils.sleep((int) (SAVE_DELAY_MS - elapsed));
        }
    }

    private void moveFurniture() {
        FurniDataTools furniData = extension.getFurniDataTools();

        List<PresetFurni> moveList = new ArrayList<>();

        synchronized (lock) {
            workingPresetConfig.getFurniture().forEach(p -> {
                if (furniData.isStackable(p.getClassName()) && !p.getClassName().startsWith("wf_trg_") && realFurniIdMap.containsKey(p.getFurniId())) {
                    moveList.add(p);
                }
            });
            workingPresetConfig.getFurniture().forEach(p -> {
                if (furniData.isStackable(p.getClassName()) && p.getClassName().startsWith("wf_trg_") && realFurniIdMap.containsKey(p.getFurniId())) {
                    moveList.add(p);
                }
            });
        }

        Map<Integer, PresetFurni> movesByRealId = new HashMap<>();

        int i = 0;
        while (state == BuildingImportState.MOVE_FURNITURE && i < moveList.size()) {
            PresetFurni moveFurni = moveList.get(i);
            i++;
            int realFurniId = realFurniIdMap.get(moveFurni.getFurniId());

            if (moveFurni.getState() != null) {
                attemptSetState(realFurniId, moveFurni.getState());
            }

            movesByRealId.put(realFurniId, moveFurni);
            moveFurni(
                    realFurniId,
                    moveFurni.getLocation().getX() + rootLocation.getX(),
                    moveFurni.getLocation().getY() + rootLocation.getY(),
                    moveFurni.getRotation(),
                    true,
                    moveFurni.getLocation().getZ()// + rootLocation.getZ()
            );
        }


        // second pass
        Utils.sleep(300 + extension.getSafeFeedbackTimeout());
        if (state == BuildingImportState.MOVE_FURNITURE) {
            List<HFloorItem> maybeNotMovedItems = extension.getFloorState().getFurniOnTile(stackTileLocation.getX(), stackTileLocation.getY());
            maybeNotMovedItems.addAll(extension.getFloorState().getFurniOnTile(stackTileLocation.getX(), stackTileLocation.getY()));
            maybeNotMovedItems.addAll(extension.getFloorState().getFurniOnTile(reservedSpace.getX(), reservedSpace.getY()));
            maybeNotMovedItems = maybeNotMovedItems.stream().filter(h -> {
                PresetFurni p = movesByRealId.get(h.getId());
                if (p == null) return false;
                return p.getLocation().getX() + rootLocation.getX() != h.getTile().getX() ||
                        p.getLocation().getY() + rootLocation.getY() != h.getTile().getY() ||
                        p.getRotation() != h.getFacing().ordinal();
            }).collect(Collectors.toList());

            for (HFloorItem o : maybeNotMovedItems) {
                if (state != BuildingImportState.MOVE_FURNITURE) break;

                PresetFurni p = movesByRealId.get(o.getId());

                moveFurni(
                        o.getId(),
                        p.getLocation().getX() + rootLocation.getX(),
                        p.getLocation().getY() + rootLocation.getY(),
                        p.getRotation(),
                        true,
                        p.getLocation().getZ()// + rootLocation.getZ()
                );
            }
        }

        if (state == BuildingImportState.MOVE_FURNITURE) {
            for(StackTileInfo stackTileInfo : allAvailableStackTiles) {
                moveFurni(stackTileInfo.getFurniId(), stackTileInfo.getLocation().getX(), stackTileInfo.getLocation().getY(),
                        stackTileInfo.getRotation(), false, -1);
            }

        }

        synchronized (lock) {
            if (state == BuildingImportState.MOVE_FURNITURE) {
                state = BuildingImportState.NONE;
                extension.sendVisualChatInfo("Imported the preset successfully");
                extension.getLogger().log("Finished importing the preset!", "green");
            }
        }
    }

    private void setupWired() {
        List<PresetWiredBase> allWireds = new ArrayList<>();
        Map<Integer, List<PresetWiredFurniBinding>> wiredBindings = new HashMap<>();
        Map<Integer, PresetFurni> furni = new HashMap<>();

        synchronized (lock) {
            PresetWireds presetWireds = workingPresetConfig.getPresetWireds();

            int maxSize = Math.max(presetWireds.getConditions().size(),
                    Math.max(presetWireds.getEffects().size(),
                            Math.max(presetWireds.getTriggers().size(),
                                    Math.max(presetWireds.getAddons().size(),
                                            Math.max(presetWireds.getSelectors().size(),
                                                presetWireds.getVariables().size()
                                            )
                                    )
                            )
                    )
            );

            Predicate<PresetWiredBase> filter = (wiredBase) -> realFurniIdMap.containsKey(wiredBase.getWiredId());
            Predicate<PresetWiredBase> isDependantOnVars = (wiredBase) -> !wiredBase.getVariableIds().isEmpty();

            allWireds.addAll(
                    presetWireds.getVariables().stream()
                            .filter(filter)
                            .filter(isDependantOnVars.negate())
                            .toList()
            );
            variablesProcessed = 0;
            variablesToProcess = allWireds.size();
            allWireds.addAll(
                    presetWireds.getAddons().stream()
                            .filter(filter)
                            .filter(isDependantOnVars.negate())
                            .toList()
            );
            allWireds.addAll(
                    presetWireds.getVariables().stream()
                            .filter(filter)
                            .filter(isDependantOnVars)
                            .toList()
            );
            allWireds.addAll(
                    presetWireds.getAddons().stream()
                            .filter(filter)
                            .filter(isDependantOnVars)
                            .toList()
            );
            allWireds.addAll(presetWireds.getConditions().stream().filter(filter).toList());
            allWireds.addAll(presetWireds.getEffects().stream().filter(filter).toList());
            allWireds.addAll(presetWireds.getTriggers().stream().filter(filter).toList());
            allWireds.addAll(presetWireds.getSelectors().stream().filter(filter).toList());

            workingPresetConfig.getBindings().forEach(b -> {
                if (!wiredBindings.containsKey(b.getWiredId())) wiredBindings.put(b.getWiredId(), new ArrayList<>());
                wiredBindings.get(b.getWiredId()).add(b);
            });
            workingPresetConfig.getFurniture().forEach(p -> furni.put(p.getFurniId(), p));
        }
        
        List<PresetFurniLabel> labels = workingPresetConfig.getLabels();

        int i = 0;
        int labelIndex = 0;
        // wired labels are wired data, so their settings are set up in this same step
        while (state == BuildingImportState.SETUP_WIRED && (i < allWireds.size() || labelIndex < labels.size())) {
            if (labelIndex < labels.size()) {
                saveLabel(labels.get(labelIndex++));
            }

            if (i >= allWireds.size()) continue;

            PresetWiredBase wiredBase = allWireds.get(i);

            if ((i + 1) % 50 == 0) {
                extension.getLogger().log(String.format("Setting up wired.. (%d/%d)", i+1, allWireds.size()), "orange");
            }

            i++;

            FloorState floor = extension.getFloorState();
            FurniDataTools furniData = extension.getFurniDataTools();

            List<PresetWiredFurniBinding> bindings = wiredBindings.get(wiredBase.getWiredId());

            List<FurniMoveInfo> bindMoves = new ArrayList<>();
            List<FurniMoveInfo> undoBindMoves = new ArrayList<>();

            if (bindings != null) {
                // set stuff to the right rotation/location/state
                for (PresetWiredFurniBinding binding : bindings) {
                    int furniId;
                    synchronized (lock) {
                        if (state != BuildingImportState.SETUP_WIRED) return;

                        if (!realFurniIdMap.containsKey(binding.getFurniId())) continue;
                        furniId = realFurniIdMap.get(binding.getFurniId());
                    }
                    if (binding.getState() != null) {
                        String targetState = binding.getState();
                        attemptSetState(furniId, targetState);
                    }
                    synchronized (lock) {
                        if (state != BuildingImportState.SETUP_WIRED) return;

                        boolean needMovement = binding.getLocation() != null || binding.getRotation() != null || binding.getAltitude() != null;
                        if (needMovement) {
                            HFloorItem floorItem = floor.furniFromId(furniId);
                            if (floorItem != null) {
                                boolean needStacktile =
                                        furniData.isStackable(furniData.getFloorItemName(extension.getFloorState().furniFromId(furniId).getTypeId()))
                                        || binding.getAltitude() != null;

                                FurniMoveInfo undoMovement = new FurniMoveInfo(
                                        furniId, floorItem.getTile().getX(), floorItem.getTile().getY(),
                                        floorItem.getFacing().ordinal(), null, needStacktile);
                                undoBindMoves.add(undoMovement);

                                int targetX = floorItem.getTile().getX() + rootLocation.getX();
                                int targetY = floorItem.getTile().getY() + rootLocation.getY();
                                int targetRotation = floorItem.getFacing().ordinal();
                                Integer targetAltitude = null;

                                if (binding.getLocation() != null) {
                                    targetX = binding.getLocation().getX() + rootLocation.getX();
                                    targetY = binding.getLocation().getY() + rootLocation.getY();
                                }
                                if (binding.getRotation() != null) {
                                    targetRotation = binding.getRotation();
                                }
                                if (binding.getAltitude() != null) {
                                    targetAltitude = binding.getAltitude();
                                }

                                FurniMoveInfo movement = new FurniMoveInfo(
                                        furniId, targetX, targetY, targetRotation, targetAltitude, needStacktile);
                                bindMoves.add(movement);
                            }
                        }
                    }


                }
            }

            for (FurniMoveInfo furniMoveInfo : bindMoves) {
                double altitude = furniMoveInfo.getAltitude() == null ? -1 : ((double) furniMoveInfo.getAltitude()) / 100;
                moveFurni(
                        furniMoveInfo.getFurniId(), furniMoveInfo.getX(), furniMoveInfo.getY(),
                        furniMoveInfo.getRotation(), furniMoveInfo.useStackTile(), altitude
                );
            }

            PresetFurni wiredFurni = furni.get(wiredBase.getWiredId());
            boolean holdsPlaceholderOrCapturer = Arrays.asList(
                    "wf_xtra_text_output_variable",
                    "wf_xtra_text_output_username",
                    "wf_xtra_text_output_furni_name",
                    "wf_xtra_text_input_variable",
                    "wf_trg_receive_dataframe"
                ).contains(wiredFurni.getClassName());

            int realId = realFurniIdMap.get(wiredBase.getWiredId());
            HPoint location = floor.furniFromId(realId).getTile();

            if (holdsPlaceholderOrCapturer) {
                moveFurni(realId, reservedSpace.getX(), reservedSpace.getY(), 0, true, -1);
            }

            saveWired(wiredBase, 0);

            if (holdsPlaceholderOrCapturer) {
                moveFurni(realId, location.getX(), location.getY(), wiredFurni.getRotation(), true, -1);
            }

            for (FurniMoveInfo furniMoveInfo : undoBindMoves) {
                double altitude = furniMoveInfo.getAltitude() == null ? -1 : ((double) furniMoveInfo.getAltitude()) / 100;
                moveFurni(
                        furniMoveInfo.getFurniId(), furniMoveInfo.getX(), furniMoveInfo.getY(),
                        furniMoveInfo.getRotation(), furniMoveInfo.useStackTile(), altitude
                );
                // todo maybe set to original height? perhaps needed when furni is overridden with existing furni
            }
        }


        synchronized (lock) {
            if (state == BuildingImportState.SETUP_WIRED) {
                state = BuildingImportState.MOVE_FURNITURE;
            }
        }
        if (state == BuildingImportState.MOVE_FURNITURE) {
            extension.sendVisualChatInfo("Setting furni in their correct position..");
            moveFurniture();
        }
    }

    private void dropAllOtherFurni() {
        FurniDataTools furniData = extension.getFurniDataTools();
        List<FurniDropInfo> furniDropInfos = new ArrayList<>();

        moveFurni(mainStackTile, stackTileLocation.getX(), stackTileLocation.getY(), 0, false, -1);

        synchronized (lock) {
            workingPresetConfig.getFurniture().forEach(f -> {
                if (furniData.isStackable(f.getClassName())) {
                    FurniDropInfo dropInfo = new FurniDropInfo(
                            f.getClassName().startsWith("wf_trg_") ?
                                    stackTileLocation.getX() + 1 :
                                    stackTileLocation.getX(),
                            stackTileLocation.getY(),
                            furniData.getFloorTypeId(f.getClassName()),
                            postConfig.getItemSource(),
                            0,
                            f.getClassName().contains("recolorable") ?
                                    workingPresetConfig.getRecolorables().stream()
                                            .filter(r -> r.getFurniId() == f.getFurniId())
                                            .map(PresetRecolorable::toColorString)
                                            .findFirst()
                                            .orElse("") :
                                    ""
                            );
                    furniDropInfos.add(dropInfo);

                    String key = String.format("%d|%d|%d", dropInfo.getX(), dropInfo.getY(), dropInfo.getTypeId());
                    if (!expectFurniDrops.containsKey(key))
                        expectFurniDrops.put(key, new LinkedList<>());
                    expectFurniDrops.get(key).add(f.getFurniId());
                }
            });
        }

        int i = 0;
        while (i < furniDropInfos.size() && state == BuildingImportState.ADD_FURNITURE) {
            FurniDropInfo dropInfo = furniDropInfos.get(i);

            if ((i + 1) % 50 == 0) {
                extension.getLogger().log(String.format("Dropping furni.. (%d/%d)", i+1, furniDropInfos.size()), "orange");
            }

            dropFurni(dropInfo);
            i++;
        }

        if (state == BuildingImportState.ADD_FURNITURE) {
            int j = 0;
            boolean done;
            do {
                Utils.sleep(500);
                synchronized (lock) {
                    done = state != BuildingImportState.ADD_FURNITURE || expectFurniDrops.isEmpty() || j++ > 7;
                }
            } while (!done);

            synchronized (lock) {
                if (state == BuildingImportState.ADD_FURNITURE) {
                    if (expectFurniDrops.isEmpty() || extension.allowIncompleteBuilds()) {
                        state = BuildingImportState.SETUP_ADS;
                    }
                    else {
                        state = BuildingImportState.NONE;
                        extension.sendVisualChatInfo("ERROR: not all furniture were placed");
                        extension.getLogger().log("ERROR: not all furniture were placed", "red");
                    }
                }
            }
            if (state == BuildingImportState.SETUP_ADS) {
                setupAds();
            }
        }
    }

    private void setupAds() {
        List<PresetAdsBackground> adsBackgrounds = workingPresetConfig.getAdsBackgrounds();
        if (!adsBackgrounds.isEmpty()) {
            extension.sendVisualChatInfo("Setting up ads backgrounds..");

            int index = 0;
            while (index < adsBackgrounds.size() && state == BuildingImportState.SETUP_ADS) {
                PresetAdsBackground adsBackground = adsBackgrounds.get(index);

                synchronized (lock) {
                    if (state == BuildingImportState.SETUP_ADS && realFurniIdMap.containsKey(adsBackground.getFurniId())) {
                        int realFurni = realFurniIdMap.get(adsBackground.getFurniId());
                        extension.sendToServer(
                                new HPacket("SetObjectData", HMessage.Direction.TOSERVER,
                                        realFurni, 8,
                                        "imageUrl", adsBackground.getImageUrl(),
                                        "offsetX", adsBackground.getOffsetX(),
                                        "offsetY", adsBackground.getOffsetY(),
                                        "offsetZ", adsBackground.getOffsetZ()
                                )
                        );
                    }
                }

                Utils.sleep(100);
                index++;
            }
        }

        synchronized (lock) {
            if (state == BuildingImportState.SETUP_ADS) {
                state = BuildingImportState.SETUP_WIRED;
            }
        }
        if (state == BuildingImportState.SETUP_WIRED) {
            extension.sendVisualChatInfo("Setting up wired..");
            setupWired();
        }
    }

    private boolean locationContainsWired(int x, int y) {
        FloorState floor = extension.getFloorState();
        FurniDataTools furniData = extension.getFurniDataTools();

        return floor.getFurniOnTile(x, y).stream().anyMatch(i -> {
            String className = furniData.getFloorItemName(i.getTypeId());
            return className.startsWith("wf_cnd_") ||
                    className.startsWith("wf_trg_") ||
                    className.startsWith("wf_act_");
        });
    }

    private HPoint findStackTileLocation() {
        // can not be the unoccupied space
        // can also not be a place with wired in the first or second tile (top row)
        // this will be the place where all furniture is placed on
        FloorState floor = extension.getFloorState();


        for (int x = 0; x < 64 - mainStackDimensions; x++) {

            nextIteration:
            for (int y = 0; y < 64 - mainStackDimensions; y++) {

                char reference = floor.floorHeight(x, y);
                if (reference == 'x') continue;

                for (int xOffset = 0; xOffset < mainStackDimensions; xOffset++) {
                    for (int yOffset = 0; yOffset < mainStackDimensions; yOffset++) {
                        if (floor.floorHeight(x + xOffset, y + yOffset) != reference) {
                            continue nextIteration;
                        }
                    }
                }

                if (!locationContainsWired(x, y) && !locationContainsWired(x + 1, y)) {
                    return new HPoint(x, y);
                }
            }
        }
        return new HPoint(0, 0);
    }

    private void addUnstackables() {
        FurniDataTools furniData = extension.getFurniDataTools();
        List<FurniDropInfo> furniDropInfos = new ArrayList<>();

        synchronized (lock) {
            workingPresetConfig.getFurniture().forEach(f -> {
                if (!furniData.isStackable(f.getClassName())) {
                    FurniDropInfo dropInfo = new FurniDropInfo(
                            f.getLocation().getX() + rootLocation.getX(),
                            f.getLocation().getY() + rootLocation.getY(),
                            furniData.getFloorTypeId(f.getClassName()),
                            postConfig.getItemSource(),
                            f.getRotation(),
                            f.getClassName().contains("recolorable") ?
                                    workingPresetConfig.getRecolorables().stream()
                                            .filter(r -> r.getFurniId() == f.getFurniId())
                                            .map(PresetRecolorable::toColorString)
                                            .findFirst()
                                            .orElse("") :
                                    ""
                            );

                    furniDropInfos.add(dropInfo);

                    String key = String.format("%d|%d|%d", dropInfo.getX(), dropInfo.getY(), dropInfo.getTypeId());
                    if (!expectFurniDrops.containsKey(key))
                        expectFurniDrops.put(key, new LinkedList<>());
                    expectFurniDrops.get(key).add(f.getFurniId());
                }
            });
        }

        int i = 0;
        while (i < furniDropInfos.size() && state == BuildingImportState.ADD_UNSTACKABLES) {
            FurniDropInfo dropInfo = furniDropInfos.get(i);
            dropFurni(dropInfo);

            i++;
        }
//
//        if (state == WiredImportState.ADD_UNSTACKABLES) {
//            if (furniDropInfos.size() > 0) Utils.sleep(2500);
            synchronized (lock) {
                if (state == BuildingImportState.ADD_UNSTACKABLES) {
                    state = BuildingImportState.ADD_FURNITURE;
                }
            }
//        }

        if (state == BuildingImportState.ADD_FURNITURE) {
            stackTileLocation = findStackTileLocation();
            dropAllOtherFurni();
        }

    }

    public void reset() {
        state = BuildingImportState.NONE;
    }

    public void setPresetConfig(PresetConfig presetConfig) {
        this.presetConfig = presetConfig;
    }

    public PresetConfig getPresetConfig() {
        return presetConfig;
    }

    public void setPostConfig(PostConfig postConfig) {
        this.postConfig = postConfig;
    }

//    public PostConfig getPostConfig() {
//        return postConfig;
//    }

    public BuildingImportState getState() {
        return state;
    }
}
