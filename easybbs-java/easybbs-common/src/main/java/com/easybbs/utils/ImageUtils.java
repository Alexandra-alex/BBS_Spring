package com.easybbs.utils;

import com.easybbs.entity.config.AppConfig;
import com.easybbs.entity.constants.Constants;
import com.easybbs.entity.enums.DateTimePatternEnum;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component("imageUtils")
public class ImageUtils {
    private static final Logger logger = LoggerFactory.getLogger(ImageUtils.class);
    @Resource
    private AppConfig appConfig;
    //生成缩略图
    public Boolean createThumbnail(File file, int thumbnailWidth, int thumbnailHeight, File targetFile){
        try{
            BufferedImage src = ImageIO.read(file);
            //thumbnailWidth 略缩图宽度 thumbnailHeight 略缩图高度
            int sorceWidth = src.getWidth();
            int sorceHeight = src.getHeight();
            //小于指定高宽则不压缩
            if(sorceWidth <= thumbnailWidth){
               return false;
            }
            int height = sorceHeight;//目标文件高度
            if(sorceWidth > thumbnailWidth){//目标文件宽度大于指定宽度
                height = thumbnailWidth * sorceHeight/sorceWidth;
            }else {//目标文件宽度小于指定宽度则略缩图大小与原图相同
                thumbnailWidth = sorceWidth;
                height = sorceHeight;
            }
            //生成宽度为150的略缩图
            BufferedImage dst = new BufferedImage(thumbnailWidth,height,BufferedImage.TYPE_INT_RGB);
            Image scaleImage = src.getScaledInstance(thumbnailWidth,height,Image.SCALE_SMOOTH);
            Graphics2D g = dst.createGraphics();
            g.drawImage(scaleImage,0,0,thumbnailWidth,height,null);
            g.dispose();

            int resultHeight = dst.getHeight();
            //高度过大，裁剪图片
            if(resultHeight > thumbnailHeight){
                resultHeight = thumbnailHeight;
                dst = dst.getSubimage(0,0,thumbnailWidth,resultHeight);
            }
            ImageIO.write(dst,"JPEG",targetFile);
            return true;
        }catch (Exception e){
            logger.error("略缩图生成失败",e);
        }
        return false;
    }
    //替换图片源
    public String resetImageHtml(String html){
        //日期
        String month = DateUtil.format(new Date(), DateTimePatternEnum.YYYY_MM.getPattern());
        List<String> imageList = getImageList(html);
        for(String img : imageList){
            resetImage(img, month);
        }
        return month;
    }

    private String resetImage(String imagePath, String month){
        //contains（包含）
        if(StringTools.isEmpty(imagePath) || !imagePath.contains(Constants.FILE_FOLDER_TEMP_TWO)){
            return imagePath;
        }
        //截取路径
        //  /api/file/getImage/temp/Vdb4DCHV4jIPtmW66Kj1IdzRTHeGhg.png  -> 202309/Vdb4DCHV4jIPtmW66Kj1IdzRTHeGhg.png
        imagePath = imagePath.replace(Constants.READ_IMAGE_PATH,"");
        if(StringTools.isEmpty(month)){
            month = DateUtil.format(new Date(), DateTimePatternEnum.YYYY_MM.getPattern());
        }
        String imageFileName = month+"/"+imagePath.substring(imagePath.lastIndexOf("/")+1);
        //目标文件地址
        File targetFile = new File(appConfig.getProjectFolder()+Constants.FILE_FOLDER_FILE+Constants.FILE_FOLDER_IMAGE+imageFileName);
        try {
            //临时文件地址
            File temporaryFile = new File(appConfig.getProjectFolder()+Constants.FILE_FOLDER_FILE+imagePath);
            FileUtils.copyFile(temporaryFile,targetFile);
            temporaryFile.delete();
        } catch (IOException e) {
            logger.error("复制图片失败",e);
            return imagePath;
        }
        return imageFileName;
    }

    //使用正则匹配图片
    public List<String> getImageList(String html){
        java.util.List<String> imageList = new ArrayList<>();
        String regEX_img = "(<img.*src\\s*=\\s*(.*?)[^>]*?>)";
        Pattern p_image = Pattern.compile(regEX_img,Pattern.CASE_INSENSITIVE);
        Matcher m_image = p_image.matcher(html);
        while(m_image.find()){
            String img = m_image.group();
            Matcher m = Pattern.compile("src\\s*=\\s*\"?(.*?)(\"|>|\\s+)").matcher(img);
            while (m.find()){
                String imageUrl = m.group(1);
                imageList.add(imageUrl);
            }
        }
        return imageList;
    }
}
