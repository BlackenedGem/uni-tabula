/*
 * Due to the way table views are designed, it's easiest to create a separate class to handle rows
 * This also gives us the opportunity to handle conversions from moves to strings
 */

public class TableMove {
	private String sourceLoc;
	private String endLoc;
	private String diceValue;
	
	public TableMove(MoveInterface move) {
		sourceLoc = Integer.toString(move.getSourceLocation());
		diceValue = Integer.toString(move.getDiceValue());
		
		int endLocInt = move.getSourceLocation() + move.getDiceValue();
		if (endLocInt > BoardInterface.NUMBER_OF_LOCATIONS) {
			endLoc = "End";
		} else {
			endLoc = Integer.toString(endLocInt);
		}
	}
	
	public String getFrom() {
		return sourceLoc;
	}
	
	public String getTo() {
		return endLoc;
	}
	
	public String getRoll() {
		return diceValue;
	}
}
