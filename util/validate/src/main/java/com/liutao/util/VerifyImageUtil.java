package com.liutao.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/**
 * 滑块验证工具类
 *
 * @author: LIUTAO
 * @Description:
 * @Date: Created in 10:57 2018/6/25
 * @Modified By:
 */
public class VerifyImageUtil {

  //源文件宽度
  private static int ORI_WIDTH = 590;
  //源文件高度
  private static int ORI_HEIGHT = 360;

  //抠图坐标x
  private static int X;
  //抠图坐标y
  private static int Y;

  //模板图宽度
  private static int WIDTH;
  //模板图高度
  private static int HEIGHT;

  //X位置移动百分比
  private static float xPercent;
  //Y位置移动百分比
  private static float yPercent;


  public static int getX() {

    return X;
  }

  public static int getY() {

    return Y;
  }

  public static float getxPercent() {

    return xPercent;
  }

  public static float getyPercent() {

    return yPercent;
  }

  /**
   * 根据模板切图
   *
   * templateFile = 模板图的copy
   * targetFile = 背景图的copy
   * templateFileType = "png"
   * oriFileType = "jpg"
   */
  public static Map<String, byte[]> pictureTemplatesCut(
      File templateFile,
      File targetFile,
      String templateFileType,
      String oriFileType) throws Exception {

    Map<String, byte[]> pictureMap = new HashMap<>();

    // 文件类型
    if (StringUtils.isEmpty(templateFileType) || StringUtils.isEmpty(oriFileType)) {
      throw new RuntimeException("file type is empty");
    }

    // 模板图
    BufferedImage imageTemplate = ImageIO.read(templateFile);
    WIDTH = imageTemplate.getWidth();
    HEIGHT = imageTemplate.getHeight();
    generateCutoutCoordinates();

    // 最终图像
    BufferedImage newImage = new BufferedImage(WIDTH, HEIGHT, imageTemplate.getType());
    Graphics2D graphics = newImage.createGraphics();
    graphics.setBackground(Color.white);

    // 源文件流
    File Orifile = targetFile;
    InputStream oriis = new FileInputStream(Orifile);

    // 获取感兴趣的目标区域
    BufferedImage targetImageNoDeal = getTargetArea(X, Y, WIDTH, HEIGHT, oriis, oriFileType);

    // 根据模板图片抠图
    newImage = DealCutPictureByTemplate(targetImageNoDeal, imageTemplate, newImage);

    // 设置“抗锯齿”的属性
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    graphics.setStroke(new BasicStroke(5, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
    graphics.drawImage(newImage, 0, 0, null);
    graphics.dispose();

    //新建流
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    ImageIO.write(newImage, "png", os);
    byte[] newImages = os.toByteArray();
    pictureMap.put("newImage", newImages);

    // 源图生成遮罩
    BufferedImage oriImage = ImageIO.read(Orifile);
    byte[] oriCopyImages = DealOriPictureByTemplate(oriImage, imageTemplate, X, Y);
    pictureMap.put("oriCopyImage", oriCopyImages);
    return pictureMap;
  }

  /**
   * 抠图后原图生成
   */
  private static byte[] DealOriPictureByTemplate(BufferedImage oriImage,
      BufferedImage templateImage, int x,
      int y) throws Exception {
    // 源文件备份图像矩阵 支持alpha通道的rgb图像
    BufferedImage ori_copy_image = new BufferedImage(oriImage.getWidth(), oriImage.getHeight(),
        BufferedImage.TYPE_4BYTE_ABGR);

    // 源文件图像矩阵
    int[][] oriImageData = getData(oriImage);

    // 模板图像矩阵
    int[][] templateImageData = getData(templateImage);

    //copy 源图做不透明处理
    for (int i = 0; i < oriImageData.length; i++) {
      for (int j = 0; j < oriImageData[0].length; j++) {
        int rgb = oriImage.getRGB(i, j);
        int r = (0xff & rgb);
        int g = (0xff & (rgb >> 8));
        int b = (0xff & (rgb >> 16));
        //无透明处理
        rgb = r + (g << 8) + (b << 16) + (255 << 24);
        ori_copy_image.setRGB(i, j, rgb);
      }
    }

    for (int i = 0; i < templateImageData.length; i++) {
      for (int j = 0; j < templateImageData[0].length - 5; j++) {
        int rgb = templateImage.getRGB(i, j);
        //对源文件备份图像(x+i,y+j)坐标点进行透明处理
        if (rgb != 16777215 && rgb <= 0) {
          int rgb_ori = ori_copy_image.getRGB(x + i, y + j);
          int r = (0xff & rgb_ori);
          int g = (0xff & (rgb_ori >> 8));
          int b = (0xff & (rgb_ori >> 16));
          rgb_ori = r + (g << 8) + (b << 16) + (150 << 24);
          ori_copy_image.setRGB(x + i, y + j, rgb_ori);
        }
        else {
          //do nothing
        }
      }
    }

    //新建流。
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    //利用ImageIO类提供的write方法，将bi以png图片的数据模式写入流。
    ImageIO.write(ori_copy_image, "png", os);
    //从流中获取数据数组。
    byte b[] = os.toByteArray();
    return b;
  }


  /**
   * 根据模板图片抠图
   */
  private static BufferedImage DealCutPictureByTemplate(
      BufferedImage oriImage,
      BufferedImage templateImage,
      BufferedImage targetImage) throws Exception {

    // 源文件图像矩阵
    int[][] oriImageData = getData(oriImage);

    // 模板图像矩阵
    int[][] templateImageData = getData(templateImage);

    // 模板图像宽度
    for (int i = 0; i < templateImageData.length; i++) {
      // 模板图片高度
      for (int j = 0; j < templateImageData[0].length; j++) {
        // 如果模板图像当前像素点不是白色 copy源文件信息到目标图片中
        int rgb = templateImageData[i][j];
        if (rgb <= 0 && rgb != 16777215) {
          targetImage.setRGB(i, j, oriImageData[i][j]);
        }
      }
    }
    return targetImage;
  }


  /**
   * 获取目标区域
   *
   * x = X
   * y = Y
   * targetWidth = WIDTH
   * targetHeight = HEIGHT
   * ois = oriis
   * fileType = oriFileTyp
   */
  private static BufferedImage getTargetArea(int x, int y, int targetWidth, int targetHeight,
      InputStream ois, String fileType) throws Exception {

    Iterator<ImageReader> imageReaderList = ImageIO.getImageReadersByFormatName(fileType);
    ImageReader imageReader = imageReaderList.next();
    // 获取图片流
    ImageInputStream iis = ImageIO.createImageInputStream(ois);
    // 输入源中的图像将只按顺序读取
    imageReader.setInput(iis, true);

    ImageReadParam param = imageReader.getDefaultReadParam();
    Rectangle rec = new Rectangle(x, y, targetWidth, targetHeight);
    param.setSourceRegion(rec);
    BufferedImage targetImage = imageReader.read(0, param);
    return targetImage;
  }

  /**
   * 生成图像矩阵
   */
  private static int[][] getData(BufferedImage bimg) throws Exception {

    int[][] data = new int[bimg.getWidth()][bimg.getHeight()];
    for (int i = 0; i < bimg.getWidth(); i++) {
      for (int j = 0; j < bimg.getHeight(); j++) {
        data[i][j] = bimg.getRGB(i, j);
      }
    }
    return data;
  }

  /**
   * 随机生成抠图坐标(X, Y)
   */
  private static void generateCutoutCoordinates() {

    Random random = new Random();

    int widthDifference = ORI_WIDTH - WIDTH;
    int heightDifference = ORI_HEIGHT - HEIGHT;

    if (widthDifference <= 0) {
      X = 5;
    }
    else {
      X = random.nextInt(ORI_WIDTH - WIDTH) + 5;
    }

    if (heightDifference <= 0) {
      Y = 5;
    }
    else {
      Y = random.nextInt(ORI_HEIGHT - HEIGHT) + 5;
    }

    NumberFormat numberFormat = NumberFormat.getInstance();
    numberFormat.setMaximumFractionDigits(2);
    xPercent = Float.parseFloat(numberFormat.format((float) X / (float) ORI_WIDTH));
    yPercent = Float.parseFloat(numberFormat.format((float) Y / (float) ORI_HEIGHT));
  }
}


