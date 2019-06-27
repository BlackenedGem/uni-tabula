import java.util.Random;

public class Die implements DieInterface {
	//Static random variable
	private static final Random random = new Random();
	//Object variables
	private int rollValue; //Value of 0 indicates that the die has not been rolled
	
	public Die() {
		rollValue = 0;
	}

	@Override
	public boolean hasRolled() {
		return (rollValue != 0);
	}

	@Override
	public void roll() {
		rollValue = random.nextInt(NUMBER_OF_SIDES_ON_DIE);
		rollValue++; //Since nextInt returns [0, n)
	}

	@Override
	public int getValue() throws NotRolledYetException {
		if (!hasRolled()) {
			throw new NotRolledYetException("Die not rolled yet");
		}
		return rollValue;
	}

	@Override
	public void setValue(int value) {
		if (value < 1 || value > NUMBER_OF_SIDES_ON_DIE) {
			rollValue = 0;
		} else {
			rollValue = value;
		}

	}

	@Override
	public void clear() {
		rollValue = 0;
	}

	@Override
	public void setSeed(long seed) {
		random.setSeed(seed);
	}

}
