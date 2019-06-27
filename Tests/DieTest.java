import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
/**
 * Created by rfkd33 on 11/04/2017.
 */
public class DieTest {
    private Die d;

    public DieTest(){

    }

    @Before
    public void setUp(){
        d = new Die();
    }

    @After
    public void tearDown(){

    }

    @Test
    public void initially_not_rolled(){
        assertFalse(d.hasRolled());
    }

    @Test
    public void roll_has_rolled(){
        d.roll();
        assertTrue(d.hasRolled());
    }

    @Test(expected = NotRolledYetException.class)
    public void get_value_no_roll() throws Exception{
        d.getValue();
    }

    @Test
    public void set_seed_check() throws Exception{
        d.setSeed(1);
        d.roll();
        assertEquals(d.getValue(), 4);
        d.roll();
        assertEquals(d.getValue(), 5);
        d.roll();
        assertEquals(d.getValue(), 2);
    }

    @Test(expected = NotRolledYetException.class)
    public void set_value_0() throws Exception{
        d.setValue(0);
        assertFalse(d.hasRolled());
        d.getValue();
    }

    @Test
    public void set_value_die_sides() throws Exception{
        for (int i=1; i<=Die.NUMBER_OF_SIDES_ON_DIE; i++){
            assertFalse(d.hasRolled());
            d.setValue(i);
            assertEquals(d.getValue(), i);
            assertTrue(d.hasRolled());
            d.clear();
        }
    }

    @Test(expected = NotRolledYetException.class)
    public void set_value_die_sides_plus_1() throws Exception{
        d.setValue(Die.NUMBER_OF_SIDES_ON_DIE+1);
        assertFalse(d.hasRolled());
        d.getValue();
    }

    @Test(expected = NotRolledYetException.class)
    public void die_clear() throws Exception{
        d.setSeed(1);
        assertEquals(d.getValue(), 4);
        assertTrue(d.hasRolled());
        d.clear();
        assertFalse(d.hasRolled());
        d.getValue();
    }
}
