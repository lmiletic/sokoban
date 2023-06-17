package game.sokoban

import org.cosplay.games.*
import org.cosplay.*
import org.cosplay.CPColor.*
import org.cosplay.CPArrayImage.*
import org.cosplay.prefabs.shaders.*
import org.cosplay.prefabs.sprites.*
import org.cosplay.CPPixel.*
import org.cosplay.CPKeyboardKey.*

/*
   _________            ______________
   __  ____/_______________  __ \__  /_____ _____  __
   _  /    _  __ \_  ___/_  /_/ /_  /_  __ `/_  / / /
   / /___  / /_/ /(__  )_  ____/_  / / /_/ /_  /_/ /
   \____/  \____//____/ /_/     /_/  \__,_/ _\__, /
                                            /____/

          2D ASCII JVM GAME ENGINE FOR SCALA3
              (C) 2021 Rowan Games, Inc.
                All rights reserved.
*/

/**
  * Snake game title scene.
  */
object SokobanMainMenuScene extends CPScene("title", None, BG_PX):
  private val introSnd = CPSound("sounds/games/snake/intro.wav", 0.5f)
  private val logoImg = CPImage.loadRexXp("images/games/snake/snake_logo.xp").trimBg()
  private val fadeInShdr = CPSlideInShader.sigmoid(
    CPSlideDirection.LEFT_TO_RIGHT,
    true,
    3000,
    BG_PX,
    onFinish = _ => eyesShdr.start()
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
  private val eyesShdr = CPShimmerShader(false, CS, keyFrame = 7, false, (zpx, _, _) => zpx.px.char != '8')

  // Add scene objects...
  addObjects(
    // Main logo.
    CPCenteredImageSprite(img = logoImg, 1, shaders = Seq(eyesShdr)),
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
    eyesShdr.stop()
    stopBgAudio()

end SokobanMainMenuScene