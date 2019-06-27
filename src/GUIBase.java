import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.Parent;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;

/*
 * The entry point for GUI creation
 * Most of the logic is contained within the controllers (GUIMain and GUINew)
 * However since we do not initialise the stages and controllers ourselves, this class is used to store them in an appropriate way
 */

public class GUIBase extends Application {
	// Use maps to store stages/controllers (allows more flexibility)
	private static Map<String, Object> controllers;
	private static Map<String, Stage> stages;
	
	@Override
	public void init() {
		// Store in concurrent hashmaps since they can be accessed across different threads (but are barely written to)
		controllers = new ConcurrentHashMap<String, Object>();
		stages = new ConcurrentHashMap<String, Stage>();
	}
	
	@Override
	public void start(Stage stage) throws IOException {
		stages.put("main", stage);
		Parent root = FXMLLoader.load(getClass().getResource("GUI_Main.fxml"));
		Scene scene = new Scene(root, 650, 630);
		
		// Apply css to scene
		scene.getStylesheets().add("GUI_Main.css");

		// Set icon
		// Code used from http://stackoverflow.com/questions/10121991/javafx-application-icon
		// Handle exception if icon not found (not too important)
		try {
			stage.getIcons().add(new Image("res/icon.png"));
		} catch (Exception e) {
			System.out.println("Could not load icon");
			System.out.println(e.toString());
		}
		
		stage.setTitle("Tabula - wrtl76");
		stage.setScene(scene);
		stage.setResizable(true);
		stage.setMinWidth(665);
		stage.setMinHeight(400); // This height makes sure that at least 1 row of numeric locations are visible
		stage.show();
		
		// When we first load the program, show the new stage dialog automatically
		GUIMain guiMain = (GUIMain) controllers.get("main");
		guiMain.promptNewGame();
		
	}
	
	// Getters for map objects
	public static Map<String, Object> getControllers() {
		return controllers;
	}
	
	public static Map<String, Stage> getStages() {
		return stages;
	}
}
