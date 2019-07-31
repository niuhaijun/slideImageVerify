package com.liutao.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Random;
import org.apache.commons.io.FileUtils;

public class Test {

  public static void main(String[] args) throws Exception {

    Test test = new Test();
    test.testImg();
  }


  // 图片验证
  public void testCode() throws IOException {

    byte[] newImages = VerifyCodeUtil.getVerifyImg();
    FileOutputStream fout = new FileOutputStream("C:/Users/Administrator/Desktop/verigy.png");
    fout.write(newImages);
    fout.close();

  }


  // 图片滑动验证
  public void testImg() throws Exception {

    Map<String, byte[]> pictureMap;
    InputStream stream;

    Random random = new Random();
    int templateNo = random.nextInt(4) + 1;
    int targetNo = random.nextInt(20) + 1;

    //模板图片
    File templateFile = new File(templateNo + ".png");
//    InputStream stream = getClass().getClassLoader().getResourceAsStream("static/templates/" + templateNo + ".png");
    stream = new FileInputStream(
        "/Users/niuhaijun/projects/TOOLS_other/util/validate/src/main/resources/static/templates/"
            + templateNo + ".png");
    FileUtils.copyInputStreamToFile(stream, templateFile);

    // 背景图
    File targetFile = new File(targetNo + ".jpg");
//    stream = getClass().getClassLoader().getResourceAsStream("static/targets/" + targetNo + ".jpg");
    stream = new FileInputStream(
        "/Users/niuhaijun/projects/TOOLS_other/util/validate/src/main/resources/static/targets/"
            + targetNo + ".jpg");
    FileUtils.copyInputStreamToFile(stream, targetFile);



    pictureMap = VerifyImageUtil.pictureTemplatesCut(templateFile, targetFile, "png", "jpg");
    byte[] oriCopyImages = pictureMap.get("oriCopyImage");
    byte[] newImages = pictureMap.get("newImage");

    // 切图（被拖拽的小图）
    FileOutputStream fout = new FileOutputStream("/Users/niuhaijun/Downloads/oriCopyImage.png");
    fout.write(oriCopyImages);
    fout.close();

    // 原图（少一块的背景图）
    FileOutputStream newImageFout = new FileOutputStream("/Users/niuhaijun/Downloads/newImage.png");
    newImageFout.write(newImages);
    newImageFout.close();
  }
}
