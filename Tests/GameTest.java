import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by rfkd33 on 12/04/2017.
 */
public class GameTest {
    private Colour col0;
    private Colour col1;
    private GameInterface g;
    public GameTest() {
        col0 = Colour.values()[0];
        col1 = Colour.values()[1];
    }

    @Before
    public void setUp() {
        g = new GameCLI();
    }

    @After
    public void tearDown() {

    }

    @Test
    public void set_player_test() throws Exception{
        //check name is in clone and is the same as original
        Field players = g.getClass().getSuperclass().getDeclaredField("players");
        players.setAccessible(true);
        assertEquals(players.get(g), new HashMap<Colour,PlayerInterface>());
        g.setPlayer(col0,new PlayerGUI());
        g.setPlayer(col1,new PlayerCLI());
        Map<Colour,PlayerInterface> p = (Map)players.get(g);
        assertEquals(p.get(col0).getClass(), PlayerGUI.class);
        assertEquals(p.get(col1).getClass(), PlayerCLI.class);
        g.setPlayer(col1, new PlayerComputerOld());
        p = (Map)players.get(g);
        assertEquals(p.get(col1).getClass(), PlayerComputerOld.class);
    }

    @Test
    public void current_player() throws Exception{
        Field currentTurn = g.getClass().getSuperclass().getDeclaredField("currentPlayer");
        currentTurn.setAccessible(true);
        assertEquals(currentTurn.get(g), col0);
        currentTurn.set(g, col1);
        assertEquals(currentTurn.get(g), col1);
    }

    @Test
    public void save_test() throws Exception{
        //setup game as if player has started
        g.setPlayer(col0,new PlayerGUI());
        g.setPlayer(col1,new PlayerCLI());
        Field dice = g.getClass().getSuperclass().getDeclaredField("dice");
        dice.setAccessible(true);
        Dice d = (Dice)dice.get(g);
        d.roll();
        dice.set(g, d);
        g.saveGame("testLoads/trailSave");
        File saveFile = new File("gameSaves/trialSave.json");
        saveFile.delete();
    }
    /*Load Test
    * 1) PIECES rule change
    * 2) LOCATIONS rule change
    * 3) SIDES_ON_DIE rule change
    * 4) a,b,c) Rules missing
    * 5) current player missing b) illegal ordinal
    * 6) illegal dice value a) to high b) 0
    * 7) not 2 dice
    * 8) not two players
    * 9) incompatible types b) illegal ordinal
    * 10) right human player is created -use original file
    * 11) tampered location names a) dif int b) dif string
    * 12) missing col from loc
    * 13) miss loc from board
    * 14) load invalid board
    * 15) file not found
    * */

    @Test(expected = IOException.class)
    public void load1() throws Exception{
        g.loadGame("testLoads/testLoad1.json");
    }
    @Test(expected = IOException.class)
    public void load2() throws Exception{
        g.loadGame("testLoads/testLoad2.json");
    }
    @Test(expected = IOException.class)
    public void load3() throws Exception{
        g.loadGame("testLoads/testLoad3.json");
    }
    @Test(expected = IOException.class)
    public void load4a() throws Exception{
        g.loadGame("testLoads/testLoad4a.json");
    }
    @Test(expected = IOException.class)
    public void load4b() throws Exception{
        g.loadGame("testLoads/testLoad4b.json");
    }
    @Test(expected = IOException.class)
    public void load4c() throws Exception{
        g.loadGame("testLoads/testLoad4c.json");
    }
    @Test(expected = IOException.class)
    public void load5a() throws Exception{
        g.loadGame("testLoads/testLoad5a.json");
    }
    @Test(expected = IOException.class)
    public void load5b() throws Exception{
        g.loadGame("testLoads/testLoad5b.json");
    }
    @Test(expected = IOException.class)
    public void load6a() throws Exception{
        g.loadGame("testLoads/testLoad6a.json");
    }
    @Test(expected = IOException.class)
    public void load6b() throws Exception{
        g.loadGame("testLoads/testLoad6b.json");
    }
    @Test(expected = IOException.class)
    public void load7() throws Exception{
        g.loadGame("testLoads/testLoad7.json");
    }
    @Test(expected = IOException.class)
    public void load8() throws Exception{
        g.loadGame("testLoads/testLoad8.json");
    }
    @Test(expected = IOException.class)
    public void load9a() throws Exception{
        g.loadGame("testLoads/testLoad9a.json");
    }
    @Test(expected = IOException.class)
    public void load9b() throws Exception{
        g.loadGame("testLoads/testLoad9b.json");
    }
    @Test
    public void load10() throws Exception{
        GameGUI g1 = new GameGUI();
        g1.loadGame("/Users/kieran/IdeaProjects/tabula/gameSaves/testLoads/testLoad.json");
        Field players = g1.getClass().getSuperclass().getDeclaredField("players");
        players.setAccessible(true);
        Map<Colour,PlayerInterface> g1p = (Map)players.get(g1);
        assertEquals(g1p.get(col0).getClass(), PlayerGUI.class);
        assertEquals(g1p.get(col1).getClass(), PlayerComputerOld.class);

        GameCLI g2 = new GameCLI();
        g2.loadGame("testLoads/testLoad.json");
        Map<Colour,PlayerInterface> g2p = (Map)players.get(g2);
        assertEquals(g2p.get(col0).getClass(), PlayerCLI.class);
        assertEquals(g2p.get(col1).getClass(), PlayerComputerOld.class);

    }
    @Test(expected = IOException.class)
    public void load11a() throws Exception{
        g.loadGame("testLoads/testLoad11a.json");
    }
    @Test(expected = IOException.class)
    public void load11b() throws Exception{
        g.loadGame("testLoads/testLoad11b.json");
    }
    @Test(expected = IOException.class)
    public void load12() throws Exception{
        g.loadGame("testLoads/testLoad12.json");
    }
    @Test(expected = IOException.class)
    public void load13() throws Exception{
        g.loadGame("testLoads/testLoad13.json");
    }
    @Test(expected = IOException.class)
    public void load14() throws Exception{
        g.loadGame("testLoads/testLoad14.json");
    }
    @Test(expected = IOException.class)
    public void load15() throws Exception{
        g.loadGame("testLoads/fictionFile.json");
    }
}
