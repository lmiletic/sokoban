package game.sokoban

import org.cosplay.games.*
import org.cosplay.*
import org.cosplay.CPColor.*
import org.cosplay.CPArrayImage.*
import org.cosplay.prefabs.shaders.*
import org.cosplay.prefabs.sprites.*
import org.cosplay.CPPixel.*
import org.cosplay.CPKeyboardKey.*

object SokobanMainMenuScene extends CPScene("menu", None, BG_PX):
  private val introSnd = CPSound("sounds/games/snake/intro.wav", 0.5f)
  private val logoImg = CPImage.loadRexXp("src/main/resources/sokoban_logo.xp").trimBg()
  private val fadeInShdr = CPSlideInShader.sigmoid(
    CPSlideDirection.LEFT_TO_RIGHT,
    true,
    3000,
    BG_PX,
    onFinish = _ =>
      sqBracLeftShdr.start()
      sqBracRightShdr.start()
  )
  private val starStreakShdr = CPStarStreakShader(
    true,
    BG_PX.bg.get,
    Seq(
      CPStarStreak('.', CS, 0.025, 30, (-.5f, 0f), 0),
      CPStarStreak('.', CS, 0.015, 25, (-1.5f, 0f), 0),
      CPStarStreak('_', CS, 0.005, 50, (-2.0f, 0f), 0)
    ),
    skip = (zpx, _, _) => zpx.z == 1
  )
  private val fadeOutShdr = CPFadeOutShader(true, 500, BG_PX)
  private val sqBracLeftShdr = CPShimmerShader(false, CS, keyFrame = 7, false, (zpx, _, _) => zpx.px.char != '[')
  private val sqBracRightShdr = CPShimmerShader(false, CS, keyFrame = 7, false, (zpx, _, _) => zpx.px.char != ']')

  // Add scene objects...
  addObjects(
    // Main logo.
    CPCenteredImageSprite(img = logoImg, 1, shaders = Seq(sqBracLeftShdr, sqBracRightShdr)),
    // Off screen sprite since shaders are applied to entire screen.
    new CPOffScreenSprite(shaders = Seq(fadeInShdr, starStreakShdr, fadeOutShdr)),
    // Exit on 'Q' press.
    CPKeyboardSprite(KEY_LO_Q, _.exitGame()),
    // Toggle audio on 'CTRL+A' press.
    CPKeyboardSprite(KEY_CTRL_A, _ => toggleAudio()),
    // Transition to the next scene on 'Enter' press fixing the dimension.
    CPKeyboardSprite(KEY_ENTER, ctx =>
      if !fadeOutShdr.isActive then
        fadeOutShdr.start(_.addScene(new SokobanPlayScene(ctx.getCanvas.dim), true))
    )
  )

  private def startBgAudio(): Unit = introSnd.loop(2000)
  private def stopBgAudio(): Unit = introSnd.stop(400)

  /**
    * Toggles audio on and off.
    */
  private def toggleAudio(): Unit =
    if audioOn then
      stopBgAudio()
      audioOn = false
    else
      startBgAudio()
      audioOn = true

  override def onActivate(): Unit =
    // Reset the shaders.
    fadeInShdr.start()
    starStreakShdr.start()
    if audioOn then startBgAudio()

  override def onDeactivate(): Unit =
    // Stop shaders.
    starStreakShdr.stop()
    sqBracLeftShdr.stop()
    sqBracRightShdr.stop()
    stopBgAudio()

end SokobanMainMenuScene