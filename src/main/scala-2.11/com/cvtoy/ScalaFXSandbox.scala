package com.cvtoy

import scala.util.Random
import scalafx.animation.AnimationTimer
import scalafx.application.JFXApp
import scalafx.scene.{Group, Scene}
import scalafx.scene.paint.Color
import scalafx.scene.shape.Rectangle

/**
 * Created by haha on 2015/8/9.
 */
object ScalaFXSandbox extends JFXApp { self =>
  lazy val random = new Random()
  lazy val STAR_COUNT = 20000
  lazy val nodes = new Array[Rectangle](STAR_COUNT)
  lazy val angles = new Array[Double](STAR_COUNT)
  lazy val start = new Array[Long](STAR_COUNT)

  for(i <- 0 until STAR_COUNT){
    nodes(i) = Rectangle(1, 1, Color.White)
    angles(i) = 2.0 * Math.PI * random.nextDouble
    start(i) = random.nextInt(2000000000)
  }

  stage = new JFXApp.PrimaryStage {
    scene = new Scene(800, 600) {
      fill = Color.Black
      content = new Group {
        children = nodes
      }
    }
  }

  val timer = AnimationTimer {
    (now: Long) => {
      val width: Double = 0.5 * stage.getWidth
      val height: Double = 0.5 * stage.getHeight
      val radius: Double = Math.sqrt(2) * Math.max(width, height)
      for(i <- 0 until STAR_COUNT){
        val node = nodes(i)
        val angle: Double = angles(i)
        val t: Long = (now - self.start(i)) % 2000000000
        val d: Double = t * radius / 2000000000.0
        node.setTranslateX(Math.cos(angle) * d + width)
        node.setTranslateY(Math.sin(angle) * d + height)
      }
    }
  }

  timer.start()
}
