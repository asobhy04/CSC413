package edu.sfsu.csc413.chess.view;

import edu.sfsu.csc413.chess.engine.Game;
import edu.sfsu.csc413.chess.engine.GameStatus;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Plays games through {@link AnsiView} with scripted input. */
class AnsiViewTest {

    private static String strip(String ansi) {
        return ansi.replaceAll("\u001B\\[[0-9;]*m", "");
    }

    private static Game play(ByteArrayOutputStream bytes, String input) {
        Game game = new Game();
        AnsiView view = new AnsiView(new PrintStream(bytes, true, StandardCharsets.UTF_8),
                new Scanner(input));
        new GameController(game, view).start();
        return game;
    }

    @Test
    void foolsMatePlaysToTheEndWithAnIllegalMoveAndAnUndo() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Game game = play(bytes, "f2f3\ne7e5\ne2e5\nundo\ne7e5\ng2g4\nd8h4\n");
        String text = strip(bytes.toString(StandardCharsets.UTF_8));

        assertEquals(GameStatus.CHECKMATE, game.status());
        assertTrue(text.contains("Not a legal move: 'e2e5'."), text);
        assertTrue(text.contains("Checkmate. BLACK wins."), text);
        assertTrue(text.contains("Checkmate — Black wins."), text);
        assertTrue(bytes.toString(StandardCharsets.UTF_8).contains("\u001B[48;2;"),
                "squares are drawn with ANSI background colours");
    }

    @Test
    void theBoardIsDrawnFromTheSideToMove() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        AnsiView view = new AnsiView(new PrintStream(bytes), new Scanner(""));
        Game game = new Game();

        String white = strip(view.board(game));
        String[] whiteLines = white.split("\n");
        assertEquals("    a  b  c  d  e  f  g  h ", whiteLines[0]);
        assertTrue(whiteLines[1].startsWith(" 8 "), whiteLines[1]);

        new GameController(game, view).handle(new PlayerAction.Move("e2e4"));
        String[] blackLines = strip(view.board(game)).split("\n");
        assertEquals("    h  g  f  e  d  c  b  a ", blackLines[0]);
        assertTrue(blackLines[1].startsWith(" 1 "), blackLines[1]);
    }

    @Test
    void endOfInputQuits() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Game game = play(bytes, "e2e4\n");
        assertEquals(1, game.history().size());
        assertTrue(strip(bytes.toString(StandardCharsets.UTF_8)).contains("Goodbye."));
    }
}
