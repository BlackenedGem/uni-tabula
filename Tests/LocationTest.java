import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by rfkd33 on 11/04/2017.
 */
public class LocationTest {
    private LocationInterface l;

    public LocationTest(){

    }

    @Before
    public void setUp(){
        l = new Location("loc");
    }

    @After
    public void tearDown(){

    }

    @Test
    public void set_name(){
        assertEquals(l.getName(), "loc");
        l.setName("location");
        assertEquals(l.getName(), "location");
    }

    @Test
    public void set_mixed(){
        assertFalse(l.isMixed());
        l.setMixed(true);
        assertTrue(l.isMixed());
        l.setMixed(false);
        assertFalse(l.isMixed());
    }

    @Test(expected = IllegalMoveException.class)
    public void initial_is_empty() throws Exception{
        assertTrue(l.isEmpty());
        l.removePiece(Colour.values()[0]);
    }

    @Test
    public void not_mixed_test() throws Exception{
        assertFalse(l.isMixed());
        //can add either colour to empty
        assertTrue(l.canAddPiece(Colour.values()[0]));
        assertTrue(l.canAddPiece(Colour.values()[1]));
        //add piece of colour0
        assertNull(l.addPieceGetKnocked(Colour.values()[0]));
        assertEquals(l.numberOfPieces(Colour.values()[0]), 1);
        //can add either by knocking
        assertTrue(l.canAddPiece(Colour.values()[0]));
        assertTrue(l.canAddPiece(Colour.values()[1]));
        //knock col0 by adding col1
        assertEquals(l.addPieceGetKnocked(Colour.values()[1]), Colour.values()[0]);
        assertEquals(l.numberOfPieces(Colour.values()[1]), 1);
        assertEquals(l.numberOfPieces(Colour.values()[0]), 0);
        //can only add appropriate colour: 2 of col0 0 of col1
        l.addPieceGetKnocked(Colour.values()[0]);
        l.addPieceGetKnocked(Colour.values()[0]);
        assertTrue(l.canAddPiece(Colour.values()[0]));
        assertFalse(l.canAddPiece(Colour.values()[1]));
        //can only remove appropriate colour
        assertTrue(l.canRemovePiece(Colour.values()[0]));
        assertFalse(l.canRemovePiece(Colour.values()[1]));
        //remove appropriate colour (twice to empty)
        l.removePiece(Colour.values()[0]);
        assertEquals(l.numberOfPieces(Colour.values()[0]), 1);
        l.removePiece(Colour.values()[0]);
        assertEquals(l.numberOfPieces(Colour.values()[0]), 0);
        assertTrue(l.isEmpty());
    }

    @Test
    public void mixed_test() throws Exception{
        assertFalse(l.isMixed());
        l.setMixed(true);
        assertTrue(l.isMixed());
        //can add either colour to empty
        assertTrue(l.canAddPiece(Colour.values()[0]));
        assertTrue(l.canAddPiece(Colour.values()[1]));
        //add piece of both colour
        l.addPieceGetKnocked(Colour.values()[0]);
        assertEquals(l.numberOfPieces(Colour.values()[0]), 1);
        l.addPieceGetKnocked(Colour.values()[1]);
        assertEquals(l.numberOfPieces(Colour.values()[1]), 1);
        //can still add both colours
        assertTrue(l.canAddPiece(Colour.values()[0]));
        assertTrue(l.canAddPiece(Colour.values()[1]));
        //no knocking
        assertNull(l.addPieceGetKnocked(Colour.values()[0]));
        assertEquals(l.numberOfPieces(Colour.values()[0]), 2);
        assertNull(l.addPieceGetKnocked(Colour.values()[1]));
        assertEquals(l.numberOfPieces(Colour.values()[1]), 2);
        //can remove both colours
        assertTrue(l.canRemovePiece(Colour.values()[0]));
        assertTrue(l.canRemovePiece(Colour.values()[1]));
        //remove both colours
        l.removePiece(Colour.values()[0]);
        l.removePiece(Colour.values()[0]);
        assertEquals(l.numberOfPieces(Colour.values()[0]), 0);
        l.removePiece(Colour.values()[1]);
        l.removePiece(Colour.values()[1]);
        assertEquals(l.numberOfPieces(Colour.values()[1]), 0);
        assertTrue(l.isEmpty());

    }

    @Test(expected = IllegalMoveException.class)
    public void add_illegal() throws Exception{
        assertFalse(l.isMixed());
        l.addPieceGetKnocked(Colour.values()[0]);
        l.addPieceGetKnocked(Colour.values()[0]);
        assertEquals(l.numberOfPieces(Colour.values()[0]), 2);
        l.addPieceGetKnocked(Colour.values()[1]);
    }

    //no pieces in loc tested earlier
    /*
    * 1) Remove wrong col from not mixed
    * 2) Remove a col with 0 pieces in mixed
    * */
    @Test(expected = IllegalMoveException.class)
    public void remove_illegal_1() throws Exception{
        assertFalse(l.isMixed());
        l.addPieceGetKnocked(Colour.values()[0]);
        assertEquals(l.numberOfPieces(Colour.values()[0]), 1);
        l.removePiece(Colour.values()[1]);
    }

    @Test(expected = IllegalMoveException.class)
    public void remove_illegal_2() throws Exception{
        l.setMixed(true);
        assertTrue(l.isMixed());
        l.addPieceGetKnocked(Colour.values()[0]);
        assertFalse(l.isEmpty());
        assertEquals(l.numberOfPieces(Colour.values()[0]), 1);
        assertEquals(l.numberOfPieces(Colour.values()[1]), 0);
        l.removePiece(Colour.values()[1]);
    }

    @Test
    public void not_mixed_valid() throws Exception{
        assertFalse(l.isMixed());
        assertTrue(l.isValid());
        for (int i=0; i<Board.PIECES_PER_PLAYER; i++){
            assertEquals(l.numberOfPieces(Colour.values()[0]), i);
            l.addPieceGetKnocked(Colour.values()[0]);
            assertEquals(l.numberOfPieces(Colour.values()[0]), i+1);
            assertTrue(l.isValid());
        }
        l.addPieceGetKnocked(Colour.values()[0]);
        assertEquals(l.numberOfPieces(Colour.values()[0]),Board.PIECES_PER_PLAYER+1);
        assertFalse(l.isValid());
        l.removePiece(Colour.values()[0]);
        assertTrue(l.isValid());
    }

    @Test
    public void mixed_valid() throws Exception{
        l.setMixed(true);
        assertTrue(l.isMixed());
        assertTrue(l.isValid());
        for (int i=0; i<Board.PIECES_PER_PLAYER; i++){
            assertEquals(l.numberOfPieces(Colour.values()[0]), i);
            assertEquals(l.numberOfPieces(Colour.values()[1]), i);
            assertNull(l.addPieceGetKnocked(Colour.values()[0]));
            assertNull(l.addPieceGetKnocked(Colour.values()[1]));
            assertEquals(l.numberOfPieces(Colour.values()[0]), i+1);
            assertEquals(l.numberOfPieces(Colour.values()[1]), i+1);
            assertTrue(l.isValid());
        }
        l.addPieceGetKnocked(Colour.values()[0]);
        assertEquals(l.numberOfPieces(Colour.values()[0]),Board.PIECES_PER_PLAYER+1);
        assertFalse(l.isValid());
        l.removePiece(Colour.values()[0]);
        assertTrue(l.isValid());
        l.addPieceGetKnocked(Colour.values()[1]);
        assertEquals(l.numberOfPieces(Colour.values()[1]),Board.PIECES_PER_PLAYER+1);
        assertFalse(l.isValid());
        l.removePiece(Colour.values()[1]);
        assertTrue(l.isValid());
    }

    @Test
    public void numOfPiecesNull(){
        assertEquals(l.numberOfPieces(null),0);
    }

    @Test
    public void canAddNull(){
        assertFalse(l.canAddPiece(null));
    }

    @Test
    public void canRemoveNull(){
        assertFalse(l.canRemovePiece(null));
    }

    @Test(expected = IllegalMoveException.class)
    public void addPieceGetKnockedNull() throws Exception{
        l.addPieceGetKnocked(null);
    }

    @Test(expected = IllegalMoveException.class)
    public void removePieceNull() throws Exception{
        l.removePiece(null);
    }
}
