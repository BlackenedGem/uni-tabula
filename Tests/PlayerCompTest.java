import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by rfkd33 on 14/04/2017.
 */
public class PlayerCompTest {
    private PlayerInterface p;
    private BoardInterface b;
    private Colour col0;
    private Colour col1;

    public PlayerCompTest(){
        col0 = Colour.values()[0];
        col1 = Colour.values()[1];
    }
    @Before
    public void setUp(){
        b = new Board();
        p = new PlayerComputerOld();
    }

    @After
    public void tearDown(){

    }

    private List<Integer> setup_dice(int a, Integer b, Integer c, Integer d){
        List<Integer> diceVals = new ArrayList<>();
        diceVals.add(a);
        if (b!=null) {
            diceVals.add(b);
        }
        if (c!=null){
            diceVals.add(c);
        }
        if (d!=null) {
            diceVals.add(d);
        }
        return diceVals;
    }

    private void maxMoveCheck(BoardInterface b, List<Integer> diceVals, int expectedMud) throws Exception{
        Method mud = p.getClass().getDeclaredMethod("maxUsableDice", List.class, BoardInterface.class, Colour.class);
        mud.setAccessible(true);
        assertEquals(mud.invoke(p,diceVals,b,col0), expectedMud);

    }

    private void setup_board(int[] colour0,int[] colour1) throws Exception{
        Field locations = b.getClass().getDeclaredField("board");
        locations.setAccessible(true);
        List<LocationInterface> locs = (List<LocationInterface>) locations.get(b);
        for (int i=0; i<=24; i++){
            Field pieces = locs.get(i).getClass().getDeclaredField("pieces");
            pieces.setAccessible(true);
            Map<Colour,Integer> p = (Map)pieces.get(locs.get(i));
            if (colour0[i] != 0 || i==0){
                p.put(col0,colour0[i]);
            }
            if (colour1[i] != 0 || i==0){
                p.put(col1,colour1[i]);
            }
            pieces.set(locs.get(i), p);
        }
        //knocked and end setting
        Field piecesKnocked = b.getKnockedLocation().getClass().getDeclaredField("pieces");
        piecesKnocked.setAccessible(true);
        Map<Colour,Integer> pk = (Map)piecesKnocked.get(b.getKnockedLocation());
        if (colour0[25] != 0){
            pk.put(col0, colour0[25]);
        }
        if (colour1[25] != 0){
            pk.put(col1, colour1[25]);
        }
        piecesKnocked.set(b.getKnockedLocation(), pk);
        Field piecesEnd = b.getKnockedLocation().getClass().getDeclaredField("pieces");
        piecesEnd.setAccessible(true);
        Map<Colour,Integer> pe = (Map)piecesEnd.get(b.getEndLocation());
        if (colour0[26] != 0){
            pe.put(col0, colour0[26]);
        }
        if (colour1[26] != 0){
            pe.put(col1, colour1[26]);
        }
        piecesEnd.set(b.getEndLocation(), pe);
        locations.set(b,locs);
        //System.out.println(b.toString());
    }

    //This by extension tests the max useable dice methods in the player main class.
    /*Setup boards for testing
    * 1 dice
    * 2 dice
    * 3 dice
    * 4 dice
    * */
    @Test
    public void test1() throws Exception{
        int[] colour1 = {6,0,0,2,0,0,0,2,2,3,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
        int[] colour0 = {5,0,0,0,4,3,3,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
        setup_board(colour0,colour1);
        maxMoveCheck(b,setup_dice(3,5,null,null), 2);
    }

    @Test
    public void test2() throws Exception{
        int[] colour1 = {0,0,1,3,4,0,0,1,4,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
        int[] colour0 = {6,0,0,0,0,5,4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
        setup_board(colour0,colour1);
        maxMoveCheck(b,setup_dice(1,3,null,null), 2);
    }
}
