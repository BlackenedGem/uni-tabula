import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ComputerPlayerOld implements PlayerInterface {
	// We use this at various points (cloning is used to ensure we always have the original)
	private BoardInterface originalBoard;

	public ComputerPlayerOld() {
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
	
	// Returns the number of pieces that are alone in the specified locations
	private int getAlonePieces(BoardInterface board, Set<Integer> locations, Colour colour) {
		int count = 0;
		
		for (Integer loc : locations) {
			try {
				if (board.getBoardLocation(loc).numberOfPieces(colour) == 1) {
					count++;
				}
			} catch (NoSuchLocationException e) {
				// This should never be reached
				System.out.println("Logic error when counting lone pieces");
			}
		}
		
		return count;
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
		 * Getting a piece to the end
		 * 
		 * Things that we want to avoid: Breaking chains of doubles
		 * Splitting a double into 1 or more singles
		 * Not using all of the dice
		 * 
		 * We can score these things using weights (addition/subtraction), coefficients (multiplication), and other formulae
		 * 
		 * Extra considerations - singles behind opponents last piece, adding to doubles, stretching thin to find holes
		 */
		
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
		
		int alonePiecesBefore = getAlonePieces(boardBefore, changedLocations, colour);
		int alonePiecesAfter = getAlonePieces(boardAfter, changedLocations, colour);
		
		return (double) (alonePiecesBefore - alonePiecesAfter);
	}
}
