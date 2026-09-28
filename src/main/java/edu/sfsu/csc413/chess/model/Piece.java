package edu.sfsu.csc413.chess.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A chess piece = a color + a type. Immutable.
 * Now abstract for M2, each subclass (Knight, Rook, etc) says how it moves.
 */
public abstract class Piece {

    private final Color color;
    private final PieceType type;

    // color goes first, M2 subclasses rely on this order
    // protected now since you can't do new Piece anymore
    protected Piece(Color color, PieceType type) {
        this.color = color;
        this.type = type;
    }

    public Color color() {
        return color;
    }

    public PieceType type() {
        return type;
    }

    /** FEN letter: uppercase for white, lowercase for black */
    public char symbol() {
        if (color == Color.WHITE) {
            return type.symbol();
        }
        return Character.toLowerCase(type.symbol());
    }

    /** every move this piece can make from "from" (not checking for check yet, that's M5) */
    public abstract List<Move> pseudoLegalMoves(Board board, Position from);

    /** by default a piece attacks a square if it can move there. Pawn overrides this */
    public boolean attacks(Board board, Position from, Position target) {
        List<Move> moves = pseudoLegalMoves(board, from);
        for (Move m : moves) {
            if (m.to().equals(target)) {
                return true;
            }
        }
        return false;
    }

    /**
     * for rook, bishop and queen. goes in each direction until it hits
     * the edge or a piece. enemy piece = capture, own piece = stop before it
     */
    protected List<Move> slidingMoves(Board board, Position from, int[][] directions) {
        List<Move> moves = new ArrayList<>();

        for (int[] d : directions) {
            Position next = from.offsetOrNull(d[0], d[1]);

            while (next != null) {
                Piece other = board.pieceAt(next);
                if (other == null) {
                    moves.add(Move.quiet(from, next, this));
                } else {
                    if (other.color() != this.color) {
                        moves.add(Move.capture(from, next, this, other));
                    }
                    break; // stop either way, can't go through pieces
                }
                next = next.offsetOrNull(d[0], d[1]);
            }
        }
        return moves;
    }

    /** for knight and king. just tries each offset one time */
    protected List<Move> steppingMoves(Board board, Position from, int[][] offsets) {
        List<Move> moves = new ArrayList<>();

        for (int[] o : offsets) {
            Position next = from.offsetOrNull(o[0], o[1]);
            if (next == null) {
                continue; // off the board
            }
            Piece other = board.pieceAt(next);
            if (other == null) {
                moves.add(Move.quiet(from, next, this));
            } else if (other.color() != this.color) {
                moves.add(Move.capture(from, next, this, other));
            }
        }
        return moves;
    }

    @Override
    public String toString() {
        return String.valueOf(symbol());
    }
}
