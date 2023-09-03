package com.easybbs.controller;

import com.easybbs.annotation.GlobalInterceptor;
import com.easybbs.annotation.VerifyParam;
import com.easybbs.controller.base.ABaseController;
import com.easybbs.entity.constants.Constants;
import com.easybbs.entity.enums.ArticleStatusEnum;
import com.easybbs.entity.enums.ResponseCodeEnum;
import com.easybbs.entity.enums.UserStatusEnum;
import com.easybbs.entity.po.LikeRecord;
import com.easybbs.entity.po.UserInfo;
import com.easybbs.entity.query.ForumArticleQuery;
import com.easybbs.entity.query.LikeRecordQuery;
import com.easybbs.entity.vo.ResponseVO;
import com.easybbs.entity.vo.web.UserInfoVO;
import com.easybbs.exception.BusinessException;
import com.easybbs.service.ForumArticleService;
import com.easybbs.service.LikeRecordService;
import com.easybbs.service.UserInfoService;
import com.easybbs.utils.CopyTools;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

@RestController("userCenterController")
@RequestMapping("/userCenter")
public class UserCenterController extends ABaseController {

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private ForumArticleService forumArticleService;

    @Resource
    private LikeRecordService likeRecordService;

    @RequestMapping("/getUserInfo")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO getUserInfo(HttpSession session,@VerifyParam(required = true) String userId){
        UserInfo userInfo = userInfoService.getUserInfoByUserId(userId);
        //用户信息为空或用户已被禁用
        if(userInfo == null || userInfo.getStatus().equals(UserStatusEnum.DISABLE.getStatus())){
            throw new BusinessException(ResponseCodeEnum.CODE_404);
        }
        //查询已审核文章数量
        ForumArticleQuery articleQuery = new ForumArticleQuery();
        articleQuery.setUserId(userId);
        articleQuery.setStatus(ArticleStatusEnum.AUDIT.getStatus());
        Integer postCount = forumArticleService.findCountByParam(articleQuery);

        //将UserInfoVO中属性值复制到userInfo中
        UserInfoVO userInfoVO = CopyTools.copy(userInfo,UserInfoVO.class);
        userInfoVO.setPostCount(postCount);

        //查询点赞数
        LikeRecordQuery likeRecordQuery = new LikeRecordQuery();
        likeRecordQuery.setAuthorUserId(userId);
        Integer likeCount = likeRecordService.findCountByParam(likeRecordQuery);
        userInfoVO.setLikeCount(likeCount);

        return getSuccessResponseVO(userInfoVO);
    }
}
