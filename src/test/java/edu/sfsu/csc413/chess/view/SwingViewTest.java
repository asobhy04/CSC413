package edu.sfsu.csc413.chess.view;

import edu.sfsu.csc413.chess.engine.Game;
import edu.sfsu.csc413.chess.engine.GameStatus;
import edu.sfsu.csc413.chess.model.Position;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives {@link SwingView}'s click logic without opening a window: the view is
 * attached to a real controller, and squares are "clicked" on the event
 * thread, as Swing would. Nothing here builds a frame.
 */
class SwingViewTest {

    @BeforeAll
    static void headless() {
        System.setProperty("java.awt.headless", "true");
    }

    private final Game game = new Game();
    private final SwingView view = new SwingView();
    private final GameController controller = new GameController(game, view);

    SwingViewTest() {
        view.attach(controller);
    }

    private void click(String square) throws Exception {
        SwingUtilities.invokeAndWait(() -> view.squareClicked(Position.parse(square)));
    }

    /** Waits for every task already queued on the event thread. */
    private void settle() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }

    @Test
    void topLeftScreenCellIsA8AndBottomRightIsH1() {
        assertEquals(Position.parse("a8"), SwingView.squareAt(0, 0));
        assertEquals(Position.parse("h1"), SwingView.squareAt(7, 7));
        assertEquals(Position.parse("e2"), SwingView.squareAt(4, 6));
        assertEquals(6, SwingView.rowOf(1));
    }

    @Test
    void twoClicksPlayAMove() throws Exception {
        click("e2");
        assertEquals(Position.parse("e2"), view.selectedSquare());
        click("e4");
        assertNull(view.selectedSquare());
        assertEquals("[e2e4]", game.history().toString());
    }

    @Test
    void anIllegalDestinationIsReportedAndNotSent() throws Exception {
        click("e2");
        click("e5");
        settle();
        assertTrue(game.history().isEmpty());
        assertTrue(view.lastMessage().startsWith("Not a legal move"), view.lastMessage());
        assertNull(view.selectedSquare());
    }

    @Test
    void theOpponentsPieceCannotBeSelected() throws Exception {
        click("e7");
        settle();
        assertNull(view.selectedSquare());
        assertEquals("It is White's turn.", view.lastMessage());
    }

    @Test
    void foolsMateEndsTheGameAndBlocksFurtherClicks() throws Exception {
        for (String[] move : new String[][] {
                {"f2", "f3"}, {"e7", "e5"}, {"g2", "g4"}, {"d8", "h4"}}) {
            click(move[0]);
            click(move[1]);
        }
        settle();
        assertEquals(GameStatus.CHECKMATE, game.status());
        assertEquals("Checkmate. BLACK wins.", view.lastMessage());
        assertEquals("Checkmate — Black wins", SwingView.statusText(game));

        click("a2");
        assertNull(view.selectedSquare());
    }

    @Test
    void undoTakesBackTheLastMoveThroughTheController() throws Exception {
        click("e2");
        click("e4");
        SwingUtilities.invokeAndWait(() -> controller.handle(new PlayerAction.Undo()));
        assertTrue(game.history().isEmpty());
        SwingUtilities.invokeAndWait(() -> controller.handle(new PlayerAction.Undo()));
        settle();
        assertEquals("Nothing to undo.", view.lastMessage());
    }
}
