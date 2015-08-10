package com.cvtoy

import java.io.File
import org.bytedeco.javacpp.flandmark._
import org.bytedeco.javacpp.helper.opencv_core._
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_imgproc._
import org.bytedeco.javacpp.opencv_objdetect._
import org.bytedeco.javacv.{Java2DFrameConverter, FrameGrabber, OpenCVFrameConverter}
import scala.util.Try
import scalafx.animation.AnimationTimer
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.embed.swing.SwingFXUtils
import scalafx.geometry.Insets
import scalafx.scene.control.Button
import scalafx.scene.layout.{VBox, HBox}
import scalafx.scene.{Group, Scene}
import scalafx.scene.canvas.Canvas
import scalafx.Includes._

/**
 * Created by haha on 2015/8/9.
 */
object FXFaceDetector extends JFXApp {
  var last = 0.0

  def detectFaceInImage(input: IplImage, cascade: CvHaarClassifierCascade, model: FLANDMARK_Model, bbox: Array[Int], landmarks: Array[Double]): Option[IData] = {
    val storage = cvCreateMemStorage(0)
    cvClearMemStorage(storage)

    val search_scale_factor = 1.1
    val flags = CV_HAAR_DO_CANNY_PRUNING
    val minFeatureSize = cvSize(40, 40)

    val t = Try {
      val rects = cvHaarDetectObjects(input, cascade, storage, search_scale_factor, 2, flags, minFeatureSize, cvSize(0, 0))
      val faces = (0 until rects.total).map { iface =>
        val elem = cvGetSeqElem(rects, iface)
        val rect = new CvRect(elem)
        bbox(0) = rect.x
        bbox(1) = rect.y
        bbox(2) = rect.x + rect.width
        bbox(3) = rect.y + rect.height
        flandmark_detect(input, bbox, model, landmarks)

        val face = IFace(IPoint(bbox(0), bbox(1)), IPoint(bbox(2), bbox(3))) // CV_RGB(255, 0, 0)
        val facebound = IFaceBound(IPoint(model.bb.get(0).toInt, model.bb.get(1).toInt), IPoint(model.bb.get(2).toInt, model.bb.get(3).toInt)) // CV_RGB(0, 0, 255)
        val nose = INose(IPoint(landmarks(0).toInt, landmarks(1).toInt)) // 3, CV_RGB(0, 0, 255), CV_FILLED, 8, 0
        val points = IPoints((2 until 2 * model.data().options().M() by 2).map(i => IPoint(landmarks(i).toInt, landmarks(i + 1).toInt)).toList) // 3, CV_RGB(255, 0, 0), CV_FILLED, 8, 0
        IFaceData(face, facebound, nose, points)
      }
      IData(faces.toList)
    }
    cvReleaseMemStorage(storage)
    t.toOption
  }

  def drawFaceInImage(img: IplImage): Unit = {
    val resizedImg = cvCreateImage(cvSize((grabber.getImageWidth * Config.ratio).toInt, (grabber.getImageHeight * Config.ratio).toInt), img.depth(), img.nChannels())
    cvResize(img, resizedImg)
    val imageBW = cvCreateImage(cvGetSize(resizedImg), IPL_DEPTH_8U, 1)
    cvCvtColor(resizedImg, imageBW, CV_BGR2GRAY)

    val bbox: Array[Int] = new Array[Int](4)
    val landmarks: Array[Double] = new Array[Double](2 * model.data.options.M)
    detectFaceInImage(imageBW, faceCascade, model, bbox, landmarks) match {
      case Some(data) =>
        data.data.foreach {
          case IFaceData(face, facebound, nose, points) =>
            cvRectangle(img, face.leftCorner, face.rightCorner, CV_RGB(255, 0, 0))
            cvRectangle(img, facebound.leftCorner, facebound.rightCorner, CV_RGB(0, 0, 255))
            cvCircle(img, nose.point, 3, CV_RGB(0, 0, 255), CV_FILLED, 8, 0)
            points.points.foreach { point =>
              cvCircle(img, point, 3, CV_RGB(255, 0, 0), CV_FILLED, 8, 0)
            }
        }
      case None =>
    }
    cvReleaseImage(imageBW)
    cvReleaseImage(resizedImg)
  }

  def timerJob(state: => WorkState)(now: Long): Unit = {
    println(s"${now / 1000000000.0 - last} s")
    last = now / 1000000000.0
    Try{
      val img = converter.convert(grabber.grab())
      state match {
        case DetectFace =>
          drawFaceInImage(img)
        case DirectShow =>
      }
      gc.drawImage(SwingFXUtils.toFXImage(imgConverter.convert(converter.convert(img)), null), 0, 0)
    }.recover{
      case _ =>
        println("failed")
    }
  }

  lazy val faceCascadeFile = new File("haarcascade_frontalface_alt.xml")
  lazy val flandmarkModelFile = new File("flandmark_model.dat")

  lazy val faceCascade = cvLoadHaarClassifierCascade(faceCascadeFile.getCanonicalPath, cvSize(0, 0))
  println("Count: " + faceCascade.count)

  lazy val model: FLANDMARK_Model = flandmark_init(flandmarkModelFile.getAbsolutePath)
  println("Model w_cols: " + model.W_COLS)
  println("Model w_rows: " + model.W_ROWS)

  lazy val converter = new OpenCVFrameConverter.ToIplImage()
  lazy val imgConverter = new Java2DFrameConverter()

  lazy val grabber = FrameGrabber.createDefault(0)
  grabber.start()
  println("grabber started")
//  Thread.sleep(1000)
//  detectFaceInImage(converter.convert(grabber.grab()), faceCascade, model, new Array[Int](4), new Array[Double](2 * model.data.options.M))
//  println("init detector")


  val timer = AnimationTimer(timerJob(Config.state))

  timer.start()

  lazy val canvas = new Canvas(grabber.getImageWidth, grabber.getImageHeight)
  lazy val gc = canvas.graphicsContext2D

  lazy val detectButton = new Button {
    text = Config.state.text
    onAction = handle {
      Config.state = Config.state match {
        case DirectShow => DetectFace
        case DetectFace => DirectShow
      }
      text = Config.state.text
    }
  }

  lazy val emptyButton = new Button {
    margin = Insets(0, 0, 0, 5)
    text = "empty"
    onAction = handle {}
  }

  stage = new PrimaryStage {
    title = "Canvas Test"
    scene = new Scene(grabber.getImageWidth + 40, grabber.getImageHeight + 100) {
      content = new Group {
        children = List(
          new VBox {
            layoutX = 20
            layoutY = 20
            spacing = 10
            children = List(
              new HBox {
                children = List(
                  detectButton,
                  emptyButton
                )
              },
              canvas
            )
          }
        )
      }
    }
  }

  stage.onCloseRequest = handle {
    timer.stop()
    grabber.flush()
    grabber.release()
  }
}

case class IData(data: List[IFaceData])
case class IFaceData(face: IFace, facebound: IFaceBound, nose: INose, points: IPoints)
case class IFace(leftCorner: IPoint, rightCorner: IPoint)
case class IFaceBound(leftCorner: IPoint, rightCorner: IPoint)
case class INose(point: IPoint)
case class IPoints(points: List[IPoint])
case class IPoint(x: Int, y: Int)

object IPoint {
  implicit def toCvPoint(point: IPoint): CvPoint = cvPoint((point.x / Config.ratio).toInt, (point.y / Config.ratio).toInt)
}

object Config {
  val ratio = 0.5
  var state: WorkState = DirectShow
}

sealed trait WorkState {
  def text: String
}
case object DirectShow extends WorkState {
  override val text = "Start Detect"
}
case object DetectFace extends WorkState {
  override val text = "Stop Detect"
}