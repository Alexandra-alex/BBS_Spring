package com.easybbs.controller;

import com.easybbs.annotation.GlobalInterceptor;
import com.easybbs.controller.base.ABaseController;
import com.easybbs.entity.config.WebConfig;
import com.easybbs.entity.constants.Constants;
import com.easybbs.entity.enums.ResponseCodeEnum;
import com.easybbs.entity.vo.ResponseVO;
import com.easybbs.exception.BusinessException;
import com.easybbs.utils.StringTools;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

//文件上传
@RestController
@RequestMapping("/file")
public class FileController extends ABaseController {
    private static final Logger logger = LoggerFactory.getLogger(FileController.class);
    @Resource
    private WebConfig webConfig;

    @RequestMapping("/uploadImage")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO uploadImage(MultipartFile file){
        if(file == null){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        String fileName = file.getOriginalFilename();
        if(fileName == null){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        String fileExtName = StringTools.getFileSuffix(fileName);
        //校验图片后缀
        if(!ArrayUtils.contains(Constants.IMAGE_SUFFIX,fileExtName)){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        String path = copyFile(file);
        Map<String,String> fileMap = new HashMap<>();
        fileMap.put("fileName",path);
        return getSuccessResponseVO(fileMap);
    }

    //文件上传
    private String copyFile(MultipartFile file){
        try {
            String fileName = file.getOriginalFilename();
            String fileExtName = StringTools.getFileSuffix(fileName);
            //重命名文件
            String fileRealName = StringTools.getRandomString(Constants.LENGTH_30)+fileExtName;
            //隐式目录，用于缓存文件
            String folderPath = webConfig.getProjectFolder()+Constants.FILE_FOLDER_FILE+Constants.FILE_FOLDER_TEMP;
            File folder = new File(folderPath);
            //如果目录不存在，则创建
            if(!folder.exists()){
                folder.mkdirs();
            }
            //文件存放真实目录
            File uplodeFile = new File(folderPath+"/"+fileRealName);
            file.transferTo(uplodeFile);
            //返回文件路径
            return Constants.FILE_FOLDER_TEMP_TWO+"/"+fileRealName;
        }catch (Exception e){
            logger.error("上传文件失败",e);
            throw new BusinessException("上传文件失败");
        }
    }

    //获取图片文件
    @RequestMapping("/getImage/{imageFolder}/{imageName}")
    public void getImage(HttpServletResponse response, @PathVariable("imageFolder") String imageFolder, @PathVariable("imageName") String imageName){
        readImage(response, imageFolder, imageName);
    }
    //获取头像文件
    @RequestMapping("/getAvatar/{userId}")
    public void getAvatar(HttpServletResponse response,@PathVariable("userId") String userId){
        //获取头像路径
        String avatarFolderName =webConfig.getProjectFolder()+Constants.FILE_FOLDER_FILE+Constants.FILE_FOLDER_AVATAR_NAME;
        String avatarPath = webConfig.getProjectFolder()+avatarFolderName+userId+Constants.AVATAR_SUFFIX;
        File avatarFolder = new File(avatarFolderName);
        //如果目录不存在，则创建
        if(!avatarFolder.exists()){
            avatarFolder.mkdirs();
        }
        File file = new File(avatarPath);
        String imageName = userId+Constants.AVATAR_SUFFIX;
        //如头像不存在，则使用默认头像
        if(!file.exists()){
            imageName = Constants.AVATAR_DEFAULT;
        }
        readImage(response,Constants.FILE_FOLDER_AVATAR_NAME,imageName);
    }

    //读文件（输入输出流）
    private void readImage(HttpServletResponse response, String imageFolder, String imageName){
        ServletOutputStream sos = null;
        FileInputStream in = null;
        ByteArrayOutputStream baos = null;
        try{
            if(StringTools.isEmpty(imageFolder) || StringUtils.isBlank(imageName))
            {
                return;
            }
            //获取文件后缀
            String imageSuffix = StringTools.getFileSuffix(imageName);
            //获取文件路径
            String filePath = webConfig.getProjectFolder()+Constants.FILE_FOLDER_FILE+Constants.FILE_FOLDER_IMAGE+imageFolder+"/"+imageName;
            if(Constants.FILE_FOLDER_TEMP_TWO.equals(imageFolder) || imageFolder.contains(Constants.FILE_FOLDER_AVATAR_NAME)){
                filePath = webConfig.getProjectFolder()+Constants.FILE_FOLDER_FILE+imageFolder+"/"+imageName;
            }
            File file = new File(filePath);
            if(!file.exists()){
                return;
            }
            imageSuffix = imageSuffix.replace(".","");
            //图片缓存
            if(!Constants.FILE_FOLDER_AVATAR_NAME.equals(imageFolder))
            {
                response.setHeader("Cache-Control", "max-age=2592000");
            }
            response.setContentType("image/"+imageSuffix);
            in = new FileInputStream(file);
            sos = response.getOutputStream();
            baos = new ByteArrayOutputStream();
            int ch = 0;
            while((ch = in.read()) != -1){
                baos.write(ch);
            }
            sos.write(baos.toByteArray());
        }catch (Exception e) {
            logger.error("读取图片异常", e);
        } finally {
            if(baos != null)
            {
                try{
                    baos.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
            if(sos != null){
                try{
                    sos.close();
                }catch (IOException e){
                    logger.error("IO异常",e);
                }
            }
            if(in != null){
                try{
                    in.close();
                }catch (IOException e){
                    logger.error("IO异常",e);
                }
            }
        }
    }
}
