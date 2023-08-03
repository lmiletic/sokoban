package game.sokoban

import org.cosplay.*
import org.cosplay.games.*
import org.cosplay.prefabs.shaders.*
import org.cosplay.prefabs.shaders.CPSlideDirection.*
import org.cosplay.CPFIGLetFont.*
import org.cosplay.CPArrayImage.*
import org.cosplay.CPPixel.*
import org.cosplay.CPColor.*
import org.cosplay.CPKeyboardKey.*
import org.cosplay.prefabs.particles.CPConfettiEmitter
import org.cosplay.prefabs.particles.*
import org.cosplay.prefabs.sprites.{CPBubbleSprite, CPCenteredImageSprite}

import Control._
import java.io._

class SokobanPlayScene(dim : CPDim, lvl : String) extends CPScene("play", dim.?, BG_PX):

  // Shaders.
  private val fadeInShdr = CPFadeInShader(entireFrame = true, 500, BG_PX)
  private val fadeOutShdr = CPFadeOutShader(entireFrame = true, 500, BG_PX)

  private var score = 0
  private var gameOver = false
  private var saveMenu = false
  private var gameBoard: Array[Array[Char]] = _
  private var gameBoardRows = 0
  private var gameBoardCols = 0
  private var finalBoxLocations: List[(Int, Int)] = List()

  private val DARK_RED = CPColor("0x0903749")
  private val DARK_BLUE = CPColor("0x02B2E4A")
  private val DUSICA_ROSE = CPColor("0x0FFCFDF")
  private val SKY_BLUE = CPColor("0x05A96E3")

  // should be calculated
  private var xOffset = 15
  private var yOffset = 15

  private val borderPx = ' '&&(DARK_RED, DARK_RED)
  private val finalBoxPositionPx = ' '&&(C_YELLOW, C_YELLOW)
  private val scorePx = ' '&&(DARK_BLUE, DARK_BLUE)
  private val boxPx = ' ' &&(SKY_BLUE, SKY_BLUE)
  private val boxOnFinalPositionPx = ' '&&(C_RED, C_RED)
  private val playerPx = ' '&&(DUSICA_ROSE, DUSICA_ROSE)

  private def prepDialog(art: String): CPArrayImage =
    new CPArrayImage(
      prepSeq(art),
      (ch, _, _) => ch match
        case '*' => ' ' && (C2, C2)
        case c if c.isLetter || c == '/' => c && (C4, BG_PX.bg.get)
        case _ => ch && (C3, BG_PX.bg.get)
    )

  private val youWonImg = prepDialog(
    """
      |**********************************
      |**                              **
      |**          YOU WON :-)         **
      |**          -----------         **
      |**                              **
      |**    [ESC]     Main Menu       **
      |**    [Q]       Quit            **
      |**    [CTRL+A]  Audio On/OFF    **
      |**    [CTRL+Q]  FPD Overlay     **
      |**    [CTRL+L]  Log Console     **
      |**                              **
      |**********************************
          """
  )

  private val centralShdr = CPSlideInShader.sigmoid(LEFT_TO_RIGHT, false, 1000, BG_PX)
  private val youWonSpr = new CPCenteredImageSprite(img = youWonImg, z = 6, shaders = centralShdr.seq)

  private val saveMenuImg = prepDialog(
    """
      |**********************************
      |**                              **
      |**          SAVE GAME           **
      |**         -----------          **
      |**                              **
      |**    [1]    Slot 1             **
      |**    [2]    Slot 2             **
      |**    [3]    Slot 3             **
      |**    [4]    Slot 4             **
      |**    [5]    Slot 5             **
      |**                              **
      |**********************************
          """
  )

  private val saveKeyMap = Map(KEY_1 -> 1, KEY_2 -> 2, KEY_3 -> 3, KEY_4 -> 4, KEY_5 -> 5)
  private val saveSpr = new CPCenteredImageSprite(img = saveMenuImg, z = 6, shaders = centralShdr.seq)

  private val moveHistory = MoveHistory()
  private def mkScoreImage: CPImage = FIG_ANSI_REGULAR.render(s"SCORE : $score", C3).trimBg()

  private val scoreSpr = new CPImageSprite(x = 0, y = 0, z = 1, img = mkScoreImage) :
    override def update(ctx: CPSceneObjectContext): Unit =
      val canv = ctx.getCanvas
      setX((canv.w - getImage.w) / 2)
  private val scoreH = scoreSpr.getHeight

  private def drawOneField(xy: (Int, Int), px: CPPixel, ctx: CPSceneObjectContext): Unit =
    val canv = ctx.getCanvas
    canv.drawPixel(px, xy._1 * 2, xy._2, 2)
    canv.drawPixel(px, xy._1 * 2 + 1, xy._2, 2)

  private def movePlayer(command : MovePlayer) =
    command.move()
    if (command.isMoved())
      moveHistory.push(command)

  private def undo() =
    var command = moveHistory.pop()
    if (command != null)
      command.undoMove()

  private def movementCommands(ctx: CPSceneObjectContext) =
    var command: MovePlayer = null
    ctx.getKbEvent match
      case Some(evt) =>
        evt.key match
          case KEY_LO_W | KEY_UP =>
            movePlayer(MovePlayerUp(gameBoard, finalBoxLocations))
            printGameBoard()
            checkWin()
          case KEY_LO_S | KEY_DOWN =>
            movePlayer(MovePlayerDown(gameBoard, finalBoxLocations))
            printGameBoard()
            checkWin()
          case KEY_LO_A | KEY_LEFT =>
            movePlayer(MovePlayerLeft(gameBoard, finalBoxLocations))
            printGameBoard()
            checkWin()
          case KEY_LO_D | KEY_RIGHT =>
            movePlayer(MovePlayerRight(gameBoard, finalBoxLocations))
            printGameBoard()
            checkWin()
          case KEY_CTRL_Z => undo()
          case KEY_CTRL_S =>
            saveMenu = true
            saveSpr.show()
          case KEY_CTRL_X => loadMoves()
          case _ => ()
      case None => ()

  private def saveCommands(ctx: CPSceneObjectContext) =
    ctx.getKbEvent match
      case Some(evt) =>
        evt.key match
          case KEY_1 |
               KEY_2 |
               KEY_3 |
               KEY_4 |
               KEY_5 =>
            saveLevel(saveKeyMap(evt.key))
            saveSpr.hide()
            saveMenu = false
          case _ => ()
      case None => ()

  private val borderSpr = new CPCanvasSprite :
    override def render(ctx: CPSceneObjectContext): Unit =
      val canv = ctx.getCanvas
      // Draw border
      canv.drawRect(0, scoreH, CPDim(canv.w, canv.h - scoreH), 1, (_, _) => borderPx)
      canv.drawLine(1, scoreH + 1, 1, canv.h, 1, borderPx)
      canv.drawLine(canv.w - 2, scoreH + 1, canv.w - 2, canv.h, 1, borderPx)
      // Draw score rectangle fill
      canv.fillRect(0, 0, canv.w, scoreH - 1, 1, (_, _) => scorePx)

  private val gameSpr = new CPCanvasSprite :
    override def update(ctx: CPSceneObjectContext): Unit =
      super.update(ctx)
      if (!gameOver && !saveMenu)
        movementCommands(ctx)
      if(saveMenu)
        saveCommands(ctx)

    override def render(ctx: CPSceneObjectContext): Unit =
      for (i <- 0 until gameBoardRows; j <- 0 until gameBoardCols)
        gameBoard(i)(j) match
          case '#' => drawOneField((j + xOffset, i + yOffset), borderPx, ctx)
          case '.' => drawOneField((j + xOffset, i + yOffset), finalBoxPositionPx, ctx)
          case 'S' => drawOneField((j + xOffset,i + yOffset), playerPx, ctx)
          case 'X' => drawOneField((j + xOffset, i + yOffset), boxPx, ctx)
          case 'O' => drawOneField((j + xOffset, i + yOffset), boxOnFinalPositionPx, ctx)
          case _ => ()

  private def loadLevel(): Unit =
    using(io.Source.fromFile(lvl)){ source =>
      val lines: List[String] = source.getLines().toList
      gameBoard = lines.map(_.toCharArray).toArray
      gameBoardRows = gameBoard.size
      gameBoardCols = gameBoard(0).size //handle empty file
      // calculate proper x,y offset
      for (i <- 0 until gameBoardRows; j <- 0 until gameBoardCols)
        gameBoard(i)(j) match
          case 'O' | '.' =>
            finalBoxLocations :+= (j, i)
          case _ => ()
      printGameBoard()
    }

  private def loadMoves(): Unit =
    using(io.Source.fromFile("src/main/resources/moves.txt")) { source =>
      val lines: List[String] = source.getLines().toList
      for(command <- lines)
        command match
          case "U" =>
            movePlayer(MovePlayerUp(gameBoard, finalBoxLocations))
            printGameBoard()
            checkWin()
          case "D" =>
            movePlayer(MovePlayerDown(gameBoard, finalBoxLocations))
            printGameBoard()
            checkWin()
          case "L" =>
            movePlayer(MovePlayerLeft(gameBoard, finalBoxLocations))
            printGameBoard()
            checkWin()
          case "R" =>
            movePlayer(MovePlayerRight(gameBoard, finalBoxLocations))
            printGameBoard()
            checkWin()
          case _ => ()
    }

  private def saveLevel(slotNum : Int): Unit =
    val file = new File("src/main/resources/save/slot" + slotNum + ".txt")
    val bw = new BufferedWriter(new FileWriter(file))
    for (i <- 0 until gameBoardRows)
      gameBoard(i).toList.foreach(symbol => bw.write(symbol))
      bw.write('\n')
    bw.close()

  private def checkWin(): Unit =
    var win = true
    var winScore = 0

    for(i <- 0 until finalBoxLocations.size)
      if (gameBoard(finalBoxLocations(i)._2)(finalBoxLocations(i)._1) == 'O')
        winScore += 1
        win = win && true
      else
        win = false

    score = winScore
    scoreSpr.setImage(mkScoreImage)
    gameOver = win
    if (gameOver)
      youWonSpr.show()

  private def printGameBoard() : Unit =
    for (i <- 0 until gameBoardRows)
      for(j <-0 until gameBoardCols)
        print(gameBoard(i)(j))
      println()
    println()

  override def onActivate(): Unit =
    super.onActivate()
    youWonSpr.hide()
    saveSpr.hide()
    loadLevel()

  addObjects(
    new CPOffScreenSprite(Seq(fadeInShdr, fadeOutShdr)),
    // Handle 'Q' press globally for this scene.
    CPKeyboardSprite(KEY_LO_Q, _.exitGame()),
    CPKeyboardSprite(KEY_ESC, _ =>
      fadeOutShdr.start(ctx => ctx.switchScene("menu", true))
    ),
    scoreSpr,
    borderSpr,
    gameSpr,
    youWonSpr,
    saveSpr
  )
end SokobanPlayScene
