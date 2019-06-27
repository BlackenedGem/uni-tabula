import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.NoSuchElementException;

// Static class for all our CLI needs (could be non static, but since you can only have 1 input stream at a time, that seems a bit pointless)
public class CLI {
	// Our input stream to use (initialise instantly)
	private static final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

	// Store the game
	private static Game game;

	public CLI() {

	}

	public static void run() {
		// Reflection could be used for this bit to reduce the amount of code, but that seems overkill for a simple menu system
		while (true) {
			String input = getInput("> ");

			switch (input.toLowerCase()) {
			case "help":
				System.out.println("List of commands:");
				System.out.println("Exit		- Terminate the program");
				System.out.println("Help		- Show the available commands");
				System.out.println("Load		- Load a game from a file");
				System.out.println("New 		- Create a new game with a human and a computer player");
				System.out.println("Play		- Play (or continue) the current game");
				System.out.println("Save		- Save the current game to a file");
				System.out.println("Set Player	- Change the player type of a colour");
				System.out.println("View		- Show the current state of the board");
				break;
			case "exit":
				if (commandExit()) {
					System.out.println("Program terminated");
					return;
				}
				break;
			case "load":
				commandLoad();
				break;
			case "new":
				commandNew();
				break;
			case "play":
				commandPlay();
				break;
			case "save":
				commandSave();
				break;
			case "set player":
				commandSetPlayer();
				break;
			case "view":
				commandViewBoard();
				break;
			default:
				System.out.println("Could not recognise command: " + input);
				System.out.println("Use 'help' for a list of commands");
				break;
			}
		}
	}

	private static boolean commandExit() {
		if (game != null && game.getBoard().winner() == null) {
			String input = getInput("Would you like to save before leaving: ").toLowerCase();
			if (input.equals("yes") || input.equals("y")) {
				return commandSave();
			}
		}

		return true;
	}

	private static boolean commandLoad() {
		Game g = new Game();
		String input = getInput("File to load: ");

		try {
			g.loadGame(input);
		} catch (IOException e) {
			System.out.println("Error loading game: " + e.getMessage());
			return false;
		}

		game = g;
		System.out.println("Game loaded");
		return true;
	}

	private static void commandNew() {
		// We want to set the board name
		String gameName = getInput("Name of the game: ");

		game = new Game();
		game.setName(gameName);
		game.setPlayer(Colour.values()[0], new HumanConsolePlayer());
		game.setPlayer(Colour.values()[1], new ComputerPlayer());

		System.out.println("New game '" + gameName + "', started with a human player and a computer player");
	}

	private static void commandPlay() {
		// Make sure we have a game to play
		if (game == null) {
			System.out.println("You need to create/load a game first");
			return;
		}

		// Get the result
		Colour result = null;
		try {
			result = game.play();
		} catch (PlayerNotDefinedException e) {
			System.out.println(e.toString());
			System.out.println("Not all the players have been defined yet");
		}

		// Check the result of the game
		if (result == null) {
			System.out.println("Game paused");
		} else {
			// Print out the winning game (also wipe game)
			System.out.println("Player " + result.toString() + " has won!");
			System.out.println("Game ended");
		}
	}

	private static boolean commandSave() {
		if (game == null) {
			System.out.println("There is no game to save");
			return false;
		}

		String input = getInput("File to save as: ");

		try {
			game.saveGame(input);
		} catch (IOException e) {
			System.out.println("Error saving game: " + e.getMessage());
			return false;
		}

		System.out.println("Game saved");
		return true;
	}

	private static void commandSetPlayer() {
		if (game == null) {
			System.out.println("You need to create a game first");
			return;
		}

		// Construct the message to ask which colour to change
		StringBuilder sb = new StringBuilder("Which coloured player do you want to change (");
		for (Colour c : Colour.values()) {
			sb.append(c.toString());
			sb.append("/");
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append("): ");

		// Get the colour to change
		String input = getInput(sb.toString()).toLowerCase();

		Colour colour = null;
		for (Colour c : Colour.values()) {
			if (input.equals(c.toString().toLowerCase())) {
				colour = c;
				break;
			}
		}

		if (colour == null) {
			System.out.println("Colour not recognised");
			return;
		}

		// Get the player type to change
		input = getInput("Would you like the player to be a human or computer player: ").toLowerCase();
		if (input.equals("human")) {
			game.setPlayer(colour, new HumanConsolePlayer());
		} else if (input.equals("computer")) {
			game.setPlayer(colour, new ComputerPlayer());
		} else {
			System.out.println("Input not recognised");
			return;
		}

		System.out.println("Player updated");
	}

	private static void commandViewBoard() {
		if (game == null) {
			System.out.println("No game has been loaded/created yet");
			return;
		}

		System.out.println(game.toString());
	}

	// Get input from the reader (public so that HumanConsolePlayer doesn't need to create a separate inputstream instance)
	public static String getInput(String instruction) {
		System.out.print(instruction);
		String input = null;

		try {
			input = reader.readLine().trim();
		} catch (IOException e) {
			System.out.println("An error occurred reading user input: " + e.toString());
			return "";
		} catch (NoSuchElementException e) {
			System.out.println("Input streamed closed prematurely, program will exit");
			System.exit(0);
		}

		return input;
	}
}
