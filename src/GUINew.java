import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/*
 * This is the controller for the new game window
 */
public class GUINew {
	// Variables for controls we want to manipulate
	@FXML private TextField gameName;
	@FXML private ChoiceBox<String> player1;
	@FXML private ChoiceBox<String> player2;

	public GUINew() {
	}

	@FXML
	public void initialize() {
		GUIBase.getControllers().put("new", this);

		// Setup text field
		gameName.setText(System.getProperty("user.name"));

		// Setup choice boxes
		ObservableList<String> playerTypes = FXCollections.observableArrayList("Computer", "Human");
		player1.setItems(playerTypes);
		player2.setItems(playerTypes);
		player1.getSelectionModel().select(1);
		player2.getSelectionModel().select(0);
	}

	public void createGame(ActionEvent event) {
		// Get the players selection
		String selection1 = player1.getSelectionModel().getSelectedItem();
		String selection2 = player2.getSelectionModel().getSelectedItem();

		// If the player wants to have 2 computers play, then warn them about the results
		if (selection1.equals("Computer") && selection1.equals(selection2)) {
			// Create an alert
			Alert a = new Alert(AlertType.CONFIRMATION);
			a.setTitle("Confirm choice");
			a.setHeaderText("Do you want to create a game without any human players?");
			a.setContentText("If the game consists of a computer vs a computer then you will not be able to interact with the game at all.");

			// Set the icon of the alert (getting stage from http://stackoverflow.com/questions/30728425/create-dialogs-with-default-images-in-javafx-info-warning-error)
			Stage stage = (Stage) a.getDialogPane().getScene().getWindow();
			stage.getIcons().add(new Image("res/icon.png"));

			a.showAndWait();
			
			if (a.getResult() == ButtonType.CANCEL) {
				return;
			}
		}

		// Create game, set values, then send it to GUIMain
		Game g = new Game();
		g.setName(gameName.getText());
		g.setPlayer(Colour.values()[0], stringToPlayer(selection1));
		g.setPlayer(Colour.values()[1], stringToPlayer(selection2));

		GUIMain guiMain = (GUIMain) GUIBase.getControllers().get("main");
		guiMain.setGame(g);

		// Get the stage and close it (also trigger handle event)
		Stage stage = (Stage) GUIBase.getStages().get("new");
		stage.getOnCloseRequest().handle(null);
		stage.close();
	}

	private PlayerInterface stringToPlayer(String s) {
		if (s.equals("Computer")) {
			return new ComputerPlayer();
		} else if (s.equals("Human")) {
			return new HumanGUIPlayer();
		} else {
			return null;
		}
	}
}
