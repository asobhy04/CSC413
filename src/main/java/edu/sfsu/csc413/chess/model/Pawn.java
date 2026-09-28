package edu.sfsu.csc413.chess.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The pawn — the piece that breaks every rule the others follow.
 *
 * <p>It is the only piece that moves in just one direction, the only one whose
 * capture differs from its move, the only one with a special first move, and
 * the only one that turns into something else. It is worth noticing that all of
 * that awkwardness is contained in this one file. No other class in the engine
 * knows that pawns are strange. That containment is the payoff of polymorphism:
 * the irregular case costs one class, not a special case in every method that
 * touches a piece.
 *
 * <p>En passant is not handled here. Like castling, it depends on the previous
 * move rather than on the current board, so it waits for Week 15 when
 * {@code Game} owns the move history.
 */
public class Pawn extends Piece {

    /**
     * What a pawn may become on reaching the far rank.
     */
    private static final PieceType[] PROMOTION_CHOICES = { PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT };

    public Pawn(Color color) {
        super(color, PieceType.PAWN);
    }

    @Override
    public List<Move> pseudoLegalMoves(Board board, Position from) {
        List<Move> moves = new ArrayList<>();
        int dir = color().pawnDirection(); // +1 for white, -1 for black

        // move 1 forward (has to be empty)
        Position ahead = from.offsetOrNull(0, dir);
        if (ahead != null && board.isEmpty(ahead)) {
            if (ahead.rank() == color().promotionRank()) {
                addPromotions(moves, from, ahead, null);
            } else {
                moves.add(Move.quiet(from, ahead, this));
            }

            // move 2 forward on the first move, both squares need to be empty
            // (this is inside the if so it can't jump over a piece)
            if (from.rank() == color().pawnStartRank()) {
                Position twoAhead = from.offsetOrNull(0, dir * 2);
                if (twoAhead != null && board.isEmpty(twoAhead)) {
                    moves.add(Move.quiet(from, twoAhead, this));
                }
            }
        }

        // captures: diagonal left and right, only if an enemy is there
        int[] sides = {-1, 1};
        for (int side : sides) {
            Position diag = from.offsetOrNull(side, dir);
            if (diag == null) {
                continue;
            }
            Piece target = board.pieceAt(diag);
            if (target != null && target.color() != color()) {
                if (diag.rank() == color().promotionRank()) {
                    addPromotions(moves, from, diag, target);
                } else {
                    moves.add(Move.capture(from, diag, this, target));
                }
            }
        }

        return moves;
    }

    // reaching the last rank = 4 moves (queen, rook, bishop, knight)
    private void addPromotions(List<Move> moves, Position from, Position to, Piece captured) {
        for (PieceType type : PROMOTION_CHOICES) {
            moves.add(Move.promotion(from, to, this, captured, type));
        }
    }

    /**
     * A pawn attacks the two squares diagonally ahead of it, whether or not
     * anything stands there.
     *
     * <p>This override exists because the inherited version answers "can this
     * piece move to that square", and for a pawn that is the wrong question.
     * An empty square in front of a pawn is a square the pawn can move to but
     * does <em>not</em> attack — which matters enormously for king safety: a
     * king may not be blocked from a square merely because a pawn could advance
     * onto it, but it certainly may not step onto a square a pawn guards.
     */
    @Override
    public boolean attacks(Board board, Position from, Position target) {
        // just checks the shape, doesn't care if something is there or not
        int fileDiff = Math.abs(target.file() - from.file());
        int rankDiff = target.rank() - from.rank();
        return fileDiff == 1 && rankDiff == color().pawnDirection();
    }
}
