package edu.sfsu.csc413.chess;

import edu.sfsu.csc413.chess.model.Board;
import edu.sfsu.csc413.chess.model.Color;
import edu.sfsu.csc413.chess.model.Piece;
import edu.sfsu.csc413.chess.model.PieceType;
import edu.sfsu.csc413.chess.model.Position;
import edu.sfsu.csc413.chess.view.PieceGlyphs;
import edu.sfsu.csc413.chess.view.TextBoardRenderer;

/**
 * Entry point. For M1 it sets up the starting position and prints it.
 */
public final class Main {

    public static void main(String[] args) {
        Board board = new Board();

        // back rank order from the a-file to the h-file
        PieceType[] backRank = {
                PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN,
                PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK
        };

        for (int file = 0; file < Position.BOARD_SIZE; file++) {
            // white on ranks 1 and 2, black on ranks 7 and 8
            board.place(new Position(file, 0), new Piece(Color.WHITE, backRank[file]));
            board.place(new Position(file, 1), new Piece(Color.WHITE, PieceType.PAWN));
            board.place(new Position(file, 6), new Piece(Color.BLACK, PieceType.PAWN));
            board.place(new Position(file, 7), new Piece(Color.BLACK, backRank[file]));
        }

        System.out.println(new TextBoardRenderer(PieceGlyphs.LETTERS).render(board));
    }

    private Main() {
    }
}
