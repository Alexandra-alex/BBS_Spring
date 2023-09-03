package com.easybbs.controller;

import com.easybbs.annotation.GlobalInterceptor;
import com.easybbs.annotation.VerifyParam;
import com.easybbs.controller.base.ABaseController;
import com.easybbs.entity.config.WebConfig;
import com.easybbs.entity.constants.Constants;
import com.easybbs.entity.dto.SessionWebUserDto;
import com.easybbs.entity.enums.*;
import com.easybbs.entity.po.*;
import com.easybbs.entity.query.ForumArticleAttachmentQuery;
import com.easybbs.entity.query.ForumArticleQuery;
import com.easybbs.entity.vo.PaginationResultVO;
import com.easybbs.entity.vo.ResponseVO;
import com.easybbs.entity.vo.web.ForumArticleAttachmentVO;
import com.easybbs.entity.vo.web.ForumArticleDetailVO;
import com.easybbs.entity.vo.web.ForumArticleVO;
import com.easybbs.entity.vo.web.UserDownloadInfoVO;
import com.easybbs.exception.BusinessException;
import com.easybbs.service.*;
import com.easybbs.utils.CopyTools;
import com.easybbs.utils.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.net.URLEncoder;
import java.util.List;
import java.util.Objects;

//文章列表
@RestController
@RequestMapping("/forum")
public class ForumArticleController extends ABaseController {

    private static final Logger logger = LoggerFactory.getLogger(ForumArticleController.class);
    @Resource
    private ForumArticleService forumArticleService;
    @Resource
    private ForumArticleAttachmentService forumArticleAttachmentService;
    @Resource
    private LikeRecordService likeRecordService;
    @Resource
    private UserInfoService userInfoService;
    @Resource
    private ForumArticleAttachmentDownloadService attachmentDownloadService;
    @Resource
    private ForumBoardService forumBoardService;
    @Resource
    private WebConfig webConfig;

    @RequestMapping("/loadArticle")
    public ResponseVO loadArticle(HttpSession session, Integer boardId,Integer pBoardId,Integer orderType,Integer pageNo){
        ForumArticleQuery articleQuery = new ForumArticleQuery();
        //如为空则不过滤
        articleQuery.setBoardId(boardId==null||boardId==0?null:boardId);
        articleQuery.setpBoardId(pBoardId);
        articleQuery.setPageNo(pageNo);

        SessionWebUserDto userDto = getUserInfoFromSession(session);
        if(userDto!=null){
            articleQuery.setCurrentUserId(userDto.getUserId());
        }else {
            //过滤，取已审核文章
            articleQuery.setStatus(ArticleStatusEnum.AUDIT.getStatus());
        }

        //获取排序方式
        ArticleOrderTypeEnum orderTypeEnum = ArticleOrderTypeEnum.getByType(orderType);
        orderTypeEnum=orderTypeEnum == null ? ArticleOrderTypeEnum.HOT : orderTypeEnum;
        articleQuery.setOrderBy(orderTypeEnum.getOrderSql());
        PaginationResultVO<ForumArticle> resultVO = forumArticleService.findListByPage(articleQuery);
        return getSuccessResponseVO(convert2PaginationVO(resultVO, ForumArticleVO.class));
    }

    //文章详情
    @RequestMapping("/getArticleDetail")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO getArticleDetail(HttpSession session, @VerifyParam(required = true) String articleId){
        SessionWebUserDto sessionWebUserDto = getUserInfoFromSession(session);
        ForumArticle forumArticle = forumArticleService.readArticle(articleId);

        //判断是否为空/未审核/是超级管理员/已删除
        Boolean canShowNOAudit = sessionWebUserDto!=null && (sessionWebUserDto.getUserId().equals(forumArticle.getUserId())||sessionWebUserDto.getAdmin());
        if((ArticleStatusEnum.NO_AUDIT.getStatus().equals(forumArticle.getStatus())&& !canShowNOAudit) ||
        ArticleStatusEnum.DEL.getStatus().equals(forumArticle.getStatus())){
            throw new BusinessException(ResponseCodeEnum.CODE_404);
        }
        ForumArticleDetailVO detailVO = new ForumArticleDetailVO();
        //文章详情
        detailVO.setForumArticle(CopyTools.copy(forumArticle, ForumArticleVO.class));
        //有附件
        if(Objects.equals(forumArticle.getAttachmentType(), Constants.ONE)){
            ForumArticleAttachmentQuery attachmentQuery = new ForumArticleAttachmentQuery();
            //保存文章ID
            attachmentQuery.setArticleId(articleId);
            //获取附件信息
            List<ForumArticleAttachment> forumArticleAttachmentList = forumArticleAttachmentService.findListByParam(attachmentQuery);
            if(!forumArticleAttachmentList.isEmpty()){
                detailVO.setAttachment(CopyTools.copy(forumArticleAttachmentList.get(0), ForumArticleAttachmentVO.class));
            }
        }
        //是否已经点赞
        if(sessionWebUserDto != null){
            //获取点赞记录表
            LikeRecord likeRecord = likeRecordService.getLikeRecordByObjectIdAndUserIdAndOpType(articleId,sessionWebUserDto.getUserId(), OperRecordOpTypeEnum.ARTICLE_LIKE.getType());
            if(likeRecord != null){
                detailVO.setHaveLike(true);
            }
        }
        return getSuccessResponseVO(detailVO);
    }

    //点赞
    @RequestMapping("/doLike")
    @GlobalInterceptor(checkLogin = true,checkParams = true)
    public ResponseVO doLike(HttpSession session, @VerifyParam(required = true) String articleId){
        SessionWebUserDto sessionWebUserDto = getUserInfoFromSession(session);
        likeRecordService.doLike(articleId,sessionWebUserDto.getUserId(),sessionWebUserDto.getNickname(),OperRecordOpTypeEnum.ARTICLE_LIKE);
        return getSuccessResponseVO(null);
    }

    //附件信息获取
    @RequestMapping("/getUserDownloadInfo")
    @GlobalInterceptor(checkLogin = true,checkParams = true)
    public ResponseVO getUserDownloadInfo(HttpSession session, @VerifyParam(required = true) String fileId){
        SessionWebUserDto sessionWebUserDto = getUserInfoFromSession(session);
        //获取用户信息
        UserInfo userInfo = userInfoService.getUserInfoByUserId(sessionWebUserDto.getUserId());
        UserDownloadInfoVO downloadInfoVO = new UserDownloadInfoVO();
        //获取当前积分
        downloadInfoVO.setUserIntegral(userInfo.getCurrentIntegral());
        ForumArticleAttachmentDownload attachmentDownload = attachmentDownloadService.getForumArticleAttachmentDownloadByFileIdAndUserId(fileId,sessionWebUserDto.getUserId());
        if(attachmentDownload != null){
            downloadInfoVO.setHaveDownload(true);
        }
        return getSuccessResponseVO(downloadInfoVO);
    }

    //附件下载
    @RequestMapping("/attachmentDownload")
    @GlobalInterceptor(checkLogin = true,checkParams = true)
    public void attachmentDownload(HttpSession session, HttpServletRequest request, HttpServletResponse response,@VerifyParam(required = true) String fileId)
    {
        ForumArticleAttachment attachment = forumArticleAttachmentService.downloadAttachment(fileId,getUserInfoFromSession(session));
        InputStream in =null;
        OutputStream out = null;
        String downloadFileName = attachment.getFileName();
        String filePath = webConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + Constants.FILE_FOLDER_ATTACHMENT + attachment.getFilePath();
        File file = new File(filePath);
        try{
            in = new FileInputStream(file);
            out = response.getOutputStream();
            response.setContentType(("application/x-msdownload; charset=UTF-8"));
            //解决中文名乱码问题
            if(request.getHeader("User-Agent").toLowerCase().indexOf("msie")>0){
                downloadFileName = URLEncoder.encode(downloadFileName,"UTF-8");
            }else {
                downloadFileName = new String(downloadFileName.getBytes("UTF-8"),"ISO8859-1");
            }
            response.setHeader("content-Disposition", "application;filename\""+ downloadFileName + "\"");
            byte[] byteData = new byte[1024];
            int len = 0;
            while ((len = in.read(byteData)) != -1) {
                out.write(byteData, 0, len);
            }
            out.flush();
        }catch (Exception e){
            logger.error("下载异常",e);
            throw new BusinessException("下载失败");
        }finally {
            try{
                if(in != null){
                    in.close();
                }
            }catch (IOException e){
                logger.error("IO异常",e);
            }
            try{
                if(out != null){
                    out.close();
                }
            }catch (IOException e){
                logger.error("IO异常",e);
            }
        }
    }

    //获取板块（发布文章）
    @RequestMapping("/loadBoard4Post")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO loadBoard4Post(HttpSession session){
        SessionWebUserDto sessionWebUserDto = getUserInfoFromSession(session);
        Integer postType = Constants.ZERO;
        //如不为超级管理员
        if(!sessionWebUserDto.getAdmin()){
            postType = Constants.ONE;
        }
        //根据postType获取板块列表
        return getSuccessResponseVO(forumBoardService.getBoardTree(postType));
    }

    //发布文章
    @RequestMapping("/postArticle")
    @GlobalInterceptor(checkLogin = true,checkParams = true)
    public ResponseVO postArticle(HttpSession session,
                                  MultipartFile cover,
                                  MultipartFile attachment,
                                  Integer integral,
                                  @VerifyParam(required = true,max = 150) String title,
                                  @VerifyParam(required = true) Integer pBoardId,
                                  Integer boardId,
                                  @VerifyParam(max = 200) String summary,
                                  @VerifyParam(required = true) Integer editorType,
                                  @VerifyParam(required = true) String content,
                                  String markdownContent) {
        title = StringTools.escapeHtml(title);
        SessionWebUserDto sessionWebUserDto = getUserInfoFromSession(session);

        //初始化文章记录
        ForumArticle forumArticle = new ForumArticle();
        forumArticle.setpBoardId(pBoardId);
        forumArticle.setBoardId(boardId);
        forumArticle.setTitle(title);
        forumArticle.setContent(content);

        EditorTypeEnum typeEnum = EditorTypeEnum.getByType(editorType);
        if(null == typeEnum){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        //为Markdown编辑器且内容为空
        if(EditorTypeEnum.MARKDOWN.getType().equals(editorType) && StringTools.isEmpty(markdownContent)){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        forumArticle.setMarkdownContent(markdownContent);
        forumArticle.setEditorType(editorType);
        forumArticle.setUserId(sessionWebUserDto.getUserId());
        forumArticle.setNickName(sessionWebUserDto.getNickname());
        forumArticle.setUserIpAddress(sessionWebUserDto.getProvince());
        forumArticle.setSummary(summary);

        //附件信息
        ForumArticleAttachment articleAttachment =new ForumArticleAttachment();
        articleAttachment.setIntegral(integral==null ? 0 : integral);
        forumArticleService.postArticle(sessionWebUserDto.getAdmin(),forumArticle,articleAttachment,cover,attachment);

        return getSuccessResponseVO(forumArticle.getArticleId());
    }

    //编辑文章
    @RequestMapping("/articleDetail4Update")
    @GlobalInterceptor(checkLogin = true,checkParams = true)
    public ResponseVO articleDetail4Update(HttpSession session,
                                           @VerifyParam(required = true) String articleId){
        SessionWebUserDto userDto = getUserInfoFromSession(session);
        ForumArticle article = forumArticleService.getForumArticleByArticleId(articleId);
        if(article == null || !article.getUserId().equals(userDto.getUserId())){
            throw new BusinessException("文章不存在");
        }
        ForumArticleDetailVO detailVO = new ForumArticleDetailVO();
        detailVO.setForumArticle(CopyTools.copy(article,ForumArticleVO.class));
        //有附件
        if(article.getAttachmentType() == Constants.ONE){
            ForumArticleAttachmentQuery attachmentQuery = new ForumArticleAttachmentQuery();
            //保存文章ID
            attachmentQuery.setArticleId(articleId);
            //获取附件信息
            List<ForumArticleAttachment> forumArticleAttachmentList = forumArticleAttachmentService.findListByParam(attachmentQuery);
            if(!forumArticleAttachmentList.isEmpty()){
                detailVO.setAttachment(CopyTools.copy(forumArticleAttachmentList.get(0), ForumArticleAttachmentVO.class));
            }
        }

        return getSuccessResponseVO(detailVO);
    }

    //更新文章
    @RequestMapping("/updateArticle")
    @GlobalInterceptor(checkLogin = true,checkParams = true)
    public ResponseVO updateArticle(HttpSession session,
                                  MultipartFile cover,
                                  MultipartFile attachment,
                                  Integer integral,
                                  @VerifyParam(required = true) String articleId,
                                  @VerifyParam(required = true,max = 150) String title,
                                  @VerifyParam(required = true) Integer pBoardId,
                                  Integer boardId,
                                  @VerifyParam(max = 200) String summary,
                                  @VerifyParam(required = true) Integer editorType,
                                  @VerifyParam(required = true) String content,
                                  String markdownContent,
                                  Integer attachmentType) {
        title = StringTools.escapeHtml(title);
        SessionWebUserDto sessionWebUserDto = getUserInfoFromSession(session);

        ForumArticle forumArticle = new ForumArticle();
        forumArticle.setArticleId(articleId);
        forumArticle.setpBoardId(pBoardId);
        forumArticle.setBoardId(boardId);
        forumArticle.setTitle(title);
        forumArticle.setContent(content);
        forumArticle.setMarkdownContent(markdownContent);
        forumArticle.setEditorType(editorType);
        forumArticle.setSummary(summary);
        forumArticle.setUserIpAddress(sessionWebUserDto.getProvince());
        forumArticle.setAttachmentType(attachmentType);
        forumArticle.setUserId(sessionWebUserDto.getUserId());
        //附件信息
        ForumArticleAttachment articleAttachment = new ForumArticleAttachment();
        articleAttachment.setIntegral(integral == null ? 0 : integral);

        forumArticleService.updateArticle(sessionWebUserDto.getAdmin(),forumArticle,articleAttachment,cover,attachment);

        return getSuccessResponseVO(forumArticle.getArticleId());
    }
}
