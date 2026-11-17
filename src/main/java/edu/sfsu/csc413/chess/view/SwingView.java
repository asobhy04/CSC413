package edu.sfsu.csc413.chess.view;

import edu.sfsu.csc413.chess.engine.Game;
import edu.sfsu.csc413.chess.engine.GameStatus;
import edu.sfsu.csc413.chess.model.Board;
import edu.sfsu.csc413.chess.model.Move;
import edu.sfsu.csc413.chess.model.Piece;
import edu.sfsu.csc413.chess.model.PieceType;
import edu.sfsu.csc413.chess.model.Position;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * A Swing window: click a piece, then click the square to move it to.
 *
 * <p>This is an event-driven view. {@link #start} builds the window and
 * returns; it never runs the ask-and-handle loop itself. Each completed
 * click pair becomes one {@link PlayerAction.Move} handed to
 * {@link GameController#handle}, and the Undo button hands over a
 * {@link PlayerAction.Undo}. The view never changes the game itself.
 *
 * <p>The only state this class keeps of its own is the half-finished move:
 * {@link #selected}, the square the player clicked first. It lives on the
 * Swing event thread, like every other field read by the drawing code.
 *
 * <p>Legal moves come only from {@link Game#legalMoves()}, and check,
 * checkmate, and stalemate only from {@link Game#status()}.
 */
public class SwingView implements BoardView {

    private static final int SQUARE = 72;

    private static final Color LIGHT = new Color(240, 217, 181);
    private static final Color DARK = new Color(181, 136, 99);
    private static final Color SELECTED = new Color(246, 246, 105, 200);
    private static final Color LAST_MOVE = new Color(205, 210, 106, 150);
    private static final Color TARGET = new Color(20, 85, 30, 140);
    private static final Color CHECK = new Color(220, 40, 40, 170);

    /** Filled figures; colour comes from how they are painted, not the glyph. */
    private static final String FIGURES = "♚♛♜♝♞♟";

    // ---- Swing state: touched only on the event thread -------------------

    private JFrame frame;
    private BoardPanel boardPanel;
    private JLabel statusLabel;
    private JLabel messageLabel;
    private JButton undoButton;

    private GameController controller;
    /** The game as last passed to {@link #render}; null until then. */
    private Game game;
    /** The first square of a half-finished move, or null. */
    private Position selected;
    /** Set by {@link #close()} once the game has ended; disables input. */
    private boolean closed;
    /** The latest message; shown when the window is built if it came earlier. */
    private String lastMessage = " ";

    @Override
    public void render(Game game) {
        // May be called on the main thread before start(), so hand off to the
        // event thread. Everything runs there in order, so this is seen before
        // start()'s window-building task.
        onEventThread(() -> {
            this.game = game;
            refresh();
        });
    }

    @Override
    public void showMessage(String message) {
        onEventThread(() -> {
            lastMessage = message;
            if (messageLabel != null) {
                messageLabel.setText(message);
            }
        });
    }

    /** Builds and shows the window, then returns. Never waits for a move. */
    @Override
    public void start(GameController controller) {
        onEventThread(() -> {
            attach(controller);
            buildWindow();
            refresh();
        });
    }

    /**
     * Connects clicks to the controller without building a window. Package-
     * private so tests can drive {@link #squareClicked} headlessly.
     */
    void attach(GameController controller) {
        this.controller = controller;
    }

    /** The latest message shown, for tests. */
    String lastMessage() {
        return lastMessage;
    }

    /**
     * Called after checkmate or stalemate. Keeps the window open so the result
     * can be read, and turns off further input.
     */
    @Override
    public void close() {
        onEventThread(() -> {
            closed = true;
            selected = null;
            refresh();
        });
    }

    // ---- Building the window ---------------------------------------------

    private void buildWindow() {
        frame = new JFrame("Chess");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        boardPanel = new BoardPanel();

        statusLabel = new JLabel(" ");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 16f));
        messageLabel = new JLabel(lastMessage);
        messageLabel.setFont(messageLabel.getFont().deriveFont(14f));

        undoButton = new JButton("Undo");
        undoButton.addActionListener(e -> {
            selected = null;
            controller.handle(new PlayerAction.Undo());
        });

        JPanel top = new JPanel(new BorderLayout(12, 0));
        top.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        top.add(statusLabel, BorderLayout.CENTER);
        top.add(undoButton, BorderLayout.EAST);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(BorderFactory.createEmptyBorder(8, 12, 10, 12));
        bottom.add(messageLabel, BorderLayout.CENTER);

        frame.add(top, BorderLayout.NORTH);
        frame.add(boardPanel, BorderLayout.CENTER);
        frame.add(bottom, BorderLayout.SOUTH);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /** Brings the labels and board up to date with {@link #game}. */
    private void refresh() {
        if (frame == null || game == null) {
            return; // render() before start(): drawn once the window exists
        }
        statusLabel.setText(statusText(game));
        undoButton.setEnabled(!closed);
        boardPanel.repaint();
    }

    /** "White to move", "Black to move — check!", "Checkmate — White wins", … */
    static String statusText(Game game) {
        String side = name(game.sideToMove());
        return switch (game.status()) {
            case IN_PROGRESS -> side + " to move";
            case CHECK -> side + " to move — check!";
            case CHECKMATE -> "Checkmate — " + name(game.sideToMove().opposite()) + " wins";
            case STALEMATE -> "Stalemate — draw";
        };
    }

    private static String name(edu.sfsu.csc413.chess.model.Color color) {
        return color == edu.sfsu.csc413.chess.model.Color.WHITE ? "White" : "Black";
    }

    // ---- Clicks ----------------------------------------------------------

    /**
     * Handles a click on one square. The first click selects one of the side to
     * move's pieces; the second either picks a legal destination (and sends the
     * move to the controller), picks another of the player's pieces, or is
     * reported as illegal. Never loops, sleeps, or waits.
     *
     * <p>Package-private so tests can drive the click logic without a window.
     */
    void squareClicked(Position square) {
        if (closed || controller == null) {
            return;
        }
        Game current = controller.game();
        Piece piece = current.board().pieceAt(square);
        boolean ownPiece = piece != null && piece.color() == current.sideToMove();

        if (selected == null) {
            if (ownPiece) {
                select(current, square);
            } else if (piece != null) {
                showMessage("It is " + name(current.sideToMove()) + "'s turn.");
            }
            return;
        }

        if (square.equals(selected)) {
            selected = null; // click the same square again to cancel
            showMessage(" ");
        } else if (isLegalDestination(current, selected, square)) {
            String notation = selected.toString() + square;
            selected = null;
            // Four characters even for a promotion: the controller then asks
            // promotionChoice(), which defaults to a queen.
            controller.handle(new PlayerAction.Move(notation));
        } else if (ownPiece) {
            select(current, square);
        } else {
            showMessage("Not a legal move: '" + selected + square + "'.");
            selected = null;
        }
        refresh();
    }

    private void select(Game current, Position square) {
        if (legalTargets(current, square).isEmpty()) {
            selected = null;
            showMessage("That piece has no legal moves.");
        } else {
            selected = square;
            showMessage(" ");
        }
        refresh();
    }

    /** Selection state, for tests. */
    Position selectedSquare() {
        return selected;
    }

    /** Every destination the piece on {@code from} may legally reach. */
    private static List<Position> legalTargets(Game game, Position from) {
        return game.legalMoves().stream()
                .filter(m -> m.from().equals(from))
                .map(Move::to)
                .distinct()
                .toList();
    }

    private static boolean isLegalDestination(Game game, Position from, Position to) {
        return game.legalMoves().stream()
                .anyMatch(m -> m.from().equals(from) && m.to().equals(to));
    }

    // ---- Screen ↔ board ----------------------------------------------------

    /**
     * The square under screen cell ({@code column}, {@code row}), where row 0
     * is the top of the board. Drawn from White's side: rank 8 at the top, so
     * screen rows count down while ranks count up.
     */
    static Position squareAt(int column, int row) {
        return new Position(column, Position.BOARD_SIZE - 1 - row);
    }

    /** The screen row of a rank: the inverse of {@link #squareAt}. */
    static int rowOf(int rank) {
        return Position.BOARD_SIZE - 1 - rank;
    }

    private static void onEventThread(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }

    // ---- Drawing -----------------------------------------------------------

    /** The 8×8 board. Draws from {@link #game}; turns clicks into squares. */
    private final class BoardPanel extends JPanel {

        private static final int MARGIN = 22; // room for the a–h / 1–8 labels

        BoardPanel() {
            int side = SQUARE * Position.BOARD_SIZE + 2 * MARGIN;
            setPreferredSize(new Dimension(side, side));
            setBackground(new Color(60, 60, 60));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    int size = squareSize();
                    int column = Math.floorDiv(e.getX() - originX(size), size);
                    int row = Math.floorDiv(e.getY() - originY(size), size);
                    if (column >= 0 && column < Position.BOARD_SIZE
                            && row >= 0 && row < Position.BOARD_SIZE) {
                        squareClicked(squareAt(column, row));
                    }
                }
            });
        }

        private int squareSize() {
            int available = Math.min(getWidth(), getHeight()) - 2 * MARGIN;
            return Math.max(8, available / Position.BOARD_SIZE);
        }

        private int originX(int size) {
            return (getWidth() - size * Position.BOARD_SIZE) / 2;
        }

        private int originY(int size) {
            return (getHeight() - size * Position.BOARD_SIZE) / 2;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (game == null) {
                return;
            }
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int size = squareSize();
            int x0 = originX(size);
            int y0 = originY(size);
            Board board = game.board();
            List<Move> history = game.history();
            Move last = history.isEmpty() ? null : history.get(history.size() - 1);
            GameStatus status = game.status();
            List<Position> targets = selected == null ? List.of() : legalTargets(game, selected);

            for (int row = 0; row < Position.BOARD_SIZE; row++) {
                for (int column = 0; column < Position.BOARD_SIZE; column++) {
                    Position square = squareAt(column, row);
                    int x = x0 + column * size;
                    int y = y0 + row * size;

                    boolean light = (square.file() + square.rank()) % 2 == 1;
                    g.setColor(light ? LIGHT : DARK);
                    g.fillRect(x, y, size, size);

                    if (last != null && (square.equals(last.from()) || square.equals(last.to()))) {
                        g.setColor(LAST_MOVE);
                        g.fillRect(x, y, size, size);
                    }
                    if (square.equals(selected)) {
                        g.setColor(SELECTED);
                        g.fillRect(x, y, size, size);
                    }

                    Piece piece = board.pieceAt(square);
                    if (piece != null && piece.type() == PieceType.KING
                            && piece.color() == game.sideToMove()
                            && (status == GameStatus.CHECK || status == GameStatus.CHECKMATE)) {
                        g.setColor(CHECK);
                        g.fillOval(x + 2, y + 2, size - 4, size - 4);
                    }
                    if (piece != null) {
                        drawPiece(g, piece, x, y, size);
                    }

                    if (targets.contains(square)) {
                        g.setColor(TARGET);
                        if (piece == null) {
                            int dot = size / 4;
                            g.fillOval(x + (size - dot) / 2, y + (size - dot) / 2, dot, dot);
                        } else {
                            g.setStroke(new BasicStroke(Math.max(3f, size / 14f)));
                            g.drawOval(x + 3, y + 3, size - 6, size - 6);
                        }
                    }
                }
            }
            drawCoordinates(g, x0, y0, size);
            g.dispose();
        }

        private void drawCoordinates(Graphics2D g, int x0, int y0, int size) {
            g.setColor(new Color(225, 225, 225));
            g.setFont(getFont().deriveFont(Font.BOLD, 13f));
            FontMetrics fm = g.getFontMetrics();
            for (int i = 0; i < Position.BOARD_SIZE; i++) {
                String file = String.valueOf((char) ('a' + squareAt(i, 0).file()));
                int fx = x0 + i * size + (size - fm.stringWidth(file)) / 2;
                g.drawString(file, fx, y0 + size * Position.BOARD_SIZE + fm.getAscent() + 3);

                String rank = String.valueOf(squareAt(0, i).rank() + 1);
                int ry = y0 + i * size + (size + fm.getAscent()) / 2 - 2;
                g.drawString(rank, x0 - fm.stringWidth(rank) - 6, ry);
            }
        }

        /**
         * Draws a piece as a filled shape with a contrasting outline: white
         * pieces are white with a black edge, black pieces black with a white
         * edge, so both stand out on light and dark squares alike. Falls back
         * to a lettered disc if no installed font has the chess figures.
         */
        private void drawPiece(Graphics2D g, Piece piece, int x, int y, int size) {
            boolean white = piece.color() == edu.sfsu.csc413.chess.model.Color.WHITE;
            Color fill = white ? Color.WHITE : new Color(20, 20, 20);
            Color edge = white ? new Color(20, 20, 20) : new Color(235, 235, 235);
            float stroke = Math.max(1.5f, size / 30f);

            Font font = new Font(Font.SERIF, Font.PLAIN, Math.round(size * 0.8f));
            String glyph = String.valueOf(FIGURES.charAt(figureIndex(piece.type())));
            Shape shape;
            if (font.canDisplayUpTo(glyph) == -1) {
                GlyphVector vector = font.createGlyphVector(g.getFontRenderContext(), glyph);
                shape = vector.getOutline();
            } else {
                // No chess font: a disc with the piece's letter.
                int pad = size / 8;
                g.setColor(fill);
                g.fillOval(x + pad, y + pad, size - 2 * pad, size - 2 * pad);
                g.setColor(edge);
                g.setStroke(new BasicStroke(stroke));
                g.drawOval(x + pad, y + pad, size - 2 * pad, size - 2 * pad);
                Font letterFont = getFont().deriveFont(Font.BOLD, size * 0.4f);
                shape = letterFont.createGlyphVector(g.getFontRenderContext(),
                        String.valueOf(piece.type().symbol())).getOutline();
                fill = edge; // letter in the edge colour, on the disc
            }

            Rectangle2D bounds = shape.getBounds2D();
            AffineTransform centre = AffineTransform.getTranslateInstance(
                    x + (size - bounds.getWidth()) / 2 - bounds.getX(),
                    y + (size - bounds.getHeight()) / 2 - bounds.getY());
            Shape placed = centre.createTransformedShape(shape);

            g.setStroke(new BasicStroke(stroke * 2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(edge);
            g.draw(placed);
            g.setColor(fill);
            g.fill(placed);
        }

        private int figureIndex(PieceType type) {
            return switch (type) {
                case KING -> 0;
                case QUEEN -> 1;
                case ROOK -> 2;
                case BISHOP -> 3;
                case KNIGHT -> 4;
                case PAWN -> 5;
            };
        }
    }
}
