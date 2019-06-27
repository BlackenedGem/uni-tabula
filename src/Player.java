import java.util.*;

/**
 * Created by rfkd33 on 20/03/2017.
 */
public abstract class Player implements PlayerInterface {

    public Player() {

    }

    public abstract TurnInterface getTurn(Colour colour, BoardInterface board, List<Integer> diceValues) throws PauseException;

    protected int maxUsable2Dice(List<Integer> diceValues, BoardInterface board, Colour colour){
        BoardInterface boardCopy = board.clone();
        //try 1st dice, then 2nd dice for 2 combination.
        //try 2nd dice, then 1st dice for 2 combination.
        //See if neither dice can be used (no moves possible)
        //if none of the above only one dice can be used
        List<MoveInterface> posMovesDie1 = new ArrayList<>(boardCopy.possibleMoves(colour, diceValues.subList(0,1)));
        List<MoveInterface> posMovesDie2 = new ArrayList<>(boardCopy.possibleMoves(colour, diceValues.subList(1,2)));

        if (!posMovesDie1.isEmpty()){
            //for all possible first dice moves try and do a second dice move after it
            for (MoveInterface move: posMovesDie1){
                //get a fresh board
                //make a move
                //see if you can make a second move with the other dice
                try {
                    boardCopy = board.clone();
                    boardCopy.makeMove(colour, move);
                    if (!boardCopy.possibleMoves(colour, diceValues.subList(1,2)).isEmpty()){
                        return 2;
                    }
                } catch (IllegalMoveException m) {
                    //moves are from possibleMoves - no illegal moves should occur
                    m.printStackTrace();
                }
            }
        }
        if (!posMovesDie2.isEmpty()){
            //for all possible second dice moves try and do a first dice move after it
            for (MoveInterface move: posMovesDie2){
                try {
                    boardCopy = board.clone();
                    boardCopy.makeMove(colour, move);
                    if (!boardCopy.possibleMoves(colour, diceValues.subList(0,1)).isEmpty()){
                        return  2;
                    }
                } catch (IllegalMoveException m) {
                    //moves are from possibleMoves - no illegal moves should occur
                    m.printStackTrace();
                }
            }
        }
        if (posMovesDie1.isEmpty() && posMovesDie2.isEmpty()){
            //if posMoves for dice 1 and 2 is empty you cant use either
            return 0;
        }
        //if you cant use neither of both dice you must only be able to use 1.
        return 1;
    }

    //will find max number of usable dice if all dice are of the same value - useful for 3 dice for computer player
    protected int maxUsableDiceOfSameValue(List<Integer> diceValues, BoardInterface board, Colour colour){
        BoardInterface boardCopy = board.clone();
        for (int x = 0; x<diceValues.size(); x++){
            try{
                //get possible moves and try to perform one of them
                List<MoveInterface> posMoves = new ArrayList<>(boardCopy.possibleMoves(colour, diceValues));
                boardCopy.makeMove(colour, posMoves.get(0));
            } catch (IllegalMoveException e){
                //moves are from possibleMoves - no illegal moves should occur
                e.printStackTrace();
            } catch (IndexOutOfBoundsException e){
                //caused by not having any moves to perform
                return x;
            }
        }
        //max possible is 4
        return diceValues.size();
    }
}