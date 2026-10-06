package edu.sfsu.csc413.chess.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Board stores which piece is on which square. That's it.
 * No printing and no move rules in here. An empty square is just null.
 */
public class Board {

    /** Indexed [file][rank], both 0-based — the same order as Position. */
    private final Piece[][] squares =
            new Piece[Position.BOARD_SIZE][Position.BOARD_SIZE];

    /** makes an empty board */
    public Board() {
    }

    /** returns the piece on that square, or null if nothing is there */
    public Piece pieceAt(Position position) {
        return squares[position.file()][position.rank()];
    }

    public boolean isEmpty(Position position) {
        return pieceAt(position) == null;
    }

    /** puts a piece on a square (replaces whatever was there). null clears it. */
    public void place(Position position, Piece piece) {
        squares[position.file()][position.rank()] = piece;
    }

    /**
     * lifts the piece off "from" and sets it down on "to" (whatever was on "to"
     * is gone). doesn't check anything, Game decides if the move is allowed.
     * a promotion puts down the new piece instead of the pawn.
     */
    public void apply(Move move) {
        Piece arriving = move.isPromotion()
                ? createPromoted(move.promotesTo(), move.moved().color())
                : move.moved();
        place(move.from(), null);
        place(move.to(), arriving);
    }

    /** puts "moved" back on "from" and "captured" (null for a quiet move) back on "to" */
    public void undo(Move move) {
        place(move.from(), move.moved());
        place(move.to(), move.captured());
    }

    /**
     * the piece a pawn promotes into. I chose a private switch here instead of
     * calling PieceFactory.create, because that would make model depend on
     * factory (arrow pointing the wrong way). the cost: this duplicates four
     * cases from PieceFactory, and a new PieceType won't break the build here,
     * it falls into the throw instead.
     */
    private static Piece createPromoted(PieceType type, Color color) {
        return switch (type) {
            case QUEEN -> new Queen(color);
            case ROOK -> new Rook(color);
            case BISHOP -> new Bishop(color);
            case KNIGHT -> new Knight(color);
            default -> throw new IllegalArgumentException("Cannot promote to " + type);
        };
    }

    /** every square that has a piece of the given color */
    public List<Position> positionsOf(Color color) {
        List<Position> result = new ArrayList<>();
        for (int file = 0; file < Position.BOARD_SIZE; file++) {
            for (int rank = 0; rank < Position.BOARD_SIZE; rank++) {
                Piece piece = squares[file][rank];
                if (piece != null && piece.color() == color) {
                    result.add(new Position(file, rank));
                }
            }
        }
        return result;
    }

    /**
     * FEN placement field, e.g. "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR".
     * Goes from rank 8 down to rank 1, and empty squares in a row get
     * counted into one digit.
     */
    @Override
    public String toString() {
        StringBuilder fen = new StringBuilder();
        for (int rank = Position.BOARD_SIZE - 1; rank >= 0; rank--) {
            int emptyCount = 0;
            for (int file = 0; file < Position.BOARD_SIZE; file++) {
                Piece piece = squares[file][rank];
                if (piece == null) {
                    emptyCount++;
                } else {
                    if (emptyCount > 0) {
                        fen.append(emptyCount);
                        emptyCount = 0;
                    }
                    fen.append(piece.symbol());
                }
            }
            // don't forget empties at the end of the rank
            if (emptyCount > 0) {
                fen.append(emptyCount);
            }
            // no slash after rank 1
            if (rank > 0) {
                fen.append('/');
            }
        }
        return fen.toString();
    }
}
