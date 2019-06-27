import java.util.ArrayList;
import java.util.List;

public class Turn implements TurnInterface {
	private List<MoveInterface> moves;
	
	public Turn() {
		moves = new ArrayList<MoveInterface>();
	}

	@Override
	public void addMove(MoveInterface move) throws IllegalTurnException {
		if (move == null) {
			throw new IllegalTurnException("The move cannot be null");
		}
		
		if (moves.size() >= 4) {
			throw new IllegalTurnException("A turn cannot consist of more than 4 moves");
		}
		
		moves.add(move);
	}

	@Override
	public List<MoveInterface> getMoves() {
		return moves;
	}

}
