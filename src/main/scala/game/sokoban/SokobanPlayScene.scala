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
import java.io._

class SokobanPlayScene(dim : CPDim, lvl : String) extends CPScene("play", dim.?, BG_PX):

  // Shaders.
  private val fadeInShdr = CPFadeInShader(entireFrame = true, 500, BG_PX)
  private val fadeOutShdr = CPFadeOutShader(entireFrame = true, 500, BG_PX)

  private var gameOver = false
  private var saveMenu = false

  private var gameBoard: Array[Array[Char]] = _
  private var gameBoardRows = 0
  private var gameBoardCols = 0
  private var finalBoxLocations: List[(Int, Int)] = List()

  private var xOffset = 15
  private var yOffset = 15

  private final val DARK_RED = CPColor("0x0903749")
  private final val DARK_BLUE = CPColor("0x02B2E4A")
  private final val LIGHT_BLUE = CPColor("0x0337CCF")
  private final val BROWN = CPColor("0x0873600")

  private final val legendPx = ' '&&(DARK_BLUE, DARK_BLUE)
  private final val borderPx = ' '&&(DARK_RED, DARK_RED)
  private final val wallPx = (' '&&(C_BLACK, C_GRAY6))
  private final val boxPxLeft = (' ' &&(BROWN, BROWN)).withFg(C_BLACK).withChar('>')
  private final val boxPxRight = (' ' &&(BROWN, BROWN)).withFg(C_BLACK).withChar('<')
  private final val finalBoxPositionPx = ' '&&(C_YELLOW, C_YELLOW)
  private final val playerPxLeft = (' '&&(LIGHT_BLUE, LIGHT_BLUE)).withFg(C_YELLOW).withChar(':')
  private final val playerPxRight = (' '&&(LIGHT_BLUE, LIGHT_BLUE)).withFg(C_YELLOW).withChar('D')

  private def prepDialog(art: String): CPArrayImage =
    new CPArrayImage(
      prepSeq(art),
      (ch, _, _) => ch match
        case '*' => ' ' && (C2, C2)
        case c if c.isLetter || c == '/' => c && (C4, BG_PX.bg.get)
        case _ => ch && (C3, BG_PX.bg.get)
    )

  private val centralShdr = CPSlideInShader.sigmoid(LEFT_TO_RIGHT, false, 1000, BG_PX)

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
      |**    [ESC]    Back             **
      |**                              **
      |**********************************
          """
  )

  private val saveKeyMap = Map(KEY_1 -> 1, KEY_2 -> 2, KEY_3 -> 3, KEY_4 -> 4, KEY_5 -> 5)
  private val saveSpr = new CPCenteredImageSprite(img = saveMenuImg, z = 6, shaders = centralShdr.seq)

  private val invalidLevelImg = prepDialog(
    """
      |**********************************
      |**                              **
      |**      WRONG LEVEL FORMAT      **
      |**     --------------------     **
      |**     Ensure that selected     **
      |**     file has proper level    **
      |**     format!                  **
      |**                              **
      |**   [ESC]  Back to Main Menu   **
      |**                              **
      |**                              **
      |**********************************
          """
  )

  private val invalidLevelSpr = new CPCenteredImageSprite(img = invalidLevelImg, z = 6, shaders = centralShdr.seq)

  private val moveHistory = MoveHistory()
  private val legendImg = CPImage.loadRexXp("src/main/resources/legend.xp").trimBg()

  private val legendSpr = new CPImageSprite(x = 0, y = 0, z = 1, img = legendImg) :
    override def update(ctx: CPSceneObjectContext): Unit =
      val canv = ctx.getCanvas
      setX((canv.w - getImage.w) / 2)

  private val legendH = legendSpr.getHeight

  private def drawOneField(xy: (Int, Int), ctx: CPSceneObjectContext, pxLeft: CPPixel, pxRight: CPPixel = null): Unit =
    val canv = ctx.getCanvas
    val pxL = pxLeft
    val pxR = if (pxRight == null) pxLeft else pxRight

    canv.drawPixel(pxL, xy._1 * 2, xy._2, 2)
    canv.drawPixel(pxR, xy._1 * 2 + 1, xy._2, 2)

  private def movePlayer(command : MovePlayer) =
    command.move()
    if (command.isMoved())
      moveHistory.push(command)

  private def undo() : Unit =
    val command = moveHistory.pop()
    if (command != null)
      command.undoMove()

  private def movementCommands(ctx: CPSceneObjectContext) : Unit =
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

  private def saveCommands(ctx: CPSceneObjectContext) : Unit =
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

  private def calculateOffset(ctx: CPSceneObjectContext): Unit =
    val canv = ctx.getCanvas
    xOffset = ((canv.w /2) - gameBoardCols) / 2
    yOffset = (canv.h + legendH - gameBoardRows) / 2

  private val borderSpr = new CPCanvasSprite :
    override def render(ctx: CPSceneObjectContext): Unit =
      val canv = ctx.getCanvas
      // Draw border
      canv.drawRect(0, legendH, CPDim(canv.w, canv.h - legendH), 1, (_, _) => borderPx)
      canv.drawLine(1, legendH + 1, 1, canv.h, 1, borderPx)
      canv.drawLine(canv.w - 2, legendH + 1, canv.w - 2, canv.h, 1, borderPx)
      // Draw legend rectangle fill
      canv.fillRect(0, 0, canv.w, legendH - 1, 1, (_, _) => legendPx)

  private val gameSpr = new CPCanvasSprite :
    override def update(ctx: CPSceneObjectContext): Unit =
      super.update(ctx)
      if (!gameOver && !saveMenu)
        movementCommands(ctx)
      if(saveMenu)
        saveCommands(ctx)

    override def render(ctx: CPSceneObjectContext): Unit =
      calculateOffset(ctx)
      for (i <- 0 until gameBoardRows; j <- 0 until gameBoardCols)
        gameBoard(i)(j) match
          case '#' => drawOneField((j + xOffset, i + yOffset), ctx, wallPx)
          case '.' => drawOneField((j + xOffset, i + yOffset), ctx, finalBoxPositionPx)
          case 'S' => drawOneField((j + xOffset, i + yOffset), ctx, playerPxLeft, playerPxRight)
          case 'X' | 'O' => drawOneField((j + xOffset, i + yOffset), ctx, boxPxLeft, boxPxRight)
          case _ => ()

  private def showInvalidLevel() : Unit =
    gameOver = true
    gameSpr.hide()
    invalidLevelSpr.show()

  private def checkForInvalidLevel() : Boolean =
    var invalid = false
    var numberOfPlayers = 0
    var numberOfBoxes = 0
    var numberOfFinalPositions = 0

    // Map is too big it can not fit on the screen, game limitation
    if (gameBoardRows > 40 || gameBoardCols > 40)
      invalid = true

    if (!invalid)
      for (i <- 0 until gameBoardRows; j <- 0 until gameBoardCols)
        gameBoard(i)(j) match
          case 'S' => numberOfPlayers += 1
          case 'X' => numberOfBoxes += 1
          case '.' =>
            numberOfFinalPositions += 1
            finalBoxLocations :+= (j, i)
          case 'O' =>
            numberOfBoxes += 1
            numberOfFinalPositions += 1
            finalBoxLocations :+= (j, i)
          case '#' | '–' => ()
          case _ => invalid = true
      if (numberOfPlayers != 1 || numberOfBoxes == 0 || numberOfBoxes != numberOfFinalPositions)
        invalid = true
    invalid

  private def loadLevel(): Unit =
    val source = io.Source.fromFile(lvl)
    try {
      val lines: List[String] = source.getLines().toList
      gameBoard = lines.map(_.toCharArray).toArray
      gameBoardRows = gameBoard.length
      gameBoardCols = gameBoard(0).length

      if (checkForInvalidLevel())
        showInvalidLevel()
      else
        checkWin()
    }
    catch {
      case e : Exception => showInvalidLevel()
    }
    finally {
      source.close()
    }

  private def loadMoves(): Unit =
    val source = io.Source.fromFile("src/main/resources/moves.txt")
    try {
      val lines: List[String] = source.getLines().toList
      for (command <- lines)
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
    finally {
      source.close()
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

    for(i <- finalBoxLocations.indices)
      if (gameBoard(finalBoxLocations(i)._2)(finalBoxLocations(i)._1) != 'O')
        win = false

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
    invalidLevelSpr.hide()
    loadLevel()

  addObjects(
    new CPOffScreenSprite(Seq(fadeInShdr, fadeOutShdr)),
    // Handle 'Q' press globally for this scene.
    CPKeyboardSprite(KEY_LO_Q, _.exitGame()),
    CPKeyboardSprite(KEY_ESC, _ =>
      if (saveMenu)
        saveSpr.hide()
        saveMenu = false
      else
        fadeOutShdr.start(ctx => ctx.switchScene("menu", true))
    ),
    legendSpr,
    borderSpr,
    gameSpr,
    youWonSpr,
    saveSpr,
    invalidLevelSpr
  )
end SokobanPlayScene
