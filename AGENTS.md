# Instructions for coding agents

This is a chess game in Java 21, built with Maven. The model (`model/`,
`engine/`, `factory/`) and the controller (`view/GameController.java`) were
written by hand and are finished. Your job is **views only**: new classes that
implement `BoardView`.

## Do

- Add each view as a new class under `src/main/java/edu/sfsu/csc413/chess/view/`.
  Tests for views, if any, go under `src/test/java/edu/sfsu/csc413/chess/view/`.
- Read `BoardView`, `GameController`, `PlayerAction`, and `ConsoleView` before
  writing anything. Implement `BoardView`.
- Read the game only through `Game`: `board()` (then `Board.pieceAt(Position)`),
  `sideToMove()`, `legalMoves()`, `status()`, `history()`, `isGameOver()`.
  A `Move` has `from()` and `to()`; a `Position` is `(file, rank)`, both 0–7,
  rank 0 is White's back rank.
- Send every player action to the controller as a `PlayerAction`, through
  `controller.handle(...)`. A move is `new PlayerAction.Move("e2e4")` (from
  square + to square). An Undo button sends `new PlayerAction.Undo()`.
- For a promotion, send the four-character move (e.g. `"e7e8"`). The
  controller then asks `promotionChoice(game)`, which defaults to queen.
- Choose a view in `Main.java`: replace the `emoji` ternary with a `switch` on
  `args[0]` (`emoji`, `swing`, and one word per new view; anything else or no
  argument gives `ConsoleView`). `Main.java` is the only existing file you may edit.
- Use only the JDK. Swing and `com.sun.net.httpserver` are in the JDK.

## Swing and other event-driven views

- `GameController.start()` calls `view.render(game)` **before** `view.start(controller)`,
  on the main thread. `render` must not fail if the window has not been
  shown yet: build the components in the constructor, or store the game
  and draw it once the window exists. Do Swing work on the event thread
  (`SwingUtilities.invokeLater`).
- `start` builds and shows the window, then returns.
- After checkmate or stalemate the controller calls `showMessage(...)` and then
  `close()`. `close()` must keep the window open so the result can be read. It
  may disable board input, but it must not call `dispose()` or `System.exit`.
- Show whose turn it is and check, checkmate, and stalemate, from `game.status()`.
  Show messages from `showMessage` (illegal moves, "Nothing to undo.", results)
  in the window.
- Highlight and accept only moves found in `game.legalMoves()`.
- Screen rows count down while ranks count up. Convert carefully.

## Don't

- Don't change anything in `model/`, `engine/`, or `factory/`.
- Don't change `BoardView`, `PlayerAction`, `GameController`, `ConsoleView`,
  `EmojiView`, `PieceGlyphs`, or `TextBoardRenderer`. Don't change or delete any
  existing test.
- Don't call `game.play`, `game.undoLastMove`, `Board.apply`, `Board.place`,
  `Board.undo`, or anything else that changes the game. Only the controller
  changes the game.
- Don't work out chess rules in a view. Legal moves come only from
  `game.legalMoves()`, and check, checkmate, and stalemate only from `game.status()`.
- Don't wait for the next move inside a window. An event-driven view's `start`
  never calls `controller.runLoop()`, and no click handler loops, sleeps, or
  blocks on input. A modal dialog that asks one question, such as which piece a
  pawn promotes to, is fine.
- Don't edit `pom.xml` or add dependencies.

## Check your work

- `./mvnw test` passes, with no test changed or deleted.
- `git diff --stat submit-m6 HEAD` lists only new files under `view/` (and
  `src/test/.../view/`), `Main.java`, `AGENTS.md`, `CLAUDE.md`,
  `m7-submission.md`, and `docs/milestones/m7.md`.
- The new view files contain no `game.play`, no `undoLastMove`, and (in an
  event-driven view) no `runLoop`.
- `./mvnw -q compile && java -cp target/classes edu.sfsu.csc413.chess.Main swing`
  starts the Swing view. Say what you could not test yourself (for example,
  that you could not open a window).

## Stop and ask

If it looks like a file under "Don't" has to change, **stop and explain why**
instead of changing it.
