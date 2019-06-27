import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class HumanGUIPlayer implements PlayerInterface {
	private boolean hasFinished;
	private boolean pauseGame;
	private TurnInterface turn;

	// Store the controller
	private GUIMain guiMain;

	// Store the data given to us by the turn (also backup the data)
	private BoardInterface board;
	private BoardInterface backupBoard;
	private Colour colour;
	private List<Integer> diceValues;
	private List<Integer> backupDiceValues;

	public HumanGUIPlayer() {
	}

	@Override
	public TurnInterface getTurn(Colour colour, BoardInterface board, List<Integer> diceValues) throws PauseException {
		turn = new Turn();

		// Check arguments
		if (colour == null || board == null || diceValues == null) {
			System.out.println("Invalid arguments were given to the human gui player, no turn can be made");
			return turn;
		}

		// Save the arguments to fields and copy the board and dice values (so that we can reset them if we need to)
		this.board = board;
		this.backupBoard = board.clone();
		this.colour = colour;
		this.diceValues = diceValues;
		this.backupDiceValues = Misc.copyList(diceValues);

		// Get the main gui controller and use it to update the gui
		guiMain = (GUIMain) GUIBase.getControllers().get("main");

		// Check how many of the dice we can use
		int maxMoves = Board.getMaximumMoves(board, colour, diceValues);
		if (maxMoves == 0) {
			Platform.runLater(new Runnable() {
				public void run() {
					showAlertNoMoves(diceValues);
				}
			});
			return turn;
		} else if (maxMoves < diceValues.size()) {
			Platform.runLater(new Runnable() {
				public void run() {
					guiMain.updateMaxMovesStatus(true, maxMoves, diceValues.size());
				}
			});
		}

		// Register events
		guiMain.updateInputControls(true);
		guiMain.updateMenuPause(true);
		guiMain.registerPlayer(this);

		// Update the UI
		updateUIForNewMove();

		hasFinished = false;
		pauseGame = false;

		// Idle this thread until we are done
		// By idling this thread we can still receive events from the FX Application thread
		while (true) {
			try {
				// Check every 10ms whether or not the game has finished
				// 10ms should be low enough that the user cannot perform 2 actions at once
				Thread.sleep(10);

				if (hasFinished) {
					break;
				}
			} catch (InterruptedException e) {
				// We swallow this interruption, since we deal with it immediately
				throw new PauseException("Thread interrupted");
			}
		}

		if (pauseGame) {
			throw new PauseException("Turn paused by player");
		} else {
			return turn;
		}
	}

	/*
	 * Public methods to communicate with GUIMain
	 */

	public synchronized void addMove(String sourceSelection, int diceValue) {
		int sourceLoc = stringToSourceLoc(sourceSelection);

		MoveInterface move = new Move();
		try {
			move.setSourceLocation(sourceLoc);
			move.setDiceValue(diceValue);
			board.makeMove(colour, move);
			diceValues.remove((Integer) diceValue);
			guiMain.addMove(move);
			turn.addMove(move);
		} catch (Exception e) {
			// This shouldn't cause an error, but if so then display the error
			// Create an alert
			Alert a = new Alert(AlertType.ERROR);
			a.setTitle("Error");
			a.setHeaderText("Could not apply the move");
			a.setContentText(e.toString());
			
			// Set the icon of the alert (getting stage from http://stackoverflow.com/questions/30728425/create-dialogs-with-default-images-in-javafx-info-warning-error)
			Stage stage = (Stage) a.getDialogPane().getScene().getWindow();
			stage.getIcons().add(new Image("res/icon.png"));

			a.showAndWait();
		}

		updateUIForNewMove();
	}

	public synchronized void clearMoves() {
		turn = new Turn();
		board = backupBoard;
		backupBoard = board.clone();
		diceValues = backupDiceValues;
		backupDiceValues = Misc.copyList(diceValues);
		guiMain.clearTable();
		updateUIForNewMove();
	}

	public synchronized void diceValueSelected(Integer selectedValue) {
		guiMain.showAddButton(true);
	}

	public synchronized void pause() {
		pauseGame = true;
		setFinished();
	}

	public synchronized void sourceLocSelected(String selectedValue) {
		// If no item is selected then clear the dice choice box
		if (selectedValue == null || selectedValue.length() == 0) {
			guiMain.updateDiceChoices(new ArrayList<Integer>(), false);
			return;
		}

		// If we can't convert the string to a number, then it must be position 0
		int sourceLoc = stringToSourceLoc(selectedValue);

		// Get the possible dice values
		List<Integer> posDiceValues = getDiceValues(sourceLoc);
		guiMain.updateDiceChoices(posDiceValues, true);
	}

	public synchronized void submitTurn() {
		// Attempt to process the turn on our copy of the board, if no exception is thrown then it is valid
		try {
			backupBoard.takeTurn(colour, turn, backupDiceValues);
		} catch (IllegalTurnException e) {
			// If the turn is invalid then notify the player using an alert
			Alert a = new Alert(AlertType.ERROR);
			a.setTitle("Invalid turn");
			a.setHeaderText("That is not a valid turn");
			a.setContentText(e.getMessage());

			// Set the icon of the alert (getting stage from http://stackoverflow.com/questions/30728425/create-dialogs-with-default-images-in-javafx-info-warning-error)
			Stage stage = (Stage) a.getDialogPane().getScene().getWindow();
			stage.getIcons().add(new Image("res/icon.png"));

			a.showAndWait();
			return;
		}

		setFinished();
	}

	/*
	 * Private methods
	 */

	// Get a list of string representations of all of the possible source locations
	private List<String> getSourceLocs() {
		Set<Integer> sourceLocsSet = new HashSet<Integer>();
		for (MoveInterface move : board.possibleMoves(colour, diceValues)) {
			sourceLocsSet.add(move.getSourceLocation());
		}
		List<Integer> sourceLocsInt = new ArrayList<Integer>(sourceLocsSet);
		Collections.sort(sourceLocsInt);

		List<String> ret = new ArrayList<String>();

		for (Integer loc : sourceLocsInt) {
			// Decide if location 0 is start or the knocked location
			if (loc == 0) {
				if (board.getKnockedLocation().numberOfPieces(colour) > 0) {
					ret.add("Knocked");
				} else {
					ret.add("Start");
				}
			} else {
				ret.add(Integer.toString(loc));
			}
		}

		return ret;
	}

	// Given a soure location, return the possible dice values that can be used
	private List<Integer> getDiceValues(int sourceLoc) {
		Set<Integer> diceSet = new HashSet<Integer>();

		for (MoveInterface move : board.possibleMoves(colour, diceValues)) {
			if (move.getSourceLocation() == sourceLoc) {
				diceSet.add(move.getDiceValue());
			}
		}

		List<Integer> ret = new ArrayList<Integer>(diceSet);
		Collections.sort(ret);

		return ret;
	}

	private synchronized void updateUIForNewMove() {
		// Clear the UI and add appropriate values so that the user can enter a new move from the current state
		List<String> sourceLocs = getSourceLocs();
		guiMain.updateSourceLocs(sourceLocs, sourceLocs.size() > 0);
		guiMain.updateDiceChoices(new ArrayList<Integer>(), false);
		guiMain.showAddButton(false);
		guiMain.updateDiceImages(diceValues);

		// Certain controls can only be updated on the main thread, so need to schedule this (but it should still happen nearly instantaneously)
		Platform.runLater(new Runnable() {
			public void run() {
				guiMain.updateText();
				guiMain.updateLocations(board);
			}
		});
	}

	private synchronized void setFinished() {
		// Disable the controls and unregister this object
		guiMain.updateInputControls(false);
		guiMain.registerPlayer(null);
		guiMain.updateMenuPause(false);
		guiMain.clearTable();
		guiMain.updateMaxMovesStatus(false, 0, 0);
		hasFinished = true;
	}

	private void showAlertNoMoves(List<Integer> diceValues) {
		// Build the content text
		StringBuilder sb = new StringBuilder("It is not possible to make any moves, so the turn is forfeited.\nDice values rolled: ");
		for (Integer roll : diceValues) {
			sb.append(roll);
			sb.append(", ");
		}
		sb.delete(sb.length() - 2, sb.length());
		sb.append(".");

		Alert a = new Alert(AlertType.INFORMATION);
		a.setTitle("Turn forfeited");
		a.setHeaderText("Player " + colour.toString() + " forfeit's their turn");
		a.setContentText(sb.toString());

		// Set the icon of the alert (getting stage from http://stackoverflow.com/questions/30728425/create-dialogs-with-default-images-in-javafx-info-warning-error)
		Stage stage = (Stage) a.getDialogPane().getScene().getWindow();
		stage.getIcons().add(new Image("res/icon.png"));

		a.showAndWait();
	}

	private int stringToSourceLoc(String s) {
		// If we can parse the number then return it, otherwise it must be the start/knocked location (which is always 0)
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

}
