package game.sokoban

import scala.collection.mutable.Stack

abstract class MovePlayer(val gameBoard : Array[Array[Char]], val finalBoxLocations : List[(Int, Int)]) {

  private val gameBoardRows = gameBoard.size
  private val gameBoardCols = gameBoard(0).size

  private var playerPosX = 0
  private var playerPosY = 0

  private var boxPosX = 0
  private var boxPosY = 0

  private var movedPlayer = false
  private var movedBox = false

  protected val moveX : Int
  protected val moveY : Int

  // locating player
  for (i <- 0 until gameBoardRows; j <- 0 until gameBoardCols)
    if (gameBoard(i)(j) == 'S')
      playerPosX = j
      playerPosY = i

  def move(): Unit =
    if (inBounds(playerPosY + moveY, playerPosX + moveX))
      gameBoard(playerPosY + moveY)(playerPosX + moveX) match
        case '–' | '.' =>
          movePlayer()
        case 'X' | 'O' =>
          boxPosX = playerPosX + moveX
          boxPosY = playerPosY + moveY
          moveBox()
          if (movedBox)
            movePlayer()
        case _ => ()

  def isMoved() : Boolean =
    movedPlayer

  private def movePlayer(): Unit =
    gameBoard(playerPosY)(playerPosX) = if (finalBoxLocations.exists((x,y) => x == playerPosX && y == playerPosY)) '.' else '–'
    gameBoard(playerPosY + moveY)(playerPosX + moveX) = 'S'
    movedPlayer = true

  private def moveBox(): Unit =
    if (inBounds(boxPosY + moveY, boxPosX + moveX))
      gameBoard(boxPosY + moveY)(boxPosX + moveX) match
        case '–' | '.' =>
          if (gameBoard(boxPosY)(boxPosX) == 'O')
            gameBoard(boxPosY)(boxPosX) = '.'
          else
            gameBoard(boxPosY)(boxPosX) = '–'
          gameBoard(boxPosY + moveY)(boxPosX + moveX) = if (finalBoxLocations.exists((x,y) => x == boxPosX + moveX && y == boxPosY + moveY)) 'O' else 'X'
          movedBox = true
        case _ => ()

  private def undoMoveBox(): Unit =
    gameBoard(boxPosY)(boxPosX) = if (finalBoxLocations.exists((x,y) => x == boxPosX && y == boxPosY)) 'O' else 'X'
    gameBoard(boxPosY + moveY)(boxPosX + moveX) = if (finalBoxLocations.exists((x,y) => x == boxPosX + moveX && y == boxPosY + moveY)) '.' else '–'

  private def undoMovePlayer(): Unit =
    gameBoard(playerPosY)(playerPosX) = 'S'
    gameBoard(playerPosY + moveY)(playerPosX + moveX) = if (finalBoxLocations.exists((x,y) => x == playerPosX + moveX && y == playerPosY + moveY)) '.' else '–'

  def undoMove(): Unit =
    if (movedPlayer)
      undoMovePlayer()
    if (movedBox)
      undoMoveBox()

  private def inBounds(row : Int, col : Int) : Boolean =
    if (row >= 0 && row < gameBoardRows
        && col >= 0 && col < gameBoardCols)
      true
    else
      false
}

class MovePlayerLeft(gameBoard: Array[Array[Char]], finalBoxLocations: List[(Int, Int)]) extends MovePlayer(gameBoard, finalBoxLocations) {
  override val moveX : Int = -1
  override val moveY : Int = 0
}

class MovePlayerRight(gameBoard: Array[Array[Char]], finalBoxLocations: List[(Int, Int)]) extends MovePlayer(gameBoard, finalBoxLocations) {
  override val moveX: Int = 1
  override val moveY: Int = 0
}

class MovePlayerUp(gameBoard: Array[Array[Char]], finalBoxLocations: List[(Int, Int)]) extends MovePlayer(gameBoard, finalBoxLocations) {
  override val moveX: Int = 0
  override val moveY: Int = -1
}

class MovePlayerDown(gameBoard: Array[Array[Char]], finalBoxLocations: List[(Int, Int)]) extends MovePlayer(gameBoard, finalBoxLocations) {
  override val moveX: Int = 0
  override val moveY: Int = 1
}

class MoveHistory {
  private var history = Stack[MovePlayer]()
  def push(command: MovePlayer) =
    history.push(command)

  def pop() : MovePlayer =
    if (history.size != 0)
      history.pop()
    else
      null
}
