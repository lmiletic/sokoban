package game.sokoban

import scala.annotation.tailrec
import scala.collection.mutable.HashMap
import scala.collection.{SortedSet, mutable}
import scala.collection.mutable.Queue

class SokobanSolver(gameBoard : Array[Array[Char]], finalBoxLocations : List[(Int, Int)]) {

  private val moveHistory = MoveHistory()
  private def checkWin(): Boolean =
    var win = true
    for (i <- finalBoxLocations.indices)
      if (gameBoard(finalBoxLocations(i)._2)(finalBoxLocations(i)._1) != 'O')
        win = false
    win

  private def movePlayer(command: MovePlayer) =
    command.move()
    if (command.isMoved())
      moveHistory.push(command)

  private def generateCommand(id : Int) : MovePlayer = id match {
    case 0 => MovePlayerUp(gameBoard, finalBoxLocations)
    case 1 => MovePlayerDown(gameBoard, finalBoxLocations)
    case 2 => MovePlayerLeft(gameBoard, finalBoxLocations)
    case 3 => MovePlayerRight(gameBoard, finalBoxLocations)
  }
  private def solve(command : MovePlayer, maxDepth : Int) : Boolean =
    if (maxDepth == 0)
      return false
    movePlayer(command)
    if (!command.isMoved())
      return false
    if (checkWin())
      return true

    var solved = false
    for(i <- 0 to 3)
      solved = solve(generateCommand(i), maxDepth - 1)
      if (solved)
        return true

    moveHistory.pop().undoMove()
    false

  def solve(): MoveHistory =
    var solved = false
    for (i <- 0 to 3)
      println(i)
      solved = solve(generateCommand(i), 100)
      if (solved)
        return moveHistory
    null


}
