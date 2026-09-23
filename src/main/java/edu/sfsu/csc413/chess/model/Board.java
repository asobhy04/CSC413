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
