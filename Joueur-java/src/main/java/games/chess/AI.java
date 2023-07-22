/**
 * This is where you build your AI for the Chess game.
 */
package games.chess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.HashMap;
import java.util.Random;

import javax.swing.text.Position;

import joueur.BaseAI;

// <<-- Creer-Merge: imports -->> - Code you add between this comment and the end comment will be preserved between Creer re-runs.
import java.util.HashSet;
// <<-- /Creer-Merge: imports -->>

/**
 * This is where you build your AI for the Chess game.
 */
public class AI extends BaseAI {
    /**
     * This is the Game object itself, it contains all the information about the current game
     */
    public Game game;

    /**
     * This is your AI's player. This AI class is not a player, but it should command this Player.
     */
    public Player player;

    // <<-- Creer-Merge: fields -->> - Code you add between this comment and the end comment will be preserved between Creer re-runs.
    /*  Constants   */
    final static float C_PAWN_WEIGHT = 1f;
    final static float C_KNIGHT_WEIGHT = 3.05f;
    final static float C_BISHOP_WEIGHT = 3.33f;
    final static float C_ROOK_WEIGHT = 5.63f;
    final static float C_QUEEN_WEIGHT = 9.5f;
    final static float C_KING_WEIGHT = 200f;
    
    final static float C_CASTLING_VALUE = 1.5f;

    /*  pawns are more valuable in the center in early game. When there
        are less than 14 pieces on the board (late-game), pawns on the
        edges become more valuable. */
    final static float [][] C_PAWN_POSITION_WHITE_EARLY = {
        {0.90f, 0.95f, 1.00f, 1.00f, 1.00f, 1.00f, 0.95f, 0.90f},
        {0.90f, 0.95f, 1.00f, 1.00f, 1.00f, 1.00f, 0.95f, 0.90f},
        {0.90f, 0.95f, 1.01f, 1.03f, 1.03f, 1.01f, 0.95f, 0.90f},
        {0.92f, 0.97f, 1.05f, 1.07f, 1.07f, 1.05f, 0.97f, 0.92f},
        {0.97f, 1.02f, 1.09f, 1.11f, 1.11f, 1.09f, 1.02f, 0.97f},
        {1.00f, 1.04f, 1.09f, 1.12f, 1.12f, 1.09f, 1.04f, 1.00f},
        {1.00f, 1.07f, 1.09f, 1.12f, 1.12f, 1.09f, 1.07f, 1.00f},
        {0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f}};

    final static float [][] C_PAWN_POSITION_WHITE_LATE = {
        {1.25f, 1.18f, 1.06f, 1.05f, 1.05f, 1.06f, 1.18f, 1.25f},
        {1.25f, 1.18f, 1.06f, 1.05f, 1.05f, 1.06f, 1.18f, 1.25f},
        {1.19f, 1.09f, 1.02f, 1.00f, 1.00f, 1.02f, 1.09f, 1.19f},
        {1.15f, 1.05f, 1.00f, 1.00f, 1.00f, 1.00f, 1.05f, 1.15f},
        {1.10f, 1.03f, 1.00f, 0.95f, 0.95f, 1.00f, 1.03f, 1.10f},
        {1.10f, 1.02f, 0.97f, 0.90f, 0.90f, 0.97f, 1.02f, 1.10f},
        {1.00f, 1.00f, 0.95f, 0.90f, 0.90f, 0.95f, 1.00f, 1.00f},
        {0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f}};

    final static float [][] C_PAWN_POSITION_BLACK_EARLY = {
        {0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f},
        {1.00f, 1.07f, 1.09f, 1.12f, 1.12f, 1.09f, 1.07f, 1.00f},
        {1.00f, 1.04f, 1.09f, 1.12f, 1.12f, 1.09f, 1.04f, 1.00f},
        {0.97f, 1.02f, 1.09f, 1.11f, 1.11f, 1.09f, 1.02f, 0.97f},
        {0.92f, 0.97f, 1.05f, 1.07f, 1.07f, 1.05f, 0.97f, 0.92f},
        {0.90f, 0.95f, 1.01f, 1.05f, 1.05f, 1.01f, 0.95f, 0.90f},
        {0.90f, 0.95f, 1.00f, 1.00f, 1.00f, 1.00f, 0.95f, 0.90f},
        {0.90f, 0.95f, 1.00f, 1.00f, 1.00f, 1.00f, 0.95f, 0.90f}};

    final static float [][] C_PAWN_POSITION_BLACK_LATE = {
        {0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f},
        {1.00f, 1.00f, 0.95f, 0.90f, 0.90f, 0.95f, 1.00f, 1.00f},
        {1.10f, 1.02f, 0.97f, 0.90f, 0.90f, 0.97f, 1.02f, 1.10f},
        {1.10f, 1.03f, 1.00f, 0.95f, 0.95f, 1.00f, 1.03f, 1.10f},
        {1.15f, 1.05f, 1.00f, 1.00f, 1.00f, 1.00f, 1.05f, 1.15f},
        {1.19f, 1.09f, 1.02f, 1.00f, 1.00f, 1.02f, 1.09f, 1.19f},
        {1.25f, 1.18f, 1.06f, 1.05f, 1.05f, 1.06f, 1.18f, 1.25f},
        {1.25f, 1.18f, 1.06f, 1.05f, 1.05f, 1.06f, 1.18f, 1.25f}};

    final static float [][] C_BISHOP_POSITION = {
        {1.00f, 1.00f, 1.00f, 1.00f, 1.00f, 1.00f, 1.00f, 1.00f},
        {1.00f, 1.20f, 1.00f, 1.00f, 1.00f, 1.00f, 1.20f, 1.00f},
        {1.00f, 1.00f, 1.00f, 1.00f, 1.00f, 1.00f, 1.00f, 1.00f},
        {1.00f, 1.00f, 1.00f, 1.00f, 1.00f, 1.00f, 1.00f, 1.00f},
        {1.00f, 1.00f, 1.00f, 1.00f, 1.00f, 1.00f, 1.00f, 1.00f},
        {1.00f, 1.00f, 1.00f, 1.00f, 1.00f, 1.00f, 1.00f, 1.00f},
        {1.00f, 1.20f, 1.00f, 1.00f, 1.00f, 1.00f, 1.20f, 1.00f},
        {1.00f, 1.00f, 1.00f, 1.00f, 1.00f, 1.00f, 1.00f, 1.00f}};

    final static char [] promotionChars = {'q', 'r', 'b', 'n'};
    final static HashSet<String> castleMoves = new HashSet<String>(Arrays.asList("e1c1", "e1g1", "e8c8", "e8g8"));

    final static String [][] whiteOpenings = {
        {"e2e4", "e7e5", "g1f3", "b8c6", "f1c4", "f8c5", "d2d3"},                                           // italian game
        {"e2e4", "c7c5", "d2d4", "c5d4", "c2c3", "d4c3", "b1c3"},                                           // smith-morra gambit
        {"e2e4", "e7e5", "g1f3", "b8c6", "f1c4", "g8f6", "f3g5", "d7d5", "e4d5", "f6d5", "g5f7", "d1f3"},   // fried liver
    };
    final static String [][] blackCounters = {
        {"e2e4", "c7c5", "g1f3", "d7d6", "d2d4", "c5d4", "f3d4", "g8f6", "b1c3", "g7g6"},                   // sicilian defense (dragon)
        {"d2d4", "d7d5", "c2c4", "e7e6", "b1c3", "g8f6", "c1g5", "b8d7"},                                   // queens gambit declined (elephant trap)
        {"f2f4", "e7e5", "f4e5", "d7d6", "e5d6", "f8d6", "d2d4", "d8h4", "g2g3", "d6g3", "e1d2", "h4d4"}    // from's gambit
    };

    final static Pair [] rookDir = {
        new Pair(1,0),
        new Pair(0,1),
        new Pair(-1,0),
        new Pair(0,-1)
    };

    final static Pair [] bishopDir = {
        new Pair(1,1),
        new Pair(1,-1),
        new Pair(-1,1),
        new Pair(-1,-1)
    };

    final static Pair [] knightDir = {
        new Pair(1,2),
        new Pair(1,-2),
        new Pair(-1,2),
        new Pair(-1,-2),
        new Pair(2,1),
        new Pair(2,-1),
        new Pair(-2,1),
        new Pair(-2,-1)
    };

    final static Pair [] kingDir = {
        new Pair(1,1),
        new Pair(1,-1),
        new Pair(-1,1),
        new Pair(-1,-1),
        new Pair(1,0),
        new Pair(0,1),
        new Pair(-1,0),
        new Pair(0,-1)
    };
    // <<-- /Creer-Merge: fields -->>


    /**
     * This returns your AI's name to the game server. Just replace the string.
     * @return string of you AI's name
     */
    public String getName() {
        // <<-- Creer-Merge: get-name -->> - Code you add between this comment and the end comment will be preserved between Creer re-runs.
        return "Jonah Yates";
        // <<-- /Creer-Merge: get-name -->>
    }

    /**
     * This is automatically called when the game first starts, once the Game object and all GameObjects have been initialized, but before any players do anything.
     * This is a good place to initialize any variables you add to your AI, or start tracking game objects.
     */
    public void start() {
        // <<-- Creer-Merge: start -->> - Code you add between this comment and the end comment will be preserved between Creer re-runs.
        super.start();
        // <<-- /Creer-Merge: start -->>
    }

    /**
     * This is automatically called every time the game (or anything in it) updates.
     * If a function you call triggers an update this will be called before that function returns.
     */
    public void gameUpdated() {
        // <<-- Creer-Merge: game-updated -->> - Code you add between this comment and the end comment will be preserved between Creer re-runs.
        super.gameUpdated();
        // <<-- /Creer-Merge: game-updated -->>
    }

    /**
     * This is automatically called when the game ends.
     * You can do any cleanup of you AI here, or do custom logging. After this function returns the application will close.
     * @param  won  true if your player won, false otherwise
     * @param  reason  a string explaining why you won or lost
     */
    public void ended(boolean won, String reason) {
        // <<-- Creer-Merge: ended -->> - Code you add between this comment and the end comment will be preserved between Creer re-runs.
        super.ended(won, reason);
        // <<-- /Creer-Merge: ended -->>
    }


    /**
     * This is called every time it is this AI.player's turn to make a move.
     *
     * @return A string in Universal Chess Interface (UCI) or Standard Algebraic Notation (SAN) formatting for the move you want to make. If the move is invalid or not properly formatted you will lose the game.
     */
    public String makeMove() {
        // <<-- Creer-Merge: makeMove -->> - Code you add between this comment and the end comment will be preserved between Creer re-runs.
        char [][] chessBoard = new char [8][8];
        
        // splitting the FEN string by spaces to parse its information later
        String splitFen [] = game.fen.split(" ");

        String boardImport = splitFen[0];
        PieceColor turnColor = splitFen[1].charAt(0) == 'w' ? PieceColor.White : PieceColor.Black;
        String castling = splitFen[2];
        String enPassant = splitFen[3];
        int turnNumber = Integer.parseInt(splitFen[5]);

        if (game.history.size() < 10) {
            for (String s : game.history) {
                System.out.print(s + " ");
            }
            System.out.println();
        }

        // splitting and parsing the part of the FEN string that contains board information (splitFen[0])
        int boardLen = boardImport.length(), row = 0, col = 0;
        for (int boardPos = 0; boardPos < boardLen; boardPos++) {
            char charAtBoardPos = boardImport.charAt(boardPos);
            if (charAtBoardPos == '/') {
                row++;
                col = 0;
            } else if (Character.isDigit(charAtBoardPos)) {
                col += Character.getNumericValue(charAtBoardPos);
            } else {
                chessBoard[row][col] = charAtBoardPos;
                col++;
            }
        }
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (isEmpty(chessBoard[i][j])) {
                    chessBoard[i][j] = '.';
                }
            }
        }        

        // creating the current ChessState given the parsed board, computing
        // possible moves for 'color', and then determining which are still valid
        ChessState currentState = new ChessState(chessBoard, turnColor);
        currentState.computeMoves(castling, enPassant);

        // getting the list moves
        ArrayList<MovePair> validMoves = currentState.getMoves();

        // outputting the number of moves, the list of moves, and which move was taken
        validMoves.sort(MovePair.alphabeticalComparator);
        System.out.println(validMoves.size());
        for (int i = 0; i < validMoves.size(); i++) {
            System.out.print(validMoves.get(i).m_move);
            if (i != validMoves.size()-1) {
                System.out.print(" ");
            }
        }

        // selecting a move and outputting then returning it
        String chosenMove = currentState.timeLimited_IterativeDeepening_DepthLimited_MiniMax_AlphaBeta(
            turnNumber, game.history, player.timeRemaining
        );

        System.out.println("\nMy move = " + chosenMove);

        return chosenMove;
        // <<-- /Creer-Merge: makeMove -->>
    }


    // <<-- Creer-Merge: methods -->> - Code you add between this comment and the end comment will be preserved between Creer re-runs.
    
    public boolean isEmpty(char c) {
        return c != 'P' && c != 'p'
            && c != 'R' && c != 'r'
            && c != 'N' && c != 'n'
            && c != 'B' && c != 'b'
            && c != 'Q' && c != 'q'
            && c != 'K' && c != 'k';
    }

    // <<-- /Creer-Merge: methods -->>
}
