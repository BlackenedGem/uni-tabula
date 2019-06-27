import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javafx.application.Application;
import javafx.concurrent.Task;

public class Game implements GameInterface {
	// Fields to store the state of the game
	private BoardInterface board; // To set the name of the board for CLI and GUI we add setName and getName methods (which are not defined in the interface)
	private DiceInterface dice;
	private List<Integer> diceValues;
	private Map<Colour, PlayerInterface> players;
	private int curPlayer;

	// Record the number of turns made (for testing purposes)
	private int turns;

	/**
	 * Starts the program
	 * 
	 * @param args
	 *            By default the program will run in GUI mode rather than CLI mode (so that running the jar outside of a shell environment will display the GUI)
	 *            To run in the CLI, set the first argument to 'cli' (case-insensitive)
	 *            Eg. 'java -jar tabula.jar cli'
	 *            NB: Both the CLI and GUI adapt depending on the interface values given, however the GUI only has images for dice rolls of 1-6
	 */
	

	public static void main(String[] args) {
		// It's neater to have separate classes for the CLI and GUI
		if (useCLI(args)) {
			CLI.run();
		} else {
			Application.launch(GUIBase.class);

			// If the game is still running when the GUI is closed then cancel execution
			GUIMain guiMain = (GUIMain) GUIBase.getControllers().get("main");
			Task<Colour> task = guiMain.getTask();
			if (task != null) {
				task.cancel();
			}
		}
	}

	public Game() {
		board = new Board();
		dice = new Dice();
		players = new HashMap<Colour, PlayerInterface>();

		// Fill the map with null values for each player required
		for (Colour c : Colour.values()) {
			players.put(c, null);
		}

		curPlayer = 0;
	}

	@Override
	public void setPlayer(Colour colour, PlayerInterface player) {
		// Check arguments. Allow player to be null, since that means we have cleared the colour
		if (colour == null) {
			return;
		}

		players.put(colour, player);
	}

	// If the game has ended, then this returns the winner (since the opponent doesn't get to go)
	@Override
	public Colour getCurrentPlayer() {
		return Colour.values()[curPlayer];
	}

	@Override
	public Colour play() throws PlayerNotDefinedException {
		// Make sure that we have a player for each colour
		for (PlayerInterface p : players.values()) {
			if (p == null) {
				throw new PlayerNotDefinedException("All players must be defined in order to play");
			}
		}

		turns = 0;

		// Main play loop
		while (board.winner() == null) {
			// Get the colour and corresponding player
			Colour curColour = getCurrentPlayer();
			PlayerInterface curPlayer = players.get(curColour);

			// Get the dice values
			// If we are resuming a game then we get the values that have been saved, otherwise we roll the dice
			// We create two copies of the dice values, so a rogue implementation of playerInterface cannot change the values given without us catching it

			// Increment the number of turns
			turns++;

			// If diceValues == null then we haven't resumed a paused game (since diceValues is set to null at the end of each turn)
			if (diceValues == null) {
				try {
					dice.roll();
					diceValues = dice.getValues();
				} catch (NotRolledYetException e) {
					// Should never happen
					System.out.println("Error rolling dice");
					return null;
				}
			}

			// Get the players turn
			TurnInterface curTurn = null;
			try {
				curTurn = curPlayer.getTurn(curColour, board.clone(), Misc.copyList(diceValues));
			} catch (PauseException e) {
				// The player wants to pause the game instead
				return null;
			}
			
			

			// Attempt to process the players turn
			// Also clear diceValues afterwards, so we know to roll the dice again
			try {
				board.takeTurn(curColour, curTurn, diceValues);
				diceValues = null;
			} catch (IllegalTurnException e) {
				/*
				System.out.println("Player " + curColour.toString() + " tried to perform an invalid turn, so forfeits the game: " + e.getMessage());
				System.out.println("Player " + curColour.toString() + " is " + players.get(curColour).getClass().getSimpleName());
				System.out.println("Dice values are:");
				for (int i : diceValues) {
					System.out.println(i);
				}
				System.out.println("Turn attempted:");
				for (MoveInterface m : curTurn.getMoves()) {
					System.out.println("Move " + m.getSourceLocation() + " by " + m.getDiceValue());
				}
				System.out.println(board.toString());
				*/
				diceValues = null;
				return curColour.otherColour();
			}
			
			// Check if the player has won
			if (board.isWinner(curColour)) {
				break;
			}

			// Pass the game to the next player
			nextPlayer();
		}

		return board.winner();
	}

	@Override
	public void saveGame(String filename) throws IOException {
		// Check that everything is valid before saving
		// throwExceptionIfCannotSave();

		Properties props = new Properties();

		// Store information about constants that could change
		// Eg. trying to load a game for 30 locations when the current program was only designed for 24
		propsAddMetadata(props);

		// Convert players to properties
		propsAddPlayers(props);

		// Convert board to properties
		propsAddBoard(props);

		// Convert dice rolls to properties
		propsAddDiceValues(props);

		// Save the properties to file
		try (FileOutputStream out = new FileOutputStream(filename);) {
			props.store(out, "Tabula savegame\nProperties are not guaranteed to be in order");
		} catch (Exception e) {
			throw new IOException("Could not save properties to file");
		}
	}

	@Override
	public void loadGame(String filename) throws IOException {
		// Load the properties from the file provided
		Properties props = new Properties();
		try (FileInputStream in = new FileInputStream(filename);) {
			props.load(in);
		} catch (FileNotFoundException e) {
			throw new IOException("Could not find the file");
		} catch (Exception e) {
			throw new IOException("Could not load properties from the file");
		}

		// Check that the metadata is correct
		if (!propsCheckMetadata(props)) {
			throw new IOException("Metadata is incorrect. This could be caused by loading a save file from a version of the game with different rules");
		}

		// Load board
		BoardInterface newBoard = propsLoadBoard(props);
		if (!newBoard.isValid()) {
			throw new IOException("The board was loaded correctly, but it is not in a valid state");
		}

		// Load dice rolls
		List<Integer> diceValuesLoaded = propsLoadDiceValues(props);

		// Load players
		Map<Colour, PlayerInterface> newPlayers = propsLoadPlayers(props);

		// If we have got to this point then we have loaded the file without error
		// We can now replace the old fields with the new fields loaded
		board = newBoard;
		dice = new Dice();
		diceValues = diceValuesLoaded;
		players = newPlayers;

	}

	/*
	 * Non interface methods (for CLI, GUI, and testing)
	 */

	public BoardInterface getBoard() {
		return board;
	}

	public String getName() {
		Board b = (Board) board; // Have to cast since getName is not defined in the interface
		return b.getName();
	}

	public int getTurns() {
		return turns;
	}

	public void setName(String newName) {
		board.setName(newName);
	}

	@Override
	public String toString() {
		return board.toString();
	}

	/*
	 * PRIVATE METHODS
	 */

	// Work out whether to display the GUI or CLI
	private static boolean useCLI(String[] args) {
		if (args.length > 0) {
			if (args[0].toLowerCase().equals("cli")) {
				return true;
			}
		}

		return false;
	}

	// Changes the current player to the next player
	private void nextPlayer() {
		curPlayer++;

		if (curPlayer >= Colour.values().length) {
			curPlayer = 0;
		}
	}

	/*
	 * Command line functions
	 */

	/*
	 * Metadata functions return String representations of constants that can be changed through interfaces
	 */

	private String getMetadataDieSides() {
		return Integer.toString(DieInterface.NUMBER_OF_SIDES_ON_DIE);
	}

	private String getMetadataNoLocs() {
		return Integer.toString(BoardInterface.NUMBER_OF_LOCATIONS);
	}

	private String getMetadataPPP() {
		return Integer.toString(BoardInterface.PIECES_PER_PLAYER);
	}

	/*
	 * Property functions to improve readability
	 */
	private void propsAddBoard(Properties props) throws IOException {
		// Cast BoardInterface to Board so we can access getName()
		Board b = (Board) board;
		String bName = b.getName();

		if (bName != null) {
			props.setProperty("Board_NameInitialised", "true");
			props.setProperty("Board_Name", b.getName());
		} else {
			props.setProperty("Board_NameInitialised", "false");
		}

		// Add locations to board
		propsAddBoardLocation(props, "Start", board.getStartLocation());
		propsAddBoardLocation(props, "Knocked", board.getKnockedLocation());
		propsAddBoardLocation(props, "End", board.getEndLocation());

		for (int i = 1; i <= Board.NUMBER_OF_LOCATIONS; i++) {
			try {
				propsAddBoardLocation(props, Integer.toString(i), board.getBoardLocation(i));
			} catch (NoSuchLocationException e) {
				throw new IOException("Could not convert board locations to properties");
			}
		}
	}

	private void propsAddBoardLocation(Properties props, String locID, LocationInterface loc) {
		String propKey = "Board_Location" + locID + "_";

		// Ideally the location name will match up to it's actual position on the board
		// However this is not guaranteed, so we the name of the location and it's name on the board (effectively storing the same name twice)
		// We do not need to save whether or not the location is mixed or not, since this is known already
		props.setProperty(propKey + "Name", loc.getName());

		for (Colour c : Colour.values()) {
			props.setProperty(propKey + c.toString(), Integer.toString(loc.numberOfPieces(c)));
		}
	}

	private void propsAddDiceValues(Properties props) {
		boolean hasRolled = true;
		List<Integer> diceValues = null;

		try {
			diceValues = dice.getValues();
		} catch (NotRolledYetException e) {
			hasRolled = false;
		}

		props.setProperty("Dice_Rolled", Boolean.toString(hasRolled));

		if (!hasRolled) {
			return;
		}

		props.setProperty("Dice_NoValues", Integer.toString(diceValues.size()));

		for (int i = 0; i < diceValues.size(); i++) {
			props.setProperty("Dice_Value" + Integer.toString(i + 1), Integer.toString(diceValues.get(i)));
		}
	}

	private void propsAddMetadata(Properties props) {
		// Colours
		for (int i = 0; i < Colour.values().length; i++) {
			props.setProperty("Metadata_Colour" + i, Colour.values()[i].toString());
		}
		
		// Other properties
		props.setProperty("Metadata_DieSides", getMetadataDieSides());
		props.setProperty("Metadata_NoLocations", getMetadataNoLocs());
		props.setProperty("Metadata_PPP", getMetadataPPP());
	}

	private void propsAddPlayers(Properties props) {
		for (Colour c : Colour.values()) {
			String playerString = "Player" + c.toString() + "_";

			// Get a player (we don't need to check if players contains the key, since we have already initialised the map with null values)
			PlayerInterface p = players.get(c);

			// Store whether or not we have initialised the player yet
			props.setProperty(playerString + "Initialised", Boolean.toString(p != null));

			// If we haven't initialised the player then continue, otherwise continue to add data
			if (p == null) {
				continue;
			}

			// Store what implementation of PlayerInterface we are using
			props.setProperty(playerString + "ClassType", p.getClass().getSimpleName());
		}
	}

	private boolean propsCheckMetadata(Properties props) throws IOException {
		// Check whether or not the supplied metadata matches the metadata expected
		// Colours
		for (int i = 0; i < Colour.values().length; i++) {
			if (!propsLoadKeyString(props, "Metadata_Colour" + i).equals(Colour.values()[i].toString())) {
				return false;
			}
		}
		
		// Other constants
		if (!propsLoadKeyString(props, "Metadata_DieSides").equals(getMetadataDieSides())) {
			return false;
		}
		if (!propsLoadKeyString(props, "Metadata_NoLocations").equals(getMetadataNoLocs())) {
			return false;
		}
		if (!propsLoadKeyString(props, "Metadata_PPP").equals(getMetadataPPP())) {
			return false;
		}

		return true;
	}

	private BoardInterface propsLoadBoard(Properties props) throws IOException {
		Board newBoard = new Board();

		// If the name was initialised then set it
		if (propsLoadKeyBoolean(props, "Board_NameInitialised")) {
			newBoard.setName(propsLoadKeyString(props, "Board_Name"));
		}

		// Load the mixed locations
		newBoard.setStartLocation(propsLoadLocation(props, "Start", true));
		newBoard.setKnockedLocation(propsLoadLocation(props, "Knocked", true));
		newBoard.setEndLocation(propsLoadLocation(props, "End", true));

		// Load the non mixed locations
		for (int i = 1; i <= BoardInterface.NUMBER_OF_LOCATIONS; i++) {
			try {
				LocationInterface loc = propsLoadLocation(props, Integer.toString(i), false);
				newBoard.setBoardLocation(i, loc);
			} catch (NoSuchLocationException e) {
				// This exception should never be caught
				throw new IOException("Error loading location into board position " + i);
			}
		}

		return (BoardInterface) newBoard;
	}

	private List<Integer> propsLoadDiceValues(Properties props) throws IOException {
		// If we haven't rolled the dice yet then return nothing
		if (!propsLoadKeyBoolean(props, "Dice_Rolled")) {
			return null;
		}

		// Get the number of dice values to return
		int noValues = propsLoadKeyInt(props, "Dice_NoValues");

		// Perform checks on the number of dice rolled
		if (noValues != 2 && noValues != 4) {
			throw new IOException("There must be either 2 or 4 dice values to load");
		}

		// Get the dice values
		List<Integer> diceValues = new ArrayList<Integer>();
		for (int i = 1; i <= noValues; i++) {
			String valString = "Dice_Value" + Integer.toString(i);

			int val = propsLoadKeyInt(props, valString);

			// Perform checks on val
			if (val < 0 || val > DieInterface.NUMBER_OF_SIDES_ON_DIE) {
				throw new IOException("Dice roll out of bounds for key '" + valString + "'");
			}

			diceValues.add(val);
		}
		
		// Convert the dice values to a map so we can perform additional checks
		Map<Integer, Integer> diceMap = Misc.valuesToMap(diceValues);
		
		if (noValues == 2) {
			if (diceMap.size() != 2) {
				throw new IOException("Dice values loaded are incorrect. The 2 values given must be different");
			}
		} else {
			if (diceMap.size() != 1) {
				throw new IOException("Dice values loaded are incorrect. The 4 values given must be the same");
			}
		}
		
		

		return diceValues;
	}

	private LocationInterface propsLoadLocation(Properties props, String name, boolean isMixed) throws IOException {
		String locKey = "Board_Location" + name + "_";

		// Create the new location. Set isMixed to true so we can pieces of multiple colours
		String locName = propsLoadKeyString(props, locKey + "Name");
		LocationInterface loc = new Location(locName);
		loc.setMixed(true);

		// Add pieces to the location
		for (Colour c : Colour.values()) {
			// Retrieve the number of pieces to add for the colour
			int noPieces = propsLoadKeyInt(props, locKey + c.toString());

			// Add the pieces
			for (int i = 0; i < noPieces; i++) {
				try {
					loc.addPieceGetKnocked(c);
				} catch (IllegalMoveException e) {
					throw new IOException("Error adding pieces to location");
				}
			}

			// Set isMixed
			loc.setMixed(isMixed);
		}

		return loc;
	}

	private Map<Colour, PlayerInterface> propsLoadPlayers(Properties props) throws IOException {
		Map<Colour, PlayerInterface> newPlayers = new HashMap<Colour, PlayerInterface>();

		for (Colour c : Colour.values()) {
			String playerString = "Player" + c.toString() + "_";

			// If we haven't initialised the player yet then use a null value and continue
			if (!propsLoadKeyBoolean(props, playerString + "Initialised")) {
				newPlayers.put(c, null);
				continue;
			}

			// Load the correct class for the players
			// Could use reflection for this, but it's easier not to (and more secure)
			String classType = propsLoadKeyString(props, playerString + "ClassType");

			if (classType.equals("HumanConsolePlayer")) {
				newPlayers.put(c, new HumanConsolePlayer());
			} else if (classType.equals("HumanGUIPlayer")) {
				newPlayers.put(c, new HumanGUIPlayer());
			} else if (classType.equals("ComputerPlayer")) {
				newPlayers.put(c, new ComputerPlayer());
			} else {
				throw new IOException("Did not recognise the player type to load for colour " + c.toString());
			}
		}

		return newPlayers;
	}

	/*
	 * Property methods for loading a key in a specified format (ie. boolean, int, string)
	 */
	private boolean propsLoadKeyBoolean(Properties props, String keyName) throws IOException {
		String value = propsLoadKeyString(props, keyName);

		return Boolean.parseBoolean(value);
	}

	private int propsLoadKeyInt(Properties props, String keyName) throws IOException {
		try {
			String valString = propsLoadKeyString(props, keyName);
			int valInt = Integer.parseInt(valString);
			return valInt;
		} catch (NumberFormatException e) {
			throw new IOException("Error converting string to int when loading key '" + keyName + "'");
		}
	}

	private String propsLoadKeyString(Properties props, String keyName) throws IOException {
		if (!props.containsKey(keyName)) {
			throw new IOException("Could not find key '" + keyName + "'");
		}

		return props.getProperty(keyName);
	}

}
