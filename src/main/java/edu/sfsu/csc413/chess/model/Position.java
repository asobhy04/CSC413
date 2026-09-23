package edu.sfsu.csc413.chess.model;

/**
 * Disclaimer: Wasnt sure if Postion.java was suppose to be given
 *              used AI to help make a skeleton then completed it
 * A square on the chess board, identified by 0-based file and rank.
 *
 * <p>File 0 is 'a', file 7 is 'h'. Rank 0 is '1', rank 7 is '8'.
 */
public record Position(int file, int rank) {

    // board is 8x8 (the renderer and BoardTest use this)
    public static final int BOARD_SIZE = 8;

    public Position {
        if (file < 0 || file > 7 || rank < 0 || rank > 7) {
            throw new IllegalArgumentException(
                    "Position off board: file=" + file + ", rank=" + rank);
        }
    }

    /**
     * Parses algebraic notation (e.g. "e2") into a Position.
     */
    public static Position parse(String algebraic) {
        char fileChar = algebraic.charAt(0);
        char rankChar = algebraic.charAt(1);
        int file = fileChar - 'a';
        int rank = rankChar - '1';
        return new Position(file, rank);
    }

    /**
     * Returns the position offset by the given file/rank deltas,
     * or null if that offset would leave the board.
     */
    public Position offsetOrNull(int fileDelta, int rankDelta) {
        try {
            return new Position(this.file() + fileDelta, this.rank() + rankDelta);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return "" + (char) ('a' + file) + (rank + 1);
    }
}
