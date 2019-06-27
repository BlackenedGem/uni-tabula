
public class Move implements MoveInterface {
	private int sourceLoc; // 0 = start/knocked, others = board location
	private int diceValue;

	public Move() {
		// Documentation doesn't specify values if none given, so use the lowest valid values
		sourceLoc = 0;
		diceValue = 1;
	}

	@Override
	public void setSourceLocation(int locationNumber) throws NoSuchLocationException {
		if (locationNumber < 0 || locationNumber > BoardInterface.NUMBER_OF_LOCATIONS) {
			throw new NoSuchLocationException("The location must be in the range 0-" + BoardInterface.NUMBER_OF_LOCATIONS);
		}

		this.sourceLoc = locationNumber;
	}

	@Override
	public int getSourceLocation() {
		return sourceLoc;
	}

	@Override
	public void setDiceValue(int diceValue) throws IllegalMoveException {
		if (diceValue < 1 || diceValue > Die.NUMBER_OF_SIDES_ON_DIE) {
			throw new IllegalMoveException("Dice value must be in the range 0-" + Die.NUMBER_OF_SIDES_ON_DIE);
		}

		this.diceValue = diceValue;
	}

	@Override
	public int getDiceValue() {
		return diceValue;
	}

}
