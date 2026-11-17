package edu.sfsu.csc413.chess.view;

import edu.sfsu.csc413.chess.engine.Game;
import edu.sfsu.csc413.chess.engine.GameStatus;
import edu.sfsu.csc413.chess.model.Color;
import edu.sfsu.csc413.chess.model.Move;
import edu.sfsu.csc413.chess.model.Piece;
import edu.sfsu.csc413.chess.model.PieceType;
import edu.sfsu.csc413.chess.model.Position;

import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

/**
 * A terminal board in ANSI colour, always drawn from the side to move.
 *
 * <p>Squares are coloured backgrounds three columns wide; pieces are the same
 * filled figure for both sides, told apart by colour (white or black) so that
 * each is readable on light and dark squares. The last move is tinted, and the
 * king of the side to move turns red when {@link Game#status()} says check.
 *
 * <p>Like {@link ConsoleView}, a terminal is happy to be asked, so
 * {@link #start} hands control to {@link GameController#runLoop()} and
 * {@link #nextAction} reads one line per turn. Input is the same notation
 * ({@code e2e4}, {@code undo}, {@code moves}, {@code quit}), turned into a
 * {@link PlayerAction} by {@link PlayerAction#parse}.
 */
public class AnsiView implements BoardView {

    private static final String ESC = "\u001B[";
    private static final String RESET = ESC + "0m";

    // 24-bit colours: background squares and foreground pieces.
    private static final String LIGHT_BG = bg(240, 217, 181);
    private static final String DARK_BG = bg(181, 136, 99);
    private static final String LIGHT_LAST_BG = bg(205, 210, 106);
    private static final String DARK_LAST_BG = bg(170, 162, 58);
    private static final String CHECK_BG = bg(220, 50, 50);
    private static final String WHITE_FG = ESC + "1m" + fg(255, 255, 255);
    private static final String BLACK_FG = ESC + "1m" + fg(0, 0, 0);
    private static final String LABEL = ESC + "2m";
    private static final String MESSAGE = ESC + "33m";
    private static final String ALERT = ESC + "1;31m";
    private static final String GOOD = ESC + "1;32m";

    private final PrintStream out;
    private final Scanner in;

    /** A view on the terminal: standard output and standard input. */
    public AnsiView() {
        this(System.out, new Scanner(System.in));
    }

    /** Injectable streams, so tests can drive the view without a terminal. */
    public AnsiView(PrintStream out, Scanner in) {
        this.out = out;
        this.in = in;
    }

    @Override
    public void render(Game game) {
        out.println();
        out.print(board(game));
        out.println();
        out.println(statusLine(game));
    }

    @Override
    public void showMessage(String message) {
        out.println(MESSAGE + message + RESET);
    }

    /** A terminal can wait for input, so the controller drives. */
    @Override
    public void start(GameController controller) {
        controller.runLoop();
    }

    @Override
    public PlayerAction nextAction(Game game) {
        out.print(name(game.sideToMove()) + ", your move (e.g. e2e4, 'undo', 'moves', 'quit'): ");
        out.flush();
        // End of input (Ctrl-D, or a piped file running out) means quit.
        return PlayerAction.parse(in.hasNextLine() ? in.nextLine() : "quit");
    }

    /** Leaves the terminal in its normal colours. */
    @Override
    public void close() {
        out.print(RESET);
        out.flush();
    }

    // ---- Drawing -----------------------------------------------------------

    /**
     * The board as coloured text, from {@code game.sideToMove()}'s side of the
     * table. Flipping turns both axes: Black sees rank 1 at the top and the
     * h-file on the left.
     */
    String board(Game game) {
        boolean flipped = game.sideToMove() == Color.BLACK;
        List<Move> history = game.history();
        Move last = history.isEmpty() ? null : history.get(history.size() - 1);
        GameStatus status = game.status();
        boolean inCheck = status == GameStatus.CHECK || status == GameStatus.CHECKMATE;

        StringBuilder text = new StringBuilder();
        String files = fileLegend(flipped);
        text.append(files).append('\n');

        for (int row = 0; row < Position.BOARD_SIZE; row++) {
            // Screen row 0 is the top line. Ranks count up; rows count down.
            int rank = flipped ? row : Position.BOARD_SIZE - 1 - row;
            text.append(LABEL).append(' ').append(rank + 1).append(' ').append(RESET);

            for (int column = 0; column < Position.BOARD_SIZE; column++) {
                int file = flipped ? Position.BOARD_SIZE - 1 - column : column;
                Position square = new Position(file, rank);
                Piece piece = game.board().pieceAt(square);

                boolean light = (file + rank) % 2 == 1;
                boolean lastMove = last != null
                        && (square.equals(last.from()) || square.equals(last.to()));
                boolean checkedKing = inCheck && piece != null
                        && piece.type() == PieceType.KING
                        && piece.color() == game.sideToMove();

                String background = checkedKing ? CHECK_BG
                        : lastMove ? (light ? LIGHT_LAST_BG : DARK_LAST_BG)
                        : light ? LIGHT_BG : DARK_BG;
                text.append(background);
                if (piece == null) {
                    text.append("   ");
                } else {
                    text.append(piece.color() == Color.WHITE ? WHITE_FG : BLACK_FG)
                            .append(' ').append(figure(piece.type())).append(' ');
                }
                text.append(RESET);
            }
            text.append(LABEL).append(' ').append(rank + 1).append(RESET).append('\n');
        }
        text.append(files).append('\n');
        return text.toString();
    }

    private static String fileLegend(boolean flipped) {
        StringBuilder legend = new StringBuilder(LABEL).append("   ");
        for (int column = 0; column < Position.BOARD_SIZE; column++) {
            int file = flipped ? Position.BOARD_SIZE - 1 - column : column;
            legend.append(' ').append((char) ('a' + file)).append(' ');
        }
        return legend.append(RESET).toString();
    }

    /** Whose turn it is and how the game stands, from {@code game.status()}. */
    static String statusLine(Game game) {
        String side = name(game.sideToMove());
        return switch (game.status()) {
            case IN_PROGRESS -> side + " to move";
            case CHECK -> ALERT + side + " to move — CHECK!" + RESET;
            case CHECKMATE -> GOOD + "Checkmate — "
                    + name(game.sideToMove().opposite()) + " wins." + RESET;
            case STALEMATE -> GOOD + "Stalemate — draw." + RESET;
        };
    }

    /** One filled figure per type; colour shows the side. */
    private static String figure(PieceType type) {
        return switch (type) {
            case KING -> "♚";
            case QUEEN -> "♛";
            case ROOK -> "♜";
            case BISHOP -> "♝";
            case KNIGHT -> "♞";
            case PAWN -> "♟";
        };
    }

    private static String name(Color color) {
        return color == Color.WHITE ? "White" : "Black";
    }

    private static String bg(int r, int g, int b) {
        return ESC + "48;2;" + r + ";" + g + ";" + b + "m";
    }

    private static String fg(int r, int g, int b) {
        return ESC + "38;2;" + r + ";" + g + ";" + b + "m";
    }
}
