package extension;

import gearth.extensions.ThemedExtensionFormCreator;
import javafx.stage.Stage;

import java.net.URL;

public class WiredPresetsLauncher extends ThemedExtensionFormCreator {

    @Override
    protected String getTitle() {
        return "Wired Presets 0.2.1";
    }

    @Override
    protected URL getFormResource() {
        return getClass().getResource("ui/wiredpresets.fxml");
    }

    @Override
    protected void initialize(Stage primaryStage) {
        primaryStage.getScene().getStylesheets().add(getClass().getResource("ui/styles.css").toExternalForm());
    }

    public static void main(String[] args) {
        runExtensionForm(args, WiredPresetsLauncher.class);
    }

}
