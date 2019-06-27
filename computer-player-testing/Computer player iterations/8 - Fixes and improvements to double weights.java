import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
 * This class was designed to depend only on the interfaces given, so contains duplicates of Misc.copyList and Board.getMaximumMoves
 * Testing showed the player performed 600+ turns per second (including processing of the game) on a stock 4790k
 */
public class ComputerPlayer implements PlayerInterface {
	// Weights for scoring
	private static final double WEIGHT_DOUBLE = 0.2;
	private static final double WEIGHT_DOUBLE_CHAIN_BONUS = 0.05;
	private static final double WEIGHT_KNOCKED = 1;
	private static final double[] WEIGHT_KNOCKED_PROGRESS_MULT = { 1, 1.15, 1.5, 2 };
	private static final double WEIGHT_LONE_PIECES = 0.5;
	private static final double WEIGHT_PIECES_OUT = 0.2;
	private static final double WEIGHT_PIECES_HOME = 0.3;
	private static final double WEIGHT_PIECES_HOME_NOT_ALL_OUT_MULT = 0.5;
	private static final double WEIGHT_DICE_USE = -0.4;

	public ComputerPlayer() {
	}

	@Override
	public TurnInterface getTurn(Colour colour, BoardInterface board, List<Integer> diceValues) throws PauseException {
		// If the program is running in the GUI, then the game is run on a separate thread that can be interrupted (eg. closing the gui)
		if (Thread.currentThread().isInterrupted()) {
			throw new PauseException("Thread interrupted");
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

		// Score the turns
		List<Double> scores = new ArrayList<Double>();
		for (TurnInterface turn : posTurns) {
			scores.add(scoreTurn(turn, board, colour, diceValues));
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
	 * Returns how many locations contain a certain number of pieces (only for the locations given). Also applies scoring weights for chain doubles
	 * 
	 * @param board
	 *            The board to use
	 * @param locations
	 *            The locations to check on the board
	 * @param colour
	 *            The colour of pieces we are recording
	 * @return An array of length 3 containing the following:
	 *         Index 0 - Locations with 0 pieces
	 *         Index 1 - Locations with 1 piece
	 *         Index 2 - Locations with 2 or more pieces (plus weighting)
	 */
	private double[] countPieces(BoardInterface board, Set<Integer> locations, Colour colour) {
		double[] counts = new double[3];
		for (int i = 0; i < counts.length; i++) {
			counts[i] = 0;
		}

		for (Integer loc : locations) {
			try {
				int numPieces = board.getBoardLocation(loc).numberOfPieces(colour);
				if (numPieces == 0) {
					counts[0]++;
				}
				if (numPieces == 1) {
					counts[1]++;
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

	// Recursively generate turns. Returns a list of valid turns
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

	private double scoreTurn(TurnInterface turn, BoardInterface boardBefore, Colour colour, List<Integer> diceValues) {
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

		// Possible improvements:
		// Get last blue piece after turn, penalise lone pieces less (eg. 0.25 mult)

		// Get the board after the turn has been taken
		BoardInterface boardAfter = boardBefore.clone();
		try {
			boardAfter.takeTurn(colour, turn, diceValues);
		} catch (IllegalTurnException e) {
			// This should never be reached
			System.out.println("Logic error scoring a turn");
			return -1000;
		}

		double scoreKnocked = 0;
		int totMovement = 0;

		// Get the locations 1 - 24 that have been changed (also perform some calculations)
		Set<Integer> changedLocations = new HashSet<Integer>();
		for (MoveInterface move : turn.getMoves()) {
			int sourceLoc = move.getSourceLocation();
			int endLoc = move.getSourceLocation() + move.getDiceValue();

			if (sourceLoc != 0) {
				changedLocations.add(sourceLoc);
			}
			if (endLoc <= BoardInterface.NUMBER_OF_LOCATIONS) {
				totMovement += move.getDiceValue();
				changedLocations.add(endLoc);

				// Check if we knocked a piece off
				try {
					if (boardBefore.getBoardLocation(endLoc).numberOfPieces(colour.otherColour()) > 0) {
						// Weight the piece being knocked off according to how far round it is
						if (endLoc > 12) {
							if (endLoc > 18) {
								scoreKnocked += WEIGHT_KNOCKED * WEIGHT_KNOCKED_PROGRESS_MULT[3];
							} else {
								scoreKnocked += WEIGHT_KNOCKED * WEIGHT_KNOCKED_PROGRESS_MULT[2];
							}
						} else if (endLoc > 6) {
							scoreKnocked += WEIGHT_KNOCKED * WEIGHT_KNOCKED_PROGRESS_MULT[1];
						} else {
							scoreKnocked += WEIGHT_KNOCKED * WEIGHT_KNOCKED_PROGRESS_MULT[0];
						}
					}
				} catch (NoSuchLocationException e) {
					// This should never be reached
					System.out.println("Error accessing board location when computing knocked off pieces");
				}
			} else {
				totMovement = (BoardInterface.NUMBER_OF_LOCATIONS + 1) - move.getSourceLocation();
			}
		}

		// Count the number of pieces in the changed locations
		double[] piecesBefore = countPieces(boardBefore, changedLocations, colour);
		double[] piecesAfter = countPieces(boardAfter, changedLocations, colour);

		// Get the number of pieces in the start location and end location
		int numStart = boardBefore.getStartLocation().numberOfPieces(colour);
		numStart -= boardAfter.getStartLocation().numberOfPieces(colour);
		int numEnd = boardAfter.getEndLocation().numberOfPieces(colour);
		numEnd = boardBefore.getEndLocation().numberOfPieces(colour);

		// Get all the individual metrics (except those that have been calculated already - scoreKnocked)
		double scoreAlone = (piecesBefore[1] - piecesAfter[1]) * WEIGHT_LONE_PIECES;
		double scoreDoubles = piecesAfter[2] - piecesBefore[2];
		double scoreNumStart = (double) numStart;
		scoreNumStart *= WEIGHT_PIECES_OUT;
		double scoreDiceUse = 0;
		if (totMovement < diceValues.size()) {
			scoreDiceUse = WEIGHT_DICE_USE;
		}
		double scoreNumEnd = (double) numEnd;
		scoreNumEnd *= WEIGHT_PIECES_HOME;
		if (boardAfter.getStartLocation().numberOfPieces(colour) > 0) {
			scoreNumEnd *= WEIGHT_PIECES_HOME_NOT_ALL_OUT_MULT;
		}

		// Combine all these individual metrics into a single score
		double score = scoreAlone;
		score += scoreDoubles;
		score += scoreKnocked;
		score += scoreNumStart;
		score += scoreNumEnd;
		score += scoreDiceUse;
		return score;
	}

	/*
	 * Duplicate methods so that this class works on its own
	 */
	private static int getMaximumMoves(BoardInterface b, Colour colour, List<Integer> diceValues) {
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

	private static List<Integer> copyList(List<Integer> originalList) {
		List<Integer> newList = new ArrayList<Integer>();

		for (Integer i : originalList) {
			newList.add(i);
		}

		return (List<Integer>) newList;
	}
}
