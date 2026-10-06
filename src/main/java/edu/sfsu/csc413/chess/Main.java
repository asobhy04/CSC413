package edu.sfsu.csc413.chess;

import edu.sfsu.csc413.chess.engine.Game;
import edu.sfsu.csc413.chess.view.PieceGlyphs;
import edu.sfsu.csc413.chess.view.TextBoardRenderer;

import java.util.List;

/**
 * Entry point. Plays e2e4 and e7e5 through Game (M3), then undoes one,
 * printing the board each time. Main only talks to Game.
 */
public final class Main {

    public static void main(String[] args) {
        Game game = new Game();
        TextBoardRenderer renderer = new TextBoardRenderer(PieceGlyphs.LETTERS);
        System.out.println(renderer.render(game.board()));

        for (String notation : List.of("e2e4", "e7e5")) {
            game.play(game.findLegalMove(notation).orElseThrow());
        }
        System.out.println(renderer.render(game.board()));

        game.undoLastMove();
        System.out.println(renderer.render(game.board()));
    }

    private Main() {
    }
}
