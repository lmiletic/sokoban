package game.sokoban

import scala.collection.mutable.Stack

abstract class MovePlayer(val gameBoard: Array[Array[Char]]) {

  protected val gameBoardRows = gameBoard.size
  protected val gameBoardCols = gameBoard(0).size

  protected var playerPosX = 0
  protected var playerPosY = 0
  protected var movedPlayer = false
  //abstract protected var movedBoxPosition : Int

  // locating player
  for (i <- 0 until gameBoardRows; j <- 0 until gameBoardCols)
    if (gameBoard(i)(j) == 'S')
      playerPosX = j
      playerPosY = i

  def move() : Unit
  def undoMove() : Unit

  protected def inBounds(row : Int, col : Int) : Boolean =
    if (row >= 0 && row < gameBoardRows
        && col >= 0 && col < gameBoardCols)
      true
    else
      false
}

class MovePlayerLeft(gameBoard: Array[Array[Char]]) extends MovePlayer(gameBoard) {
  def move(): Unit =
    println(gameBoard(playerPosY)(playerPosX - 1))
    if (inBounds(playerPosY, playerPosX - 1) && gameBoard(playerPosY)(playerPosX - 1) == '–')
      gameBoard(playerPosY)(playerPosX) = '–'
      gameBoard(playerPosY)(playerPosX - 1) = 'S'
      movedPlayer = true
    else
      movedPlayer = false

  def undoMove() : Unit =
    if (movedPlayer)
      gameBoard(playerPosY)(playerPosX) = 'S'
      gameBoard(playerPosY)(playerPosX - 1) = '–'
}

class MovePlayerRight(gameBoard: Array[Array[Char]]) extends MovePlayer(gameBoard) {
  def move(): Unit =
    println(gameBoard(playerPosY)(playerPosX + 1))
    if (inBounds(playerPosY, playerPosX + 1) && gameBoard(playerPosY)(playerPosX + 1) == '–')
      gameBoard(playerPosY)(playerPosX) = '–'
      gameBoard(playerPosY)(playerPosX + 1) = 'S'
      movedPlayer = true
    else
      movedPlayer = false

  def undoMove() : Unit =
    if (movedPlayer)
      gameBoard(playerPosY)(playerPosX) = 'S'
      gameBoard(playerPosY)(playerPosX + 1) = '–'
}

class MovePlayerUp(gameBoard: Array[Array[Char]]) extends MovePlayer(gameBoard) {
  def move(): Unit =
    println(gameBoard(playerPosY - 1)(playerPosX))
    if (inBounds(playerPosY - 1, playerPosX) && gameBoard(playerPosY - 1)(playerPosX) == '–')
      gameBoard(playerPosY)(playerPosX) = '–'
      gameBoard(playerPosY - 1)(playerPosX) = 'S'
      movedPlayer = true
    else
      movedPlayer = false

  def undoMove() =
    if (movedPlayer)
      gameBoard(playerPosY)(playerPosX) = 'S'
      gameBoard(playerPosY - 1)(playerPosX) = '–'
}

class MovePlayerDown(gameBoard: Array[Array[Char]]) extends MovePlayer(gameBoard) {
  def move(): Unit =
    println(gameBoard(playerPosY + 1)(playerPosX))
    if (inBounds(playerPosY + 1, playerPosX) && gameBoard(playerPosY + 1)(playerPosX) == '–')
      gameBoard(playerPosY)(playerPosX) = '–'
      gameBoard(playerPosY + 1)(playerPosX) = 'S'
      movedPlayer = true
    else
      movedPlayer = false

  def undoMove() =
    if (movedPlayer)
      gameBoard(playerPosY)(playerPosX) = 'S'
      gameBoard(playerPosY + 1)(playerPosX) = '–'
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
