package com.easybbs.utils;

import com.easybbs.entity.config.AppConfig;
import com.easybbs.entity.constants.Constants;
import com.easybbs.entity.dto.FileUploadDto;
import com.easybbs.entity.enums.DateTimePatternEnum;
import com.easybbs.entity.enums.FileUploadTypeEnum;
import com.easybbs.exception.BusinessException;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;

@Component
public class FileUtils {
    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);
    @Resource
    private AppConfig appConfig;

    @Resource
    private ImageUtils imageUtils;

    //图片上传
    public FileUploadDto uploadFile2Local(MultipartFile file,String folder,FileUploadTypeEnum uploadTypeEnum){
        try{
            FileUploadDto uploadDto = new FileUploadDto();
            //获取文件名
            String originalFileName = file.getOriginalFilename();
            //获取文件后缀名
            String fileSuffix = StringTools.getFileSuffix(originalFileName);
            if(originalFileName.length() > Constants.LENGTH_200){
                //限制文件名长度
                originalFileName = StringTools.getFileName(originalFileName).substring(0,Constants.LENGTH_180) + fileSuffix;
            }
            //验证后缀名
            if(!ArrayUtils.contains(uploadTypeEnum.getSuffixArray(),fileSuffix)){
                throw new BusinessException("文件类型不正确");
            }
            //日期
            String month = DateUtil.format(new Date(), DateTimePatternEnum.YYYY_MM.getPattern());
            //file目录
            String baseFolder = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE;
            File targetFileFolder = new File(baseFolder+folder+month+"/");
            //重命名文件
            String fileName = StringTools.getRandomString(Constants.LENGTH_15) + fileSuffix;
            File targetFile = new File(targetFileFolder.getPath()+"/"+fileName);
            String localPath = month + "/" + fileName;

            if(uploadTypeEnum == FileUploadTypeEnum.AVATAR) {
                //头像上传
                targetFileFolder = new File(baseFolder+Constants.FILE_FOLDER_AVATAR_NAME);
                targetFile = new File(targetFileFolder.getPath()+"/"+folder+Constants.AVATAR_SUFFIX);
                localPath = folder + Constants.AVATAR_SUFFIX;
            }
            if(!targetFileFolder.exists()) {
                targetFileFolder.mkdirs();
            }
            file.transferTo(targetFile);

            //压缩图片（如为评论图片）
            if(uploadTypeEnum == FileUploadTypeEnum.COMMENT_IMAGE) {
                String thumbnailName = targetFile.getName().replace(".","_.");
                //路径与原图相同
                File thumbnail = new File(targetFile.getParent() + "/" + thumbnailName);
                Boolean thumbnailCreated = imageUtils.createThumbnail(targetFile,Constants.LENGTH_200,Constants.LENGTH_200,thumbnail);
                //没有生成略缩图时（小于指定高宽则不压缩）
                if(!thumbnailCreated){
                    //拷贝
                    org.apache.commons.io.FileUtils.copyFile(targetFile,thumbnail);
                }
            }
            //如为个人头像或封面
            else if(uploadTypeEnum == FileUploadTypeEnum.AVATAR || uploadTypeEnum == FileUploadTypeEnum.ARTICLE_COVER){
                imageUtils.createThumbnail(targetFile,Constants.LENGTH_200,Constants.LENGTH_200,targetFile);
            }
            uploadDto.setLocalPath(localPath);
            uploadDto.setOriginalFileName(originalFileName);
            return uploadDto;
        }catch (BusinessException e){
            logger.error("上传文件失败",e);
            throw e;
        }catch (Exception e){
            logger.error("上传文件失败",e);
            throw new BusinessException("上传文件失败");
        }
    }
}
