import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ComputerPlayer implements PlayerInterface {
	// We use this at various points (cloning is used to ensure we always have the original)
	
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

		int movesRequired = Board.getMaximumMoves(board, colour, diceValues);
		List<TurnInterface> posTurns = getValidTurns(board, colour, diceValues, new Turn(), movesRequired);
		
		if (posTurns.size() == 0) {
			return new Turn();
		} else {
			return posTurns.get(0);
		}
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
}
