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

class SokobanPlayScene(dim : CPDim) extends CPScene("play", dim.?, BG_PX):

  // Shaders.
  private val fadeInShdr = CPFadeInShader(entireFrame = true, 500, BG_PX)
  private val fadeOutShdr = CPFadeOutShader(entireFrame = true, 500, BG_PX)

  private val score = 0
  private var gameBoard: Array[Array[Char]] = _
  private var gameBoardRows = 0
  private var gameBoardCols = 0
  private val DARK_RED = CPColor("0x0903749")
  private val DARK_BLUE = CPColor("0x02B2E4A")

  private var xOffset = 15
  private var yOffset = 15

  private val borderPx = ' '&&(DARK_RED, DARK_RED)
  private val finalBoxPositionPx = ' '&&(C_YELLOW, C_YELLOW)
  private val scorePx = ' '&&(DARK_BLUE, DARK_BLUE)
  private val playerPx = ' '&&(DARK_RED, DARK_RED)
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

  private val borderSpr = new CPCanvasSprite :
    override def render(ctx: CPSceneObjectContext): Unit =
      val canv = ctx.getCanvas
      // Draw border
      canv.drawRect(0, scoreH, CPDim(canv.w, canv.h - scoreH), 1, (_, _) => borderPx)
      canv.drawLine(1, scoreH + 1, 1, canv.h, 1, borderPx)
      canv.drawLine(canv.w - 2, scoreH + 1, canv.w - 2, canv.h, 1, borderPx)
      // Draw score rectangle fill
      canv.fillRect(0, 0, canv.w, scoreH - 1, 1, (_, _) => scorePx)

  private val levelSpr = new CPCanvasSprite :
    override def update(ctx: CPSceneObjectContext): Unit =
      super.update(ctx)
      ctx.getKbEvent match
        case Some(evt) =>
          evt.key match
            case KEY_LO_W | KEY_UP => gameBoard = MovePlayerUp(gameBoard).move(); printGameBoard();// make it as command and execute below
            case KEY_LO_S | KEY_DOWN => gameBoard = MovePlayerDown(gameBoard).move(); printGameBoard();
            case KEY_LO_A | KEY_LEFT => gameBoard = MovePlayerLeft(gameBoard).move(); printGameBoard();
            case KEY_LO_D | KEY_RIGHT => gameBoard = MovePlayerRight(gameBoard).move(); printGameBoard();
            case _ => ()
        case None => ()

    override def render(ctx: CPSceneObjectContext): Unit =
      for (i <- 0 until gameBoardRows; j <- 0 until gameBoardCols)
        gameBoard(i)(j) match
          case '#' => drawOneField((j + xOffset, i + yOffset), borderPx, ctx)
          case '.' => drawOneField((j + xOffset, i + yOffset), finalBoxPositionPx, ctx)
          case 'S' => drawOneField((j + xOffset,i + yOffset), playerPx.withChar('*').withFg(C_BLACK), ctx)
          case _ => ()

  private def loadLevel(): Unit =
    using(io.Source.fromFile("src/main/resources/lvl1.txt")){ source =>
      val lines: List[String] = source.getLines().toList
      gameBoard = lines.map(_.toCharArray).toArray
      gameBoardRows = gameBoard.size
      gameBoardCols = gameBoard(0).size //handle empty file
      // calculate proper x,y offset
      printGameBoard()
    }

  private def printGameBoard() : Unit =
    for (i <- 0 until gameBoardRows)
      for(j <-0 until gameBoardCols)
        print(gameBoard(i)(j))
      println()


  override def onActivate(): Unit =
    super.onActivate()
    loadLevel()


  addObjects(
    new CPOffScreenSprite(Seq(fadeInShdr, fadeOutShdr)),
    // Handle 'Q' press globally for this scene.
    CPKeyboardSprite(KEY_LO_Q, _.exitGame()),
    scoreSpr,
    borderSpr,
    levelSpr
  )
end SokobanPlayScene
