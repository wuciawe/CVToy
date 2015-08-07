package com.cvtoy

import javax.swing.JFrame._

import org.bytedeco.javacv.{FrameGrabber, CanvasFrame}

import scala.util.Try

/**
 * Created by haha on 2015/8/7.
 */
object CameraCapture {
  def main(args: Array[String]) {
    val canvas = new CanvasFrame("Camera", 1)
    canvas.setDefaultCloseOperation(EXIT_ON_CLOSE)
    val grabber = FrameGrabber.createDefault(0)
    grabber.start()
    println("grabber started")
    while(true){
      Try{
        val img = grabber.grab()
        canvas.setCanvasSize(grabber.getImageWidth, grabber.getImageHeight)
        if(img != null){
          canvas.showImage(img)
        }
        println("success")
      }.recover{
        case _ =>
          println("failed")
          Thread.sleep(1000)
      }

    }
  }
}
