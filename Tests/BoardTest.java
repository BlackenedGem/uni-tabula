import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by rfkd33 on 12/04/2017.
 */
public class BoardTest {
    private BoardInterface b;
    private Colour col0;
    private Colour col1;

    public BoardTest() {
        col0 = Colour.values()[0];
        col1 = Colour.values()[1];
    }

    @Before
    public void setUp() {
        b = new Board();
    }

    @After
    public void tearDown() {

    }

    private void fixPieces(int col0_pieces, int col1_pieces, LocationInterface loc) throws Exception{
        Field pieces = loc.getClass().getDeclaredField("pieces");

        pieces.setAccessible(true);
        Map<Colour,Integer> newPieces = new HashMap<>();
        if (col0_pieces==0 && col1_pieces==0){
            pieces.set(loc,newPieces);
            assertTrue(loc.isEmpty());
        } else {
            if (col0_pieces != 0) {
                newPieces.put(col0, col0_pieces);
            }
            if (col1_pieces != 0) {
                newPieces.put(col1,col1_pieces);
            }
            pieces.set(loc, newPieces);
        }

        switch (loc.getName()){
            case "Start":
                assertEquals(b.getStartLocation().numberOfPieces(col0), col0_pieces);
                assertEquals(b.getStartLocation().numberOfPieces(col1), col1_pieces);
                break;
            case "Knocked":
                assertEquals(b.getKnockedLocation().numberOfPieces(col0), col0_pieces);
                assertEquals(b.getKnockedLocation().numberOfPieces(col1), col1_pieces);
                break;
            case "End":
                assertEquals(b.getEndLocation().numberOfPieces(col0), col0_pieces);
                assertEquals(b.getEndLocation().numberOfPieces(col1), col1_pieces);
                break;
            default:
                assertEquals(b.getBoardLocation(Integer.parseInt(loc.getName())).numberOfPieces(col0), col0_pieces);
                assertEquals(b.getBoardLocation(Integer.parseInt(loc.getName())).numberOfPieces(col1), col1_pieces);
                break;

        }

    }
    @Test
    public void setNameTest() throws Exception{
        Field name = b.getClass().getDeclaredField("name");
        name.setAccessible(true);
        assertNull(name.get(b));
        b.setName("TestName");
        assertEquals(name.get(b), "TestName");
    }

    @Test
    public void constructorTest() throws Exception{
        assertTrue(b.getStartLocation().isMixed());
        assertEquals(b.getStartLocation().numberOfPieces(col0), 15);
        assertEquals(b.getStartLocation().numberOfPieces(col1), 15);
        assertEquals(b.getStartLocation().getName(), "Start");

        assertTrue(b.getKnockedLocation().isMixed());
        assertTrue(b.getKnockedLocation().isEmpty());
        assertEquals(b.getKnockedLocation().getName(), "Knocked");

        assertTrue(b.getEndLocation().isMixed());
        assertTrue(b.getEndLocation().isEmpty());
        assertEquals(b.getEndLocation().getName(), "End");

        for (int i=1; i<=Board.NUMBER_OF_LOCATIONS; i++){
            assertFalse(b.getBoardLocation(i).isMixed());
            assertTrue(b.getBoardLocation(i).isEmpty());
            assertEquals(b.getBoardLocation(i).getName(), Integer.toString(i));
        }
    }

    @Test(expected = NoSuchLocationException.class)
    public void get_board_loc_0() throws Exception{
        b.getBoardLocation(0);
    }

    @Test(expected = NoSuchLocationException.class)
    public void get_board_loc_end() throws Exception{
        b.getBoardLocation(Board.NUMBER_OF_LOCATIONS+1);
    }

    /*
    * 1)Move to end
    * 2)Move past end
    * 3)Move from knocked while piece on start (a) move illegal (b) clear knocked
    * 4)Move whilst piece on knocked (no start) (a) move illegal (b) clear knocked
    * 5)Move and knock (a) num lock (b) to end
    * 6)Move from where not at
    * 7)Move to loc controlled by other col (numbered)
    * 8)Move to end when it appears controlled by other colour
    * */

    @Test
    public void move1() throws Exception{
        for (int i=1; i<=Die.NUMBER_OF_SIDES_ON_DIE; i++){
            b.getBoardLocation(Board.NUMBER_OF_LOCATIONS-(i-1)).addPieceGetKnocked(col0);
            b.getStartLocation().removePiece(col0);
            MoveInterface m = new Move();
            m.setSourceLocation(Board.NUMBER_OF_LOCATIONS-(i-1));
            m.setDiceValue(i);
            assertTrue(b.canMakeMove(col0, m));
            assertFalse(b.canMakeMove(col1, m));
            //make the move and ensure source is emptied and end is filled
            b.makeMove(col0, m);
            assertEquals(b.getEndLocation().numberOfPieces(col0), i);
            assertTrue(b.getBoardLocation(Board.NUMBER_OF_LOCATIONS-(i-1)).isEmpty());
        }

    }

    @Test
    public void move2() throws Exception{
        //before end to 1 after end
        b.getStartLocation().removePiece(col0);
        b.getBoardLocation(Board.NUMBER_OF_LOCATIONS).addPieceGetKnocked(col0);
        MoveInterface m = new Move();
        m.setSourceLocation(Board.NUMBER_OF_LOCATIONS);
        m.setDiceValue(2);
        assertTrue(b.canMakeMove(col0, m));
        assertFalse(b.canMakeMove(col1, m));
        //make the move and ensure source is emptied and end is filled
        b.makeMove(col0, m);
        assertEquals(b.getEndLocation().numberOfPieces(col0), 1);
        assertTrue(b.getBoardLocation(Board.NUMBER_OF_LOCATIONS).isEmpty());
    }

    private MoveInterface move3_setup() throws Exception{
        //fix start to free up to col0 pieces
        fixPieces(13,15,b.getStartLocation());
        assertEquals(b.getStartLocation().numberOfPieces(col0),13);
        assertEquals(b.getStartLocation().numberOfPieces(col1), 15);
        //add piece to knocked and 2
        b.getKnockedLocation().addPieceGetKnocked(col0);
        b.getBoardLocation(2).addPieceGetKnocked(col0);
        assertEquals( b.getKnockedLocation().numberOfPieces(col0),1);
        assertEquals( b.getBoardLocation(2).numberOfPieces( col0), 1);
        //setup move from loc 1
        MoveInterface m = new Move();
        m.setSourceLocation(2);
        m.setDiceValue(1);
        //cant make move as piece on knocked or not on loc
        assertFalse(b.canMakeMove(col0, m));
        assertFalse(b.canMakeMove(col1, m));
        return m;
    }

    @Test(expected = IllegalMoveException.class)
    public void move3a() throws Exception{
        //try to make the move while on knocked
        b.makeMove(col0, move3_setup());
    }

    @Test
    public void move3b() throws Exception{
        MoveInterface m = move3_setup();
        //try to move from knocked
        m.setSourceLocation(0);
        assertTrue(b.canMakeMove(col0, m));
        b.makeMove(col0, m);
        assertTrue(b.getKnockedLocation().isEmpty());
        assertEquals(b.getBoardLocation(1).numberOfPieces(col0),1);
        assertTrue(b.isValid());
    }


    private MoveInterface move4_setup() throws Exception{
        //clear start
        fixPieces(0,0,b.getStartLocation());
        assertTrue(b.getStartLocation().isEmpty());
        //put spare counters in end
        fixPieces(13, 15, b.getEndLocation());
        //add piece to knocked and 1
        b.getKnockedLocation().addPieceGetKnocked(col0);
        b.getBoardLocation(2).addPieceGetKnocked(col0);
        //setup move from 2 by 1 (3 is empty) with piece on knocked
        MoveInterface m = new Move();
        m.setSourceLocation(2);
        m.setDiceValue(1);
        assertFalse(b.canMakeMove(col0, m));
        assertFalse(b.canMakeMove(col1, m));
        return m;
    }

    @Test(expected = IllegalMoveException.class)
    public void move4a() throws Exception{
        b.makeMove(col0, move4_setup());
    }

    @Test
    public void move4b() throws Exception{
        MoveInterface m = move4_setup();
        //try to move from locked
        m.setSourceLocation(0);
        assertTrue(b.canMakeMove(col0, m));
        b.makeMove(col0, m);
        assertTrue(b.getKnockedLocation().isEmpty());
        assertEquals(b.getBoardLocation(1).numberOfPieces(col0),1);
        assertTrue(b.isValid());
    }

    @Test
    public void move5a() throws Exception{
        fixPieces(15, 14, b.getStartLocation());
        b.getBoardLocation(1).addPieceGetKnocked(col1);
        MoveInterface m = new Move();
        m.setSourceLocation(0);
        m.setDiceValue(1);
        assertTrue(b.canMakeMove(col0, m));
        b.makeMove(col0,m);
        assertEquals(b.getBoardLocation(1).numberOfPieces(col0), 1);
        assertEquals(b.getBoardLocation(1).numberOfPieces(col1), 0);
        assertEquals(b.getKnockedLocation().numberOfPieces(col0), 0);
        assertEquals(b.getKnockedLocation().numberOfPieces(col1), 1);
    }

    @Test
    public void move5b() throws Exception{
        fixPieces(14, 14, b.getStartLocation());
        fixPieces(1,0, b.getBoardLocation(24));
        fixPieces(0,1,b.getEndLocation());
        assertEquals(b.getBoardLocation(BoardInterface.NUMBER_OF_LOCATIONS).numberOfPieces(col0), 1);
        assertEquals(b.getEndLocation().numberOfPieces(col1), 1);
        assertEquals(b.getBoardLocation(BoardInterface.NUMBER_OF_LOCATIONS).numberOfPieces(col1), 0);
        assertEquals(b.getEndLocation().numberOfPieces(col0), 0);
        MoveInterface m = new Move();
        m.setSourceLocation(Board.NUMBER_OF_LOCATIONS);
        m.setDiceValue(1);

        assertTrue(b.canMakeMove(col0, m));
        b.makeMove(col0,m);
        assertEquals(b.getBoardLocation(BoardInterface.NUMBER_OF_LOCATIONS).numberOfPieces(col0), 0);
        assertEquals(b.getBoardLocation(BoardInterface.NUMBER_OF_LOCATIONS).numberOfPieces(col1), 0);
        assertEquals(b.getEndLocation().numberOfPieces(col0), 1);
        assertEquals(b.getEndLocation().numberOfPieces(col1), 1);
        assertTrue(b.getKnockedLocation().isEmpty());
    }

    @Test(expected = IllegalMoveException.class)
    public void move6() throws Exception{
        MoveInterface m = new Move();
        m.setSourceLocation(1);
        m.setDiceValue(1);
        assertFalse(b.canMakeMove(col0, m));
        b.makeMove(col0,m);
    }

    @Test(expected = IllegalMoveException.class)
    public void move7() throws Exception{
        fixPieces(15,13, b.getStartLocation());
        fixPieces(0, 2, b.getBoardLocation(1));
        MoveInterface m = new Move();
        m.setSourceLocation(0);
        m.setDiceValue(1);
        assertFalse(b.canMakeMove(col0, m));
        assertTrue(b.canMakeMove(col1, m));
        b.makeMove(col1, m);
        assertEquals(b.getBoardLocation(1).numberOfPieces(col1),3);
        b.makeMove(col0,m);

    }

    @Test
    public void move8() throws Exception{
        fixPieces(14,13, b.getStartLocation());
        fixPieces(0,2,b.getEndLocation());
        b.getBoardLocation(Board.NUMBER_OF_LOCATIONS).addPieceGetKnocked(col0);
        MoveInterface m = new Move();
        m.setSourceLocation(Board.NUMBER_OF_LOCATIONS);
        m.setDiceValue(1);
        assertTrue(b.canMakeMove(col0, m));
        b.makeMove(col0, m);
        assertEquals(b.getEndLocation().numberOfPieces(col0),1);
        assertEquals(b.getEndLocation().numberOfPieces(col1),2);
        assertTrue(b.getBoardLocation(BoardInterface.NUMBER_OF_LOCATIONS).isEmpty());
    }

    /*TAKE TURN TEST
    * 1) not max useable dice
    * 2) Different dice to whats in list (b) repeat dice in list
    * 3) Legal moves wrong order
    * 4) Compound moves (a) 2 dice (b) 4 dice
    * 5) Illegal moves (use reflections for better testing)
    * */

    private List<Integer> setup_dice(int a, int b, Integer c, Integer d){
        List<Integer> diceVals = new ArrayList<>();
        diceVals.add(a);
        diceVals.add(b);
        if (c!=null){
            diceVals.add(c);
        }
        if (d!=null) {
            diceVals.add(d);
        }
        return diceVals;
    }

    private MoveInterface newMove(int source, int dice) throws Exception{
        MoveInterface m =new Move();
        m.setSourceLocation(source);
        m.setDiceValue(dice);
        return m;
    }

    @Test(expected = IllegalTurnException.class)
    public void turn1() throws Exception{
        TurnInterface t = new Turn();
        t.addMove(newMove(0,1));
        b.takeTurn(col0, t,setup_dice(1,2,null,null));
    }

    @Test(expected = IllegalTurnException.class)
    public void turn2a() throws Exception{
        TurnInterface t = new Turn();
        //source valid dice illegal
        t.addMove(newMove(0,4));
        t.addMove(newMove(0,3));
        b.takeTurn(col0,t,setup_dice(1,2,null,null));
    }

    @Test(expected = IllegalTurnException.class)
    public void turn2b() throws Exception{
        TurnInterface t = new Turn();
        t.addMove(newMove(0,1));
        t.addMove(newMove(0,1));
        b.takeTurn(col0,t,setup_dice(1,2,null,null));
    }

    @Test(expected = IllegalTurnException.class)
    public void turn3() throws Exception{
        TurnInterface t = new Turn();
        t.addMove(newMove(1,2));
        t.addMove(newMove(0,1));
        b.takeTurn(col0,t,setup_dice(1,2,null,null));
    }

    @Test
    public void turn4a() throws Exception{
        TurnInterface t = new Turn();
        t.addMove(newMove(0,1));
        t.addMove(newMove(1,2));
        b.takeTurn(col0,t,setup_dice(1,2,null,null));
        assertTrue(b.getBoardLocation(1).isEmpty());
        assertEquals(b.getBoardLocation(3).numberOfPieces(col0),1);
    }

    @Test
    public void turn4b() throws Exception{
        TurnInterface t = new Turn();
        t.addMove(newMove(0,4));
        t.addMove(newMove(4,4));
        t.addMove(newMove(8,4));
        t.addMove(newMove(12,4));
        b.takeTurn(col0,t,setup_dice(4,4,4,4));
        assertTrue(b.getBoardLocation(4).isEmpty());
        assertTrue(b.getBoardLocation(8).isEmpty());
        assertTrue(b.getBoardLocation(12).isEmpty());
        assertEquals(b.getBoardLocation(16).numberOfPieces(col0),1);
    }

    @Test(expected = IllegalTurnException.class)
    public void turn5() throws Exception{
        TurnInterface t = new Turn();
        //dice valid source illegal
        t.addMove(newMove(2,1));
        t.addMove(newMove(1,2));
        b.takeTurn(col0,t,setup_dice(1,2,null,null));
    }

    @Test
    public void winner() throws Exception{
        //no winners initially
        assertFalse(b.isWinner(col0));
        assertFalse(b.isWinner(col1));
        assertNull(b.winner());

        //check col0 win
        fixPieces(15,0, b.getEndLocation());
        assertTrue(b.isWinner(col0));
        assertFalse(b.isWinner(col1));;
        assertEquals(b.winner(),col0);

        //check col1 can win
        fixPieces(0,15, b.getEndLocation());
        assertTrue(b.isWinner(col1));
        assertFalse(b.isWinner(col0));
        assertEquals(b.winner(),col1);
    }

    /*isValid tests
    * 1) initially valid
    * 2) insufficient pieces (a) col0 (b) col1 (c) both
    * 3) to many pieces (a) col0 (b) col1 (c) both
    * 4) right number of pieces total, uneven per player
    * */

    @Test
    public void valid() throws Exception{
        assertTrue(b.isValid());
        fixPieces(14,15,b.getStartLocation());
        assertFalse(b.isValid());
        fixPieces(15,14,b.getStartLocation());
        assertFalse(b.isValid());
        fixPieces(14,14,b.getStartLocation());
        assertFalse(b.isValid());
        fixPieces(16,15,b.getStartLocation());
        assertFalse(b.isValid());
        fixPieces(15,16,b.getStartLocation());
        assertFalse(b.isValid());
        fixPieces(16,16,b.getStartLocation());
        assertFalse(b.isValid());
        fixPieces(10,20, b.getStartLocation());

    }

    //depends on can make move- if that returns only valid this will return only valid
    @Test
    public void possibleMoves_no_knocked_no_start() throws Exception{
        fixPieces(0,15,b.getStartLocation());
        fixPieces(15,0,b.getBoardLocation(1));
        for (MoveInterface m: b.possibleMoves(col0, setup_dice(1,2,3,4))){
            assertEquals(m.getSourceLocation(), 1);
            assertNotEquals(setup_dice(1,2,3,4).indexOf(m.getDiceValue()), -1);
        }
    }

    @Test
    public void possibleMoves_knocked_no_start() throws Exception{
        fixPieces(0, 15, b.getStartLocation());
        fixPieces(1,0,b.getKnockedLocation());
        fixPieces(14, 0, b.getStartLocation());
        for (MoveInterface m: b.possibleMoves(col0, setup_dice(1,2,3,4))){
            assertEquals(m.getSourceLocation(), 0);
            assertNotEquals(setup_dice(1,2,3,4).indexOf(m.getDiceValue()), -1);
        }
    }

    @Test
    public void possibleMoves_start(){
        for (MoveInterface m: b.possibleMoves(col0, setup_dice(1,2,3,4))){
            assertEquals(m.getSourceLocation(),0);
            assertNotEquals(setup_dice(1,2,3,4).indexOf(m.getDiceValue()), -1);
        }
    }

    @Test
    public void clone_test_original() throws Exception{
        //set name of initial board
        b.setName("Name");
        //clone
        BoardInterface b2 = b.clone();
        //check name is in clone and is the same as original
        Field name = b2.getClass().getDeclaredField("name");
        name.setAccessible(true);
        assertEquals(name.get(b2), "Name");

        //update name of copy and make sure its only applied to copy
        b2.setName("Name2");
        assertNotEquals(name.get(b), name.get(b2));
        assertEquals(name.get(b), "Name");
        assertEquals(name.get(b2), "Name2");

        //check location objects are not the same
        for (int i=1;i<=BoardInterface.NUMBER_OF_LOCATIONS; i++){
            //make sure number locations are different
            assertNotEquals(b.getBoardLocation(i),b2.getBoardLocation(i));
        }
        assertNotEquals(b.getStartLocation(), b2.getStartLocation());
        assertNotEquals(b.getKnockedLocation(), b2.getKnockedLocation());
        assertNotEquals(b.getEndLocation(), b2.getEndLocation());
    }

    @Test
    public void clone_updates() throws Exception{
        //add piece to each location
        fixPieces(1,15, b.getStartLocation());
        fixPieces(1,0, b.getKnockedLocation());
        fixPieces(1,0, b.getEndLocation());
        for (int i=1;i<=Board.NUMBER_OF_LOCATIONS;i++){
            fixPieces(1,0,b.getBoardLocation(i));
        }
        //clone
        BoardInterface b2 = b.clone();
        //check pieces have copied across.
        assertEquals(b2.getStartLocation().numberOfPieces(col0),1);
        assertEquals(b2.getKnockedLocation().numberOfPieces(col0),1);
        assertEquals(b2.getEndLocation().numberOfPieces(col0),1);
        assertNotEquals(b.getStartLocation(), b2.getStartLocation());
        assertNotEquals(b.getKnockedLocation(), b2.getKnockedLocation());
        assertNotEquals(b.getEndLocation(), b2.getEndLocation());
        for (int i=1; i<=BoardInterface.NUMBER_OF_LOCATIONS; i++){
            assertNotEquals(b.getBoardLocation(i),b2.getBoardLocation(i));
            assertEquals(b2.getBoardLocation(i).numberOfPieces(col0),1);
        }
    }

    /*Max useable dice
    * 0 die in 4
    * 1 die in 4
    * 2 die in 4
    * 3 die in 4
    * 4 die in 4
    * more than 4 in 4
    * 0 die in 2
    * 1 die in 2
    * 2 die in 2
    * */
    @Test
    public void max_usable_4dice() throws Exception{
        Method m_u_d = Board.class.getDeclaredMethod("maxUsableDice", List.class, Colour.class);
        m_u_d.setAccessible(true);
        //4 dice tests
        List<Integer> d4 = setup_dice(1,1,1,1);

        fixPieces(14,13,b.getEndLocation());
        fixPieces(0,0,b.getStartLocation());
        //stuck on 1
        fixPieces(1,0,b.getBoardLocation(1));
        fixPieces(0,2,b.getBoardLocation(2));
        //1 to 2
        fixPieces(0,0,b.getBoardLocation(2));
        fixPieces(0,2,b.getBoardLocation(3));
        assertEquals(m_u_d.invoke(b, d4, col0),1);
        //1 to 2 to 3
        fixPieces(0,0,b.getBoardLocation(3));
        fixPieces(0,2,b.getBoardLocation(4));
        assertEquals(m_u_d.invoke(b, d4, col0),2);
        //1 to 2 to 3 to 4
        fixPieces(0,0,b.getBoardLocation(4));
        fixPieces(0,2,b.getBoardLocation(5));
        assertEquals(m_u_d.invoke(b, d4, col0),3);
        //1 to 2 to 3 to 4 to 5
        fixPieces(0,0,b.getBoardLocation(5));
        fixPieces(0,2,b.getBoardLocation(6));
        assertEquals(m_u_d.invoke(b, d4, col0),4);
        //free
        fixPieces(0,0,b.getBoardLocation(6));
        fixPieces(14,15,b.getEndLocation());
        assertEquals(m_u_d.invoke(b, d4, col0),4);

    }

    @Test
    public void max_usable_2dice() throws Exception{
        Method m_u_d = Board.class.getDeclaredMethod("maxUsableDice", List.class, Colour.class);
        m_u_d.setAccessible(true);
        //2 dice tests
        List<Integer> d2 = setup_dice(1,2,null,null);

        fixPieces(13,11, b.getEndLocation());
        fixPieces(0,0, b.getStartLocation());
        //stuck on 1
        fixPieces(2,0,b.getBoardLocation(1));
        fixPieces(0,2,b.getBoardLocation(2));
        fixPieces(0,2,b.getBoardLocation(3));
        assertEquals(m_u_d.invoke(b, d2, col0), 0);
        //can move using 1
        fixPieces(0,0,b.getBoardLocation(2));
        fixPieces(0,2,b.getBoardLocation(3));
        fixPieces(0,2,b.getBoardLocation(4));
        assertEquals(m_u_d.invoke(b, d2, col0), 1);
        //can move using 2
        fixPieces(0,2,b.getBoardLocation(2));
        fixPieces(0,0,b.getBoardLocation(3));
        fixPieces(0,2,b.getBoardLocation(4));
        assertEquals(m_u_d.invoke(b, d2, col0), 1);
        //can move using 1 and 2 off 1
        fixPieces(0,0,b.getBoardLocation(2));
        fixPieces(0,0,b.getBoardLocation(3));
        fixPieces(0,4,b.getBoardLocation(4));
        assertEquals(m_u_d.invoke(b, d2, col0), 2);

        //compound moves
        fixPieces(14,11, b.getEndLocation());
        fixPieces(1,0,b.getBoardLocation(1));
        //can move using 1 then 2
        fixPieces(0,0,b.getBoardLocation(2));
        fixPieces(0,4,b.getBoardLocation(3));
        fixPieces(0,0,b.getBoardLocation(4));
        assertEquals(m_u_d.invoke(b, d2, col0), 2);
        //can move using 2 then 1
        fixPieces(0,4,b.getBoardLocation(2));
        fixPieces(0,0,b.getBoardLocation(3));
        fixPieces(0,0,b.getBoardLocation(4));
        assertEquals(m_u_d.invoke(b, d2, col0), 2);
    }

    @Test
    public void new1() throws Exception{
        Method m_u_d = Board.class.getDeclaredMethod("maxUsableDice", List.class, Colour.class);
        m_u_d.setAccessible(true);

        fixPieces(0,6,b.getStartLocation());
        fixPieces(0,0,b.getBoardLocation(1));
        fixPieces(1,0, b.getBoardLocation(2));
        fixPieces(3,0,b.getBoardLocation(3));
        fixPieces(4,0,b.getBoardLocation(4));
        fixPieces(0,5,b.getBoardLocation(5));
        fixPieces(0,4,b.getBoardLocation(6));
        fixPieces(1,0,b.getBoardLocation(7));
        fixPieces(4,0,b.getBoardLocation(8));
        fixPieces(2,0,b.getBoardLocation(9));
        assertEquals(m_u_d.invoke(b,setup_dice(1,3,null,null), col1), 2);
    }

    @Test
    public void winnerNull(){
        assertFalse(b.isWinner(null));
    }

    @Test
    public void nullColourPossibleMoves(){
        assertEquals(b.possibleMoves(null,setup_dice(1,2,null,null)).size(), 0);
    }

    @Test
    public void nullDicePossibleMoves(){
        assertEquals(b.possibleMoves(col0,null).size(), 0);
    }

    @Test
    public void setNullName(){
        b.setName(null);
    }

    @Test
    public void possibleMovesNullParams(){
        assertEquals(b.possibleMoves(null,null).size(), 0);
        assertEquals(b.possibleMoves(null, setup_dice(1,2,null,null)).size(), 0);
        assertEquals(b.possibleMoves(col0,null).size(),0);
    }

    @Test
    public void possibleMovesIllegalDice(){
        assertEquals(b.possibleMoves(col0,setup_dice(7,-4,null, null)).size(), 0);
    }
}

