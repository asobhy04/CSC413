package edu.sfsu.csc413.chess.view;

import edu.sfsu.csc413.chess.engine.Game;
import edu.sfsu.csc413.chess.engine.GameStatus;
import edu.sfsu.csc413.chess.model.Move;

import java.util.List;
import java.util.Optional;

/**
 * Connects the player's decisions to the game: carry out what was asked, then
 * ask the view to redraw.
 *
 * <p>This is the C in MVC. It is the only class that knows about both the
 * {@link Game} and the {@link BoardView}. It holds no game state of its own:
 * the model owns the position, the view owns the display, and the controller
 * owns only the conversation between them.
 *
 * <p>When you are done, this class mentions no {@code Scanner}, no
 * {@code System.out}, no window, and none of the words "undo", "moves", or
 * "quit". Those belong to the view and to {@link PlayerAction#parse}.
 */
public class GameController {

    private final Game game;
    private final BoardView view;

    /** The only state the controller keeps: whether the player asked to stop. */
    private boolean quit;

    public GameController(Game game, BoardView view) {
        this.game = game;
        this.view = view;
    }

    /** The game being played, so a view can read it in order to draw it. */
    public Game game() {
        return game;
    }

    /**
     * Greets the player, draws the opening position, and hands control to the
     * view with {@link BoardView#start}.
     */
    public void start() {
        view.showMessage("Welcome to chess. " + game.sideToMove() + " moves first.");
        view.render(game);
        view.start(this);
    }

    /**
     * The ask-and-handle loop, for views that can wait for input.
     *
     * <p>Asks the view for the next action and handles it, until the player
     * quits or the game is over. A console view calls this from its
     * {@code start}; an event-driven view never calls it.
     */
    public void runLoop() {
        while (!quit && !game.isGameOver()) {
            handle(view.nextAction(game));
        }
    }

    /**
     * Carries out one player action and redraws.
     *
     * <p>Every route into the game goes through here: {@link #runLoop()} calls
     * it, and so will a button's click handler in M7. The handout lists what
     * each of the four actions does and what the player is told.
     */
    public void handle(PlayerAction action) {
        // No default: PlayerAction is sealed, so the compiler checks all four.
        switch (action) {
            case PlayerAction.Move move -> playMove(move.notation());
            case PlayerAction.Undo undo -> {
                if (game.undoLastMove().isEmpty()) {
                    view.showMessage("Nothing to undo.");
                }
            }
            case PlayerAction.ShowMoves showMoves -> showLegalMoves();
            case PlayerAction.Quit q -> {
                quit = true;
                view.showMessage("Goodbye.");
                view.close();
                return; // no redraw after quitting
            }
        }

        view.render(game);
        if (game.isGameOver()) {
            announceResult();
            view.close();
        }
    }

    /**
     * Plays the legal move with this notation, or reports that there is none.
     * A four-character move that matches nothing may be a promotion without
     * its letter, so the view is asked which piece and we try once more.
     */
    private void playMove(String notation) {
        Optional<Move> move = game.findLegalMove(notation);
        if (move.isEmpty() && notation.length() == 4) {
            char letter = Character.toLowerCase(view.promotionChoice(game).symbol());
            move = game.findLegalMove(notation + letter);
        }
        if (move.isPresent()) {
            game.play(move.get());
        } else {
            view.showMessage("Not a legal move: '" + notation + "'.");
        }
    }

    /** Tells the player every legal move, sorted. Changes nothing. */
    private void showLegalMoves() {
        List<String> moves = game.legalMoves().stream()
                .map(Move::toString)
                .sorted()
                .toList();
        view.showMessage(moves.size() + " legal moves: " + String.join(" ", moves));
    }

    /** The side to move is the side that has no move, so a mate means the other side won. */
    private void announceResult() {
        if (game.status() == GameStatus.CHECKMATE) {
            view.showMessage("Checkmate. " + game.sideToMove().opposite() + " wins.");
        } else {
            view.showMessage("Stalemate. The game is a draw.");
        }
    }
}
