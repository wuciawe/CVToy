package com.cvtoy

import java.io.File
import javax.swing.JFrame._
import org.bytedeco.javacpp.helper.opencv_core._
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_imgproc._
import org.bytedeco.javacpp.opencv_objdetect._
import org.bytedeco.javacpp.flandmark._
import org.bytedeco.javacv.{OpenCVFrameConverter, FrameGrabber, CanvasFrame}
import scala.util.Try

/**
 * Created by haha on 2015/8/7.
 */
object CameraCapture {
  def detectFaceInImage(orig: IplImage, input: IplImage, cascade: CvHaarClassifierCascade, model: FLANDMARK_Model, bbox: Array[Int], landmarks: Array[Double]): Unit = {
    val storage = cvCreateMemStorage(0)
    cvClearMemStorage(storage)
    try {
      val search_scale_factor = 1.1
      val flags = CV_HAAR_DO_CANNY_PRUNING
      val minFeatureSize = cvSize(40, 40)
      val rects = cvHaarDetectObjects(input, cascade, storage, search_scale_factor, 2, flags, minFeatureSize, cvSize(0, 0))
      val nFaces = rects.total
      if (nFaces == 0)  return
      for(iface <- 0 until nFaces) {
        val elem = cvGetSeqElem(rects, iface)
        val rect = new CvRect(elem)
        bbox(0) = rect.x
        bbox(1) = rect.y
        bbox(2) = rect.x + rect.width
        bbox(3) = rect.y + rect.height
        flandmark_detect(input, bbox, model, landmarks)

        cvRectangle(orig, cvPoint(bbox(0), bbox(1)), cvPoint(bbox(2), bbox(3)), CV_RGB(255, 0, 0))
        cvRectangle(orig, cvPoint(model.bb.get(0).toInt, model.bb.get(1).toInt), cvPoint(model.bb.get(2).toInt, model.bb.get(3).toInt), CV_RGB(0, 0, 255))
        cvCircle(orig, cvPoint(landmarks(0).toInt, landmarks(1).toInt), 3, CV_RGB(0, 0, 255), CV_FILLED, 8, 0)

        for(i <- 2 until 2 * model.data().options().M() by 2){
          cvCircle(orig, cvPoint(landmarks(i).toInt, landmarks(i + 1).toInt), 3, CV_RGB(255, 0, 0), CV_FILLED, 8, 0)
        }

      }
    } finally {
      cvReleaseMemStorage(storage)
    }
  }

  def main(args: Array[String]) {
    val faceCascadeFile = new File("haarcascade_frontalface_alt.xml")
    val flandmarkModelFile = new File("flandmark_model.dat")

    val faceCascade = cvLoadHaarClassifierCascade(faceCascadeFile.getCanonicalPath, cvSize(0, 0))
    println("Count: " + faceCascade.count)

    val model: FLANDMARK_Model = flandmark_init(flandmarkModelFile.getAbsolutePath)
    println("Model w_cols: " + model.W_COLS)
    println("Model w_rows: " + model.W_ROWS)

    val converter = new OpenCVFrameConverter.ToIplImage()

    val canvas = new CanvasFrame("Camera", 1)
    canvas.setDefaultCloseOperation(EXIT_ON_CLOSE)
    val grabber = FrameGrabber.createDefault(0)
    grabber.start()
    println("grabber started")
    while(true){
      Try{
        val img = converter.convert(grabber.grab())
        val imageBW = cvCreateImage(cvGetSize(img), IPL_DEPTH_8U, 1)
        cvCvtColor(img, imageBW, CV_BGR2GRAY)

        val bbox: Array[Int] = new Array[Int](4)
        val landmarks: Array[Double] = new Array[Double](2 * model.data.options.M)
        detectFaceInImage(img, imageBW, faceCascade, model, bbox, landmarks)

        canvas.setCanvasSize(grabber.getImageWidth, grabber.getImageHeight)
        if(img != null){
          canvas.showImage(converter.convert(img))
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