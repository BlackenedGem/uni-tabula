import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javafx.application.Platform;

/*
 * This class was designed to depend only on the interfaces given, so contains duplicates of Misc.copyList and Board.getMaximumMoves
 * Testing showed the player performed 400+ turns per second (including processing of the game) on a stock 4790k
 */
public class ComputerPlayer implements PlayerInterface {
	// Weights and thresholds for scoring
	// Optimised for the given constants, although different constants should not impact the win rate drastically
	private static final double[] THRESHOLD_DICE_USE = { 1, 2, 3 };
	private static final double WEIGHT_DOUBLE = 0.15;
	private static final double WEIGHT_DOUBLE_CHAIN_BONUS = 0.075;
	private static final double[] WEIGHT_KNOCKED = { 1, 1.15, 1.5, 2 };
	private static final double WEIGHT_LONE_PIECES = 0.5;
	private static final double WEIGHT_LONE_PIECES_BEHIND_MULT = -0.4;
	private static final double WEIGHT_PIECES_OUT = 0.2;
	private static final double WEIGHT_PIECES_HOME = 0.3;
	private static final double WEIGHT_PIECES_HOME_NOT_ALL_OUT = 0.15;
	private static final double[] WEIGHT_DICE_USE = { -0.2, -0.5 }; // Element 0 = all pieces out, Element 1 = pieces still in source location
	private static final double[][] WEIGHT_DICE_USE_THRESHOLDS_MULT = { { 0, 0.2 }, { 1, 0.5 }, { 2, 1 } }; // Element [i][0] = ith threshold, all pieces out
	private static final double[] WEIGHT_BLOCKING_START = { 0.6, 0.8, 1, 1.5 };
	private static final double WEIGHT_BLOCKING_START_OPP_BEHIND_MULT = 1.5;

	// Rather than passing variables around, store the commonly used ones
	private BoardInterface boardBefore;
	private Colour colour;
	private List<Integer> diceValues;
	private int lastOpponentBefore;
	private double scoreDoublesBefore;

	public ComputerPlayer() {
	}

	@Override
	public TurnInterface getTurn(Colour colour, BoardInterface board, List<Integer> diceValues) throws PauseException {
		// If the program is running in the GUI, then the game is run on a separate thread that can be interrupted (eg. closing the gui)
		if (Thread.currentThread().isInterrupted()) {
			throw new PauseException("Thread interrupted");
		}

		// Attempt to update the GUI if it exists (if it doesn't exist then we just catch the exception and move on)
		try {
			GUIMain guiMain = (GUIMain) GUIBase.getControllers().get("main");

			if (guiMain != null) {
				Platform.runLater(new Runnable() {
					public void run() {
						guiMain.updateText();
						guiMain.updateLocations(board);
					}
				});
			}
		} catch (Exception e) {
			// Not running the game in a gui
		}

		// If arguments are null or invalid then return a turn without any moves
		if (colour == null || board == null || diceValues == null || !board.isValid() || (diceValues.size() != 2 && diceValues.size() != 4)) {
			System.out.println("The computer player received invalid arguments");
			return new Turn();
		}

		// Implementation: Generate all possible turns, assign a score to each one, pick highest scoring valid turn
		// Get all the valid turns
		int movesRequired = getMaximumMoves(board, colour, diceValues);
		List<TurnInterface> posTurns = getValidTurns(board, colour, diceValues, new Turn(), movesRequired);

		if (posTurns.size() == 0) {
			return new Turn();
		}

		// Setup variables and perform initial analysis on the board (that doesn't depend on an individual turn)
		this.boardBefore = board;
		this.diceValues = diceValues;
		this.colour = colour;
		this.lastOpponentBefore = getLastLocation(board, colour.otherColour());
		this.scoreDoublesBefore = scoreDoublesOnFirstLocs(board, colour, lastOpponentBefore);

		// Score the turns
		List<Double> scores = new ArrayList<Double>();
		for (TurnInterface turn : posTurns) {
			scores.add(scoreTurn(turn));
		}

		// Find the maximum score
		int index = 0;
		double maxValue = scores.get(0);
		for (int i = 1; i < scores.size(); i++) {
			if (scores.get(i) > maxValue) {
				maxValue = scores.get(i);
				index = i;
			}
		}

		// Return the turn with the highest score
		return posTurns.get(index);
	}

	private TurnInterface cloneTurn(TurnInterface turn) {
		TurnInterface newTurn = new Turn();

		for (MoveInterface move : turn.getMoves()) {
			try {
				MoveInterface newMove = new Move();
				newMove.setSourceLocation(move.getSourceLocation());
				newMove.setDiceValue(move.getDiceValue());
				newTurn.addMove(newMove);
			} catch (Exception e) {
				// This should never be reached
				System.out.println("Error when cloning a turn");
				System.out.println(e.toString());
			}
		}

		return newTurn;
	}

	/**
	 * Calculates how many pieces are in each location, and applies scoring weights if necessary
	 * 
	 * @param board
	 *            The board to use
	 * @param locations
	 *            The locations to check on the board
	 * @param colour
	 *            The colour of pieces we are recording
	 * @param colour
	 *            The location as an integer of the opponents furthest behind piece
	 * @return An array of length 3 containing the following:
	 *         Index 0 - Number Locations with 0 pieces
	 *         Index 1 - Weighted score of locations with 1 piece
	 *         Index 2 - Weighted score of locations with 2 or more pieces
	 */
	private double[] countPieces(BoardInterface board, Set<Integer> locations, Colour colour, int lastOpponent) {
		// Initialise array
		double[] counts = new double[3];
		for (int i = 0; i < counts.length; i++) {
			counts[i] = 0;
		}

		// Iterate over the locations given, find out number of pieces in each location and apply calculations
		for (Integer loc : locations) {
			try {
				int numPieces = board.getBoardLocation(loc).numberOfPieces(colour);
				if (numPieces == 0) {
					counts[0]++;
				}
				if (numPieces == 1) {
					// Change weighting if there are no opposition pieces behind the current location
					if (loc > lastOpponent) {
						counts[1]++;
					} else {
						counts[1] += WEIGHT_LONE_PIECES_BEHIND_MULT;
					}
				} else if (numPieces > 1) {
					double score = WEIGHT_DOUBLE;

					if (loc < BoardInterface.NUMBER_OF_LOCATIONS && board.getBoardLocation(loc + 1).numberOfPieces(colour) > 1) {
						score += WEIGHT_DOUBLE_CHAIN_BONUS;
					}
					if (loc > 1 && board.getBoardLocation(1).numberOfPieces(colour) > 1) {
						score += WEIGHT_DOUBLE_CHAIN_BONUS;
					}

					counts[2] += score;
				}
			} catch (NoSuchLocationException e) {
				// This should never be reached
				System.out.println("Logic error when counting pieces");
			}
		}

		return counts;
	}

	// Calculate how many values were not used
	private int getDiceNotUsed(TurnInterface turn) {
		int discrepancy = 0;

		for (MoveInterface move : turn.getMoves()) {
			int endLocation = move.getSourceLocation() + move.getDiceValue();

			if (endLocation > BoardInterface.NUMBER_OF_LOCATIONS) {
				discrepancy += endLocation - (BoardInterface.NUMBER_OF_LOCATIONS + 1);
			}
		}

		return discrepancy;
	}

	private int getLastLocation(BoardInterface board, Colour colour) {
		if (board.getStartLocation().numberOfPieces(colour) > 0) {
			return 0;
		}
		if (board.getKnockedLocation().numberOfPieces(colour) > 0) {
			return 0;
		}

		for (int i = 1; i <= BoardInterface.NUMBER_OF_LOCATIONS; i++) {
			try {
				if (board.getBoardLocation(i).numberOfPieces(colour) > 0) {
					return i;
				}
			} catch (NoSuchLocationException e) {
				// This should never be reached
				System.out.println("Logic error finding the opponent's lowest location: " + e.getMessage());
			}
		}

		return BoardInterface.NUMBER_OF_LOCATIONS + 1;
	}

	// Recursively generate turns. Returns a list of valid turns (needs a lot of arguments due to recursive nature)
	private List<TurnInterface> getValidTurns(BoardInterface b, Colour colour, List<Integer> diceValues, TurnInterface curTurn, int movesRequired) {
		List<TurnInterface> turns = new ArrayList<TurnInterface>();
		Set<MoveInterface> posMoves = b.possibleMoves(colour, diceValues);

		for (MoveInterface move : posMoves) {
			// Copy the board and make a move
			BoardInterface newB = b.clone();
			TurnInterface newT = cloneTurn(curTurn);

			try {
				newB.makeMove(colour, move);
				newT.addMove(move);
			} catch (Exception e) {
				// Should never be reached
				System.out.println("Logic error when generating possible turns");
				System.out.println(e.toString());
				continue;
			}

			// Copy the dice values and remove the die used
			List<Integer> diceValuesCopy = copyList(diceValues);
			diceValuesCopy.remove((Integer) move.getDiceValue()); // Casting is required since we want to remove the object, not the object at the location of the primitive (int)

			// Recursively call itself if we have not reached the base case
			if (newT.getMoves().size() == movesRequired) {
				turns.add(newT);
			} else {
				List<TurnInterface> recursiveTurns = getValidTurns(newB, colour, diceValuesCopy, newT, movesRequired);
				turns.addAll(recursiveTurns);
			}
		}

		return turns;
	}

	// Doubles that are immediately after the start/knocked location are much better doubles
	private double scoreDoublesOnFirstLocs(BoardInterface board, Colour colour, int lastOpponent) {
		int doubleCount = 0;
		double score;
		int iterations = DieInterface.NUMBER_OF_SIDES_ON_DIE;

		if (BoardInterface.NUMBER_OF_LOCATIONS < iterations) {
			iterations = BoardInterface.NUMBER_OF_LOCATIONS;
		}

		for (int i = 1; i <= iterations; i++) {
			try {
				if (board.getBoardLocation(i).numberOfPieces(colour) > 1) {
					doubleCount++;
				}
			} catch (NoSuchLocationException e) {
				// Should never be reached (unless the sides on the dice are bigger than the number of locations)
				System.out.println("Logic error when counting doubles on the initial locations: " + e.getMessage());
			}
		}

		doubleCount += 3 - DieInterface.NUMBER_OF_SIDES_ON_DIE;
		if (doubleCount < 0) {
			return 0;
		}

		score = WEIGHT_BLOCKING_START[doubleCount];

		if (lastOpponent == 0) {
			score *= WEIGHT_BLOCKING_START_OPP_BEHIND_MULT;
		} else {
			score /= WEIGHT_BLOCKING_START_OPP_BEHIND_MULT;
		}

		return score;
	}

	private double scoreTurn(TurnInterface turn) {
		/*
		 * Things that are good:
		 * Winning the game - doesn't need to be checked for (due to the way the turns are generated)
		 * Knocking opponents pieces off
		 * Making a single piece into a double
		 * Making a chain of doubles
		 * Getting a piece to the end (this is actually the least good out of all the good things)
		 * 
		 * Things that we want to avoid: Breaking chains of doubles
		 * Splitting a double into 1 or more singles
		 * Not using all of the dice
		 * 
		 * We can score these things using weights (addition/subtraction), coefficients (multiplication), and other formulae
		 */

		// Get the board after the turn has been taken
		BoardInterface boardAfter = boardBefore.clone();
		try {
			boardAfter.takeTurn(colour, turn, diceValues);
		} catch (IllegalTurnException e) {
			// This should never be reached
			System.out.println("Logic error scoring a turn (could not perform a turn)");
			return -1000;
		}

		double scoreKnocked = 0;
		boolean allPiecesOut = (boardAfter.getStartLocation().numberOfPieces(colour) == 0);

		// Get the locations 1 - 24 that have been changed (also perform some calculations)
		Set<Integer> changedLocations = new HashSet<Integer>();
		for (MoveInterface move : turn.getMoves()) {
			int sourceLoc = move.getSourceLocation();
			int endLoc = move.getSourceLocation() + move.getDiceValue();

			if (sourceLoc != 0) {
				changedLocations.add(sourceLoc);
			}
			if (endLoc <= BoardInterface.NUMBER_OF_LOCATIONS) {
				changedLocations.add(endLoc);

				// Check if we knocked a piece off
				try {
					if (boardBefore.getBoardLocation(endLoc).numberOfPieces(colour.otherColour()) > 0) {
						int boardQuarter = BoardInterface.NUMBER_OF_LOCATIONS / 4;

						// Weight the piece being knocked off according to how far round it is
						for (int i = boardQuarter * 3; i >= 0; i -= boardQuarter) {
							if (endLoc > i) {
								scoreKnocked += WEIGHT_KNOCKED[i / boardQuarter];
							}
						}
					}
				} catch (NoSuchLocationException e) {
					// This should never be reached
					System.out.println("Error accessing board location when computing knocked off pieces");
				}
			}
		}

		// Get the location of the opponents piece that is in the lowest position
		int lastOpponentAfter = getLastLocation(boardAfter, colour.otherColour());

		// Count the number of pieces in the changed locations
		double[] piecesBefore = countPieces(boardBefore, changedLocations, colour, lastOpponentBefore);
		double[] piecesAfter = countPieces(boardAfter, changedLocations, colour, lastOpponentAfter);

		// Get the number of pieces in the start location and end location
		int numStart = boardBefore.getStartLocation().numberOfPieces(colour);
		numStart -= boardAfter.getStartLocation().numberOfPieces(colour);
		int numEnd = boardAfter.getEndLocation().numberOfPieces(colour);
		numEnd = boardBefore.getEndLocation().numberOfPieces(colour);

		// Calculate how many dice were used
		double scoreDiceUse = 0;
		int diceNotUsed = getDiceNotUsed(turn);
		if (diceNotUsed > 0) {
			// Get the index of the threshold array to use
			int diceThreshold = 0;
			int arraySize = THRESHOLD_DICE_USE.length - 1;
			while (diceThreshold < arraySize && THRESHOLD_DICE_USE[diceThreshold + 1] <= diceNotUsed) {
				diceThreshold++;
			}

			// Get the second index (whether or not all the pieces are out)
			int piecesOutIndex = 0;
			if (!allPiecesOut) {
				piecesOutIndex = 1;
			}

			// Apply weightings
			scoreDiceUse = WEIGHT_DICE_USE[piecesOutIndex] * WEIGHT_DICE_USE_THRESHOLDS_MULT[diceThreshold][piecesOutIndex];
		}

		// Get all the individual metrics (except those that have been calculated already - scoreKnocked)
		double scoreAlone = (piecesBefore[1] - piecesAfter[1]) * WEIGHT_LONE_PIECES;
		double scoreDoubles = piecesAfter[2] - piecesBefore[2];
		double scoreNumStart = (double) numStart;
		scoreNumStart *= WEIGHT_PIECES_OUT;
		double scoreNumEnd = (double) numEnd;
		if (allPiecesOut) {
			scoreNumEnd *= WEIGHT_PIECES_HOME;
		} else {
			scoreNumEnd *= WEIGHT_PIECES_HOME_NOT_ALL_OUT;
		}
		double scoreInitialDoubles = scoreDoublesOnFirstLocs(boardAfter, colour, lastOpponentAfter) - scoreDoublesBefore;

		// Combine all these individual metrics into a single score
		double score = scoreAlone;
		score += scoreDoubles;
		score += scoreKnocked;
		score += scoreNumStart;
		score += scoreNumEnd;
		score += scoreDiceUse;
		score += scoreInitialDoubles;
		return score;
	}

	/*
	 * Duplicate methods so that this class works on its own
	 */
	private int getMaximumMoves(BoardInterface b, Colour colour, List<Integer> diceValues) {
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
			List<Integer> diceValuesCopy = copyList(diceValues);
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

	private List<Integer> copyList(List<Integer> originalList) {
		List<Integer> newList = new ArrayList<Integer>();

		for (Integer i : originalList) {
			newList.add(i);
		}

		return newList;
	}
}
