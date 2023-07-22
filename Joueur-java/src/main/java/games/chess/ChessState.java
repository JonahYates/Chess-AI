/**
 * @author  Jonah Yates
 * @file    ChessState.java
 * @brief   contains information about a given ChessState.
 */

package games.chess;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.time.Instant;

public class ChessState {

    /*  piece lookup tables */
    HashSet<Character> whitePieces = new HashSet<Character>();
    HashSet<Character> blackPieces = new HashSet<Character>();

    /*  ChessState member variables */
    private char [][] m_board;
    private PieceColor m_currentTurnColor;
    private ArrayList<MovePair> m_moves = new ArrayList<MovePair>();

    public ChessState(final char [][] board, final PieceColor currColor) {
        m_board = board;
        m_currentTurnColor = currColor;
        
        if (!whitePieces.contains('P')) {
            whitePieces.add('P');
            whitePieces.add('R');
            whitePieces.add('N');
            whitePieces.add('B');
            whitePieces.add('Q');
            whitePieces.add('K');

            blackPieces.add('p');
            blackPieces.add('r');
            blackPieces.add('n');
            blackPieces.add('b');
            blackPieces.add('q');
            blackPieces.add('k');
        }
    }


    /*  desc:   determines a new state based on the passed uciMove.
     *   ret:   a board after uciMove is applied.
     */
    public ChessState transitionFunction(final String uciMove) {
        char [][] tempBoard = new char[8][8];
        int [] unpackedMove = unpackMoveAsString(uciMove);

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                tempBoard[row][col] = m_board[row][col];
            }
        }

        if (!AI.castleMoves.contains(uciMove)) {
            if (uciMove.length() == 4) {
                tempBoard[unpackedMove[2]][unpackedMove[3]] = tempBoard[unpackedMove[0]][unpackedMove[1]];
            } else {
                tempBoard[unpackedMove[2]][unpackedMove[3]] = uciMove.charAt(4);
            }
            tempBoard[unpackedMove[0]][unpackedMove[1]] = '.';
        } else {
            switch (uciMove) {
                case "e1c1":
                    tempBoard[7][2] = 'K';
                    tempBoard[7][3] = 'R';
                    break;
                case "e1g1":
                    tempBoard[7][6] = 'K';
                    tempBoard[7][5] = 'R';
                    break;
                case "e8c8":
                    tempBoard[0][2] = 'k';
                    tempBoard[0][3] = 'r';
                    break;
                case "e8g8":
                    tempBoard[0][6] = 'k';
                    tempBoard[0][5] = 'r';
                    break;
            }
        }
        
        return new ChessState(tempBoard, m_currentTurnColor);
    }

    /*  desc:   gets the calling ChessState's list of moves (m_moves).
     *   ret:   the m_moves ArrayList<MovePair>.
     */
    public ArrayList<MovePair> getMoves() {
        return m_moves;
    }
    
    /*  desc:   computes and adds possible moves to m_moves given m_currentTurnColor.
     *  ret:    n/a.
     */
    public void computeMoves(final String castling, final String enPassant) {
        // getting the moves from all pieces on the board
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                if (m_currentTurnColor == PieceColor.White) {
                    switch (m_board[row][col]) {
                        case 'P':   addPawnMoves(row, col);   break;
                        case 'R':   addRookMoves(row, col);   break;
                        case 'N':   addKnightMoves(row, col); break;
                        case 'B':   addBishopMoves(row, col); break;
                        case 'Q':   addQueenMoves(row, col);  break;
                        case 'K':   addKingMoves(row, col);   break;
                    }
                } else {
                    switch (m_board[row][col]) {
                        case 'p':   addPawnMoves(row, col);   break;
                        case 'r':   addRookMoves(row, col);   break;
                        case 'n':   addKnightMoves(row, col); break;
                        case 'b':   addBishopMoves(row, col); break;
                        case 'q':   addQueenMoves(row, col);  break;
                        case 'k':   addKingMoves(row, col);   break;
                    }
                }
            }
        }

        // adding possible castling moves
        addCastlingMoves(castling);

        // adding possible enPassant moves
        addEnPassantMoves(enPassant);

        // parsing the m_moves ArrayList so only valid moves remain
        determineValidMoves();
    }

    /*  desc:   determines which moves won't put you in check and returns them.
     *  ret:    an ArrayList<MovePair> of valid moves.
     */
    private void determineValidMoves() {
        ArrayList<MovePair> validMoves = new ArrayList<MovePair>();

        for (MovePair m : m_moves) {
            ChessState tempState = transitionFunction(m.m_move);
            int kingRow = -1, kingCol = -1;
            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    if (tempState.isKing(row, col)) {
                        kingRow = row;
                        kingCol = col;
                    }
                }
            }
            
            if (!tempState.inCheck(kingRow, kingCol)) {
                validMoves.add(m);
            }
        }

        m_moves = validMoves;
    }

    /*  desc:   chooses and returns a random move.
     *  ret:    a random MovePair.m_move from m_moves.
     */
    public String randomMove() {
        Random rand = new Random();
        return m_moves.get(rand.nextInt(m_moves.size())).m_move;
    }

    /*  desc:   implements TL-ID-DL-MM with a depth limit determined by the game time, and
     *          a state evaluation heuristic function using material and positioning advantages.
     *  args:   the game's turn number, history of moves played, and player's time remaining.
     *  ret:    the best move the algorithm could determine in the given time.
     */
    public String timeLimited_IterativeDeepening_DepthLimited_MiniMax_AlphaBeta(
        final int turnNumber, final List<String> history, final double timeRemaining)
    {
        // shortcutting the decision process for the first couple of moves
        if (turnNumber <= 10) {  // use a set of good opening moves
            if (turnNumber == 1 && m_currentTurnColor == PieceColor.White) {        // if white, move e4 (kings pawn) for agressive attack
                return "e2e4";
            } else if (turnNumber == 1 && m_currentTurnColor == PieceColor.Black) { // if black's move, lookup and respond to white's move
                // if white played e4, counter with Silicans defense
                if ("e2e4".equals(history.get(0))) {
                    return "c7c5";
                }
            } else if (m_currentTurnColor == PieceColor.White) {                    // follow through with the initiative
                /*  checking if the series of previous moves
                    matches any openings in whiteOpenings    */
                for (String [] preset : AI.whiteOpenings) {
                    boolean match = true;
                    for (int i = 0; i < 2*(turnNumber-1) && match && 2*(turnNumber-1) < preset.length; i++) {
                        if (!preset[i].equals(history.get(i))) {
                            match = false;
                        }
                    }
                    if (match && 2*(turnNumber-1) < preset.length) {
                        return preset[2*(turnNumber-1)];
                    }
                }
            } else {                                                                // counter and punish white's offensive
                /*  checking if white's opening 2 moves
                    resembles scholar's mate and countering  */
                if (turnNumber == 2
                    && ("e2e3".equals(history.get(0)) || "e2e4".equals(history.get(0)))
                    && ("f1c4".equals(history.get(2)) || "d1h5".equals(history.get(2)))) {
                    return "g7g6";
                }
        
                /*  checking if any of moves in
                    blackCounters can be played */
                for (String [] preset : AI.blackCounters) {
                    boolean match = true;
                    for (int i = 0; i <= 2*(turnNumber-1) && match && 2*(turnNumber-1)+1 < preset.length; i++) {
                        if (!preset[i].equals(history.get(i))) {
                            match = false;
                        }
                    }
                    if (match && 2*(turnNumber-1)+1 < preset.length) {
                        return preset[2*(turnNumber-1)+1];
                    }
                }

            }
        }

        // start thinking
        Instant t0 = Instant.now();
        Instant timeLimit = t0.plusNanos(Double.valueOf(timeRemaining*.022).longValue());

        MovePair best_move = miniMax(
            1,
            Float.NEGATIVE_INFINITY,
            Float.POSITIVE_INFINITY,
            true,
            this,
            null,
            history);

        for (int depth = 2; true; depth++) {
            best_move = miniMax(
                depth,
                Float.NEGATIVE_INFINITY,
                Float.POSITIVE_INFINITY,
                true,
                this,
                null,
                history);
            Instant t1 = Instant.now();
            if (t1.isAfter(timeLimit)) {
                System.out.println("hit depth: " + depth);
                break;
            }
        }
        
        return best_move.m_move;
    }

    /*  desc:   computes the best MovePair using a miniMax time-limited iterative deepening algorithm.
     *  ret:    returns the best MovePair given the allocated time.
     */
    private MovePair miniMax(
        final int depth,
        float alpha,
        float beta,
        boolean isMaxingPlayer,
        final ChessState prevState,
        final String prevMove,
        final List<String> history) {

        // checking if the search depth is reached or an terminal state hit
        if (depth == 0 || this.cutoffTest(prevMove, isMaxingPlayer)) {
            return new MovePair(prevMove, hEval(prevState, prevMove, history));
        }

        String bestMove = null;
        float bestValue = isMaxingPlayer ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY;

        // sorting by heuristic order
        this.m_moves.sort(MovePair.priorityComparator);

        // looping over possible moves in heuristic order
        for (MovePair priorityMove : this.m_moves) {
            ChessState nextState = this.transitionFunction(priorityMove.m_move);
            nextState.computeMoves("-", "-");

            MovePair computedMove = nextState.miniMax(
                depth-1,
                alpha,
                beta,
                !isMaxingPlayer,
                this,
                priorityMove.m_move,
                history);

            if (isMaxingPlayer && computedMove.m_value > bestValue) {
                bestMove = priorityMove.m_move;
                bestValue = computedMove.m_value;
                alpha = Math.max(alpha, bestValue);
            } else if (!isMaxingPlayer && computedMove.m_value < bestValue) {
                bestMove = priorityMove.m_move;
                bestValue = computedMove.m_value;
                beta = Math.min(beta, bestValue);
            }

            // alpha-beta pruning
            if (beta <= alpha) {
                break;
            }
        }

        return new MovePair(bestMove, bestValue);
    }

    /*  desc:   takes the previous ChessState and uci move and returns utility
     *          associated with making the move and its resulting ChessState.
     *  args:   prevState (ChessState) - the previous state, prevUCIMove (String) - the UCIMove.
     *  ret:    the value of the new state as a float.
     */
    private float hEval(final ChessState prevState, final String prevUCIMove, final List<String> history) {
        float points = 0;

        /*  counting the number of pieces remaining.
            pawn[0], rook[1], knight[2], bishop[3], queen[4], king[5] */
        int [] whiteCount = {0, 0, 0, 0, 0, 0};
        int [] blackCount = {0, 0, 0, 0, 0, 0};

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                switch (m_board[row][col]) {
                    case 'P':   whiteCount[0]++;    break;
                    case 'p':   blackCount[0]++;    break;              
                    case 'R':   whiteCount[1]++;    break;
                    case 'r':   blackCount[1]++;    break;                   
                    case 'N':   whiteCount[2]++;    break;
                    case 'n':   blackCount[2]++;    break;                    
                    case 'B':   whiteCount[3]++;    break;
                    case 'b':   blackCount[3]++;    break;                    
                    case 'Q':   whiteCount[4]++;    break;
                    case 'q':   blackCount[4]++;    break;                    
                    case 'K':   whiteCount[5]++;    break;
                    case 'k':   blackCount[5]++;    break;                    
                }
            }
        }
        int numWhite = 0, numBlack = 0;
        for (int i : whiteCount) {
            numWhite += i;
        }
        for (int i : blackCount) {
            numBlack += i;
        }
        int piecesRemaining = numWhite + numBlack;


        /*  awarding points for capturing an opponent's piece
         *  and subtracting points if the capture is not worth it */
        float capturingPointsToAward = 0;
        int [] unpackedPrevMove = unpackMoveAsString(prevUCIMove);
        if (m_currentTurnColor == PieceColor.White
            && Character.isLowerCase(prevState.m_board[unpackedPrevMove[2]][unpackedPrevMove[3]]))
        {
            capturingPointsToAward += 
                charAsPoints(prevState.m_board[unpackedPrevMove[2]][unpackedPrevMove[3]])
                /charAsPoints(prevState.m_board[unpackedPrevMove[0]][unpackedPrevMove[1]]);
        
            // taking away 1/3 the points if the piece is now vulnerable
            if (this.inCheck(unpackedPrevMove[2], unpackedPrevMove[3])) {
                capturingPointsToAward *= .66;
            }
        } else if (m_currentTurnColor == PieceColor.Black
            && Character.isUpperCase(prevState.m_board[unpackedPrevMove[2]][unpackedPrevMove[3]]))
        {
            capturingPointsToAward += 
                charAsPoints(prevState.m_board[unpackedPrevMove[2]][unpackedPrevMove[3]])
                /charAsPoints(prevState.m_board[unpackedPrevMove[0]][unpackedPrevMove[1]]);
        
            // taking away 1/3 the points if the piece is now vulnerable
            if (this.inCheck(unpackedPrevMove[2], unpackedPrevMove[3])) {
                capturingPointsToAward *= .66;
            }
        }
        points += capturingPointsToAward;


        /*  awarding points if you put the enemy king in check/captured
         *  it by adding points to checkPointsToAward   */
        float checkPointsToAward = 0;
        if (m_currentTurnColor == PieceColor.White && blackCount[5] == 0) {
            checkPointsToAward += 1;
        } else if (m_currentTurnColor == PieceColor.Black && whiteCount[5] == 0) {
            checkPointsToAward += 1;
        }
        
        // awarding points based on how many more pieces you have than the other player
        if (m_currentTurnColor == PieceColor.White) {
            checkPointsToAward += (float)numWhite/numBlack;
        } else {
            checkPointsToAward += (float)numBlack/numWhite;
        }
        points += checkPointsToAward;


        /*  awarding points based on the states
         *  mobility (number of moves available)    */
        float mobilityPointsToAward = getMoves().size() * .025f;

        // subtracting points if the move was made recently
        ArrayList<String> most_recent = new ArrayList<String>();
        int k = 0;
        for (int i = history.size()-1; i >= 0; i--) {
            most_recent.add(history.get(i));
            k++;
            if (k == 6) {
                break;
            }
        }
        for (String s : most_recent) {
            if (s.equals(prevUCIMove)) {
                mobilityPointsToAward -= 1;
            }
        }
        points += mobilityPointsToAward;


        /*  awarding points based on the state's turn's material possessed and
         *  placement. Incrementing materialPoints to award with these points.  */
        float materialPointsToAward = 0;

        // awarding points if promoting
        if (prevUCIMove.length() == 5) {
            materialPointsToAward += charAsPoints(prevUCIMove.charAt(4));
        }
        
        // getting points from material on the board
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                float piecePointsToAward = 0;
                if (Character.isUpperCase(m_board[row][col]) && m_board[row][col] != 'K') {
                    piecePointsToAward = charAsPoints(m_board[row][col]);
                } else if (Character.isLowerCase(m_board[row][col]) && m_board[row][col] != 'k') {
                    piecePointsToAward = charAsPoints(m_board[row][col]);
                }

                // applying multipliers to piecePointsToAward
                if (m_board[row][col] == 'P' && m_currentTurnColor == PieceColor.White) {
                    // reducing pawn weight for doubled pawns
                    if (row-1 >= 0 && m_board[row-1][col] == 'P') {
                        piecePointsToAward -= .25;
                    }

                    // increasing pawn weight if supported by 2 or 1 pawns
                    if (row+1 < 8
                        && ((col-1 >= 0 && m_board[row+1][col-1] == 'P')
                            && (col+1 < 8 && m_board[row+1][col+1] == 'P'))) {
                        piecePointsToAward += .25;
                    } else if (row+1 < 8
                        && ((col-1 >= 0 && m_board[row+1][col-1] == 'P')
                            || (col+1 < 8 && m_board[row+1][col+1] == 'P'))) {
                        piecePointsToAward += .1;
                    }

                    if (piecesRemaining > 14) {
                        piecePointsToAward *= AI.C_PAWN_POSITION_WHITE_EARLY[row][col];
                    } else {
                        piecePointsToAward *= AI.C_PAWN_POSITION_WHITE_LATE[row][col];
                    }
                } else if (m_board[row][col] == 'p' && m_currentTurnColor == PieceColor.Black) {
                    // reducing pawn weight for doubled pawns
                    if (row+1 < 8 && m_board[row+1][col] == 'p') {
                        piecePointsToAward -= .25;
                    }

                    // increasing pawn weight if supported by 2 or 1 pawns
                    if (row-1 >= 0
                        && ((col-1 >= 0 && m_board[row-1][col-1] == 'p')
                            && (col+1 < 8 && m_board[row-1][col+1] == 'p'))) {
                        piecePointsToAward += .25;
                    } else if (row-1 >= 0
                        && ((col-1 >= 0 && m_board[row-1][col-1] == 'p')
                            || (col+1 < 8 && m_board[row-1][col+1] == 'p'))) {
                        piecePointsToAward += .1;
                    }
                    
                    if (piecesRemaining > 14) {
                        piecePointsToAward *= AI.C_PAWN_POSITION_BLACK_EARLY[row][col];
                    }  else {
                        piecePointsToAward *= AI.C_PAWN_POSITION_BLACK_LATE[row][col];
                    }
                } else if ((m_board[row][col] == 'B' && m_currentTurnColor == PieceColor.White)
                    || (m_board[row][col] == 'b' && m_currentTurnColor == PieceColor.Black)) {
                    piecePointsToAward *= AI.C_BISHOP_POSITION[row][col];
                } else if (piecesRemaining <= 14
                    && ((m_board[row][col] == 'R' && m_currentTurnColor == PieceColor.White)
                        || (m_board[row][col] == 'r' && m_currentTurnColor == PieceColor.Black))) {
                    // bonus points for late game rooks
                    piecePointsToAward += .25;
                } else if (piecesRemaining > 14
                    && ((m_board[row][col] == 'B' && m_currentTurnColor == PieceColor.White)
                        || (m_board[row][col] == 'b' && m_currentTurnColor == PieceColor.Black))) {
                    // bonus points for early game bishops
                    piecePointsToAward += .25;
                }
                materialPointsToAward += piecePointsToAward * .5f;
            }
        }
        // awarding points for bishop pairs in early game
        if (piecesRemaining > 14
            && (m_currentTurnColor == PieceColor.White && whiteCount[3] == 2)
                || (m_currentTurnColor == PieceColor.Black && blackCount[3] == 2)) {
            materialPointsToAward += .5;
        }

        // awarding points for rook pairs in late game
        if (piecesRemaining <= 14
            && (m_currentTurnColor == PieceColor.White && whiteCount[1] == 2)
                || (m_currentTurnColor == PieceColor.Black && blackCount[1] == 2)) {
            materialPointsToAward += .5;
        }
        if (m_currentTurnColor == PieceColor.White) {
            materialPointsToAward = 2*materialPointsToAward/(
                whiteCount[0] * AI.C_PAWN_WEIGHT +
                whiteCount[1] * AI.C_ROOK_WEIGHT +
                whiteCount[2] * AI.C_KNIGHT_WEIGHT +
                whiteCount[3] * AI.C_BISHOP_WEIGHT +
                whiteCount[4] * AI.C_QUEEN_WEIGHT);
        } else {
            materialPointsToAward = 2*materialPointsToAward/(
                blackCount[0] * AI.C_PAWN_WEIGHT +
                blackCount[1] * AI.C_ROOK_WEIGHT +
                blackCount[2] * AI.C_KNIGHT_WEIGHT +
                blackCount[3] * AI.C_BISHOP_WEIGHT +
                blackCount[4] * AI.C_QUEEN_WEIGHT);
        }
        points += materialPointsToAward;

        return points;
    }

    /*  desc:   determines whether a state is quiescent or not, that is, whether no
                wild captures, checks, or important moves were made.
        ret:    a boolean informing whether the state is quiet or not.
     * 
     */
    private boolean isQuietState(final String prevMove, final boolean isMaxingPlayer) {
        if (prevMove == null) { 
            return false;
        }
        int [] unpackedMove = unpackMoveAsString(prevMove);
        if (m_board[unpackedMove[2]][unpackedMove[3]] != '.') {
            return false;
        }
        
        char kingChar = (isMaxingPlayer ? 'K' : 'k');
        int kingRow = -1, kingCol = -1;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                if (m_board[row][col] == kingChar) {
                    kingRow = row;
                    kingCol = col;
                    break;
                }
            }
        }
        if (kingRow == -1) {
            return false;
        } else {
            if (this.inCheck(kingRow, kingCol)) {
                return false;
            } else {
                if (this.m_moves.size() == 0) {
                    return true;
                }
            }
        }

        return true;
    }

    /*  desc:   determines whether the cutoff is reached for miniMax.
     *  ret:    true if a terminal state is hit (stalemate or checkmate).
     */
    private boolean cutoffTest(final String prevMove, final boolean isMaxingPlayer) {
        /*  when in stalemate or checkmate, the number of available
            moves (size of m_moves) will be 0.  */
        return (this.m_moves.size() == 0 || this.isQuietState(prevMove, isMaxingPlayer));
    }

    /*  desc:   infroms whether the king is in check for the calling ChessState.
     *  args:   the pre-determined kingRow and kingCol for faster checking.
     *  ret:    a boolean informing whether the king is in check.
     */
    public boolean inCheck(final int kingRow, final int kingCol) {

        // checking if in check from a pawn
        if (m_currentTurnColor == PieceColor.White) {
            if (kingRow-1 >= 0 && kingCol+1 < 8) {
                if (m_board[kingRow-1][kingCol+1] == 'p') {
                    return true;
                }
            }
            if (kingRow-1 >= 0 && kingCol-1 >= 0) {
                if (m_board[kingRow-1][kingCol-1] == 'p') {
                    return true;
                }
            }
        } else {
            if (kingRow+1 < 8 && kingCol+1 < 8) {
                if (m_board[kingRow+1][kingCol+1] == 'P') {
                    return true;
                }
            }
            if (kingRow+1 < 8 && kingCol-1 >= 0) {
                if (m_board[kingRow+1][kingCol-1] == 'P') {
                    return true;
                }
            }
        }

        // checking if in check from a rook or queen
        for (Pair c : AI.rookDir) {
            for (int range = 1; range <= 7; range++) {
                int newKingRow = kingRow + c.m_row*range;
                int newKingCol = kingCol + c.m_col*range;
                if (newKingCol >= 0 && newKingCol < 8 && newKingRow >= 0 && newKingRow < 8) {
                    if (m_currentTurnColor == PieceColor.White) {
                        if (m_board[newKingRow][newKingCol] == 'r' || m_board[newKingRow][newKingCol] == 'q') {
                            return true;
                        } else if (m_board[newKingRow][newKingCol] != '.') {
                            break;
                        }
                    } else {
                        if (m_board[newKingRow][newKingCol] == 'R' || m_board[newKingRow][newKingCol] == 'Q') {
                            return true;
                        } else if (m_board[newKingRow][newKingCol] != '.') {
                            break;
                        }
                    }
                }
            }
        }

        // checking if in check from a bishop or queen
        for (Pair c : AI.bishopDir) {
            for (int range = 1; range <= 7; range++) {
                int newKingRow = kingRow + c.m_row*range;
                int newKingCol = kingCol + c.m_col*range;
                if (newKingCol >= 0 && newKingCol < 8 && newKingRow >= 0 && newKingRow < 8) {
                    if (m_currentTurnColor == PieceColor.White) {
                        if (m_board[newKingRow][newKingCol] == 'b' || m_board[newKingRow][newKingCol] == 'q') {
                            return true;
                        } else if (m_board[newKingRow][newKingCol] != '.') {
                            break;
                        }
                    } else {
                        if (m_board[newKingRow][newKingCol] == 'B' || m_board[newKingRow][newKingCol] == 'Q') {
                            return true;
                        } else if (m_board[newKingRow][newKingCol] != '.') {
                            break;
                        }
                    }
                }
            }
        }

        // checking if in check from a knight
        for (Pair c : AI.knightDir) {
            int newKingRow = kingRow + c.m_row;
            int newKingCol = kingCol + c.m_col;
            if (newKingCol >= 0 && newKingCol < 8 && newKingRow >= 0 && newKingRow < 8) {
                if (m_currentTurnColor == PieceColor.White && m_board[newKingRow][newKingCol] == 'n') {
                    return true;
                } else if (m_currentTurnColor == PieceColor.Black && m_board[newKingRow][newKingCol] == 'N') {
                    return true;
                }
            }
        }

        // checking if in check from a king
        for (Pair c : AI.rookDir) {
            int newKingRow = kingRow + c.m_row;
            int newKingCol = kingCol + c.m_col;
            if (newKingCol >= 0 && newKingCol < 8 && newKingRow >= 0 && newKingRow < 8) {
                if (m_currentTurnColor == PieceColor.White && m_board[newKingRow][newKingCol] == 'k') {
                    return true;
                } else if (m_currentTurnColor == PieceColor.Black && m_board[newKingRow][newKingCol] == 'K') {
                    return true;
                }
            }
        }
        for (Pair c : AI.bishopDir) {
            int newKingRow = kingRow + c.m_row;
            int newKingCol = kingCol + c.m_col;
            if (newKingCol >= 0 && newKingCol < 8 && newKingRow >= 0 && newKingRow < 8) {
                if (m_currentTurnColor == PieceColor.White && m_board[newKingRow][newKingCol] == 'k') {
                    return true;
                } else if (m_currentTurnColor == PieceColor.Black && m_board[newKingRow][newKingCol] == 'K') {
                    return true;
                }
            }
        }

        return false;
    }
    
    /*  desc:   informs whether a tile is a king.
     *  args:   row (int), col (int) - representing a row and column on m_board.
     *  ret:    a boolean informing whether the tile is m_currentTurnColor's king.
     */
    public boolean isKing(final int row, final int col) {
        return m_board[row][col] == (m_currentTurnColor == PieceColor.White ? 'K' : 'k');
    }

    private void addPawnMoves(final int m_row, final int m_col) {
        if (m_currentTurnColor == PieceColor.White) {
            // adding possible forward movements
            if (m_row-1 >= 0
                && m_board[m_row-1][m_col] == '.')
            {
                if (m_row-1 == 0) {
                    // pawn reached other side, adding each kind of promotion
                    for (char c : AI.promotionChars) {
                        m_moves.add(new MovePair(moveAsString(m_row, m_col) + moveAsString(m_row-1, m_col) + c, 0));
                    }
                } else {
                    m_moves.add(new MovePair(moveAsString(m_row, m_col) + moveAsString(m_row-1, m_col), 0));
                }
                // adding the 2 tile move if it is the pawn's first move
                if (m_row == 6
                    && m_board[m_row-2][m_col] == '.') {
                        m_moves.add(new MovePair(moveAsString(m_row, m_col) + moveAsString(m_row-2, m_col), 0));
                }
            }

            // adding possible diagonal left capture movement
            if (m_row-1 >= 0 && m_col-1 >= 0
                && blackPieces.contains(m_board[m_row-1][m_col-1]))
            {
                if (m_row-1 == 0) {
                    // pawn reached other side, adding each kind of promotion
                    for (char c : AI.promotionChars) {
                        m_moves.add(new MovePair(moveAsString(m_row, m_col) + moveAsString(m_row-1, m_col-1) + c, 0));
                    }
                } else {
                    m_moves.add(new MovePair(moveAsString(m_row, m_col) + moveAsString(m_row-1, m_col-1), 0));
                }
            }

            // adding possible diagonal right capture movement
            if (m_row-1 >= 0 && m_col+1 < 8
                && blackPieces.contains(m_board[m_row-1][m_col+1]))
            {
                if (m_row-1 == 0) {
                    // pawn reached other side, adding each kind of promotion
                    for (char c: AI.promotionChars) {
                        m_moves.add(new MovePair(moveAsString(m_row, m_col) + moveAsString(m_row-1, m_col+1) + c, 0));
                    }
                } else {
                    m_moves.add(new MovePair(moveAsString(m_row, m_col) + moveAsString(m_row-1, m_col+1), 0));
                }
            }

        } else {
            // adding possible forward movements
            if (m_row+1 < 8
                && m_board[m_row+1][m_col] == '.')
            {
                if (m_row+1 == 7) {
                    // pawn reached other side, adding each kind of promotion
                    for (char c : AI.promotionChars) {
                        m_moves.add(new MovePair(moveAsString(m_row, m_col) + moveAsString(m_row+1, m_col) + c, 0));
                    }
                } else {
                    m_moves.add(new MovePair(moveAsString(m_row, m_col) + moveAsString(m_row+1, m_col), 0));
                }
                // adding the 2 tile move if it is the pawn's first move
                if (m_row == 1
                    && m_board[m_row+2][m_col] == '.') {
                    m_moves.add(new MovePair(moveAsString(m_row, m_col) + moveAsString(m_row+2, m_col), 0));
                }
            }

            // adding possible diagonal left capture movement
            if (m_row+1 < 8 && m_col-1 >= 0
                && whitePieces.contains(m_board[m_row+1][m_col-1]))
            {
                if (m_row+1 == 7) {
                    // pawn reached other side, adding each kind of promotion
                    for (char c : AI.promotionChars) {
                        m_moves.add(new MovePair(moveAsString(m_row, m_col) + moveAsString(m_row+1, m_col-1) + c, 0));
                    }
                } else {
                    m_moves.add(new MovePair(moveAsString(m_row, m_col) + moveAsString(m_row+1, m_col-1), 0));
                }
            }

            // adding possible diagonal right capture movement
            if (m_row+1 < 8 && m_col+1 < 8
                && whitePieces.contains(m_board[m_row+1][m_col+1]))
            {
                if (m_row+1 == 7) {
                    // pawn reached other side, adding each kind of promotion
                    for (char c : AI.promotionChars) {
                        m_moves.add(new MovePair(moveAsString(m_row, m_col) + moveAsString(m_row+1, m_col+1) + c, 0));
                    }
                } else {
                    m_moves.add(new MovePair(moveAsString(m_row, m_col) + moveAsString(m_row+1, m_col+1), 0));
                }
            }
            
        }
    }

    private void addRookMoves(final int m_row, final int m_col) {
        for (Pair c : AI.rookDir) {
            for (int range = 1; range <= 7; range++) {
                int newRow = m_row + c.m_row*range;
                int newCol = m_col + c.m_col*range;
                if (newCol >= 0 && newCol < 8 && newRow >= 0 && newRow < 8) {
                    if (m_board[newRow][newCol] == '.') {
                        m_moves.add(new MovePair(moveAsString(m_row, m_col) + moveAsString(newRow, newCol), 0));
                    } else if (m_currentTurnColor == PieceColor.White && whitePieces.contains(m_board[newRow][newCol])) {
                        break;
                    } else if (m_currentTurnColor == PieceColor.Black && blackPieces.contains(m_board[newRow][newCol])) {
                        break;
                    } else if (m_currentTurnColor == PieceColor.White && blackPieces.contains(m_board[newRow][newCol])) {
                        m_moves.add(new MovePair(moveAsString(m_row, m_col) + moveAsString(newRow, newCol), 0));
                        break;
                    } else if (m_currentTurnColor == PieceColor.Black && whitePieces.contains(m_board[newRow][newCol])) {
                        m_moves.add(new MovePair(moveAsString(m_row, m_col) + moveAsString(newRow, newCol), 0));
                        break;
                    }
                }
            }
        }
    }

    private void addKnightMoves(final int m_row, final int m_col) {
        for (Pair c : AI.knightDir) {
            int newRow = m_row + c.m_row;
            int newCol = m_col + c.m_col;
            if (newCol >= 0 && newCol < 8 && newRow >= 0 && newRow < 8) {
                if (m_board[newRow][newCol] == '.') {
                    m_moves.add(new MovePair(moveAsString(m_row, m_col) + moveAsString(newRow, newCol), 0));
                } else if (m_currentTurnColor == PieceColor.White && blackPieces.contains(m_board[newRow][newCol])) {
                    m_moves.add(new MovePair(moveAsString(m_row, m_col) + moveAsString(newRow, newCol), 0));
                } else if (m_currentTurnColor == PieceColor.Black && whitePieces.contains(m_board[newRow][newCol])) {
                    m_moves.add(new MovePair(moveAsString(m_row, m_col) + moveAsString(newRow, newCol), 0));
                }
            }
        }
    }

    private void addBishopMoves(final int m_row, final int m_col) {
        for (Pair c : AI.bishopDir) {
            for (int range = 1; range <= 7; range++) {
                int newRow = m_row + c.m_row*range;
                int newCol = m_col + c.m_col*range;
                if (newCol >= 0 && newCol < 8 && newRow >= 0 && newRow < 8) {
                    if (m_board[newRow][newCol] == '.') {
                        m_moves.add(new MovePair(moveAsString(m_row, m_col) + moveAsString(newRow, newCol), 0));
                    } else if (m_currentTurnColor == PieceColor.White && whitePieces.contains(m_board[newRow][newCol])) {
                        break;
                    } else if (m_currentTurnColor == PieceColor.Black && blackPieces.contains(m_board[newRow][newCol])) {
                        break;
                    } else if (m_currentTurnColor == PieceColor.White && blackPieces.contains(m_board[newRow][newCol])) {
                        m_moves.add(new MovePair(moveAsString(m_row, m_col) + moveAsString(newRow, newCol), 0));
                        break;
                    } else if (m_currentTurnColor == PieceColor.Black && whitePieces.contains(m_board[newRow][newCol])) {
                        m_moves.add(new MovePair(moveAsString(m_row, m_col) + moveAsString(newRow, newCol), 0));
                        break;
                    }
                }
            }
        }
    }

    private void addQueenMoves(final int m_row, final int m_col) {
        addRookMoves(m_row, m_col);
        addBishopMoves(m_row, m_col);
    }

    private void addKingMoves(final int m_row, final int m_col) {
        for (Pair c : AI.kingDir) {
            int newRow = m_row + c.m_row;
            int newCol = m_col + c.m_col;
            if (newCol >= 0 && newCol < 8 && newRow >= 0 && newRow < 8) {
                if (m_board[newRow][newCol] == '.') {
                    m_moves.add(new MovePair(moveAsString(m_row, m_col) + moveAsString(newRow, newCol), 0));
                } else if (m_currentTurnColor == PieceColor.White
                    && blackPieces.contains(m_board[newRow][newCol])
                    && m_board[newRow][newCol] != 'k') {
                    m_moves.add(new MovePair(moveAsString(m_row, m_col) + moveAsString(newRow, newCol), 0));
                } else if (m_currentTurnColor == PieceColor.Black
                    && whitePieces.contains(m_board[newRow][newCol])
                    && m_board[newRow][newCol] != 'K') {
                    m_moves.add(new MovePair(moveAsString(m_row, m_col) + moveAsString(newRow, newCol), 0));
                }
            }
        }
    }

    private void addCastlingMoves(final String castling) {
        if (castling.charAt(0) == '-') {
            return;
        } else {
            for (int i = 0; i < castling.length(); i++) {
                if (m_currentTurnColor == PieceColor.White && m_board[7][4] == 'K') {
                    if (castling.charAt(i) == 'K'
                        && m_board[7][5] == '.'
                        && m_board[7][6] == '.'
                        && !this.inCheck(7, 4)
                        && !transitionFunction("e1g1").inCheck(7, 5)
                        && !transitionFunction("e1g1").inCheck(7,6))
                    {
                        m_moves.add(new MovePair("e1g1", AI.C_CASTLING_VALUE));
                    }
                    if (castling.charAt(i) == 'Q'
                        && m_board[7][1] == '.'
                        && m_board[7][2] == '.'
                        && m_board[7][3] == '.'
                        && !this.inCheck(7, 4)
                        && !transitionFunction("e1c1").inCheck(7, 3)
                        && !transitionFunction("e1c1").inCheck(7, 2))
                    {
                        m_moves.add(new MovePair("e1c1", AI.C_CASTLING_VALUE));
                    }
                } else if (m_currentTurnColor == PieceColor.Black && m_board[0][4] == 'k') {
                    if (castling.charAt(i) == 'k'
                        && m_board[0][5] == '.'
                        && m_board[0][6] == '.'
                        && !this.inCheck(0, 4)
                        && !transitionFunction("e8g8").inCheck(0, 5)
                        && !transitionFunction("e8g8").inCheck(0, 6))
                    {
                        m_moves.add(new MovePair("e8g8", AI.C_CASTLING_VALUE));
                    }
                    if (castling.charAt(i) == 'q'
                        && m_board[0][1] == '.'
                        && m_board[0][2] == '.'
                        && m_board[0][3] == '.'
                        && !this.inCheck(0, 4)
                        && !transitionFunction("e8c8").inCheck(0, 3)
                        && !transitionFunction("e8c8").inCheck(0, 2))
                    {
                        m_moves.add(new MovePair("e8c8", AI.C_CASTLING_VALUE));
                    }
                }
            }
        }
    }

    private void addEnPassantMoves(final String enPassant) {
        if (enPassant.charAt(0) == '-') {
            return;
        } else {
            int targetRow = -1, targetCol = -1;
            targetRow = 8 - Character.getNumericValue(enPassant.charAt(1));
            switch(enPassant.charAt(0)) {
                case 'a':   targetCol = 0;    break;
                case 'b':   targetCol = 1;    break;
                case 'c':   targetCol = 2;    break;
                case 'd':   targetCol = 3;    break;
                case 'e':   targetCol = 4;    break;
                case 'f':   targetCol = 5;    break;
                case 'g':   targetCol = 6;    break;
                case 'h':   targetCol = 7;    break;
            }
            
            if (m_currentTurnColor == PieceColor.White) {
                if (targetCol-1 >= 0 && m_board[targetRow][targetCol-1] == 'P') {
                    m_moves.add(new MovePair(moveAsString(targetRow, targetCol-1) + moveAsString(targetRow-1, targetCol), 0));
                }
                if (targetCol+1 < 8 && m_board[targetRow][targetCol+1] == 'P') {
                    m_moves.add(new MovePair(moveAsString(targetRow, targetCol+1) + moveAsString(targetRow-1, targetCol), 0));
                }
            } else {
                if (targetCol-1 >= 0 && m_board[targetRow][targetCol-1] == 'p') {
                    m_moves.add(new MovePair(moveAsString(targetRow, targetCol-1) + moveAsString(targetRow+1, targetCol), 0));
                }
                if (targetCol+1 < 8 && m_board[targetRow][targetCol+1] == 'p') {
                    m_moves.add(new MovePair(moveAsString(targetRow, targetCol+1) + moveAsString(targetRow+1, targetCol), 0));
                }
            }
        }
    }

    /*  desc:   informs whether the passed piece is a white piece.
     *  ret:    boolean whether the piece is or is not.
     */
    private boolean isPieceWhite(char c) {
        return c == 'P'
            || c == 'R'
            || c == 'N'
            || c == 'B'
            || c == 'Q'
            || c == 'K';
    }

    /*  desc:   informs whether the passed piece is a black piece.
     *  ret:    boolean whether the piece is or is not.
     */
    private boolean isPieceBlack(char c) {
        return c == 'p'
            || c == 'r'
            || c == 'n'
            || c == 'b'
            || c == 'q'
            || c == 'k';
    }

    /*  desc:   determines the points associated with capturing a peice.
     *  args:   c (Character) - represents a chess piece.
     *  ret:    a float value associated with capturing c.
     */
    private float charAsPoints(final char c) {
        switch (c) {
            case '.':   return 0;
            case 'P':   return AI.C_PAWN_WEIGHT;
            case 'p':   return AI.C_PAWN_WEIGHT;
            case 'Q':   return AI.C_QUEEN_WEIGHT;
            case 'q':   return AI.C_QUEEN_WEIGHT;
            case 'N':   return AI.C_KNIGHT_WEIGHT;
            case 'n':   return AI.C_KNIGHT_WEIGHT;
            case 'R':   return AI.C_ROOK_WEIGHT;
            case 'r':   return AI.C_ROOK_WEIGHT;
            case 'B':   return AI.C_BISHOP_WEIGHT;
            case 'b':   return AI.C_BISHOP_WEIGHT;
            case 'K':   return AI.C_KING_WEIGHT;
            case 'k':   return AI.C_KING_WEIGHT;
        }
        return 0;
    }

    /*  desc:   converts a row and column int to a UCI notation position.
     *  ret:    a String representing the row, col pair in UCI notation.
     */
    private String moveAsString(final int row, final int col) {
        return "" + Character.toString((char)(col+97)) + (8-row);
    }

    /*  desc:   unpacked a UCI notation move as coords in m_board.
     *  ret:    an array of length 4: [0] - oldRow, [1] - oldCol, [2] - newRow, [3] - newCol .
     */
    private int [] unpackMoveAsString(final String uciMove) {
        return new int [] {
            8 - Character.getNumericValue(uciMove.charAt(1)),  // unpackedMove[1] - old row
            Character.getNumericValue(uciMove.charAt(0)) - 10, // unpackedMove[0] - old col
            8 - Character.getNumericValue(uciMove.charAt(3)),  // unpackedMove[3] - new row
            Character.getNumericValue(uciMove.charAt(2)) - 10  // unpackedMove[2] - new col
        };
    }

    /*  desc:   prints m_board to the terminal.
     *  ret:    n/a.
     */
    public void printBoardAsString(final char [][] chessBoard) {
        for (int row = 0; row < 8; row++) {
            String rowAsString = "";
            for (int col = 0; col < 8; col++) {
                    rowAsString += chessBoard[row][col];
            }
            System.out.println(rowAsString);
        }
    }

}
