import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/*
 * Each location in the game must have a control in the GUI
 * This class is used to represent those obejcts
 * http://docs.oracle.com/javafx/2/fxml_get_started/custom_control.htm
 */
public class ControlLocation extends VBox {
	private LocationInterface location;
	
	// Handles to controls
	@FXML private Label labelName;
	@FXML private VBox vboxContainer;	
	
	private Map<Colour, Label> pieces;

	public ControlLocation(LocationInterface loc) {
		this.location = loc;

		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Control_location.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);

		try {
			fxmlLoader.load();
		} catch (IOException e) {
			System.out.println("Error loading location control resource");
			System.out.println(e.toString());
		}
		
		
	}
	
	@FXML
	private void initialize() {
		pieces = new HashMap<Colour, Label>();
		
		for (Colour c : Colour.values()) {
			Label l = new Label();
			vboxContainer.getChildren().add(l);
			
			pieces.put(c, l);
		}
		
		update();
	}
	
	public void setLocation(LocationInterface loc) {
		this.location = loc;
		update();
	}
	
	public void update() {
		// If we can convert the location's name to an int, then display it as a roman numeral
		// Otherwise display it in capitals
		try {
			int number = Integer.parseInt(location.getName());
			labelName.setText(RomanNumber.toRoman(number));
		} catch (NumberFormatException e) {
			labelName.setText(location.getName().toUpperCase());
		}
		
		
		for (Colour c : pieces.keySet()) {
			int numPieces = location.numberOfPieces(c);
			if (numPieces > 0) {
				pieces.get(c).setText(c.toString() + ": " + numPieces);
			} else {
				pieces.get(c).setText("");
			}
			
		}
	}
}
