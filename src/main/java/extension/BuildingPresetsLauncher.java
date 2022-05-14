package extension;

import gearth.extensions.ThemedExtensionFormCreator;
import javafx.stage.Stage;

import java.net.URL;

public class BuildingPresetsLauncher extends ThemedExtensionFormCreator {

    @Override
    protected String getTitle() {
        return "Building Presets 0.2.3";
    }

    @Override
    protected URL getFormResource() {
        return getClass().getResource("ui/buildingpresets.fxml");
    }

    @Override
    protected void initialize(Stage primaryStage) {
        primaryStage.getScene().getStylesheets().add(getClass().getResource("ui/styles.css").toExternalForm());
    }

    public static void main(String[] args) {
        runExtensionForm(args, BuildingPresetsLauncher.class);
    }

}
