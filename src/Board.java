import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Board implements BoardInterface {
	private String name;
	private List<LocationInterface> locations; // 0 = Start
												// 1 = Knocked off
												// i + 1 = Position i
												// NUMBER_OF_LOCATIONS + 2 = End

	/*
	 * Constructors
	 */

	public Board() {
		locations = new ArrayList<LocationInterface>();

		// Predefined locations
		// Start location
		LocationInterface start = new Location("Start");
		start.setMixed(true);

		for (Colour c : Colour.values()) {
			for (int i = 0; i < PIECES_PER_PLAYER; i++) {
				try {
					start.addPieceGetKnocked(c);
				} catch (IllegalMoveException e) {
					// This should never be reached
					System.out.println("Error when adding pieces to start location");
				}
			}
		}
		locations.add(start);

		LocationInterface knocked = new Location("Knocked");
		knocked.setMixed(true);
		locations.add(knocked);

		// Locations 1 to NUMBER_OF_LOCATIONS
		for (int i = 0; i < NUMBER_OF_LOCATIONS; i++) {
			locations.add(new Location(Integer.toString(i + 1)));
		}

		LocationInterface end = new Location("End");
		end.setMixed(true);
		locations.add(end);
	}

	@Override
	public void setName(String name) {
		if (name != null) {
			this.name = name;
		}
	}

	@Override
	public LocationInterface getStartLocation() {
		return locations.get(0);
	}

	@Override
	public LocationInterface getEndLocation() {
		return locations.get(NUMBER_OF_LOCATIONS + 2);
	}

	@Override
	public LocationInterface getKnockedLocation() {
		return locations.get(1);
	}

	@Override
	public LocationInterface getBoardLocation(int locationNumber) throws NoSuchLocationException {
		if (locationNumber < 1 || locationNumber > NUMBER_OF_LOCATIONS) {
			throw new NoSuchLocationException("The location must be between 1 and " + Integer.toString(NUMBER_OF_LOCATIONS));
		}

		return locations.get(locationNumber + 1);
	}

	@Override
	public boolean canMakeMove(Colour colour, MoveInterface move) {
		// Check args
		if (colour == null || move == null) {
			return false;
		}
		
		// Get source location
		LocationInterface sourceLoc = intToLocation(colour, move.getSourceLocation());
		if (sourceLoc == null) {
			return false;
		}

		// Get end location
		LocationInterface endLoc = intToLocation(colour, move.getSourceLocation() + move.getDiceValue());
		if (endLoc == null) {
			return false;
		}

		// Check if we can remove the piece and then add it
		if (!sourceLoc.canRemovePiece(colour)) {
			return false;
		}
		return endLoc.canAddPiece(colour);
	}

	@Override
	public void makeMove(Colour colour, MoveInterface move) throws IllegalMoveException {
		// Throw an exception if we do not think we can make the move
		if (!canMakeMove(colour, move)) {
			throw new IllegalMoveException("Invalid move");
		}

		// Get source location
		LocationInterface sourceLoc = intToLocation(colour, move.getSourceLocation());
		if (sourceLoc == null) {
			throw new IllegalMoveException("Could not retrieve source location");
		}

		// Get end location
		LocationInterface endLoc = intToLocation(colour, move.getSourceLocation() + move.getDiceValue());
		if (endLoc == null) {
			throw new IllegalMoveException("Could not retrieve end location");
		}

		// Remove the piece from its source
		sourceLoc.removePiece(colour);
		
		// Add the piece and if a piece was knocked off then add it to the knocked location
		Colour knockedColour = endLoc.addPieceGetKnocked(colour);
		if (knockedColour != null) {
			getKnockedLocation().addPieceGetKnocked(knockedColour);
		}
	}

	@Override
	public void takeTurn(Colour colour, TurnInterface turn, List<Integer> diceValues) throws IllegalTurnException {
		// Make sure that arguments are not null
		if (colour == null || turn == null || diceValues == null) {
			throw new IllegalTurnException("Arguments cannot be null");
		}
		
		// Backup the board, since there are many ways that this method could fail
		// It would require a lot of computation to predict them all before modifying the board, it's much easier to restore the board for these edge cases
		BoardInterface oldBoard = this.clone();

		// Create a variable to check whether or not the colour has won during this turn
		// This is so that we can skip constraints that may make it impossible to win (eg. requiring the use of 2 dice when you can win using 1)
		boolean hasWon = false;

		// Create maps for diceValues and turn (maps dice roll --> number of occurrences)
		Map<Integer, Integer> diceMap = Misc.valuesToMap(diceValues);
		Map<Integer, Integer> turnMap = turnToMap(turn);

		// Enforce rules on dice rolls
		if (diceValues.size() == 4) {
			if (diceMap.size() != 1) {
				throw new IllegalTurnException("If 4 dice rolls are given then they must all contain the same dice roll");
			}
		} else if (diceValues.size() != 2) {
			throw new IllegalTurnException("Incorrect amount of dice values given (" + Integer.toString(diceValues.size()) + ")");
		} else if (diceMap.size() == 1) {
			throw new IllegalTurnException("2 dice of the same value cannot be given, 4 must be provided instead for a double");
		}

		// Make sure that the number of moves is less than the number of dice rolls (could contain less moves if attempting to win the game)
		for (Integer roll : turnMap.keySet()) {
			if (!diceMap.containsKey(roll) || turnMap.get(roll) > diceMap.get(roll)) {
				throw new IllegalTurnException("For each move there must be a unique dice roll corresponding to it");
			}
		}

		// Get the maximum number of dice that can be used
		int maxMoves = getMaximumMoves(this, colour, diceValues);

		// Perform the moves
		try {
			for (MoveInterface m : turn.getMoves()) {
				// Don't bother to check for validity, since makeMove does this already
				makeMove(colour, m);

				// Stop performing moves if a colour has won
				if (isWinner(colour)) {
					break;
				}
			}
		} catch (IllegalMoveException e) {
			// If we attempt to make an invalid move then restore the board and then throw an exception
			restoreBoard(oldBoard);
			throw new IllegalTurnException("The turn contains an invalid sequence of moves\n" + e.toString());
		}

		// If we haven't won then we need to have used all the dice available
		if (!hasWon) {
			if (turn.getMoves().size() != maxMoves) {
				restoreBoard(oldBoard);
				throw new IllegalTurnException("Not all the dice were used (" + turn.getMoves().size() + "/" + maxMoves + ")");
			}
		}
	}

	@Override
	public boolean isWinner(Colour colour) {
		// If there are PIECES_PER_PLAYER in the end location then the player has won
		return (getEndLocation().numberOfPieces(colour) == PIECES_PER_PLAYER);
	}

	@Override
	public Colour winner() {
		// Iterate over the colours until we find one that has won
		for (Colour c : Colour.values()) {
			if (isWinner(c)) {
				return c;
			}
		}

		// If none can be found then return null
		return null;
	}

	@Override
	public boolean isValid() {
		// Two checks are performed here
		// 1 - Is each individual location valid
		// 2 - Do we have the correct amount of pieces on the board per player

		// Map to store total number of pieces per player
		Map<Colour, Integer> pieceCheck = new HashMap<Colour, Integer>();
		for (Colour c : Colour.values()) {
			pieceCheck.put(c, 0);
		}

		for (LocationInterface loc : locations) {
			// Perform check 1
			if (!loc.isValid()) {
				return false;
			}

			// Update map for check 2
			for (Colour c : Colour.values()) {
				pieceCheck.put(c, pieceCheck.get(c) + loc.numberOfPieces(c));
			}
		}

		// Now that we have the total number of pieces per player, perform check 2
		for (Colour c : Colour.values()) {
			if (pieceCheck.get(c) != PIECES_PER_PLAYER) {
				return false;
			}
		}

		return true;
	}

	@Override
	public Set<MoveInterface> possibleMoves(Colour colour, List<Integer> diceValues) {
		// Create the set to return
		// Make sure that we haven't been passed any null objects
		Set<MoveInterface> posMoves = new HashSet<MoveInterface>();
		if (diceValues == null || colour == null) {
			return posMoves;
		}

		// Remove duplicates from diceValues if they exist
		Set<Integer> dice = new HashSet<Integer>(diceValues);

		// Find the locations that colours exist at
		List<Integer> sourceLocations = new ArrayList<Integer>();
		if (getKnockedLocation().numberOfPieces(colour) > 0) {
			// If we have a piece at knocked then our only source location is 0
			sourceLocations.add(0);
		} else {
			// Do we have any pieces in the start location?
			if (getStartLocation().numberOfPieces(colour) > 0) {
				sourceLocations.add(0);
			}

			for (int i = 1; i <= NUMBER_OF_LOCATIONS; i++) {
				try {
					if (getBoardLocation(i).numberOfPieces(colour) > 0) {
						sourceLocations.add(i);
					}
				} catch (NoSuchLocationException e) {
					// This should never be reached, since i will be between the ranges specified
					System.out.println("Error accessing board location when calculating possible moves");
				}
			}
		}

		// Iterate over possible dice values for each source location
		for (Integer sourceLoc : sourceLocations) {
			for (Integer diceRoll : dice) {
				try {
					MoveInterface move = new Move();
					move.setSourceLocation(sourceLoc);
					move.setDiceValue(diceRoll);

					if (canMakeMove(colour, move)) {
						posMoves.add(move);
					}
				} catch (IllegalMoveException e) {
					// Catch exception if diceValues contains a value out of bounds
					// We can continue anyway
					// System.out.println(e.toString());
				} catch (NoSuchLocationException e) {
					// This should never be reached
					// System.out.println("Error when generating possible moves");
				}
			}

		}
		
		return posMoves;
	}

	@Override
	public BoardInterface clone() {
		// Create a new Board with the new locations and name
		Board boardClone = new Board();
		boardClone.setName(this.name);

		boardClone.setStartLocation(cloneLocation(getStartLocation()));
		boardClone.setKnockedLocation(cloneLocation(getKnockedLocation()));
		boardClone.setEndLocation(cloneLocation(getEndLocation()));

		for (int i = 1; i <= NUMBER_OF_LOCATIONS; i++) {
			try {
				boardClone.setBoardLocation(i, cloneLocation(getBoardLocation(i)));
			} catch (NoSuchLocationException e) {
				// This should never be reached
				System.out.println("Logic error when cloning board");
			}
		}

		// Return a cast version of boardClone
		return (BoardInterface) boardClone;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("Board: " + name + "\n\n");

		// Work out the maximum width of all the data, so we can force all the columns to be the same width
		int maxStringLength = "Location".length();
		for (Colour c : Colour.values()) {
			if (c.toString().length() > maxStringLength) {
				maxStringLength = c.toString().length();
			}
		}
		for (LocationInterface loc : locations) {
			if (loc.getName().length() > maxStringLength) {
				maxStringLength = loc.getName().length();
			}
		}
		// Add padding
		maxStringLength += 2;

		// Add the column headings to the string being built
		sb.append(padString("Location", maxStringLength) + "|");

		for (Colour c : Colour.values()) {
			sb.append(padString(c.toString(), maxStringLength) + "|");
		}
		sb.deleteCharAt(sb.length() - 1);

		// Add line to differentiate between column headings and row data
		StringBuilder line = new StringBuilder();
		int noChars = maxStringLength * (1 + Colour.values().length); // Number of columns x length per columns
		for (int i = 0; i < noChars; i++) {
			line.append("-"); // Was going to use character — instead of -, but decided not to for compatability reasons
		}
		sb.append("\n");
		sb.append(line);

		// Add the locations to the string being built
		for (LocationInterface loc : locations) {
			// We need to build a string for the location
			StringBuilder sl = new StringBuilder();
			sl.append(padString(loc.getName(), maxStringLength) + "|");
			for (Colour c : Colour.values()) {
				sl.append(padString(Integer.toString(loc.numberOfPieces(c)), maxStringLength) + "|");
			}
			sl.deleteCharAt(sl.length() - 1);

			// Add the location to sb as a new row
			sb.append("\n");
			sb.append(sl);
		}

		return sb.toString();
	}

	/*
	 * PUBLIC METHODS NOT DEFINED IN INTERFACE (casting required if method not static)
	 */

	// Recursive function to compute the number of maximum moves possible
	public static int getMaximumMoves(BoardInterface b, Colour colour, List<Integer> diceValues) {
		// Get the possible moves
		Set<MoveInterface> posMoves = b.possibleMoves(colour, diceValues);

		
		// The maximum number of moves we can make with the current diceValues
		int maxValue = 0;

		for (MoveInterface move : posMoves) {
			// Copy the board and make a move
			BoardInterface newB = b.clone();
			try {
				newB.makeMove(colour, move);
			} catch (IllegalMoveException e) {
				// Should never be reached
				System.out.println("Logic error when computing maximum number of possible moves");
				System.out.println(e.toString());
				continue;
			}

			// Copy the dice values and remove the die used
			List<Integer> diceValuesCopy = Misc.copyList(diceValues);
			diceValuesCopy.remove((Integer) move.getDiceValue()); // Casting is required since we want to remove the object, not the object at the location of the primitive (int)

			// Recursively call itself
			int val = getMaximumMoves(newB, colour, diceValuesCopy) + 1;

			// Update the maximum value
			if (val > maxValue) {
				maxValue = val;
				if (maxValue == diceValues.size()) {
					return maxValue;
				}
			}
		}

		return maxValue;
	}

	// Interface does not define this
	public String getName() {
		return name;
	}

	// Set location methods required for cloning (either this or adding an additional constructor)
	public void setStartLocation(LocationInterface loc) {
		locations.set(0, loc);
	}

	public void setEndLocation(LocationInterface loc) {
		locations.set(NUMBER_OF_LOCATIONS + 2, loc);
	}

	public void setKnockedLocation(LocationInterface loc) {
		locations.set(1, loc);
	}

	public void setBoardLocation(int locationNumber, LocationInterface loc) throws NoSuchLocationException {
		if (locationNumber < 1 || locationNumber > NUMBER_OF_LOCATIONS) {
			throw new NoSuchLocationException("The location must be between 1 and " + Integer.toString(NUMBER_OF_LOCATIONS));
		}

		locations.set(locationNumber + 1, loc);
	}

	/*
	 * PRIVATE METHODS
	 */

	// Given an integer >= 0 return the location
	private LocationInterface intToLocation(Colour colour, int pos) {
		LocationInterface endLoc = null;

		if (pos > NUMBER_OF_LOCATIONS) {
			endLoc = getEndLocation();
		} else if (pos < 0) {
			return null;
		} else if (pos == 0) {
			// If the position is 0 then we need to choose either the start or knocked location
			if (getKnockedLocation().numberOfPieces(colour) == 0) {
				return getStartLocation();
			} else {
				return getKnockedLocation();
			}
		} else {
			try {
				endLoc = this.getBoardLocation(pos);
			} catch (NoSuchLocationException e) { 
				// This exception should never be caught
				System.out.println("Warning - logic error when retrieving location");
				return null;
			}
		}

		return endLoc;
	}

	// Given another board, set this board to the oldboard (doesn't clone locations)
	private void restoreBoard(BoardInterface oldBoard) {
		// Don't bother updating name (we can't anyway since the interface doesn't provide a getName() method)
		// Set the locations to the one in the clone (Don't bother to clone since this is an internal method)

		locations = new ArrayList<LocationInterface>();
		locations.add(oldBoard.getStartLocation());
		locations.add(oldBoard.getKnockedLocation());

		for (int i = 1; i <= NUMBER_OF_LOCATIONS; i++) {
			try {
				locations.add(oldBoard.getBoardLocation(i));
			} catch (NoSuchLocationException e) {
				// Shouldn't reach this bit
				System.out.println("Error when restoring board from clone");
			}
		}

		locations.add(oldBoard.getEndLocation());
	}

	// We can't clone all instances of LocationInterface, only Location
	private Location cloneLocation(LocationInterface oldLoc) {
		// Return null if we are given a null value
		if (oldLoc == null || !oldLoc.isValid()) {
			return null;
		}

		Location locClone = new Location(oldLoc.getName()); // Don't need to clone string's as they are immutable
		locClone.setMixed(true); // If we set the location to be mixed then we can add multiple colours to it (needed in case the location is in an invalid state)

		// Add colours to the locations
		for (Colour c : Colour.values()) {
			if (oldLoc.numberOfPieces(c) > 0) {
				try {
					// Add the pieces to the location
					int numPieces = oldLoc.numberOfPieces(c);
					for (int i = 0; i < numPieces; i++) {
						locClone.addPieceGetKnocked(c);
					}
				} catch (IllegalMoveException e) {
					// Shouldn't get to this point
					System.out.println("Error when cloning Location");
					return null;
				}
			}
		}

		// We can now set isMixed, since we have put all the pieces into the location
		locClone.setMixed(oldLoc.isMixed());

		return locClone;
	}

	// Returns a map from turn for takeTurn
	// We can't use Misc.valuesToMap as we need to extract the values from the move (we could create a new list, but that would waste memory and time)
	private Map<Integer, Integer> turnToMap(TurnInterface turn) {
		Map<Integer, Integer> turnMap = new HashMap<Integer, Integer>();

		for (MoveInterface move : turn.getMoves()) {
			// Add the current value from turn into turnMap
			int turnRoll = move.getDiceValue();
			turnMap.put(turnRoll, turnMap.getOrDefault(turnRoll, 0) + 1);
		}

		return turnMap;
	}

	// Pad the string until it is the length specified. Pads the string centrally
	private String padString(String s, int length) {
		StringBuilder sb = new StringBuilder(s);
		boolean addToEnd = true;

		while (sb.length() < length) {
			if (addToEnd) {
				sb.append(" ");
			} else {
				sb.insert(0, " ");
			}

			addToEnd = !addToEnd;
		}

		return sb.toString();
	}
}
