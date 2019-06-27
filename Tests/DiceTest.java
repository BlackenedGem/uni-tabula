import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by rfkd33 on 11/04/2017.
 */
public class DiceTest {
    private Dice d;
    private Die die;

    public DiceTest(){

    }

    @Before
    public void setUp(){
        d = new Dice();
        die = new Die();
    }

    @After
    public void tearDown(){

    }

    @Test
    public void initially_not_rolled(){
        assertFalse(d.haveRolled());
    }

    @Test
    public void roll_has_rolled(){
        d.roll();
        assertTrue(d.haveRolled());
    }

    @Test(expected = NotRolledYetException.class)
    public void get_values_no_roll() throws Exception{
        d.getValues();
    }

    @Test
    public void set_seed_check() throws Exception{
        die.setSeed(1);
        d.roll();
        assertEquals(d.getDice().size(), 2);
        assertEquals(d.getValues().size(), 2);
        assertNotEquals(d.getValues().get(0),d.getValues().get(1));
        d.roll();
        d.roll();
        d.roll();
        d.roll();
        assertEquals(d.getDice().size(), 2);
        assertEquals(d.getValues().size(), 4);
        //check all 4 values in the returned list are the same
        assertEquals(d.getDice().get(0).getValue(), d.getDice().get(1).getValue());
    }

    @Test(expected = NotRolledYetException.class)
    public void dice_clear() throws Exception{
        assertFalse(d.haveRolled());
        d.roll();
        assertTrue(d.haveRolled());
        d.clear();
        assertFalse(d.haveRolled());
        d.getValues();
    }
}
