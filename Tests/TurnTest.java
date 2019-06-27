import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by rfkd33 on 11/04/2017.
 */
public class TurnTest {
    private Move m1;
    private Move m2;
    private Move m3;
    private Move m4;
    private Move m5;

    private Turn t;

    public TurnTest() {

    }

    @Before
    public void setUp() {
        m1 = new Move();
        m2 = new Move();
        m3 = new Move();
        m4 = new Move();
        m5 = new Move();
        t = new Turn();
    }

    @After
    public void tearDown() {

    }

    @Test(expected = IllegalTurnException.class)
    public void move_test() throws Exception{
        assertEquals(t.getMoves().size(), 0);

        t.addMove(m1);
        assertEquals(t.getMoves().size(), 1);
        assertEquals(t.getMoves().get(0), m1);

        t.addMove(m2);
        assertEquals(t.getMoves().size(), 2);
        assertEquals(t.getMoves().get(1), m2);

        t.addMove(m3);
        assertEquals(t.getMoves().size(), 3);
        assertEquals(t.getMoves().get(2), m3);

        t.addMove(m4);
        assertEquals(t.getMoves().size(), 4);
        assertEquals(t.getMoves().get(3), m4);

        t.addMove(m5);
    }

}