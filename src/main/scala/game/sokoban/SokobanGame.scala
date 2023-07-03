package game.sokoban

import org.cosplay.games.*
import org.cosplay.*
import org.cosplay.CPColor.*
import org.cosplay.CPPixel.*
import org.cosplay.CPKeyboardKey.*
import org.cosplay.prefabs.scenes.CPFadeShimmerLogoScene

val BLUE_BLACK = CPColor("0x00000F")
val BG_PX = ' '&&(BLUE_BLACK, BLUE_BLACK) // Background pixel.
var audioOn = true // By default, the audio is ON.

object SokobanGame {
  def main(args: Array[String]): Unit = {
    val gameInfo = CPGameInfo(name = "Sokoban")

    // Initialize the engine.
    CPEngine.init(gameInfo, System.console() == null || args.contains("emuterm"))

    // Start the game & wait for exit.
    try
      CPEngine.startGame(
        CPFadeShimmerLogoScene("logo", None, BG_PX, CS, "menu"),
        SokobanMainMenuScene
      )
    finally CPEngine.dispose()

    sys.exit(0)
  }
}


