package com.easybbs.service.impl;

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import com.easybbs.entity.constants.Constants;
import com.easybbs.entity.enums.*;
import com.easybbs.entity.po.ForumArticle;
import com.easybbs.entity.po.ForumComment;
import com.easybbs.entity.po.UserMessage;
import com.easybbs.entity.query.*;
import com.easybbs.exception.BusinessException;
import com.easybbs.mappers.ForumArticleMapper;
import com.easybbs.mappers.ForumCommentMapper;
import com.easybbs.mappers.UserMessageMapper;
import org.springframework.stereotype.Service;

import com.easybbs.entity.po.LikeRecord;
import com.easybbs.entity.vo.PaginationResultVO;
import com.easybbs.mappers.LikeRecordMapper;
import com.easybbs.service.LikeRecordService;
import com.easybbs.utils.StringTools;
import org.springframework.transaction.annotation.Transactional;


/**
 * 点赞记录 业务接口实现
 */
@Service("likeRecordService")
public class LikeRecordServiceImpl implements LikeRecordService {

	@Resource
	private LikeRecordMapper<LikeRecord, LikeRecordQuery> likeRecordMapper;

	@Resource
	private UserMessageMapper<UserMessage,UserMessageQuery> userMessageMapper;

	@Resource
	private ForumArticleMapper<ForumArticle, ForumArticleQuery> forumArticleMapper;

	@Resource
	private ForumCommentMapper<ForumComment, ForumCommentQuery> forumCommentMapper;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<LikeRecord> findListByParam(LikeRecordQuery param) {
		return this.likeRecordMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(LikeRecordQuery param) {
		return this.likeRecordMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<LikeRecord> findListByPage(LikeRecordQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<LikeRecord> list = this.findListByParam(param);
		PaginationResultVO<LikeRecord> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(LikeRecord bean) {
		return this.likeRecordMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<LikeRecord> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.likeRecordMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<LikeRecord> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.likeRecordMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(LikeRecord bean, LikeRecordQuery param) {
		StringTools.checkParam(param);
		return this.likeRecordMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(LikeRecordQuery param) {
		StringTools.checkParam(param);
		return this.likeRecordMapper.deleteByParam(param);
	}

	/**
	 * 根据OpId获取对象
	 */
	@Override
	public LikeRecord getLikeRecordByOpId(Integer opId) {
		return this.likeRecordMapper.selectByOpId(opId);
	}

	/**
	 * 根据OpId修改
	 */
	@Override
	public Integer updateLikeRecordByOpId(LikeRecord bean, Integer opId) {
		return this.likeRecordMapper.updateByOpId(bean, opId);
	}

	/**
	 * 根据OpId删除
	 */
	@Override
	public Integer deleteLikeRecordByOpId(Integer opId) {
		return this.likeRecordMapper.deleteByOpId(opId);
	}

	/**
	 * 根据ObjectIdAndUserIdAndOpType获取对象
	 */
	@Override
	public LikeRecord getLikeRecordByObjectIdAndUserIdAndOpType(String objectId, String userId, Integer opType) {
		return this.likeRecordMapper.selectByObjectIdAndUserIdAndOpType(objectId, userId, opType);
	}

	/**
	 * 根据ObjectIdAndUserIdAndOpType修改
	 */
	@Override
	public Integer updateLikeRecordByObjectIdAndUserIdAndOpType(LikeRecord bean, String objectId, String userId, Integer opType) {
		return this.likeRecordMapper.updateByObjectIdAndUserIdAndOpType(bean, objectId, userId, opType);
	}

	/**
	 * 根据ObjectIdAndUserIdAndOpType删除
	 */
	@Override
	public Integer deleteLikeRecordByObjectIdAndUserIdAndOpType(String objectId, String userId, Integer opType) {
		return this.likeRecordMapper.deleteByObjectIdAndUserIdAndOpType(objectId, userId, opType);
	}

	//根据ObjectIdAndUserIdAndOpTypeEnum点赞
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void doLike(String objectId, String userId, String nickName, OperRecordOpTypeEnum opTypeEnum) {
		UserMessage userMessage = new UserMessage();
		userMessage.setCreateTime(new Date());
		LikeRecord likeRecord = null;
		switch (opTypeEnum) {
			case ARTICLE_LIKE:
				//获取文章
				ForumArticle forumArticle = forumArticleMapper.selectByArticleId(objectId);
				if(forumArticle == null){
					throw new BusinessException("文章不存在");
				}
				articleLike(objectId, userId, opTypeEnum, forumArticle);
				//记录消息
				userMessage.setArticleId(objectId);
				userMessage.setArticleTitle(forumArticle.getTitle());
				userMessage.setMessageType(MessageTypeEnum.ARTICLE_LIKE.getType());
				userMessage.setCommentId(Constants.ZERO);
				userMessage.setReceivedUserId(forumArticle.getUserId());
				break;
			case COMMENT_LIKE:
				//获取评论
				ForumComment forumComment = forumCommentMapper.selectByCommentId(Integer.parseInt(objectId));
				if(forumComment == null){
					throw new BusinessException("评论不存在");
				}
				commentLike(objectId,userId,opTypeEnum,forumComment);
				//获取文章
				forumArticle = forumArticleMapper.selectByArticleId(forumComment.getArticleId());
				//记录消息
				userMessage.setArticleId(forumArticle.getArticleId());
				userMessage.setArticleTitle(forumArticle.getTitle());
				userMessage.setMessageType(MessageTypeEnum.COMMENT_LIKE.getType());
				userMessage.setCommentId(forumComment.getCommentId());
				userMessage.setReceivedUserId(forumComment.getUserId());
				userMessage.setMessageContent(forumComment.getContent());
				break;
		}
		userMessage.setSendUserId(userId);
		userMessage.setSendNickName(nickName);
		userMessage.setStatus(MessageStatusEnum.NO_READ.getStatus());
		if(!userId.equals(userMessage.getReceivedUserId())){
			UserMessage dbInfo = userMessageMapper.selectByArticleIdAndCommentIdAndSendUserIdAndMessageType(userMessage.getArticleId(),userMessage.getCommentId(),userMessage.getSendUserId(),userMessage.getMessageType());
			if(dbInfo == null) {
				userMessageMapper.insert(userMessage);
			}
		}
	}

	//文章点赞
	public void articleLike(String objectId,String userId,OperRecordOpTypeEnum opTypeEnum,ForumArticle forumArticle){
		LikeRecord record = this.likeRecordMapper.selectByObjectIdAndUserIdAndOpType(objectId,userId,opTypeEnum.getType());
		Integer changeCount = 0;
		//如已点赞
		if(record != null){
			this.likeRecordMapper.deleteByObjectIdAndUserIdAndOpType(objectId,userId,opTypeEnum.getType());
			//点赞数减1
			changeCount = -1;
		}
		//未点赞
		else {
			LikeRecord likeRecord = new LikeRecord();
			likeRecord.setObjectId(objectId);
			likeRecord.setUserId(userId);
			likeRecord.setOpType(opTypeEnum.getType());
			likeRecord.setCreateTime(new Date());
			likeRecord.setAuthorUserId(forumArticle.getUserId());
			//插入点赞记录
			this.likeRecordMapper.insert(likeRecord);

			//点赞数加1
			changeCount = 1;
		}
		this.forumArticleMapper.updateArticleCount(UpdateArticleCountTypeEnum.GOOD_COUNT.getType(), changeCount,objectId);
	}

	//评论点赞
	public void commentLike(String objectId,String userId,OperRecordOpTypeEnum opTypeEnum,ForumComment forumComment){
		LikeRecord record = this.likeRecordMapper.selectByObjectIdAndUserIdAndOpType(objectId,userId,opTypeEnum.getType());
		Integer changeCount = 0;
		//如已点赞
		if(record != null){
			this.likeRecordMapper.deleteByObjectIdAndUserIdAndOpType(objectId,userId,opTypeEnum.getType());
			//点赞数减1
			changeCount = -1;
		}
		//未点赞
		else {
			LikeRecord likeRecord = new LikeRecord();
			likeRecord.setObjectId(objectId);
			likeRecord.setUserId(userId);
			likeRecord.setOpType(opTypeEnum.getType());
			likeRecord.setCreateTime(new Date());
			likeRecord.setAuthorUserId(forumComment.getUserId());
			//插入点赞记录
			this.likeRecordMapper.insert(likeRecord);

			//点赞数加1
			changeCount = 1;
		}
		this.forumCommentMapper.updateCommentGoodCount(changeCount,Integer.parseInt(objectId));
	}
}