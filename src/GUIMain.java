import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

/*
 * This is the controller for the main window
 */
public class GUIMain {
	// Store the game state, as well as the task (separate thread) to run it on
	private Game game;
	private Task<Colour> task;

	// Variables for controls we want to manipulate
	@FXML private Button buttonAdd;
	@FXML private Button buttonClear;
	@FXML private Button buttonSubmit;
	@FXML private ChoiceBox<Integer> choiceDiceValues;
	@FXML private ChoiceBox<String> choiceSourceLoc;
	@FXML private FlowPane flowPaneLocations;
	@FXML private HBox hboxDiceContainer;
	@FXML private HBox hboxSpecialLocations;
	@FXML private HBox hboxTurnInput;
	@FXML private Label labelGame;
	@FXML private Label labelPlayer;
	@FXML private Label labelStatus;
	@FXML private MenuItem menuLoad;
	@FXML private MenuItem menuNew;
	@FXML private MenuItem menuPause;
	@FXML private MenuItem menuPlay;
	@FXML private MenuItem menuSave;
	@FXML private TableView<TableMove> tableView;

	// List to store the moves in for displaying in the table view
	private ObservableList<TableMove> tableMoveItems;
	// List to store all the custom location controls (0 = start, 1 = knocked, 2 = end, rest are the numbers)
	private List<ControlLocation> controlLocations;
	// List to store the dice images
	private List<Image> diceImages;

	public GUIMain() {
		game = new Game();
		game.setPlayer(Colour.values()[0], new HumanGUIPlayer());
		game.setPlayer(Colour.values()[1], new ComputerPlayer());
		game.setName(System.getProperty("user.name"));
	}

	@FXML
	public void initialize() {
		GUIBase.getControllers().put("main", this);
		initialiseDiceImages();
		initialiseLocations();
		initialiseTable();
		updateMenus(false);
		updateMenuPause(false);
		updateText();
		updateInputControls(false);
		labelStatus.setWrapText(true);
	}

	/*
	 * Public methods
	 */
	public Task<Colour> getTask() {
		return task;
	}

	public void promptNewGame() {
		Game oldGame = game;

		// If we want to create a new game, then create the popup window
		Parent root;
		try {
			root = FXMLLoader.load(getClass().getResource("GUI_New.fxml"));
		} catch (IOException e) {
			return;
		}
		Scene scene = new Scene(root, 250, 140);
		scene.getStylesheets().add("GUI_New.css");
		Stage child = new Stage();
		GUIBase.getStages().put("new", child);

		try {
			// See GUIMain
			child.getIcons().add(new Image("res/icon.png"));
		} catch (Exception e) {
			System.out.println("Could not load icon");
			System.out.println(e.toString());
		}

		// We can't get the stage from the MenuItem object, but we can from other objects (in this case a label)
		// We want the child window to be attached to the parent window, and the only window you can focus on
		child.initOwner(GUIBase.getStages().get("main"));
		child.initModality(Modality.APPLICATION_MODAL);

		// Set other parameters of the child window
		child.setTitle("New Game");
		child.setScene(scene);
		child.setResizable(false);
		child.setOnCloseRequest(e -> {
			updateNewGame(oldGame);
		});
		child.show();
	}

	public void setGame(Game g) {
		this.game = g;
	}

	/*
	 * We need some methods to be synchronized, since they are used by HumanGUIPlayer, which exists in a separate thread
	 */
	public synchronized void addMove(MoveInterface move) {
		TableMove entry = new TableMove(move);
		tableMoveItems.add(entry);
	}

	public synchronized void clearTable() {
		tableMoveItems = FXCollections.observableArrayList();
		tableView.setItems(tableMoveItems);
	}

	public synchronized void registerPlayer(HumanGUIPlayer player) {
		// If a null value is given then we unregister all of the events
		if (player == null) {
			buttonAdd.setOnAction(null);
			buttonClear.setOnAction(null);
			buttonSubmit.setOnAction(null);
			menuPause.setOnAction(null);
			choiceSourceLoc.setOnAction(null);
			choiceDiceValues.setOnAction(null);
		} else {
			buttonAdd.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent e) {
					// Get source location and dice value
					player.addMove(choiceSourceLoc.getSelectionModel().getSelectedItem(), choiceDiceValues.getSelectionModel().getSelectedItem());
				}
			});
			buttonClear.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent e) {
					player.clearMoves();
				}
			});
			buttonSubmit.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent e) {
					player.submitTurn();
				}
			});
			menuPause.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent e) {
					player.pause();
				}
			});
			choiceSourceLoc.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent e) {
					String selection = choiceSourceLoc.getSelectionModel().getSelectedItem();
					player.sourceLocSelected(selection);
				}
			});
			choiceDiceValues.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent e) {
					Integer selection = choiceDiceValues.getSelectionModel().getSelectedItem();
					player.diceValueSelected(selection);
				}
			});
		}
	}

	public synchronized void showAddButton(boolean show) {
		buttonAdd.setDisable(!show);
	}

	public synchronized void updateDiceChoices(List<Integer> data, boolean show) {
		ObservableList<Integer> diceValues = FXCollections.observableArrayList(data);
		choiceDiceValues.setItems(diceValues);
		choiceDiceValues.setDisable(!show);

		if (diceValues.size() == 1) {
			choiceDiceValues.getSelectionModel().selectFirst();
		}
	}

	public synchronized void updateDiceImages(List<Integer> diceValues) {
		// Update the dice images

		int noImageViews = hboxDiceContainer.getChildren().size();
		for (int i = 0; i < noImageViews; i++) {
			int rollToUse = 0;

			if (diceValues.size() > i) {
				rollToUse = diceValues.get(i);
			}

			ImageView iv = (ImageView) hboxDiceContainer.getChildren().get(i);
			iv.setImage(diceImages.get(rollToUse));
		}
	}

	// Enable/disable the controls used to input a turn, depending on whether or not the user needs to make a turn or not
	public synchronized void updateInputControls(boolean isTurn) {
		hboxTurnInput.setDisable(!isTurn);
	}

	public synchronized void updateLocations(BoardInterface board) {
		controlLocations.get(0).setLocation(board.getStartLocation());
		controlLocations.get(1).setLocation(board.getKnockedLocation());
		controlLocations.get(2).setLocation(board.getEndLocation());

		for (int i = 1; i <= BoardInterface.NUMBER_OF_LOCATIONS; i++) {
			try {
				controlLocations.get(i + 2).setLocation(board.getBoardLocation(i));
			} catch (NoSuchLocationException e) {
				// Should never be reached
				System.out.println("Error retrieving board location when updating controls");
			}
		}
	}

	public synchronized void updateMaxMovesStatus(boolean show, int movesPossible, int diceAvailable) {
		if (show) {
			labelStatus.setText("Only " + movesPossible + "/" + diceAvailable + " of the dice can be used");
		} else {
			labelStatus.setText("");
		}
	}

	public synchronized void updateMenus(boolean isPlaying) {
		// Enable/disable the menu controls depending on whether or not the game is being played or not
		menuLoad.setDisable(isPlaying);
		menuNew.setDisable(isPlaying);

		// Only show the save/play game menu item if there is an unfinished game to save
		if (game != null && game.getBoard().winner() == null) {
			menuPlay.setDisable(isPlaying);
			menuSave.setDisable(isPlaying);
		} else {
			menuPlay.setDisable(true);
			menuSave.setDisable(true);
		}
	}

	public synchronized void updateMenuPause(boolean canPause) {
		menuPause.setDisable(!canPause);
	}

	public synchronized void updateSourceLocs(List<String> data, boolean show) {
		ObservableList<String> sourceLocs = FXCollections.observableArrayList(data);
		choiceSourceLoc.setItems(sourceLocs);
		choiceSourceLoc.setDisable(!show);
	}

	public synchronized void updateText() {
		StringBuilder sGame = new StringBuilder("Game: ");
		StringBuilder sPlayer = new StringBuilder("Player: ");

		if (game != null) {
			sGame.append(game.getName());
			sPlayer.append(game.getCurrentPlayer().toString());
		}

		labelGame.setText(sGame.toString());
		labelPlayer.setText(sPlayer.toString());
	}

	/*
	 * Private methods
	 */

	private void alertIOError(String ioType, IOException e) {
		Alert a = new Alert(AlertType.ERROR);
		a.setTitle("I/O Error");
		a.setHeaderText("Error " + ioType + " the game");
		a.setContentText(e.getMessage());

		// Set the icon of the alert (getting stage from http://stackoverflow.com/questions/30728425/create-dialogs-with-default-images-in-javafx-info-warning-error)
		Stage stage = (Stage) a.getDialogPane().getScene().getWindow();
		stage.getIcons().add(new Image("res/icon.png"));

		a.showAndWait();
	}

	@FXML
	private void handleMenuItem(ActionEvent event) throws IOException {
		// Handle menu item presses
		// We don't handle the pause menu item, since that is controlled by HumanGUIPlayer
		if (event.getSource() == menuNew) {
			promptNewGame();
		}
		if (event.getSource() == menuPlay) {
			playGame();
		}
		if (event.getSource() == menuSave) {
			saveGame();
		}
		if (event.getSource() == menuLoad) {
			loadGame();
		}
	}

	private void handleTaskEnd() {
		// When the game ends (paused or a player has won), this method is called by the task
		updateMenus(false);
		updateSourceLocs(new ArrayList<String>(), false); // Clear the choice boxes
		updateDiceChoices(new ArrayList<Integer>(), false);

		// Schedule this for later, in case other classes have scheduled this (eg. computer player)
		Platform.runLater(new Runnable() {
			public void run() {
				updateDiceImages(new ArrayList<Integer>());
				updateLocations(game.getBoard());
			}
		});

		// If there is a winner then show the dialogue
		if (task.getValue() != null) {
			Alert a = new Alert(AlertType.INFORMATION);
			a.setTitle("Game over");
			a.setHeaderText("Player " + task.getValue().toString() + " has won!");

			// Set the icon of the alert (getting stage from http://stackoverflow.com/questions/30728425/create-dialogs-with-default-images-in-javafx-info-warning-error)
			Stage stage = (Stage) a.getDialogPane().getScene().getWindow();
			stage.getIcons().add(new Image("res/icon.png"));

			a.showAndWait();
		}
	}

	private void initialiseDiceImages() {
		// Load the dice images (0 is a blank image). Could also be done using a primitive array
		// http://clipart-library.com/clipart/pT7KbK78c.htm
		diceImages = new ArrayList<Image>();
		for (int i = 0; i <= DieInterface.NUMBER_OF_SIDES_ON_DIE; i++) {
			diceImages.add(new Image("res/die" + Integer.toString(i) + ".png"));
		}

		// Create 4 image view nodes in the hbox
		for (int i = 0; i < 4; i++) {
			ImageView iv = new ImageView();
			iv.setFitHeight(26);
			iv.setFitWidth(26);
			iv.setCache(true);
			iv.setSmooth(true);
			iv.setPreserveRatio(true);
			iv.setImage(diceImages.get(0));

			hboxDiceContainer.getChildren().add(iv);
		}
	}

	private void initialiseLocations() {
		// When we initialise the locations we want them to represent the default board location
		// To get the default board state we create a blank board, since we might not have created a game yet
		BoardInterface board = new Board();
		controlLocations = new ArrayList<ControlLocation>();

		// Special locations for the HBox
		ControlLocation start = new ControlLocation(board.getStartLocation());
		ControlLocation knocked = new ControlLocation(board.getKnockedLocation());
		ControlLocation end = new ControlLocation(board.getEndLocation());
		hboxSpecialLocations.getChildren().add(start);
		hboxSpecialLocations.getChildren().add(knocked);
		hboxSpecialLocations.getChildren().add(end);
		controlLocations.add(start);
		controlLocations.add(knocked);
		controlLocations.add(end);

		for (int i = 1; i <= BoardInterface.NUMBER_OF_LOCATIONS; i++) {
			try {
				ControlLocation cloc = new ControlLocation(board.getBoardLocation(i));
				flowPaneLocations.getChildren().add(cloc);
				controlLocations.add(cloc);
			} catch (NoSuchLocationException e) {
				// This should never be reached
				System.out.println("Error initialising control locations");
			}
		}
	}

	private void initialiseTable() {
		// https://docs.oracle.com/javase/8/javafx/user-interface-tutorial/table-view.htm
		for (TableColumn<TableMove, ?> col : tableView.getColumns()) {
			col.setMinWidth(40);
			col.setCellValueFactory(new PropertyValueFactory<>(col.getText()));
		}
		tableMoveItems = FXCollections.observableArrayList();
		tableView.setItems(tableMoveItems);
		tableView.setPlaceholder(new Label("No moves made"));
	}

	private void loadGame() {
		// Open a filechooser to get the file to load
		// http://docs.oracle.com/javafx/2/ui_controls/file-chooser.htm
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Load game");
		fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Text file", "*.txt"), new FileChooser.ExtensionFilter("Properties file", "*.properties"));
		File file = fileChooser.showOpenDialog(GUIBase.getStages().get("main"));

		if (file == null) {
			return;
		}

		try {
			game.loadGame(file.toString());
		} catch (IOException e) {
			alertIOError("loading", e);
		}

		updateText();
		updateMenus(false);
		updateLocations(game.getBoard());
	}

	private void playGame() {
		// Can't start a game if one doesn't exist
		if (game == null) {
			// Create an alert
			Alert a = new Alert(AlertType.WARNING);
			a.setTitle("Warning");
			a.setHeaderText("No game to play");
			a.setContentText("You need to create/load a game first");

			// Set the icon of the alert (getting stage from http://stackoverflow.com/questions/30728425/create-dialogs-with-default-images-in-javafx-info-warning-error)
			Stage stage = (Stage) a.getDialogPane().getScene().getWindow();
			stage.getIcons().add(new Image("res/icon.png"));

			a.showAndWait();
			return;
		}

		updateMenus(true);

		// Create a new task (which runs on a separate thread) and play the game on it
		// We want to be able to perform processing after it has completed
		// https://docs.oracle.com/javafx/2/api/javafx/concurrent/Task.html
		// http://www.programcreek.com/java-api-examples/index.php?api=javafx.concurrent.WorkerStateEvent
		task = new Task<Colour>() {
			@Override
			protected Colour call() {
				Colour ret = null;
				try {
					ret = game.play();
				} catch (PlayerNotDefinedException e) {
					// This should never be reached
					System.out.println("Player's are undefined");
				}
				return ret;
			}
		};
		task.setOnSucceeded((WorkerStateEvent event) -> {
			handleTaskEnd();
		});

		Thread th = new Thread(task);
		th.start();
	}

	private void saveGame() {
		// Make sure that there is a valid game to save
		if (game == null || game.getBoard().winner() != null) {
			// This shouldn't be reached if the menu item update logic is correct
			Alert a = new Alert(AlertType.INFORMATION);
			a.setTitle("Can't save game");
			a.setHeaderText("There is no valid game to save");
			a.setContentText("A game must exist and must not have finished in order to be saved");

			// Set the icon of the alert (getting stage from http://stackoverflow.com/questions/30728425/create-dialogs-with-default-images-in-javafx-info-warning-error)
			Stage stage = (Stage) a.getDialogPane().getScene().getWindow();
			stage.getIcons().add(new Image("res/icon.png"));

			a.showAndWait();
			return;
		}

		// Select the file
		// http://docs.oracle.com/javafx/2/ui_controls/file-chooser.htm
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Save game");
		fileChooser.setInitialFileName(game.getName() + ".txt");
		fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Text file", "*.txt"), new FileChooser.ExtensionFilter("Properties file", "*.properties"));
		File file = fileChooser.showSaveDialog(GUIBase.getStages().get("main"));

		// If the user cancels the operation then return
		if (file == null) {
			return;
		}

		try {
			game.saveGame(file.toString());
		} catch (IOException e) {
			alertIOError("saving", e);
		}
	}

	private void updateNewGame(Game oldGame) {
		// If the user did create a new game
		if (oldGame != game) {
			updateText();
			updateDiceImages(new ArrayList<Integer>());
			updateMenus(false);
		}
	}

}
