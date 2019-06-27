import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by rfkd33 on 11/04/2017.
 */
public class MoveTest {
    private MoveInterface m;

    public MoveTest(){

    }

    @Before
    public void setUp(){
        m = new Move();
    }

    @After
    public void tearDown(){

    }

    @Test
    public void init_test(){
        assertEquals(m.getSourceLocation(), 0);
        assertEquals(m.getDiceValue(), 0);
    }

    @Test
    public void set_source_board_locs() throws Exception{
        for (int i=0; i<=Board.NUMBER_OF_LOCATIONS; i++){
            m.setSourceLocation(i);
            assertEquals(m.getSourceLocation(), i);
        }
    }

    @Test(expected = NoSuchLocationException.class)
    public void set_source_after_board() throws Exception{
        m.setSourceLocation(Board.NUMBER_OF_LOCATIONS+1);
    }

    @Test(expected = NoSuchLocationException.class)
    public void set_source_before_board() throws Exception{
        m.setSourceLocation(-1);
    }

    @Test
    public void set_die_on_die_val() throws Exception{
        for (int i=1; i<=Die.NUMBER_OF_SIDES_ON_DIE; i++){
            m.setDiceValue(i);
            assertEquals(m.getDiceValue(), i);
        }
    }

    @Test(expected = IllegalMoveException.class)
    public void set_die_0() throws Exception{
        m.setDiceValue(0);
    }

    @Test(expected = IllegalMoveException.class)
    public void set_die_over_die() throws Exception{
        m.setDiceValue(Die.NUMBER_OF_SIDES_ON_DIE+1);
    }
}
