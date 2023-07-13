package extension.tools;

import extension.GPresets;
import extension.tools.presetconfig.PresetConfig;
import extension.tools.presetconfig.PresetConfigUtils;
import extension.tools.presetconfig.ads_bg.PresetAdsBackground;
import extension.tools.presetconfig.binding.PresetWiredFurniBinding;
import extension.tools.presetconfig.furni.PresetFurni;
import extension.tools.presetconfig.wired.*;
import extension.tools.presetconfig.wired.incoming.*;
import furnidata.FurniDataTools;
import game.FloorState;
import gearth.extensions.parsers.HFloorItem;
import gearth.extensions.parsers.HPoint;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import utils.StateExtractor;
import utils.Utils;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class GPresetExporter {

    private final Object lock = new Object();

    public enum PresetExportState {
        NONE,
        AWAITING_RECT1,
        AWAITING_RECT2,
        AWAITING_NAME,
        FETCHING_UNKNOWN_CONFIGS
    }

    private volatile HPoint rectCorner1 = null;
    private volatile HPoint rectCorner2 = null;
    private volatile String exportName = null;

    private volatile PresetExportState state = PresetExportState.NONE;

    private final Map<Integer, PresetWiredCondition> wiredConditionConfigs = Collections.synchronizedMap(new HashMap<>());
    private final Map<Integer, PresetWiredEffect> wiredEffectConfigs = Collections.synchronizedMap(new HashMap<>());
    private final Map<Integer, PresetWiredTrigger> wiredTriggerConfigs = Collections.synchronizedMap(new HashMap<>());
    private final Map<Integer, PresetWiredAddon> wiredAddonConfigs = Collections.synchronizedMap(new HashMap<>());
    private final Map<Integer, PresetWiredSelector> wiredSelectorConfigs = Collections.synchronizedMap(new HashMap<>());

    private static final Set<String> requireBindings = new HashSet<>(Arrays.asList(
            "wf_act_match_to_sshot", "wf_cnd_match_snapshot", "wf_cnd_not_match_snap", "wf_trg_stuff_state"));
    private final Map<Integer, List<PresetWiredFurniBinding>> wiredFurniBindings = Collections.synchronizedMap(new HashMap<>());


    private final GPresets extension;

    public GPresetExporter(GPresets extension) {
        this.extension = extension;

        extension.intercept(HMessage.Direction.TOSERVER, "UpdateCondition", this::saveCondition);
        extension.intercept(HMessage.Direction.TOSERVER, "UpdateTrigger", this::saveTrigger);
        extension.intercept(HMessage.Direction.TOSERVER, "UpdateAddon", this::saveAddon);
        extension.intercept(HMessage.Direction.TOSERVER, "UpdateSelector", this::saveSelector);
        extension.intercept(HMessage.Direction.TOSERVER, "UpdateAction", this::saveEffect);

        extension.intercept(HMessage.Direction.TOSERVER, "Chat", this::onChat);
        extension.intercept(HMessage.Direction.TOSERVER, "MoveAvatar", this::moveAvatar);

        extension.intercept(HMessage.Direction.TOSERVER, "Open", this::openWired);
        extension.intercept(HMessage.Direction.TOCLIENT, "WiredFurniTrigger", this::retrieveTriggerConf);
        extension.intercept(HMessage.Direction.TOCLIENT, "WiredFurniCondition", this::retrieveConditionConf);
        extension.intercept(HMessage.Direction.TOCLIENT, "WiredFurniAction", this::retrieveActionConf);
        extension.intercept(HMessage.Direction.TOCLIENT, "WiredFurniAddon", this::retrieveAddonConf);
        extension.intercept(HMessage.Direction.TOCLIENT, "WiredFurniSelector", this::retrieveSelectorConf);
    }

    private boolean isReady() {
        return extension.getFloorState().inRoom() && extension.furniDataReady();
    }

    private void onChat(HMessage hMessage) {
        synchronized (lock) {
            String text = hMessage.getPacket().readString();

            if (state == PresetExportState.AWAITING_NAME) {
                hMessage.setBlocked(true);
                int x1 = rectCorner1.getX();
                int y1 = rectCorner1.getY();
                int x2 = rectCorner2.getX();
                int y2 = rectCorner2.getY();

                int dimX = Math.abs(x1 - x2) + 1;
                int dimY = Math.abs(y1 - y2) + 1;

                if (x1 > x2) x1 = x2;
                if (y1 > y2) y1 = y2;

                attemptExport(text, x1, y1, dimX, dimY);
            }
            else if(text.equals(":ep") || text.equals(":exportpreset") || text.equals(":ep all") || text.equals(":exportpreset all")) {
                hMessage.setBlocked(true);

                if (state != PresetExportState.NONE) {
                    extension.sendVisualChatInfo("Already exporting preset.. finish up or abort first");
                }
                else if (!isReady()) {
                    extension.sendVisualChatInfo("Error: no room detected or furnidata not available");
                }
                else {
                    if (text.equals(":ep") || text.equals(":exportpreset")) {
                        state = PresetExportState.AWAITING_RECT1;
                        extension.sendVisualChatInfo("Select the start of the rectangle");
                    }
                    else {
                        // export all
                        rectCorner1 = new HPoint(0,0 );
                        rectCorner2 = new HPoint(100, 100);
                        state = PresetExportState.AWAITING_NAME;
                        extension.sendVisualChatInfo("Enter the name of the preset");
                    }

                }
            }
            else if (text.equals(":abort") || text.equals(":a")) {
                hMessage.setBlocked(true);
                if (state != PresetExportState.NONE) {
                    reset();
                    extension.sendVisualChatInfo("Aborted preset export");
                }
            }
        }
    }

    private void moveAvatar(HMessage hMessage) {
        synchronized (lock) {
            HPacket packet = hMessage.getPacket();

            if (state == PresetExportState.AWAITING_RECT1) {
                hMessage.setBlocked(true);
                int x = packet.readInteger();
                int y = packet.readInteger();
                rectCorner1 = new HPoint(x, y);

                state = PresetExportState.AWAITING_RECT2;
                extension.sendVisualChatInfo("Select the end of the rectangle");
            }
            else if(state == PresetExportState.AWAITING_RECT2) {
                hMessage.setBlocked(true);
                int x = packet.readInteger();
                int y = packet.readInteger();
                rectCorner2 = new HPoint(x, y);

                state = PresetExportState.AWAITING_NAME;
                extension.sendVisualChatInfo("Enter the name of the preset");
            }
        }
    }

    private void maybeSaveBindings(PresetWiredBase wiredBase) {
        wiredFurniBindings.remove(wiredBase.getWiredId());
        FloorState floorState = extension.getFloorState();
        FurniDataTools furniDataTools = extension.getFurniDataTools();

        HFloorItem wiredItem = floorState.furniFromId(wiredBase.getWiredId());
        String className = furniDataTools.getFloorItemName(wiredItem.getTypeId());

        if(requireBindings.contains(className) && wiredBase.getOptions().size() >= 4) {
            boolean bindState = wiredBase.getOptions().get(0) == 1;
            boolean bindDirection = wiredBase.getOptions().get(1) == 1;
            boolean bindPosition = wiredBase.getOptions().get(2) == 1;
            boolean bindAltitude = wiredBase.getOptions().get(3) == 1;

            List<PresetWiredFurniBinding> bindings = new ArrayList<>();
            wiredBase.getItems().forEach(bindItemId -> {
                HFloorItem bindItem = floorState.furniFromId(bindItemId);

                if (bindItem != null) {
                    PresetWiredFurniBinding binding = new PresetWiredFurniBinding(
                            bindItemId,
                            wiredBase.getWiredId(),
                            !bindPosition ? null : new HPoint(bindItem.getTile().getX(), bindItem.getTile().getY()),
                            !bindDirection ? null : bindItem.getFacing().ordinal(),
                            !bindState ? null : StateExtractor.stateFromItem(bindItem),
                            !bindAltitude ? null : (int) (Math.round(bindItem.getTile().getZ() * 100))
                    );
                    bindings.add(binding);
                }
            });

            if (bindings.size() > 0) {
                wiredFurniBindings.put(wiredBase.getWiredId(), bindings);
            }
        }
    }

    private void saveEffect(HMessage hMessage) {
        if (isReady()) {
            PresetWiredEffect preset = new PresetWiredEffect(hMessage.getPacket());
            wiredEffectConfigs.put(preset.getWiredId(), preset);
            maybeSaveBindings(preset);
        }
    }

    private void saveTrigger(HMessage hMessage) {
        if (isReady()) {
            PresetWiredTrigger preset = new PresetWiredTrigger(hMessage.getPacket());
            wiredTriggerConfigs.put(preset.getWiredId(), preset);
            maybeSaveBindings(preset);
        }
    }

    private void saveAddon(HMessage hMessage) {
        if (isReady()) {
            PresetWiredAddon preset = new PresetWiredAddon(hMessage.getPacket());
            wiredAddonConfigs.put(preset.getWiredId(), preset);
            maybeSaveBindings(preset);
        }
    }

    private void saveSelector(HMessage hMessage) {
        if (isReady()) {
            PresetWiredSelector preset = new PresetWiredSelector(hMessage.getPacket());
            wiredSelectorConfigs.put(preset.getWiredId(), preset);
            maybeSaveBindings(preset);
        }
    }

    private void saveCondition(HMessage hMessage) {
        if (isReady()) {
            PresetWiredCondition preset = new PresetWiredCondition(hMessage.getPacket());
            wiredConditionConfigs.put(preset.getWiredId(), preset);
            maybeSaveBindings(preset);
        }
    }

    public void reset() {
        rectCorner1 = null;
        rectCorner2 = null;
        exportName = null;
        state = PresetExportState.NONE;
    }

    public synchronized List<Integer> unRegisteredWiredsInArea(int x, int y, int dimX, int dimY) {
        if (isReady()) {
            FloorState floor = extension.getFloorState();
            FurniDataTools furniDataTools = extension.getFurniDataTools();

            List<Integer> unregisteredWired = new ArrayList<>();
            for (int xi = x; xi < x + dimX; xi++) {
                for (int yi = y; yi < y + dimY; yi++) {
                    floor.getFurniOnTile(xi, yi).forEach(f -> {
                        String classname = furniDataTools.getFloorItemName(f.getTypeId());
                        if (    (classname.startsWith("wf_trg_") && !wiredTriggerConfigs.containsKey(f.getId()))
                                || (classname.startsWith("wf_cnd_") && !wiredConditionConfigs.containsKey(f.getId()))
                                || (classname.startsWith("wf_act_") && !wiredEffectConfigs.containsKey(f.getId()))
                                || (classname.startsWith("wf_xtra_") && !wiredAddonConfigs.containsKey(f.getId()))
                                || (classname.startsWith("wf_slc_") && !wiredSelectorConfigs.containsKey(f.getId()))) {
                            unregisteredWired.add(f.getId());
                        }
                    });
                }
            }

            return unregisteredWired;
        }
        return null;
    }

    private void export(String name, int x, int y, int dimX, int dimY) {
        if (isReady() && (unRegisteredWiredsInArea(x, y, dimX, dimY).size() == 0 || !extension.shouldExportWired())) {
            FloorState floor = extension.getFloorState();
            FurniDataTools furniDataTools = extension.getFurniDataTools();

            List<PresetFurni> allFurni = new ArrayList<>();
            List<PresetWiredCondition> allConditions = new ArrayList<>();
            List<PresetWiredEffect> allEffects = new ArrayList<>();
            List<PresetWiredTrigger> allTriggers = new ArrayList<>();
            List<PresetWiredAddon> allAddons = new ArrayList<>();
            List<PresetWiredSelector> allSelectors = new ArrayList<>();
            List<List<? extends PresetWiredBase>> wiredLists = Arrays.asList(allConditions, allEffects, allTriggers, allAddons, allSelectors);
            List<PresetWiredFurniBinding> allBindings = new ArrayList<>();
            List<PresetAdsBackground> allAdsBackgrounds = new ArrayList<>();



            // first pass: fill all items with copies
            for (int xi = x; xi < x + dimX; xi++) {
                for (int yi = y; yi < y + dimY; yi++) {
                    List<HFloorItem> items = floor.getFurniOnTile(xi, yi);
                    for (HFloorItem f : items) {
                        String classname = furniDataTools.getFloorItemName(f.getTypeId());

                        PresetFurni presetFurni = new PresetFurni(f.getId(), classname, f.getTile(),
                                f.getFacing().ordinal(), StateExtractor.stateFromItem(f));
                        allFurni.add(presetFurni);

                        if (classname.equals("ads_background") && f.getCategory() == 1) {
                            Map<String, String> stuffDataMap = new HashMap<>();
                            Object[] stuff = f.getStuff();
                            for (int i = 0; i < (int)(stuff[0]); i++) {
                                String key = (String)(stuff[i*2 + 1]);
                                String value = (String)(stuff[i*2 + 2]);
                                stuffDataMap.put(key, value);
                            }

                            allAdsBackgrounds.add(new PresetAdsBackground(
                                    f.getId(),
                                    stuffDataMap.get("imageUrl"),
                                    stuffDataMap.get("offsetX"),
                                    stuffDataMap.get("offsetY"),
                                    stuffDataMap.get("offsetZ")
                            ));
                        }

                        if (extension.shouldExportWired()) {
                            if (classname.startsWith("wf_trg_")) {
                                allTriggers.add(new PresetWiredTrigger(wiredTriggerConfigs.get(f.getId())));
                            }
                            else if (classname.startsWith("wf_cnd_")) {
                                allConditions.add(new PresetWiredCondition(wiredConditionConfigs.get(f.getId())));
                            }
                            else if (classname.startsWith("wf_act_")) {
                                allEffects.add(new PresetWiredEffect(wiredEffectConfigs.get(f.getId())));
                            }
                            else if (classname.startsWith("wf_xtra_")) {
                                allAddons.add(new PresetWiredAddon(wiredAddonConfigs.get(f.getId())));
                            }
                            else if (classname.startsWith("wf_slc_")) {
                                allSelectors.add(new PresetWiredSelector(wiredSelectorConfigs.get(f.getId())));
                            }

                            if (wiredFurniBindings.containsKey(f.getId())) {
                                allBindings.addAll(wiredFurniBindings.get(f.getId()).stream()
                                        .map(b -> new PresetWiredFurniBinding(b)).collect(Collectors.toList()));
                            }
                        }
                    }
                }
            }



            // second pass: filter wired furni selections & bindings to only contain items in allFurni
            Set<Integer> allFurniIds = allFurni.stream().map(PresetFurni::getFurniId).collect(Collectors.toSet());

            wiredLists.forEach(l -> l.forEach((Consumer<PresetWiredBase>) w ->
                    w.setItems(w.getItems().stream().filter(allFurniIds::contains).collect(Collectors.toList()))));
            allBindings = allBindings.stream().filter(b -> allFurniIds.contains(b.getFurniId())).collect(Collectors.toList());




            // third pass: normalize furni IDs and subtract offset from locations
            int lowestFloorPoint = PresetUtils.lowestFloorPoint(
                    floor,
                    new HPoint(x, y),
                    new HPoint(x + dimX, y + dimY)
            );
            Map<Integer, Integer> mappedFurniIds = new HashMap<>();
            List<Integer> orderedFurniIds = allFurni.stream().map(PresetFurni::getFurniId).collect(Collectors.toList());
            for (int i = 0; i < orderedFurniIds.size(); i++) {
                mappedFurniIds.put(orderedFurniIds.get(i), i + 1);
            }

            allFurni.forEach(presetFurni -> {
                presetFurni.setFurniId(mappedFurniIds.get(presetFurni.getFurniId()));
                HPoint oldLocation = presetFurni.getLocation();
                presetFurni.setLocation(new HPoint(
                        oldLocation.getX() - x,
                        oldLocation.getY() - y,
                        oldLocation.getZ() - lowestFloorPoint
                ));
            });
            wiredLists.forEach(l -> l.forEach((Consumer<PresetWiredBase>) w -> {
                w.setWiredId(mappedFurniIds.get(w.getWiredId()));
                w.setItems(w.getItems().stream().map(mappedFurniIds::get).collect(Collectors.toList()));
            }));
            allBindings.forEach(b -> {
                b.setFurniId(mappedFurniIds.get(b.getFurniId()));
                b.setWiredId(mappedFurniIds.get(b.getWiredId()));
                HPoint oldLocation = b.getLocation();
                Integer oldAltitude = b.getAltitude();
                if (oldLocation != null) {
                    b.setLocation(new HPoint(
                            oldLocation.getX() - x,
                            oldLocation.getY() - y
                    ));
                }
                if (oldAltitude != null) {
                    b.setAltitude(oldAltitude - lowestFloorPoint * 100);
                }
            });
            allAdsBackgrounds.forEach(a -> {
                a.setFurniId(mappedFurniIds.get(a.getFurniId()));
            });


            // fourth pass: assign names to furniture based on class & remove state information from wired class
            Map<String, Integer> classToCount = new HashMap<>();
            allFurni.forEach(presetFurni -> {
                String className = presetFurni.getClassName();
                if (!classToCount.containsKey(className)) {
                    classToCount.put(className, 0);
                }
                int number = classToCount.get(className);
                classToCount.put(className, number + 1);

                String furniName = String.format("%s[%d]", className, number);
                presetFurni.setFurniName(furniName);

                if (className.startsWith("wf_trg_") || className.startsWith("wf_cnd_")
                        || className.startsWith("wf_act_") || className.startsWith("wf_xtra_")
                        || className.startsWith("wf_slc_")) {
                    presetFurni.setState(null);
                }
            });



            PresetWireds presetWireds = new PresetWireds(allConditions, allEffects, allTriggers, allAddons, allSelectors);
            PresetConfig presetConfig = new PresetConfig(allFurni, presetWireds, allBindings, allAdsBackgrounds);

            PresetConfigUtils.savePreset(name, presetConfig);
            extension.updateInstalledPresets();

            extension.sendVisualChatInfo(String.format(String.format("Exported \"%s\" successfully", name), name));
            extension.getLogger().log(String.format("Exported preset \"%s\" successfully", name), "green");
        }
        else {
            extension.sendVisualChatInfo("ERROR - Couldn't export due to unsufficient resources");
            extension.getLogger().log("Couldn't export due to unsufficient resources", "red");
        }
        reset();
    }

    private void requestWiredConfigsLoop(int x, int y, int dimX, int dimY) {
        try {
            LinkedList<Integer> linkedList;
            synchronized (lock) {
                linkedList = new LinkedList<>(unRegisteredWiredsInArea(x, y, dimX, dimY));
            }

            int originalRemaining = linkedList.size();

            while (state == PresetExportState.FETCHING_UNKNOWN_CONFIGS && linkedList.size() > 0) {
                Integer wiredId = linkedList.pollFirst();
//                Utils.sleep(570);
                Utils.sleep(100);
                extension.sendToServer(new HPacket("Open", HMessage.Direction.TOSERVER, wiredId));
            }

            if (state == PresetExportState.FETCHING_UNKNOWN_CONFIGS) {
                Utils.sleep(300);
                boolean retry = false;
                synchronized (lock) {
                    if (state == PresetExportState.FETCHING_UNKNOWN_CONFIGS) {
                        int remainingLeft = unRegisteredWiredsInArea(x, y, dimX, dimY).size();
                        if (remainingLeft <= originalRemaining / 2) {
                            extension.sendVisualChatInfo(String.format("WARNING - Did not retrieve all wired. Retrying %d missing wired..", remainingLeft));
                            extension.getLogger().log(String.format("Did not retrieve all wired. Retrying %d missing wired..", remainingLeft), "orange");
                            retry = true;
                        }
                        else {
                            // if it's still in this state, something failed..
                            extension.sendVisualChatInfo("ERROR - Couldn't export due to missing wired configurations");
                            extension.getLogger().log("Couldn't export due to missing wired configurations", "red");
                            reset();
                        }
                    }
                }
                if (retry) {
                    requestWiredConfigsLoop(x, y, dimX, dimY);
                }
            }
        }
        catch (Exception e) {
            synchronized (lock) {
                extension.sendVisualChatInfo("ERROR - Something went wrong while fetching configurations..");
                extension.getLogger().log("Something went wrong while fetching configurations..", "red");
                reset();
            }
        }

    }

    private void maybeFinishExportAfterRetrieve() {
        synchronized (lock) {
            if (state == PresetExportState.FETCHING_UNKNOWN_CONFIGS) {
                if (!isReady()) {
                    reset();
                    return;
                }

                int x1 = rectCorner1.getX();
                int y1 = rectCorner1.getY();
                int x2 = rectCorner2.getX();
                int y2 = rectCorner2.getY();

                int dimX = Math.abs(x1 - x2) + 1;
                int dimY = Math.abs(y1 - y2) + 1;

                if (x1 > x2) x1 = x2;
                if (y1 > y2) y1 = y2;

                int remaining = unRegisteredWiredsInArea(x1, y1, dimX, dimY).size();
                if (remaining == 0) {
                    export(exportName, x1, y1, dimX, dimY);
                }
                else if (remaining % 10 == 0) {
                    extension.getLogger().log(String.format("%d wired configurations left to retrieve..", remaining), "orange");
                }
            }
        }
    }

    private void maybeRetrieveBindings(int typeId, PresetWiredBase wired) {
        if (isReady()) {
            FurniDataTools furniData = extension.getFurniDataTools();
            String className = furniData.getFloorItemName(typeId);
            if (requireBindings.contains(className) && !wired.getStringConfig().isEmpty()) {

                List<PresetWiredFurniBinding> bindings = new ArrayList<>();

                String[] bindingsAsStrings = wired.getStringConfig().split(";");
                for (String bindingAsString : bindingsAsStrings) {
                    String[] fields = bindingAsString.split(",");

                    int bindFurniId = Integer.parseInt(fields[0]);
                    String state = fields[1].equals("N") ? null : fields[1];
                    Integer rotation = fields[2].equals("N") ? null : Integer.parseInt(fields[2]);
                    HPoint location = fields[3].equals("N") || fields[4].equals("N") ? null :
                            new HPoint(Integer.parseInt(fields[3]), Integer.parseInt(fields[4]));
                    Integer altitude = fields[5].equals("N") ? null : Integer.parseInt(fields[5]);

                    PresetWiredFurniBinding binding = new PresetWiredFurniBinding(bindFurniId, wired.getWiredId(), location, rotation, state, altitude);
                    bindings.add(binding);
                }

                wiredFurniBindings.put(wired.getWiredId(), bindings);
                wired.setStringConfig("");
            }
        }
    }

    private void retrieveActionConf(HMessage hMessage) {
        if (state == PresetExportState.FETCHING_UNKNOWN_CONFIGS) {
            hMessage.setBlocked(true);
            RetrievedWiredEffect effect = RetrievedWiredEffect.fromPacket(hMessage.getPacket());
            wiredEffectConfigs.put(effect.getWiredId(), effect);

            maybeRetrieveBindings(effect.getTypeId(), effect);
            maybeFinishExportAfterRetrieve();
        }
    }

    private void retrieveAddonConf(HMessage hMessage) {
        if (state == PresetExportState.FETCHING_UNKNOWN_CONFIGS) {
            hMessage.setBlocked(true);
            RetrievedWiredAddon addon = RetrievedWiredAddon.fromPacket(hMessage.getPacket());
            wiredAddonConfigs.put(addon.getWiredId(), addon);

            maybeRetrieveBindings(addon.getTypeId(), addon); // not needed
            maybeFinishExportAfterRetrieve();
        }
    }

    private void retrieveSelectorConf(HMessage hMessage) {
        if (state == PresetExportState.FETCHING_UNKNOWN_CONFIGS) {
            hMessage.setBlocked(true);
            RetrievedWiredSelector selector = RetrievedWiredSelector.fromPacket(hMessage.getPacket());
            wiredSelectorConfigs.put(selector.getWiredId(), selector);

            maybeRetrieveBindings(selector.getTypeId(), selector); // not needed
            maybeFinishExportAfterRetrieve();
        }
    }

    private void retrieveConditionConf(HMessage hMessage) {
        if (state == PresetExportState.FETCHING_UNKNOWN_CONFIGS) {
            hMessage.setBlocked(true);
            RetrievedWiredCondition cond = RetrievedWiredCondition.fromPacket(hMessage.getPacket());
            wiredConditionConfigs.put(cond.getWiredId(), cond);

            maybeRetrieveBindings(cond.getTypeId(), cond);
            maybeFinishExportAfterRetrieve();
        }
    }

    private void retrieveTriggerConf(HMessage hMessage) {
        if (state == PresetExportState.FETCHING_UNKNOWN_CONFIGS) {
            hMessage.setBlocked(true);
            RetrievedWiredTrigger trig = RetrievedWiredTrigger.fromPacket(hMessage.getPacket());
            wiredTriggerConfigs.put(trig.getWiredId(), trig);

            maybeRetrieveBindings(trig.getTypeId(), trig);
            maybeFinishExportAfterRetrieve();
        }
    }

    private void openWired(HMessage hMessage) {
        if (state == PresetExportState.FETCHING_UNKNOWN_CONFIGS) {
            hMessage.setBlocked(true);
            extension.sendVisualChatInfo("Do not open wired while the extension is fetching configurations");
        }
    }

    private void attemptExport(String name, int x, int y, int dimX, int dimY) {
        if (isReady()) {
            List<Integer> unregisteredWired = unRegisteredWiredsInArea(x, y, dimX, dimY);
            if (unregisteredWired.size() > 0 && extension.shouldExportWired()) {
                exportName = name;
                state = PresetExportState.FETCHING_UNKNOWN_CONFIGS;
                extension.sendVisualChatInfo(String.format(
                        "Fetching additional %s wired configurations before exporting... do not alter the room",
                        unregisteredWired.size()
                ));
                new Thread(() -> requestWiredConfigsLoop(x, y, dimX, dimY)).start();
            }
            else {
                export(name, x, y, dimX, dimY);
            }
        }
        else {
            reset();
            extension.sendVisualChatInfo("ERROR - Couldn't export due to missing floorstate or furnidata");
            extension.getLogger().log("Couldn't export due to missing floorstate or furnidata", "red");
        }
    }

    public PresetExportState getState() {
        return state;
    }


    // called from wired importer
    public void cacheWiredConfig(PresetWiredBase presetWiredBase) {

        FurniDataTools furniData = extension.getFurniDataTools();
        FloorState floorState = extension.getFloorState();
        if (furniData == null || floorState == null) return;
        HFloorItem floorItem = floorState.furniFromId(presetWiredBase.getWiredId());
        if (floorItem == null) return;
        int typeId = floorItem.getTypeId();

        String className = furniData.getFloorItemName(typeId);
        if (requireBindings.contains(className)) {
            return;
        }

        if (presetWiredBase instanceof PresetWiredCondition) {
            PresetWiredCondition condition = (PresetWiredCondition) presetWiredBase;
            wiredConditionConfigs.put(condition.getWiredId(), condition);
        }
        if (presetWiredBase instanceof PresetWiredEffect) {
            PresetWiredEffect effect = (PresetWiredEffect) presetWiredBase;
            wiredEffectConfigs.put(effect.getWiredId(), effect);
        }
        if (presetWiredBase instanceof PresetWiredTrigger) {
            PresetWiredTrigger trigger = (PresetWiredTrigger) presetWiredBase;
            wiredTriggerConfigs.put(trigger.getWiredId(), trigger);
        }
        if (presetWiredBase instanceof PresetWiredAddon) {
            PresetWiredAddon addon = (PresetWiredAddon) presetWiredBase;
            wiredAddonConfigs.put(addon.getWiredId(), addon);
        }
        if (presetWiredBase instanceof PresetWiredSelector) {
            PresetWiredSelector selector = (PresetWiredSelector) presetWiredBase;
            wiredSelectorConfigs.put(selector.getWiredId(), selector);
        }
    }
}
