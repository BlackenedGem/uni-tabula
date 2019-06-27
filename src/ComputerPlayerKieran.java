import java.util.*;

/**
 * Created by rfkd33 on 29/04/2017.
 */
public class ComputerPlayerKieran extends Player{
    /* Many other weighting options have been tried including; opening, middle, end variants of all existing params,
    * spread, average position, bonus for empty start, linear stack weight (i.e. 5 stack = 5pts). None of these produced significant advantages.
    * Integers seem to provide sufficient granularity to produce a strong player - weight determined through simulation of thousands of games.
    * */
    private static final int KNOCK = 13;
    private static final int LATE_KNOCK = 15;
    private static final int TWO_STACK = 2;
    private static final int MORE_STACK = 3;
    private static final int KNOCK_RISK_END = -2;
    private static final int KNOCK_RISK = -1;
    private static final int KNOCK_FROM_KNOCKED = 1;

    //saves passing around unnecessarily
    private Colour colour;

    public ComputerPlayerKieran(){
        super();
    }

    public TurnInterface getTurn(Colour colour, BoardInterface board, List<Integer> diceValues) throws PauseException {
        //Check parameters are not null and can actually make moves.
        TurnInterface turn = new Turn();
        if (colour == null || board == null || diceValues == null){
            return turn;
        } else {
            this.colour = colour;
        }
        int maxUsableDice = maxUsableDice(diceValues, board, colour);
        if (maxUsableDice == 0) {
            return turn;
        }
        //Object[] = {List<MoveInterface>, <BoardInterface> board after moves, List<Integer> dice values remaining}
        List<Object[]> allTurns = new ArrayList<>();
        //get all 1 move turns
        allTurns.addAll(addToTurn(new ArrayList<>(), board, diceValues, maxUsableDice));
        //extend turns as far as possible until they meet the legal dice requirement
        for (int i = 1; i<maxUsableDice; i++){
            List<Object[]> newTurns = new ArrayList<>();
            //go through all existing turns and try to extend them by 1 move, storing the valid ones
            for (Object[] turnConditions: allTurns){
                newTurns.addAll(addToTurn((List<MoveInterface>)turnConditions[0], (BoardInterface)turnConditions[1], (List<Integer>)turnConditions[2], maxUsableDice));
            }
            //update all turns to hold the extended turns using more dice values
            allTurns.clear();
            allTurns.addAll(newTurns);
        }

        //make the final turn objects and score them.
        List<TurnInterface> turns = new ArrayList<>();
        List<Integer> scores = new ArrayList<>();
        for (Object[] turnConditions: allTurns){
            Turn t = new Turn();
            for (MoveInterface m: (List<MoveInterface>)turnConditions[0]){
                try {
                    t.addMove(m);
                } catch (IllegalTurnException e){
                    //cannot happen
                    e.printStackTrace();
                }
            }
            turns.add(t);
            scores.add(scoreBoard((BoardInterface) turnConditions[1], board));
        }
        //select the turn with the best score and return it
        int maxScoreIndex = Collections.max(scores);
        return turns.get(scores.indexOf(maxScoreIndex));
    }

    private List<Object[]> addToTurn(List<MoveInterface> existingTurn, BoardInterface boardBeforeMove, List<Integer> diceBeforeTurn, int maxUsableDice){
        List<Object[]> newTurns = new ArrayList<>();
        //take the board before the move, attempt all moves,
        //if maxUsableDice only reduce by one move will lead to a valid turn
        //add updated move list, board and remaining dice to a list
        for (MoveInterface move: boardBeforeMove.possibleMoves(colour, diceBeforeTurn)){
            BoardInterface tempBoard = boardBeforeMove.clone();
            try {
                tempBoard.makeMove(colour, move);
            } catch (IllegalMoveException e){
                //cannot happen as move is from possibleMoves of tempBoard
                e.printStackTrace();
            }
            List<Integer> diceAfterMove = cloneDiceRemove(diceBeforeTurn, move.getDiceValue());
            //make sure the move done is leading to a viable move - save the move if it is
            if (maxUsableDice(diceAfterMove, tempBoard, colour) +1 == maxUsableDice-existingTurn.size()){
                Object[] updatedTurn = {cloneMoveAdd(existingTurn, move), tempBoard, diceAfterMove};
                newTurns.add(updatedTurn);
            }
        }
        return newTurns;
    }

    private List<Integer> cloneDiceRemove(List<Integer> diceValues, int toRemove){
        //clones the dice list and removes a desired value
        List<Integer> newDice = new ArrayList<>(diceValues);
        newDice.remove(newDice.indexOf(toRemove));
        return newDice;
    }

    private List<MoveInterface> cloneMoveAdd(List<MoveInterface> moves, MoveInterface newMove){
        //clones the move list and adds a desired move
        List<MoveInterface> newMoves = new ArrayList<>(moves);
        newMoves.add(newMove);
        return newMoves;
    }

    private int scoreBoard(BoardInterface board, BoardInterface originalBoard){
        int score = 0;
        if (board.isWinner(colour)){
            //if can win from a move take this move
            return 10000;
        }

        for (int i=1; i<=BoardInterface.NUMBER_OF_LOCATIONS; i++){
            try {
                //evaluate where the knocks were in the turn
                if (board.getBoardLocation(i).numberOfPieces(colour.otherColour()) != originalBoard.getBoardLocation(i).numberOfPieces(colour.otherColour())){
                    if (i >= (BoardInterface.PIECES_PER_PLAYER - DieInterface.NUMBER_OF_SIDES_ON_DIE)){
                        score += LATE_KNOCK;
                    } else {
                        score += KNOCK;
                    }
                }
                //evaluate strength of each position held by the computer player
                switch (board.getBoardLocation(i).numberOfPieces(colour)){
                    case 0: //not occupied
                        break;
                    case 1: //single piece- evaluate risk of being knocked off
                        int potentialKnockers = 0;
                        int pieceOnKnockedCanKnock = 0;
                        for (int j=i-1; j>=i-DieInterface.NUMBER_OF_SIDES_ON_DIE; j--) {
                            if (j < 0) {
                                break;
                            } else if (j==0 && board.getKnockedLocation().canRemovePiece(colour.otherColour())){
                                //if other colour is on knocked they have to move the piece and if they roll the right dice they will certainly knock - higher risk
                                potentialKnockers += 1;
                                pieceOnKnockedCanKnock += 1;
                            } else if (j==0 && board.getStartLocation().canRemovePiece(colour.otherColour())) {
                                potentialKnockers += 1; //can be knocked from start
                            } else if (j>0 && board.getBoardLocation(j).canRemovePiece(colour.otherColour())){
                                potentialKnockers+=1; //can be knocked from normal location
                            }
                        }
                        if (i >= BoardInterface.NUMBER_OF_LOCATIONS - DieInterface.NUMBER_OF_SIDES_ON_DIE){
                            score += KNOCK_RISK_END*potentialKnockers;
                        } else {
                            score += KNOCK_RISK*potentialKnockers;
                        }
                        score += KNOCK_FROM_KNOCKED*pieceOnKnockedCanKnock;
                        break;
                    case 2: //two pieces - a minimal safe stack
                        score += TWO_STACK;
                        break;
                    default: //three or more stack - pieces are safe but don't provide optimal blocking
                        score += MORE_STACK;
                        break;
                }

            } catch (NoSuchLocationException e){
                //should not happen as getBoardLocation parameter controlled by loop index
                e.printStackTrace();
            }
        }

        return score;
    }

    private int maxUsableDice(List<Integer> diceValues, BoardInterface board, Colour colour){
        switch (diceValues.size()){
            case 4:
            case 3:
                return maxUsableDiceOfSameValue(diceValues, board, colour);
            case 2:
                return maxUsable2Dice(diceValues, board, colour);
            case 1:
                if (!board.possibleMoves(colour, diceValues).isEmpty()){
                    return 1;
                }
                break;
        }
        return 0;
    }
}
