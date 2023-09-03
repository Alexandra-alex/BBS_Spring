package com.easybbs.controller;

import com.easybbs.annotation.GlobalInterceptor;
import com.easybbs.annotation.VerifyParam;
import com.easybbs.controller.base.ABaseController;
import com.easybbs.entity.constants.Constants;
import com.easybbs.entity.dto.SessionWebUserDto;
import com.easybbs.entity.enums.*;
import com.easybbs.entity.po.ForumComment;
import com.easybbs.entity.po.LikeRecord;
import com.easybbs.entity.po.UserInfo;
import com.easybbs.entity.query.ForumCommentQuery;
import com.easybbs.entity.query.UserInfoQuery;
import com.easybbs.entity.vo.ResponseVO;
import com.easybbs.exception.BusinessException;
import com.easybbs.mappers.UserInfoMapper;
import com.easybbs.service.ForumCommentService;
import com.easybbs.service.LikeRecordService;
import com.easybbs.service.UserInfoService;
import com.easybbs.utils.StringTools;
import com.easybbs.utils.SysCacheUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.List;

@RestController
@RequestMapping("/comment")
public class ForumCommentController extends ABaseController {
    @Resource
    private ForumCommentService commentService;
    @Resource
    private LikeRecordService likeRecordService;

    //获取评论列表
    @RequestMapping("/loadComment")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO loadComment(HttpSession session, @VerifyParam(required = true) String articleId, Integer pageNo, Integer orderType) {

        //以点赞数排序
        final String ORDER_TYPE_0 = "good_count desc,comment_id asc";

        //以最新排序，comment_id为自增id
        final String ORDER_TYPE_1 = "comment_id desc";

        //评论是否打开
        if (!SysCacheUtils.getSysSetting().getCommentSetting().getCommentOpen()) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        ForumCommentQuery commentQuery = new ForumCommentQuery();
        commentQuery.setArticleId(articleId);
        //排序方式
        String orderBy = orderType == null || orderType == Constants.ZERO ? ORDER_TYPE_0 : ORDER_TYPE_1;
        //置顶
        commentQuery.setOrderBy("top_type desc," + orderBy);

        SessionWebUserDto userDto = getUserInfoFromSession(session);
        if (userDto != null) {
            commentQuery.setQueryLikeType(true);
            //保存当前用户id
            commentQuery.setCurrentUserId(userDto.getUserId());
        } else {
            //过滤，取已审核评论
            commentQuery.setStatus(ArticleStatusEnum.AUDIT.getStatus());
        }
        //分页
        commentQuery.setPageNo(pageNo);
        commentQuery.setPageSize(PageSize.SIZE50.getSize());
        commentQuery.setpCommentId(Constants.ZERO);
        commentQuery.setLoadChildren(true);
        //返回评论一级列表
        return getSuccessResponseVO(commentService.findListByPage(commentQuery));
    }

    @RequestMapping("/doLike")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO doLike(HttpSession session, @VerifyParam(required = true) Integer commentId) {
        SessionWebUserDto sessionWebUserDto = getUserInfoFromSession(session);
        String objectId = String.valueOf(commentId);
        likeRecordService.doLike(objectId,sessionWebUserDto.getUserId(),sessionWebUserDto.getNickname(), OperRecordOpTypeEnum.COMMENT_LIKE);

        //查询点赞记录
        LikeRecord likeRecord = likeRecordService.getLikeRecordByObjectIdAndUserIdAndOpType(objectId,sessionWebUserDto.getUserId(),OperRecordOpTypeEnum.COMMENT_LIKE.getType());

        //获取评论信息
        ForumComment comment = commentService.getForumCommentByCommentId(commentId);
        comment.setLikeType(likeRecord == null ? 0 : 1);
        return getSuccessResponseVO(comment);
    }

    //评论置顶
    @RequestMapping("/changeTopType")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO changeTopType(HttpSession session, @VerifyParam(required = true) Integer commentId,@VerifyParam(required = true) Integer topType) {
        SessionWebUserDto sessionWebUserDto = getUserInfoFromSession(session);
        commentService.changeTopType(sessionWebUserDto.getUserId(), commentId, topType);
        return getSuccessResponseVO(null);
    }

    //发布评论
    @RequestMapping("/postComment")
    @GlobalInterceptor(checkLogin = true,checkParams = true)
    public ResponseVO postComment(HttpSession session,
                                  @VerifyParam(required = true) String articleId,
                                  @VerifyParam(required = true) Integer pCommentId,
                                  @VerifyParam(min = 5,max = 800) String content,
                                  MultipartFile image,
                                  String replayUserId){
        //评论是否打开
        if (!SysCacheUtils.getSysSetting().getCommentSetting().getCommentOpen()) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        //评论是否为空
        if(image == null && StringTools.isEmpty(content)){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        //转译
        content = StringTools.escapeHtml(content);
        SessionWebUserDto sessionWebUserDto = getUserInfoFromSession(session);
        ForumComment forumComment= new ForumComment();
        forumComment.setUserId(sessionWebUserDto.getUserId());
        forumComment.setNickName(sessionWebUserDto.getNickname());
        forumComment.setUserIpAddress(sessionWebUserDto.getProvince());
        forumComment.setpCommentId(pCommentId);
        forumComment.setArticleId(articleId);
        forumComment.setContent(content);
        forumComment.setReplyUserId(replayUserId);
        forumComment.setTopType(CommentTopTypeEnum.NO_TOP.getType());

        commentService.postComment(forumComment,image);

        if(pCommentId != 0){
            //获取二级评论列表
            ForumCommentQuery forumCommentQuery = new ForumCommentQuery();
            forumCommentQuery.setArticleId(articleId);
            forumCommentQuery.setpCommentId(pCommentId);
            forumCommentQuery.setOrderBy("comment_id asc");
            List<ForumComment> children = commentService.findListByParam(forumCommentQuery);
            return getSuccessResponseVO(children);
        }
        return getSuccessResponseVO(forumComment);
    }
}