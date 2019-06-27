import java.util.ArrayList;
import java.util.List;

public class Dice implements DiceInterface {
	private List<DieInterface> dice;
	
	public Dice() {
		dice = new ArrayList<DieInterface>();
		dice.add(new Die());
		dice.add(new Die());
	}

	@Override
	public boolean haveRolled() {
		for (DieInterface d : dice) {
			if (!d.hasRolled()) {
				return false;
			}
		}
		
		return true;
	}

	@Override
	public void roll() {
		for (DieInterface d : dice) {
			d.roll();
		}
	}

	@Override
	public List<Integer> getValues() throws NotRolledYetException {
		List<Integer> values = new ArrayList<Integer>();
		
		int value1 = dice.get(0).getValue();
		int value2 = dice.get(1).getValue();
		
		//This doesn't look particularly neat/concise, but it's this way or using loops (which is sort of unnecessary for 2 items)
		values.add(value1);
		values.add(value2);
		if (value1 == value2) {
			values.add(value1);
			values.add(value2);
		}
		
		return values;
	}

	@Override
	public void clear() {
		for (DieInterface d : dice) {
			d.clear();
		}
	}

	@Override
	public List<DieInterface> getDice() {
		return dice;
	}

}
