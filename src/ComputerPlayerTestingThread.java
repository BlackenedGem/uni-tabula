import java.util.LinkedHashMap;
import java.util.Map;

/*
 * A thread which plays a set amount of games
 * Used in ComputerPlayerTesting to improve performance on multicore processors (more testing = better computer player)
 * Made by wrtl76
 */

public class ComputerPlayerTestingThread implements Runnable {
	private long executionTime;
	private int gamesPlayed;
	private int noTurns;
	private Map<String, Integer> wins;
	
	private boolean hasFinished;

	public ComputerPlayerTestingThread() {
		executionTime = 0;
		gamesPlayed = 0;
		noTurns = 0;
		hasFinished = false;
	}
	
	@Override
	public void run() {
		// Setup variables
		PlayerInterface player1 = new ComputerPlayer();
		PlayerInterface player2 = new ComputerPlayerKieran(); // This would be an instance of the old player to test against
		PlayerInterface[][] playerMaps = { { player1, player2 }, { player2, player1 } };
		
		Game game = null;
		int mapToUse = 0;
		
		wins = new LinkedHashMap<String, Integer>();
		wins.put(null, 0);
		wins.put(player1.getClass().getSimpleName(), 0);
		wins.put(player2.getClass().getSimpleName(), 0);
		
		long startTime = System.currentTimeMillis();
		
		// Perform the games
		for (gamesPlayed = 0; gamesPlayed < ComputerPlayerTesting.GAMES_PER_THREAD; gamesPlayed++) {
			// Declare variables
			Colour winnerColour = null;
			String winner = null;
			PlayerInterface[] playerMap = playerMaps[mapToUse];

			// Play a game
			try {
				game = new Game();
				game.setPlayer(Colour.values()[0], playerMap[0]);
				game.setPlayer(Colour.values()[1], playerMap[1]);
				winnerColour = game.play();
			} catch (Exception e) {
				winnerColour = null;
			}

			// Process the game afterwards
			if (winnerColour != null) {
				winner = playerMap[winnerColour.ordinal()].getClass().getSimpleName();
			}
			wins.put(winner, wins.get(winner) + 1);
			noTurns += game.getTurns();
			
			// Update the map to use
			mapToUse++;
			if (mapToUse >= playerMaps.length) {
				mapToUse = 0;
			}
		}
		
		executionTime = System.currentTimeMillis() - startTime;
		hasFinished = true;
	}
	
	public long getExecutionTime() {
		return executionTime;
	}

	public int getGamesPlayed() {
		return gamesPlayed;
	}
	
	public boolean hasFinished() {
		return hasFinished;
	}

	public int getNoTurns() {
		return noTurns;
	}

	public Map<String, Integer> getWins() {
		return wins;
	}
}
