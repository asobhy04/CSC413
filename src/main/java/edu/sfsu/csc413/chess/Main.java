package edu.sfsu.csc413.chess;

import edu.sfsu.csc413.chess.engine.Game;
import edu.sfsu.csc413.chess.view.AnsiView;
import edu.sfsu.csc413.chess.view.BoardView;
import edu.sfsu.csc413.chess.view.ConsoleView;
import edu.sfsu.csc413.chess.view.EmojiView;
import edu.sfsu.csc413.chess.view.GameController;
import edu.sfsu.csc413.chess.view.SwingView;

/**
 * Entry point. Builds a {@link Game}, a {@link BoardView}, and a
 * {@link GameController} that joins them, then starts the game.
 *
 * <p>The first command-line word picks the view: {@code emoji} for
 * {@link EmojiView}, {@code swing} for {@link SwingView}, {@code ansi} for
 * {@link AnsiView}; anything else (or nothing) gives {@link ConsoleView}.
 */
public final class Main {

    public static void main(String[] args) {
        Game game = new Game();
        String choice = args.length > 0 ? args[0].toLowerCase() : "";
        BoardView view = switch (choice) {
            case "emoji" -> new EmojiView();
            case "swing" -> new SwingView();
            case "ansi" -> new AnsiView();
            default -> new ConsoleView();
        };
        new GameController(game, view).start();
    }

    private Main() {
    }
}
