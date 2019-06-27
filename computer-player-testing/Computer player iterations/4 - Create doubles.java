import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ComputerPlayer implements PlayerInterface {
	// We use this at various points (cloning is used to ensure we always have the original)
	private BoardInterface originalBoard;
	
	// Weights for scoring
	private static final double WEIGHT_DOUBLE_CREATION = 1;
	private static final double WEIGHT_KNOCKED = 1;
	private static final double WEIGHT_LONE_PIECES = 0.5;
	private static final double WEIGHT_PIECES_OUT = 0.5;
	

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

		// Backup the original board
		originalBoard = board;

		// Implementation: Generate all possible turns, assign a score to each one, pick highest scoring valid turn
		// Get all the valid turns
		int movesRequired = Board.getMaximumMoves(board, colour, diceValues);
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

	// Recursively generate turns. Returns a list of valid turns
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
	 *  Returns how many locations contain a certain number of pieces (only for the locations given)
	 * @param board The board to use
	 * @param locations The locations to check on the board
	 * @param colour The colour of pieces we are recording
	 * @return An array of length 3 containing the following:
	 * 			Index 0 - Locations with 0 pieces
	 * 			Index 1 - Locations with 1 piece
	 * 			Index 2 - Locations with 2 or more pieces
	 */
	private int[] countPieces(BoardInterface board, Set<Integer> locations, Colour colour) {
		int[] counts = new int[3];
		for (int i = 0; i < counts.length; i++) {
			counts[i] = 0;
		}
		
		for (Integer loc : locations) {
			try {
				int numPieces = board.getBoardLocation(loc).numberOfPieces(colour);
				if (numPieces == 0) {
					counts[0]++;
				} if (numPieces == 1) {
					counts[1]++;
				} else if (numPieces > 1) {
					counts[2]++;
				}
			} catch (NoSuchLocationException e) {
				// This should never be reached
				System.out.println("Logic error when counting pieces");
			}
		}
		
		return counts;
	}
	
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
			List<Integer> diceValuesCopy = Misc.copyList(diceValues);
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
		 * 
		 * Extra considerations - singles behind opponents last piece, adding to doubles, stretching thin to find holes
		 */
		
		// TODO add weight values
		// TODO add incentive to chain doubles
		// TODO add incentive to get pieces out
		// TODO move Board.getMaximumMoves and Misc.CopyList in here (remove dependence on non interface methods from other classes)
		// TODO calculate information for the default turn before each turn
		
		// Get the locations 1 - 24 that have been changed
		BoardInterface boardAfter = boardBefore.clone();
		try {
			boardAfter.takeTurn(colour, turn, diceValues);
		} catch (IllegalTurnException e) {
			// This should never be reached
			System.out.println("Logic error scoring a turn");
			return -1000;
		}
		
		Set<Integer> changedLocations = new HashSet<Integer>();
		for (MoveInterface move : turn.getMoves()) {
			int sourceLoc = move.getSourceLocation();
			int endLoc = move.getSourceLocation() + move.getDiceValue();
			
			if (sourceLoc != 0) {
				changedLocations.add(sourceLoc);
			}
			if (endLoc <= BoardInterface.NUMBER_OF_LOCATIONS) {
				changedLocations.add(endLoc);
			}
		}
		
		// Count the number of pieces in the changed location
		int[] piecesBefore = countPieces(boardBefore, changedLocations, colour);
		int[] piecesAfter = countPieces(boardAfter, changedLocations, colour);
		
		// Get the number of the opponents pieces knocked off
		int numKnocked = boardAfter.getKnockedLocation().numberOfPieces(colour.otherColour());
		numKnocked -= boardBefore.getKnockedLocation().numberOfPieces(colour.otherColour());
		
		// Combine all these individual metrics into a single score
		double score = (double) (piecesBefore[1] - piecesAfter[1]) * WEIGHT_LONE_PIECES; // Alone pieces
		score += (piecesAfter[2] - piecesAfter[2]) * WEIGHT_DOUBLE_CREATION; // Creating doubles
		score += numKnocked * WEIGHT_KNOCKED; // Knocking pieces off
		return score;
	}
}
