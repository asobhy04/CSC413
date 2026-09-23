package edu.sfsu.csc413.chess.model;

/**
 * The six kinds of chess pieces. Each one stores its FEN letter.
 * Knight is N because K is already taken by the king.
 */
public enum PieceType {
    PAWN('P'),
    KNIGHT('N'),
    BISHOP('B'),
    ROOK('R'),
    QUEEN('Q'),
    KING('K');

    private final char symbol;

    PieceType(char symbol) {
        this.symbol = symbol;
    }

    /** uppercase FEN letter for this type */
    public char symbol() {
        return symbol;
    }

    /**
     * Goes the other way: letter -> type. Works with upper or lower case.
     * Throws IllegalArgumentException if the letter isn't a piece.
     */
    public static PieceType fromSymbol(char letter) {
        char upper = Character.toUpperCase(letter);
        for (PieceType type : values()) {
            if (type.symbol == upper) {
                return type;
            }
        }
        throw new IllegalArgumentException("Not a piece letter: " + letter);
    }
}
