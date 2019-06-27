import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * Class used to test the performance of the ComputerPlayer
 * Made by wrtl76
 */

public class ComputerPlayerTesting {
	// All numbers should divide into each other
	private final static int NUM_GAMES = 10000; // How many games to run (should be even)
	private final static int GAMES_PER_STATUS = 500; // How games should be played before outputting a status
	private final static int NUM_THREADS = 4; // Number of threads to create (timing becomes very inaccurate if this is larger than the number of physical cores, although using more logical cores can speed up execution)
	public final static int GAMES_PER_THREAD = NUM_GAMES / NUM_THREADS;
	private final static int CPU_IDLE_TIME = 50; // Milliseconds to idle per loop

	public ComputerPlayerTesting() {
	}

	public static void main(String[] args) {
		// Make sure that I've set the constants up properly (ignore IDE warning)
		if (GAMES_PER_THREAD % 2 != 0 || NUM_GAMES % NUM_THREADS != 0) {
			System.out.println("Configuration is incorrect");
			return;
		}

		// Setup of initial variables and status update
		long startTime = System.currentTimeMillis();
		System.out.println("Performing computations:");

		// Create the threads and start them
		List<ComputerPlayerTestingThread> threads = new ArrayList<ComputerPlayerTestingThread>();
		for (int i = 0; i < NUM_THREADS; i++) {
			ComputerPlayerTestingThread thread = new ComputerPlayerTestingThread();
			threads.add(thread);

			Thread th = new Thread(thread);
			th.start();
		}

		// Main loop while processing occurs
		int nextUpdate = GAMES_PER_STATUS;
		while (true) {
			// Idle to save CPU time
			try {
				Thread.sleep(CPU_IDLE_TIME);
			} catch (InterruptedException e) {
				System.out.println("Sleep interrupted");
			}

			// Go through each thread
			int curGames = 0;
			boolean stopWaiting = true;

			for (ComputerPlayerTestingThread t : threads) {
				if (!t.hasFinished()) {
					stopWaiting = false;
				}

				curGames += t.getGamesPlayed();
			}
			
			if (curGames >= nextUpdate) {
				double percent = (double) (nextUpdate * 100) / (double) NUM_GAMES;
				
				String status; 
				if (percent % 1 == 0) {
					status = Integer.toString((int) percent);
				} else {
					status = roundDouble(percent, 1);
				}
				System.out.println("Progress: " + status + "%");
				nextUpdate += GAMES_PER_STATUS;
			}

			if (stopWaiting) {
				break;
			}
		}

		// Initialise variables for stats from finished game
		Map<String, Integer> wins = new LinkedHashMap<String, Integer>();
		long effectiveTime = 0;
		int gamesPlayed = 0;
		int noTurns = 0;

		// Get stats from finished games
		for (ComputerPlayerTestingThread t : threads) {
			effectiveTime += t.getExecutionTime();
			gamesPlayed += t.getGamesPlayed();
			noTurns += t.getNoTurns();

			Map<String, Integer> threadWins = t.getWins();
			for (String s : threadWins.keySet()) {
				wins.put(s, wins.getOrDefault(s, 0) + threadWins.get(s));
			}
		}

		// Extract player1 from the linkedhashmap
		String player1 = null;
		for (String key : threads.get(0).getWins().keySet()) {
			if (key != null) {
				player1 = key;
				break;
			}
		}

		// Calculate performance stats
		long millisTaken = System.currentTimeMillis() - startTime;
		double runningTime = ((double) millisTaken) / 1000;

		double effectiveRunningTime = (double) effectiveTime / 1000;
		int turnsPerSecond = (int) (noTurns / effectiveRunningTime);

		// Statistical analysis - Standard deviation from a binomial distribution where p = 0.5
		// Presumes that no errors have occurred
		double sigma = wins.get(player1) - (NUM_GAMES / 2);
		sigma /= Math.sqrt((double) NUM_GAMES / 4);

		// Output information
		System.out.println("Computation finished");
		System.out.println("Games played: 		" + gamesPlayed);
		System.out.println("Running Time: 		" + roundDouble(runningTime, 0) + "s");
		System.out.println("Eff. running time: 	" + roundDouble(effectiveRunningTime, 0) + "s");
		System.out.println("Turns made: 		" + noTurns);
		System.out.println("Eff. turns per second:	" + turnsPerSecond);
		System.out.println("\nResults: ");

		for (String key : wins.keySet()) {
			String s = key;
			if (key == null) {
				s = "Errors";
			}
			System.out.println(s + " - " + wins.get(key));
		}

		System.out.println("\nStandard deviations: " + roundDouble(sigma, 2));
	}

	private static String roundDouble(double number, int decimalPlaces) {
		// http://stackoverflow.com/a/154354
		BigDecimal bd = new BigDecimal(String.valueOf(number)).setScale(decimalPlaces, BigDecimal.ROUND_HALF_UP);
		return bd.toPlainString();
	}
}
