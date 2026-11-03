package edu.sfsu.csc413.chess.engine;

import edu.sfsu.csc413.chess.model.Board;
import edu.sfsu.csc413.chess.model.Color;
import edu.sfsu.csc413.chess.model.Move;
import edu.sfsu.csc413.chess.model.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates the moves a side may play in a given position.
 *
 * <p>This class exists because of a question no single piece can answer. A
 * piece knows how it moves; it does not know the rule that applies to every
 * piece at once: <strong>you may not leave your own king attacked.</strong>
 * That rule needs the whole board, and it arrives in M5. This week the class
 * is where the generation loop lives, moved out of {@code Game}, so that when
 * the rule arrives it has a home.
 *
 * <p>Notice what this class is not. It holds no state: no board, no side to
 * move, no history. Everything it needs arrives as a parameter, so its methods
 * are {@code static} and it cannot be instantiated. Ask it the same question
 * about the same board twice and you get the same answer.
 *
 * <p>Nothing outside the {@code engine} package needs this class. The view and
 * {@code Main} talk to {@link Game}, which decides what "legal" means. That is
 * why one of the two methods below has no access modifier.
 */
public final class MoveGenerator {

    private MoveGenerator() {
    }

    /**
     * Every move {@code color} may legally play in the given position.
     *
     * <p>M5: this is {@link #pseudoLegalMoves} with the king safety filter.
     * each candidate gets played on the board, we check if our own king is
     * attacked, then undo it. so the board is back the way it was when this
     * returns. {@code Game.legalMoves()} calls this and didn't have to change.
     */
    public static List<Move> legalMoves(Board board, Color color) {
        List<Move> legal = new ArrayList<>();
        for (Move move : pseudoLegalMoves(board, color)) {
            board.apply(move);
            boolean leavesKingInCheck = isInCheck(board, color);
            board.undo(move); // undo before trying the next one
            if (!leavesKingInCheck) {
                legal.add(move);
            }
        }
        return legal;
    }

    /**
     * Every geometrically possible move for {@code color}, king safety aside.
     *
     * <p>This is the loop from session 6 §3 and M3's {@code Game.legalMoves()},
     * moved here unchanged. Note how little it knows: it never asks what kind
     * of piece it is looking at. It asks each piece for its own moves.
     *
     * <p>Package-private on purpose. Only {@code Game} and this class's tests,
     * both in {@code engine}, may call it. A view that wants the moves asks
     * {@code Game}.
     */

    /**
     * Used claude to help debug
     * moves.addAll(board.pieceAt(from).pseudoLegalMoves(board, from));
     */
    static List<Move> pseudoLegalMoves(Board board, Color color) {
        List<Move> moves = new ArrayList<>();
        for (Position from : board.positionsOf(color)) {
            moves.addAll(board.pieceAt(from).pseudoLegalMoves(board, from));
        }
        return moves;
    }

    /**
     * true if color's king is being attacked by the other color.
     * if color has no king on the board (happens in the tests) it's not in check.
     */
    public static boolean isInCheck(Board board, Color color) {
        Position king = board.kingPosition(color);
        if (king == null) {
            return false;
        }
        return isAttacked(board, king, color.opposite());
    }

    /**
     * true if any piece of the attacker color attacks the target square.
     * "attacks" not "can move there", these are different for pawns
     * (a pawn moves forward but only attacks diagonally).
     */
    public static boolean isAttacked(Board board, Position target, Color attacker) {
        // ask each piece, don't check what kind it is (Pawn overrides attacks)
        for (Position from : board.positionsOf(attacker)) {
            if (board.pieceAt(from).attacks(board, from, target)) {
                return true;
            }
        }
        return false;
    }
}
