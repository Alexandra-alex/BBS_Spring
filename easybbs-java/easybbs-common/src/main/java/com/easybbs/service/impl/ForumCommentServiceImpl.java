package com.easybbs.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import com.easybbs.entity.constants.Constants;
import com.easybbs.entity.dto.FileUploadDto;
import com.easybbs.entity.enums.*;
import com.easybbs.entity.po.ForumArticle;
import com.easybbs.entity.po.UserInfo;
import com.easybbs.entity.po.UserMessage;
import com.easybbs.entity.query.ForumArticleQuery;
import com.easybbs.entity.query.UserInfoQuery;
import com.easybbs.exception.BusinessException;
import com.easybbs.mappers.ForumArticleMapper;
import com.easybbs.mappers.UserInfoMapper;
import com.easybbs.service.UserInfoService;
import com.easybbs.service.UserIntegralRecordService;
import com.easybbs.service.UserMessageService;
import com.easybbs.utils.FileUtils;
import com.easybbs.utils.SysCacheUtils;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.springframework.stereotype.Service;

import com.easybbs.entity.query.ForumCommentQuery;
import com.easybbs.entity.po.ForumComment;
import com.easybbs.entity.vo.PaginationResultVO;
import com.easybbs.entity.query.SimplePage;
import com.easybbs.mappers.ForumCommentMapper;
import com.easybbs.service.ForumCommentService;
import com.easybbs.utils.StringTools;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


/**
 * 评论 业务接口实现
 */
@Service("forumCommentService")
public class ForumCommentServiceImpl implements ForumCommentService {

	@Resource
	private ForumCommentMapper<ForumComment, ForumCommentQuery> forumCommentMapper;

	@Resource
	private ForumArticleMapper<ForumArticle, ForumArticleQuery> forumArticleMapper;

	@Resource
	private UserInfoService userInfoService;

	@Resource
	private UserMessageService userMessageService;

	@Resource
	private FileUtils fileUtils;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<ForumComment> findListByParam(ForumCommentQuery param) {
		//查询一级评论列表
		List<ForumComment> list = this.forumCommentMapper.selectList(param);
		//获取一级评论下的二级评论(有分页的情况下无法使用递归，必须将一级和二级评论分开查询)
		if(param.getLoadChildren() != null && param.getLoadChildren()){
			ForumCommentQuery subQuery = new ForumCommentQuery();
			subQuery.setQueryLikeType(param.getQueryLikeType());
			subQuery.setCurrentUserId(param.getCurrentUserId());
			subQuery.setArticleId(param.getArticleId());
			subQuery.setStatus(param.getStatus());

			//过滤出所有的二级评论
			List<Integer> pCommentIdList = list.stream().map(ForumComment::getCommentId).distinct().collect(Collectors.toList());
			subQuery.setpCommentIdList(pCommentIdList);

			List<ForumComment> subCommentList = this.forumCommentMapper.selectList(subQuery);

			//根据pCommentId分组
			Map<Integer,List<ForumComment>> tempMap = subCommentList.stream().collect(Collectors.groupingBy(ForumComment::getpCommentId));

			list.forEach(item -> {
				item.setChildren(tempMap.get(item.getCommentId()));
			});
		}
		return list;
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(ForumCommentQuery param) {
		return this.forumCommentMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<ForumComment> findListByPage(ForumCommentQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<ForumComment> list = this.findListByParam(param);
		PaginationResultVO<ForumComment> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(ForumComment bean) {
		return this.forumCommentMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<ForumComment> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.forumCommentMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<ForumComment> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.forumCommentMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(ForumComment bean, ForumCommentQuery param) {
		StringTools.checkParam(param);
		return this.forumCommentMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(ForumCommentQuery param) {
		StringTools.checkParam(param);
		return this.forumCommentMapper.deleteByParam(param);
	}

	/**
	 * 根据CommentId获取对象
	 */
	@Override
	public ForumComment getForumCommentByCommentId(Integer commentId) {
		return this.forumCommentMapper.selectByCommentId(commentId);
	}

	/**
	 * 根据CommentId修改
	 */
	@Override
	public Integer updateForumCommentByCommentId(ForumComment bean, Integer commentId) {
		return this.forumCommentMapper.updateByCommentId(bean, commentId);
	}

	/**
	 * 根据CommentId删除
	 */
	@Override
	public Integer deleteForumCommentByCommentId(Integer commentId) {
		return this.forumCommentMapper.deleteByCommentId(commentId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void changeTopType(String userId, Integer commentId, Integer topType) {
		CommentTopTypeEnum topTypeEnum = CommentTopTypeEnum.getByType(topType);
		if(topTypeEnum == null) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		//查询评论
		ForumComment forumComment = forumCommentMapper.selectByCommentId(commentId);
		if(forumComment == null) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		//查询文章
		ForumArticle forumArticle = forumArticleMapper.selectByArticleId(forumComment.getArticleId());
		if(forumArticle == null) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		//判断文章创建人与操作人是否一致
		if(!forumArticle.getUserId().equals(userId) || forumComment.getpCommentId() !=0)
		{
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		if(forumComment.getTopType().equals(topType)){
			return;
		}
		//如已有置顶则取消所有置顶
		if(CommentTopTypeEnum.TOP.getType().equals(topType)){
			forumCommentMapper.updateTopTypeByArticleId(forumArticle.getArticleId());
		}
		//更新置顶
		ForumComment updateInfo = new ForumComment();
		updateInfo.setTopType(topType);
		forumCommentMapper.updateByCommentId(updateInfo,commentId);
	}

	//评论上传
	@Override
	public void postComment(ForumComment comment, MultipartFile image) {
		ForumArticle forumArticle = forumArticleMapper.selectByArticleId(comment.getArticleId());
		if(forumArticle == null || !ArticleStatusEnum.AUDIT.getStatus().equals(forumArticle.getStatus())){
			throw new BusinessException("评论的文章不存在");
		}
		ForumComment pComment = null;
		//如果pCommentId != 0 则此评论为二级评论
		if(comment.getpCommentId() != 0) {
			//获取父评论
			pComment = forumCommentMapper.selectByCommentId(comment.getpCommentId());
			if (pComment == null){
				throw new BusinessException("回复的评论不存在");
			}
		}

		//判断回复的用户是否存在
		if(!StringTools.isEmpty(comment.getReplyUserId())){
			UserInfo userInfo = userInfoService.getUserInfoByUserId(comment.getReplyUserId());
			if(userInfo == null){
				throw new BusinessException("回复的用户不存在");
			}
			comment.setReplyNickName(userInfo.getNickName());
		}
		comment.setPostTime(new Date());
		if(image != null){
			FileUploadDto uploadDto = fileUtils.uploadFile2Local(image,Constants.FILE_FOLDER_IMAGE,FileUploadTypeEnum.COMMENT_IMAGE);
			comment.setImgPath(uploadDto.getLocalPath());
		}

		//是否需要审核
		Boolean needAudit = SysCacheUtils.getSysSetting().getAuditSetting().getCommentAudit();
		comment.setStatus(needAudit ? CommentStatusEnum.NO_AUDIT.getStatus() : CommentStatusEnum.AUDIT.getStatus());
		this.forumCommentMapper.insert(comment);

		if(needAudit){
			return;
		}

		updateCommentInfo(comment,forumArticle,pComment);
	}

	public void updateCommentInfo(ForumComment comment,ForumArticle forumArticle,ForumComment pComment){
		//定义评论积分
		Integer commentIntegral = SysCacheUtils.getSysSetting().getCommentSetting().getCommentIntegral();
		if(commentIntegral > 0){
			userInfoService.updateUserIntegral(comment.getUserId(),UserIntegralOperTypeEnum.POST_COMMENT,UserIntegralChangeTypeEnum.ADD.getChangeType(), commentIntegral);
		}
		//为一级评论
		if(comment.getCommentId()==0) {
			this.forumArticleMapper.updateArticleCount(UpdateArticleCountTypeEnum.COMMENT_COUNT.getType(),Constants.ONE, comment.getArticleId());
		}
		//记录消息
		UserMessage userMessage = new UserMessage();
		userMessage.setMessageType(MessageTypeEnum.COMMENT.getType());
		userMessage.setCreateTime(new Date());
		userMessage.setArticleId(comment.getArticleId());
		userMessage.setCommentId(comment.getCommentId());
		userMessage.setSendUserId(comment.getUserId());
		userMessage.setSendNickName(comment.getNickName());
		userMessage.setStatus(MessageStatusEnum.NO_READ.getStatus());
		userMessage.setArticleTitle(forumArticle.getTitle());
		//如为一级评论，则发信息给文章作者
		if(comment.getpCommentId() == 0){
			userMessage.setReceivedUserId(forumArticle.getUserId());
		}
		//如为二级评论且传进来的接收者为空，则发信息给父评论发送者
		else if(comment.getpCommentId() != 0 && StringTools.isEmpty(comment.getReplyUserId())){
			userMessage.setReceivedUserId(pComment.getUserId());
		}
		//如为二级评论且传进来的接收者为空，则发信息给接收者
		else if(comment.getpCommentId() != 0 && !StringTools.isEmpty(comment.getReplyUserId())){
			userMessage.setReceivedUserId(comment.getReplyUserId());
		}
		//如接收人和发送人不同
		if(!comment.getUserId().equals(userMessage.getReceivedUserId())){
			userMessageService.add(userMessage);
		}
	}
}