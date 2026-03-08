package app;

import processing.core.PImage;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

public class CamCapture {
  private final int camIndex;
  private final int width, height;
  private VideoCapture cap;
  private Mat mat;

  public CamCapture(int camIndex) {
    this.camIndex = camIndex;
    this.width = -1;
    this.height = -1;
  }

  public CamCapture(int camIndex, int width, int height) {
      this.camIndex = camIndex;
      this.width = width;
      this.height = height;
  }

  public boolean open() {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

    this.cap = new VideoCapture(this.camIndex, Videoio.CAP_DSHOW);  // force DSHOW backend

    if (!this.cap.isOpened()) {
      System.err.println("Camera failed to open.");
      return (false);
    }

    this.mat = new Mat();
    return (true);
  }

  public void close() {
    if (this.cap != null) cap.release();
    if (this.mat != null) mat.release();
    this.cap = null;
    this.mat = null;
  }

  public boolean isOpen() {
    return (this.cap != null && this.cap.isOpened());
  }


  public boolean readIntoPImage(PImage target) {
    if (!isOpen() || !this.cap.read(this.mat))
      return (false);

    target.loadPixels();

    if (this.mat.channels() == 3) {
      byte[] bufRGB = new byte[(int)(this.mat.total() * 3)];
      //Convert BGR to RGB in-place
      Imgproc.cvtColor(this.mat, this.mat, Imgproc.COLOR_BGR2RGB);
      this.mat.get(0, 0, bufRGB);

      //pack bytes into ARGB int pixels
      for (int i = 0, j = 0; i < target.pixels.length; i++, j += 3) {
        int r = bufRGB[j]   & 0xFF;
        int g = bufRGB[j+1] & 0xFF;
        int b = bufRGB[j+2] & 0xFF;
        target.pixels[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
      }
    } else if (mat.channels() == 1) {
      //grayscale
      byte[] bufY = new byte[(int) this.mat.total()];
      this.mat.get(0, 0, bufY);
      for (int i = 0; i < target.pixels.length; i++) {
        int y = bufY[i] & 0xFF;
        target.pixels[i] = 0xFF000000 | (y << 16) | (y << 8) | y;
      }

    } else {
      return (false);
    }

    target.updatePixels();
    return (true);
  }

  public VideoCapture getCamera() {
    return (this.cap);
  }
}
