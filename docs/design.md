# Design

Diagrams of the engine as of M5.

## Class diagram (engine + model)

The six piece subclasses are drawn without methods, they all just override
`pseudoLegalMoves` (Pawn also overrides `attacks`).

```mermaid
classDiagram
    direction TB

    namespace engine {
        class Game {
            -Board board
            -List~Move~ history
            -Color sideToMove
            +Game()
            +Game(Board board, Color sideToMove)
            +board() Board
            +sideToMove() Color
            +history() List~Move~
            +legalMoves() List~Move~
            +findLegalMove(String notation) Optional~Move~
            +play(Move move) void
            +undoLastMove() Optional~Move~
        }
        class MoveGenerator {
            -MoveGenerator()
            +legalMoves(Board board, Color color)$ List~Move~
            ~pseudoLegalMoves(Board board, Color color)$ List~Move~
            +isInCheck(Board board, Color color)$ boolean
            +isAttacked(Board board, Position target, Color attacker)$ boolean
        }
    }

    namespace model {
        class Board {
            -Piece[][] squares
            +Board()
            +pieceAt(Position position) Piece
            +isEmpty(Position position) boolean
            +place(Position position, Piece piece) void
            +apply(Move move) void
            +undo(Move move) void
            +positionsOf(Color color) List~Position~
            +kingPosition(Color color) Position
            +toString() String
        }
        class Piece {
            <<abstract>>
            -Color color
            -PieceType type
            #Piece(Color color, PieceType type)
            +color() Color
            +type() PieceType
            +symbol() char
            +pseudoLegalMoves(Board board, Position from)* List~Move~
            +attacks(Board board, Position from, Position target) boolean
            #slidingMoves(Board board, Position from, int[][] directions) List~Move~
            #steppingMoves(Board board, Position from, int[][] offsets) List~Move~
            +toString() String
        }
        class Pawn
        class Knight
        class Bishop
        class Rook
        class Queen
        class King
        class Move {
            <<record>>
            +Position from
            +Position to
            +Piece moved
            +Piece captured
            +PieceType promotesTo
            +quiet(Position from, Position to, Piece moved)$ Move
            +capture(Position from, Position to, Piece moved, Piece captured)$ Move
            +promotion(Position from, Position to, Piece moved, Piece captured, PieceType promotesTo)$ Move
            +isCapture() boolean
            +isPromotion() boolean
            +toString() String
        }
        class Position {
            <<record>>
            +int file
            +int rank
            +int BOARD_SIZE$
            +parse(String algebraic)$ Position
            +offsetOrNull(int fileDelta, int rankDelta) Position
            +toString() String
        }
        class Color {
            <<enumeration>>
            WHITE
            BLACK
            +opposite() Color
            +pawnDirection() int
            +pawnStartRank() int
            +promotionRank() int
        }
        class PieceType {
            <<enumeration>>
            PAWN
            KNIGHT
            BISHOP
            ROOK
            QUEEN
            KING
            +symbol() char
            +fromSymbol(char letter)$ PieceType
        }
    }

    %% inheritance
    Piece <|-- Pawn
    Piece <|-- Knight
    Piece <|-- Bishop
    Piece <|-- Rook
    Piece <|-- Queen
    Piece <|-- King

    %% what Game holds (it owns these)
    Game *-- "1" Board : board
    Game *-- "*" Move : history
    Game --> "1" Color : sideToMove
    %% what Game only uses
    Game ..> MoveGenerator : legalMoves()

    %% MoveGenerator holds nothing, everything comes in as a parameter
    MoveGenerator ..> Board
    MoveGenerator ..> Piece
    MoveGenerator ..> Move
    MoveGenerator ..> Position
    MoveGenerator ..> Color

    %% Board holds the pieces
    Board o-- "0..64" Piece : squares
    Board ..> Position
    Board ..> Move

    %% what a Move refers to
    Move --> "2" Position : from, to
    Move --> "1..2" Piece : moved, captured
    Move --> "0..1" PieceType : promotesTo

    Piece --> "1" Color : color
    Piece --> "1" PieceType : type
```

## Sequence diagram: `game.legalMoves()`

```mermaid
sequenceDiagram
    participant Caller
    participant Game
    participant MoveGenerator
    participant Board
    participant Piece

    Caller->>Game: legalMoves()
    Game->>MoveGenerator: legalMoves(board, sideToMove)

    MoveGenerator->>MoveGenerator: pseudoLegalMoves(board, color)
    MoveGenerator->>Board: positionsOf(color)
    Board-->>MoveGenerator: squares with color's pieces
    loop each square "from"
        MoveGenerator->>Board: pieceAt(from)
        Board-->>MoveGenerator: piece
        MoveGenerator->>Piece: pseudoLegalMoves(board, from)
        Piece-->>MoveGenerator: moves
    end

    loop each candidate move
        MoveGenerator->>Board: apply(move)
        MoveGenerator->>MoveGenerator: isInCheck(board, color)
        MoveGenerator->>Board: kingPosition(color)
        Board-->>MoveGenerator: king square (or null)
        MoveGenerator->>MoveGenerator: isAttacked(board, king, color.opposite())
        MoveGenerator->>Board: positionsOf(attacker)
        Board-->>MoveGenerator: attacker squares
        loop each attacker square (stops at first hit)
            MoveGenerator->>Board: pieceAt(from)
            Board-->>MoveGenerator: piece
            MoveGenerator->>Piece: attacks(board, from, king)
            Piece-->>MoveGenerator: true / false
        end
        MoveGenerator->>Board: undo(move)
        alt king not in check
            MoveGenerator->>MoveGenerator: keep move
        else king in check
            MoveGenerator->>MoveGenerator: drop move
        end
    end

    MoveGenerator-->>Game: legal moves
    Game-->>Caller: legal moves
```
