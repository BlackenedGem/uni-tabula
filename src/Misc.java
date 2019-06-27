import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Helper class to store methods that are used between classes
 */

public class Misc {

	public Misc() {
	}

	// Copy a list of integers
	public static List<Integer> copyList(List<Integer> originalList) {
		List<Integer> newList = new ArrayList<Integer>();

		for (Integer i : originalList) {
			newList.add(i);
		}

		return newList;
	}

	// Returns a map from a list of integers
	public static Map<Integer, Integer> valuesToMap(List<Integer> values) {
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();

		for (Integer val : values) {
			// Add the current value from diceValues to diceMap
			map.put(val, map.getOrDefault(val, 0) + 1);
		}

		return map;
	}

}
