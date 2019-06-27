import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HumanConsolePlayer implements PlayerInterface {
	public HumanConsolePlayer() {
	}

	@Override
	public TurnInterface getTurn(Colour colour, BoardInterface board, List<Integer> diceValues) throws PauseException {
		TurnInterface turn = new Turn();
		List<Integer> diceLeft = Misc.copyList(diceValues); // Copy list since we want to change it (and interface doesn't explicitly say that we can, unlike board)
		Collections.sort(diceLeft);

		// Check arguments
		if (colour == null || board == null || diceValues == null) {
			System.out.println("Invalid arguments were given to the human console player, no turn can be made");
			return turn;
		}

		// Backup the board in case we need to reset it (eg. user selects an invalid sequence of moves)
		BoardInterface boardBackup = board.clone();

		// Get the number of moves required (unless the player has won)
		int movesRequired = Board.getMaximumMoves(board, colour, diceValues);

		// Turn message, also notifies us if we have to forfeit the turn
		if (!printTurnMsg(colour, diceValues, movesRequired)) {
			return turn;
		}

		while (true) {
			String input = CLI.getInput(">> ");

			switch (input.toLowerCase()) {
			case "add":
				getMove(turn, colour, board, diceLeft, movesRequired);
				break;
			case "clear":
				board = boardBackup.clone();
				turn = new Turn();
				diceLeft = Misc.copyList(diceValues);
				System.out.println("Moves cleared");
				break;
			case "dice":
				showDice(diceLeft);
				break;
			case "help":
				System.out.println("List of commands:");
				System.out.println("Clear		- Remove the current moves to make");
				System.out.println("Dice		- Show the dice left (it may not be possible to use all the dice)");
				System.out.println("Help		- Show the available commands");
				System.out.println("Move		- Make a move using a dice roll (this action is reversible by using 'clear'). 'Add' can also be used");
				System.out.println("Moves		- List the current moves that have been made");
				System.out.println("Pause		- Pauses the game instead of taking a turn (inputted moves are not saved)");
				System.out.println("Submit		- Make the turn from the given moves");
				System.out.println("View		- Show the current state of the board (after making the current moves given)");
				break;
			case "move":
				getMove(turn, colour, board, diceLeft, movesRequired);
				break;
			case "moves":
				listMoves(turn);
				break;
			case "pause":
				throw new PauseException("");
			case "submit":
				if (checkMoves(turn, board, movesRequired)) {
					return turn;
				}
				break;
			case "view":
				System.out.println(board.toString());
				break;
			default:
				System.out.println("Could not recognise command: " + input);
				System.out.println("Use 'help' for a list of commands");
				break;
			}
		}

	}
	
	private boolean checkMoves(TurnInterface turn, BoardInterface board, int movesRequired) {
		if (turn.getMoves().size() < movesRequired && board.winner() == null) {
			System.out.println("Cannot submit turn (without forfeiting)");
			System.out.println("Your turn consists of " + turn.getMoves().size() + " moves, but " + movesRequired + " are needed (unless you can win with less)");
			return false;
		}

		return true;
	}

	// Get 2 inputs from the user to make a move
	// Provides appropriate feedback along the way
	// Only considers the current board, not all the possible turns (It's possible to choose moves that mean you won't be able to use all the dice)
	private void getMove(TurnInterface turn, Colour colour, BoardInterface board, List<Integer> diceAvailable, int movesRequired) {
		// Make sure we have dice to use
		if (diceAvailable.size() == 0) {
			System.out.println("You have no dice left");
			return;
		}

		// Get the possible moves, make sure that a move is available
		Set<MoveInterface> posMoves = board.possibleMoves(colour, diceAvailable);
		if (posMoves.size() == 0) {
			System.out.println("There are no possible moves available");
			return;
		}

		// Get the possible source locations and sort them
		Set<Integer> sourceLocsSet = new HashSet<Integer>();
		for (MoveInterface m : posMoves) {
			sourceLocsSet.add(m.getSourceLocation());
		}
		List<Integer> sourceLocsList = new ArrayList<Integer>(sourceLocsSet);
		Collections.sort(sourceLocsList);


		// Get source location from user (we could choose this automatically, but during testing this actually slowed down playing of the game - use GUI for speed/ease of use)
		int sourceLoc = getValue(sourceLocsSet, sourceLocsList, "You can move pieces from the following location(s): ", "Select location to move piece from: ");
		if (sourceLoc == -1) {
			return;
		}

		// Find possible dice values from the source location (a set is possibly a bit overkill, but it looks neater and would be needed if you had more dice rolls)
		Set<Integer> posDiceValues = new HashSet<Integer>();
		for (MoveInterface m : posMoves) {
			if (m.getSourceLocation() == sourceLoc) {
				posDiceValues.add(m.getDiceValue());
			}
		}

		// Get the dice value (again, don't select automatically)
		int diceValue = getValue(posDiceValues, diceAvailable, "Possible dice value(s): ", "Select a dice roll to use: ");
		if (diceValue == 0) {
			return;
		}

		// Create the move
		MoveInterface move = new Move();
		try {
			// Create the move object and add it to the turn
			move.setSourceLocation(sourceLoc);
			move.setDiceValue(diceValue);
			turn.addMove(move);

			// Remove the dice value
			diceAvailable.remove((Integer) diceValue);

			// Process the move in our copy of board, so that we can 'see ahead'
			board.makeMove(colour, move);
		} catch (Exception e) {
			// Should never be reached
			System.out.println("A logic error occurred when adding the move");
			System.out.println(e.toString());
		}

		System.out.println("Move added to your turn");

		// Check what state adding this move to the turn puts us in
		if (turn.getMoves().size() >= movesRequired) {
			System.out.println("Enough moves have been added, you can now submit the turn");
		} else if (board.possibleMoves(colour, diceAvailable).size() == 0) {
			System.out.println("Warning: You cannot currently add any more moves, but it is possible to perform more moves in this turn");
			System.out.println("Use 'clear' in order to reset your moves and 'view' to view the current state of the board");
		}
	}

	private void listMoves(TurnInterface turn) {
		// Print out the number of moves that have been made
		int noMoves = turn.getMoves().size();
		StringBuilder sb = new StringBuilder("Your turn currently consists of ");
		sb.append(noMoves);
		if (noMoves == 1) {
			sb.append(" move");
		} else {
			sb.append(" moves");
		}
		if (noMoves > 0) {
			sb.append(":");
		}

		System.out.println(sb.toString());

		for (MoveInterface m : turn.getMoves()) {
			// Get required information
			int sourceLoc = m.getSourceLocation();
			int diceValue = m.getDiceValue();
			int endLoc = m.getSourceLocation() + diceValue;

			// Build the message
			StringBuilder sb2 = new StringBuilder("Move counter at location '");
			sb2.append(sourceLoc);

			if (endLoc > BoardInterface.NUMBER_OF_LOCATIONS) {
				sb2.append("' to the end (dice value: ");
			} else {
				sb2.append("' to location '");
				sb2.append(endLoc);
				sb2.append("' (dice value: ");
			}

			sb2.append(diceValue);
			sb2.append(")");

			System.out.println(sb2.toString());
		}
	}

	// Print the message asking the player to take a turn
	// If no turns are available then return false
	private boolean printTurnMsg(Colour colour, List<Integer> diceValues, int maxMovesPossible) {
		System.out.println("Player " + colour.toString() + ", your turn");

		// Get dice values
		StringBuilder sb = new StringBuilder();
		for (Integer i : diceValues) {
			sb.append(i);
			sb.append(", ");
		}

		sb.delete(sb.length() - 2, sb.length());

		if (diceValues.size() == 4) {
			sb.append(" (a double was rolled)");
		}

		System.out.println("Dice rolls available: " + sb.toString());

		// Check how many dice we can use
		if (maxMovesPossible == 0) {
			System.out.println("It is not possible to use any of the dice, turn forfeited");
			return false;
		} else if (diceValues.size() == maxMovesPossible) {
			System.out.println("It is possible to use all of the dice");
		} else {
			System.out.println("Note: Only " + maxMovesPossible + " of the dice rolls can be used");
		}

		return true;
	}
	
	private void showDice(List<Integer> dice) {
		if (dice.size() == 0) {
			System.out.println("You have used all the dice");
			return;
		}
		
		StringBuilder sb = new StringBuilder("Dice left: ");
		for (int val : dice) {
			sb.append(val + ", ");
		}
		sb.delete(sb.length() - 2, sb.length());
		System.out.println(sb.toString());
	}

	/*
	 * Functions to make getMove() more readable
	 */

	// Given a set of possible values, return the integer the user chose
	// Return -1 if the user did not enter a valid input
	// We display 2 messages - The first lists the possible values, the second is the user input prompt
	private int getValue(Set<Integer> posValues, List<Integer> posValuesList, String msg1, String msg2) {
		// Assemble message 1, using the possible values
		StringBuilder sb = new StringBuilder();
		sb.append(msg1);
		for (Integer i : posValuesList) {
			sb.append(i.toString() + ", ");
		}
		sb.delete(sb.length() - 2, sb.length());
		System.out.println(sb.toString());
		
		String input = CLI.getInput(msg2);

		// Convert the string into an appropriate number
		int val;
		try {
			val = Integer.parseInt(input);
		} catch (NumberFormatException e) {
			System.out.println("That is not a valid number");
			return 0;
		}

		if (!posValues.contains(val)) {
			System.out.println("That number is not valid");
			return 0;
		}

		return val;
	}
}
