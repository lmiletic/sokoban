package game.sokoban

abstract class MovePlayer(val gameBoard: Array[Array[Char]]) {

  protected val gameBoardRows = gameBoard.size
  protected val gameBoardCols = gameBoard(0).size

  protected var playerPosX = 0
  protected var playerPosY = 0
  //abstract protected var movedBoxPosition : Int

  // locating player
  for (i <- 0 until gameBoardRows; j <- 0 until gameBoardCols)
    if (gameBoard(i)(j) == 'S')
      playerPosX = j
      playerPosY = i

  def move(): Array[Array[Char]]
  protected def inBounds(row : Int, col : Int) : Boolean =
    if (row >= 0 && row < gameBoardRows
        && col >= 0 && col < gameBoardCols)
      true
    else
      false
}

class MovePlayerLeft(gameBoard: Array[Array[Char]]) extends MovePlayer(gameBoard) {
  def move(): Array[Array[Char]] =
    if (inBounds(playerPosY, playerPosX + 1) && gameBoard(playerPosY)(playerPosX + 1) == '–')
      gameBoard(playerPosY)(playerPosX) = '–'
      gameBoard(playerPosY)(playerPosX + 1) = 'S'
    gameBoard
}

class MovePlayerRight(gameBoard: Array[Array[Char]]) extends MovePlayer(gameBoard) {
  def move(): Array[Array[Char]] =
    if (inBounds(playerPosY, playerPosX - 1) && gameBoard(playerPosY)(playerPosX - 1) == '–')
      gameBoard(playerPosY)(playerPosX) = '–'
      gameBoard(playerPosY)(playerPosX - 1) = 'S'
    gameBoard
}

class MovePlayerUp(gameBoard: Array[Array[Char]]) extends MovePlayer(gameBoard) {
  def move(): Array[Array[Char]] =
    if (inBounds(playerPosY - 1, playerPosX) && gameBoard(playerPosY - 1)(playerPosX) == '–')
      gameBoard(playerPosY)(playerPosX) = '–'
      gameBoard(playerPosY - 1)(playerPosX) = 'S'
    gameBoard
}

class MovePlayerDown(gameBoard: Array[Array[Char]]) extends MovePlayer(gameBoard) {
  def move(): Array[Array[Char]] =
    if (inBounds(playerPosY + 1, playerPosX) && gameBoard(playerPosY + 1)(playerPosX) == '-')
      gameBoard(playerPosY)(playerPosX) = '-'
      gameBoard(playerPosY + 1)(playerPosX) = 'S'
    gameBoard
}
