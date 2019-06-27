import java.util.HashMap;
import java.util.Map;

public class Location implements LocationInterface {
	private boolean isMixed;
	private String name;
	private Map<Colour, Integer> pieces; //We only expect 2 colours, but using a map allows a relatively easy transistion to multiple colours
	
	public Location(String name) {
		this.name = name;
		isMixed = false;
		pieces = new HashMap<Colour, Integer>();
	}
	
	// In case a blank constructor is also needed
	public Location() {
		this.name = "undefined";
		isMixed = false;
		pieces = new HashMap<Colour, Integer>();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public boolean isMixed() {
		return isMixed;
	}

	@Override
	public void setMixed(boolean isMixed) {
		this.isMixed = isMixed;
	}

	@Override
	public boolean isEmpty() {
		return pieces.isEmpty();
	}

	@Override
	public int numberOfPieces(Colour colour) {
		return pieces.getOrDefault(colour, 0);
	}

	@Override
	public boolean canAddPiece(Colour colour) {
		// Check argument
		if (colour == null) {
			return false;
		}
		
		//If the location is mixed then we can always add a piece
		if (isMixed) {
			return true;
		}
		
		//We can't add a piece if there exists 2 or more pieces of a different colour
		for (Colour c : pieces.keySet()) {
			if (pieces.get(c) >= 2 && c != colour) {
				return false;
			}
		}
		
		return true;
	}

	@Override
	public Colour addPieceGetKnocked(Colour colour) throws IllegalMoveException {
		if (colour == null) {
			throw new IllegalMoveException("colour must not be null");
		}
		
		Colour ret = null;
		
		//If it is not a mixed location then knock off all the other colours (or throw exception if colour contains 2 or more pieces)
		if (!isMixed) {
			for (Colour c : pieces.keySet()) {
				if (c == colour) {
					continue;
				}
				
				if (pieces.get(c) >= 2) {
					throw new IllegalMoveException("Cannot move to a location which contains 2 or more (same-coloured) pieces of a different colour");
				}
				
				//Remove the piece and store the colour (for when we return at the end)
				ret = c;
				this.removePiece(c);
			}
		}
		
		//Add the piece
		pieces.put(colour, pieces.getOrDefault(colour, 0) + 1);
		
		//Return (will either be a colour or null)
		return ret;
	}

	@Override
	public boolean canRemovePiece(Colour colour) {
		return pieces.containsKey(colour);
	}

	@Override
	public void removePiece(Colour colour) throws IllegalMoveException {
		//Throw exception if piece doesn't exist
		if (!canRemovePiece(colour)) {
			throw new IllegalMoveException("Cannot remove a piece that does not exist");
		}
		
		//If there is only 1 piece of the colour, then remove it from the map, otherwise decrement
		if (pieces.get(colour) == 1) {
			pieces.remove(colour);
		} else {
			pieces.put(colour, pieces.get(colour) - 1);
		}
	}

	@Override
	public boolean isValid() {
		//If the location is mixed then our location is always valid
		if (isMixed) {
			return true;
		}
		
		//If we have 2 or more colours in the location then we are not in a valid state 
		return (pieces.size() <= 1);
	}

}
