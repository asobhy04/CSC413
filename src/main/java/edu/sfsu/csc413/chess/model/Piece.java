package edu.sfsu.csc413.chess.model;

/**
 * A chess piece = a color + a type. Immutable.
 * For M1 it only knows what it is, not how it moves (that's M2).
 */
public class Piece {

    private final Color color;
    private final PieceType type;

    // color goes first, M2 subclasses rely on this order
    public Piece(Color color, PieceType type) {
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

    @Override
    public String toString() {
        return String.valueOf(symbol());
    }
}
