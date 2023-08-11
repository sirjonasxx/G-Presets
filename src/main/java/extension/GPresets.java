package extension;

import extension.logger.Logger;
import extension.tools.PresetUtils;
import extension.tools.StackTileSetting;
import extension.tools.GPresetExporter;
import extension.tools.GPresetImporter;
import extension.tools.importutils.AvailabilityChecker;
import extension.tools.importutils.FurniDropInfo;
import extension.tools.postconfig.FurniPostConfig;
import extension.tools.postconfig.ItemSource;
import extension.tools.postconfig.PostConfig;
import extension.tools.presetconfig.PresetConfig;
import extension.tools.presetconfig.PresetConfigUtils;
import extension.tools.presetconfig.furni.PresetFurni;
import furnidata.FurniDataTools;
import game.BCCatalog;
import game.FloorState;
import game.Inventory;
import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionInfo;
import gearth.extensions.parsers.HFloorItem;
import gearth.extensions.parsers.HPoint;
import gearth.misc.Cacher;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import org.json.JSONObject;
import utils.Utils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@ExtensionInfo(
        Title =  "G-Presets",
        Description =  "Never do anything twice",
        Version =  "1.0.3",
        Author =  "sirjonasxx"
)
public class GPresets extends ExtensionForm {

    public BorderPane logsBorderPane;

    public Label cndConnectedLbl;
    public Label cndRoomLbl;
    public Label cndInventoryLbl;
    public Label cndStackTileLbl;
    public Label cndFurnidataLbl;
    public Label cndBCShopLbl;

    public ToggleGroup stacktile_tgl;

    public RadioButton onlyInvCbx;
    public RadioButton preferInvCbx;
    public RadioButton preferBcCbx;
    public RadioButton onlyBcCbx;
    public ToggleGroup item_src_tgl;
    public ListView<String> presetListView;

    public Button availabilityBtn;
    public Button currentPresetBtn;
    public CheckBox allowIncompleteBuildsCbx;

    public TextField furniNamePC_txt;
    public TextField replacementIdPC_txt;
    public TableView<FurniPostConfig> postconfigTable;
    public GridPane pcgrid;
    public Label postconfigErrorLbl;
    public CheckBox noExportWiredCbx;
    public Slider ratelimiter;
    public CheckBox onTopCbx;

    private List<FurniPostConfig> furniPostConfigs = new ArrayList<>();

    private Logger logger = new Logger();

    private FurniDataTools furniDataTools = null;
    private Inventory inventory = null;
    private FloorState floorState = null;
    private BCCatalog catalog = null;
    private volatile boolean isConnected = false;

    private GPresetExporter exporter = null;
    private GPresetImporter importer = null;

    private volatile long latestPingTimestamp = -1;
    private volatile int ping = 45;
    private volatile double pingVariation = 10;

    private StackTileSetting stackTileSetting = StackTileSetting.Large;


    public void initialize() {
        setupCache();

        logsBorderPane.setPadding(new Insets(5, 5, 5, 5));
        logger.initialize(logsBorderPane);

        logger.log("Welcome to G-Presets!", "purple");
        logger.log("Use the following commands:", "purple");
        logger.log("* :exportpreset [all] / :ep [all]", "purple");
        logger.log("* :importpreset [x,y] / :ip [x,y]", "purple");

        stacktile_tgl.selectedToggleProperty().addListener(observable -> {
            String option = ((RadioButton)(stacktile_tgl.getSelectedToggle())).getText();
            Cacher.put("stacktile", option);
            stackTileSetting = StackTileSetting.fromString(option);
            updateUI();
        });

        noExportWiredCbx.selectedProperty().addListener(observable ->
                Cacher.put("noExportWired", noExportWiredCbx.isSelected())
        );

        allowIncompleteBuildsCbx.selectedProperty().addListener(observable ->
                Cacher.put("allowIncompleteBuilds", allowIncompleteBuildsCbx.isSelected())
        );



        postconfigTable = new TableView<>();
//        postconfigTable.setTableMenuButtonVisible(true);
        postconfigTable.setStyle("-fx-focus-color: white;");

//        postconfigTable.focusedProperty().addListener(observable -> {
//            if (postconfigTable.isFocused()) {
//                pcgrid.requestFocus();
//            }
//        });

        TableColumn<FurniPostConfig, String> furniNameColumn = new TableColumn<>("Furni name");
        furniNameColumn.setCellValueFactory(new PropertyValueFactory<>("furniIdentifier"));
        furniNameColumn.setPrefWidth(110);

        TableColumn<FurniPostConfig, Integer> existingFurniIdColumn = new TableColumn<>("Existing furni ID");
        existingFurniIdColumn.setCellValueFactory(new PropertyValueFactory<>("existingFurniId"));
        existingFurniIdColumn.setPrefWidth(120);

        postconfigTable.getColumns().addAll(Arrays.asList(furniNameColumn, existingFurniIdColumn));

        // https://stackoverflow.com/questions/20802208/delete-a-row-from-a-javafx-table-using-context-menu
        postconfigTable.setRowFactory(tableView -> {
            final TableRow<FurniPostConfig> row = new TableRow<>();
            final ContextMenu contextMenu = new ContextMenu();
            final MenuItem removeMenuItem = new MenuItem("Remove");
            removeMenuItem.setOnAction(event -> {
                FurniPostConfig config = row.getItem();
                if (config != null) {
                    furniPostConfigs.remove(config);
                    updatePostConfig();
                    updateFurniPostConfigsView();
                }
            });
            contextMenu.getItems().add(removeMenuItem);
            // Set context menu on row, but use a binding to make it only show for non-empty rows:
            row.contextMenuProperty().bind(
                    Bindings.when(row.emptyProperty())
                            .then((ContextMenu)null)
                            .otherwise(contextMenu)
            );
            return row ;
        });

        pcgrid.add(postconfigTable, 0, 0);



        ratelimiter.valueProperty().addListener((observable, oldValue, newValue) -> {
            int val = newValue.intValue();
            Utils.setExtraSleepTime(val);
            Cacher.put("ratelimit", val);
        });
    }

    @Override
    protected void initExtension() {
        this.floorState = new FloorState(this, logger, this::updateUI, () -> {
            this.updateUI();
            this.exporter.reset();
            logger.log("Leaving room..", "blue");
        });
        this.inventory = new Inventory(this, logger, this::updateUI);
        this.catalog = new BCCatalog(this, logger, this::updateUI);

        this.exporter = new GPresetExporter(this);
        this.importer = new GPresetImporter(this);

        onConnect((host, i, s1, s2, hClient) -> furniDataTools = new FurniDataTools(host, this::updateUI));




        intercept(HMessage.Direction.TOSERVER, "LatencyPingRequest", hMessage -> {
            latestPingTimestamp = System.currentTimeMillis();
        });
        intercept(HMessage.Direction.TOCLIENT, "LatencyPingResponse", hMessage -> {
            if (latestPingTimestamp != -1) {
                int newPing = (int) (System.currentTimeMillis() - latestPingTimestamp) / 2;
                pingVariation = pingVariation * 0.66 + (Math.abs(ping - newPing)) * 0.34;
                if (pingVariation > 10) {
                    pingVariation = 10;
                }
                ping = newPing;
            }
        });

        this.floorState.requestRoom(this);
        updateUI();
        updateInstalledPresets();

        item_src_tgl.selectedToggleProperty().addListener(o -> updatePostConfig());
        presetListView.setOnMouseClicked(event -> {
            if(event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
                String presetName = presetListView.getSelectionModel().getSelectedItem();
                PresetConfig preset = PresetConfigUtils.loadPreset(presetName);
                if (preset != null) {
                    selectPreset(preset, presetName);
                }
            }
            updateUI();
        });

        updatePostConfig();
    }

    private void setupCache() {
        File extDir = null;
        try {
            extDir = (new File(GPresets.class.getProtectionDomain().getCodeSource().getLocation().toURI())).getParentFile();
            if (extDir.getName().equals("Extensions")) {
                extDir = extDir.getParentFile();
            }
        } catch (URISyntaxException ignored) {}

        Cacher.setCacheDir(extDir + File.separator + "Cache");
        loadCache();
    }

    private void loadCache() {
        JSONObject cache = Cacher.getCacheContents();

        String stackTileKey = cache.optString("stacktile", "2x2");
        stackTileSetting = StackTileSetting.fromString(stackTileKey);
        stacktile_tgl.getToggles()
                .stream()
                .filter(s -> ((RadioButton) s).getText().equals(stackTileKey))
                .findFirst()
                .orElseGet(() -> stacktile_tgl.getToggles().get(2))
                .setSelected(true);

        String itemSourceKey = cache.optString("itemSource", "ONLY_INVENTORY");
        item_src_tgl.getToggles()
                .stream()
                .filter(s -> itemSourceKey.equals(s.getUserData()))
                .findFirst()
                .orElseGet(() -> item_src_tgl.getToggles().get(0))
                .setSelected(true);

        ratelimiter.setValue(cache.optInt("ratelimit", 22));
        Utils.setExtraSleepTime(cache.optInt("ratelimit", 22));

        noExportWiredCbx.setSelected(cache.optBoolean("noExportWired"));
        allowIncompleteBuildsCbx.setSelected(cache.optBoolean("allowIncompleteBuilds"));
    }

    private void selectPreset(PresetConfig preset, String name) {
        logger.log(String.format("Selected \"%s\" preset", name), "green");
        importer.setPresetConfig(preset);
        HPoint dim = PresetUtils.presetDimensions(preset);
        logger.log(String.format("Preset dimensions: %dx%d", dim.getX(), dim.getY()), "green");
        updateUI();
    }

    @Override
    protected void onStartConnection() {
        latestPingTimestamp = System.currentTimeMillis();
//        sendToServer(new HPacket("LatencyPingRequest", HMessage.Direction.TOSERVER, -1));

        isConnected = true;
        updateUI();
    }

    @Override
    protected void onEndConnection() {
        isConnected = false;
        furniDataTools = null;
        floorState.reset();
        inventory.clear();
        catalog.clear();
        exporter.reset();
        importer.reset();
        updateUI();
    }

    public HFloorItem stackTile() {
        if (!floorState.inRoom() || !furniDataTools.isReady()) return null;
        List<HFloorItem> items = floorState.getItemsFromType(furniDataTools, stackTileSetting.getClassName());
        if (items.size() == 0) return null;
        return items.get(0);
    }

    public boolean furniDataReady() {
        return furniDataTools != null && furniDataTools.isReady();
    }

//
//    public boolean isReady() {
//        return isConnected && floorState.inRoom() && furniDataReady() && inventory.getState() == Inventory.InventoryState.LOADED
//                && stackTile() != null;
//    }
//
//    private boolean BCCatalogAvailable() {
//        return catalog.getState() == BCCatalog.CatalogState.COLLECTED;
//    }

    private void updateLabel(Label lbl, boolean isFullfilled, boolean isBusy, boolean isOptional) {
        lbl.getStyleClass().removeAll(lbl.getStyleClass().stream().filter(p -> p.startsWith("lbl")).collect(Collectors.toList()));
        lbl.getStyleClass().add(
                isFullfilled ?
                        "lblgreen" : (isBusy ? "lblorange" : (isOptional ? "lblgrey" : "lblred")));
    }

    private void updateLabel(Label lbl, boolean isFullfilled, boolean isBusy) {
        updateLabel(lbl, isFullfilled, isBusy, false);
    }

    private void updateLabel(Label lbl, boolean isFullfilled) {
        updateLabel(lbl, isFullfilled, false);
    }

    private void updateUI() {
        Platform.runLater(() -> {
            updateLabel(cndConnectedLbl, isConnected);
            updateLabel(cndRoomLbl, floorState.inRoom());
            updateLabel(cndFurnidataLbl, furniDataReady());
            updateLabel(cndBCShopLbl, catalog.getState() == BCCatalog.CatalogState.COLLECTED,
                    catalog.getState() == BCCatalog.CatalogState.COLLECTING_PAGES);
            updateLabel(cndInventoryLbl, inventory.getState() == Inventory.InventoryState.LOADED,
                    inventory.getState() == Inventory.InventoryState.LOADING);
            updateLabel(cndStackTileLbl, stackTile() != null);

            availabilityBtn.setDisable(importer.getPresetConfig() == null);
            currentPresetBtn.setDisable(presetListView.getSelectionModel().getSelectedItem() == null);
        });
    }

    public void updateInstalledPresets() {
        List<String> installed = PresetConfigUtils.listPresets();
        Platform.runLater(() -> {
            presetListView.getItems().clear();
            presetListView.getItems().addAll(installed);
        });
    }

    public void sendVisualChatInfo(String text) {
        sendToClient(new HPacket("Whisper", HMessage.Direction.TOCLIENT, -1, text, 0, 30, 0, -1));
    }

    public void loadInventoryClick(ActionEvent actionEvent) {
        inventory.requestInventory();
    }

    public void loadBCClick(ActionEvent actionEvent) {
        catalog.requestIndex();
    }

    public void clearBCClick(ActionEvent actionEvent) throws URISyntaxException {
        catalog.clearCache();
    }

    public FurniDataTools getFurniDataTools() {
        return furniDataTools;
    }

    public Logger getLogger() {
        return logger;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public FloorState getFloorState() {
        return floorState;
    }

    public BCCatalog getCatalog() {
        return catalog;
    }

    public StackTileSetting getStackTileSetting() {
        return stackTileSetting;
    }

    public int getSafeFeedbackTimeout() {
        return (int)((ping + pingVariation + 10) * 3);
    }

    private void updatePostConfig() {
        importer.setPostConfig(createPostConfig());
        updateUI();
    }

    private PostConfig createPostConfig() {
        PostConfig postConfig = new PostConfig();

        ItemSource itemSource = ItemSource.valueOf((String) item_src_tgl.getSelectedToggle().getUserData());
        Cacher.put("itemSource", itemSource);

        postConfig.setItemSource(itemSource);

        furniPostConfigs.forEach(postConfig::addFurniPostConfig);
        return postConfig;
    }

    public void availabilityBtnClick(ActionEvent actionEvent) {
        PresetConfig presetConfig = importer.getPresetConfig();
        PostConfig postConfig = createPostConfig();

        if (presetConfig != null && furniDataReady()) {
            PresetConfig combined = new PresetConfig(presetConfig.toJsonObject());
            combined.applyPostConfig(postConfig);

            List<FurniDropInfo> fakeDropInfo = new ArrayList<>();
            for (PresetFurni f : combined.getFurniture()) {
                fakeDropInfo.add(new FurniDropInfo(-1, -1, furniDataTools.getFloorTypeId(f.getClassName()), postConfig.getItemSource(), -1));
            }

            AvailabilityChecker.printAvailability(logger, fakeDropInfo, inventory, catalog, furniDataTools);
        }
        else {
            logger.log("No preset chosen or furnidata not ready", "red");
        }

    }

    public void openPresetsFolderClick(ActionEvent actionEvent) {
        try {
            Desktop.getDesktop().open(new File(PresetConfigUtils.presetPath()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void openCurrentPresetClick(ActionEvent actionEvent) {
        String selectedPreset = presetListView.getSelectionModel().getSelectedItem();
        if (selectedPreset != null) {
            try {
                Desktop.getDesktop().edit(new File(PresetConfigUtils.presetPath(), selectedPreset + ".txt"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void reloadPresetsClick(ActionEvent actionEvent) {
        updateInstalledPresets();
        updateUI();
    }

    public boolean allowIncompleteBuilds() {
        return allowIncompleteBuildsCbx.isSelected();
    }

    private void updateFurniPostConfigsView() {
        Platform.runLater(() -> {
            postconfigTable.getItems().clear();
            postconfigTable.getItems().addAll(furniPostConfigs);
        });
        updateUI();
    }

    public void updatePostconfigClick(ActionEvent actionEvent) {
        try {
            if (furniNamePC_txt.getText().isEmpty() || replacementIdPC_txt.getText().isEmpty()) throw new Exception();

            FurniPostConfig furniPostConfig = new FurniPostConfig(
                    furniNamePC_txt.getText(),
                    true, Integer.parseInt(replacementIdPC_txt.getText()),
                    null, null, null, null, new HashMap<>(), new HashMap<>()
            );
            furniPostConfigs = furniPostConfigs.stream().filter(c -> !c.getFurniIdentifier().equals(furniPostConfig.getFurniIdentifier())).collect(Collectors.toList());
            furniPostConfigs.add(furniPostConfig);
            updatePostConfig();
            updateFurniPostConfigsView();
            Platform.runLater(() -> postconfigErrorLbl.setText(""));
        }
        catch (Exception e) {
//            e.printStackTrace();
            Platform.runLater(() -> postconfigErrorLbl.setText("You entered invalid information!"));
        }
    }

    public boolean shouldExportWired() {
        return !noExportWiredCbx.isSelected();
    }

    public GPresetExporter getExporter() {
        return exporter;
    }

    public void alwaysOnTopClick(ActionEvent actionEvent) {
        primaryStage.setAlwaysOnTop(onTopCbx.isSelected());
    }
}
