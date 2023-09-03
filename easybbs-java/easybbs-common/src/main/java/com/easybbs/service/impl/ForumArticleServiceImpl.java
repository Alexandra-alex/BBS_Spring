package com.easybbs.service.impl;

import java.io.File;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import com.easybbs.entity.config.AppConfig;
import com.easybbs.entity.constants.Constants;
import com.easybbs.entity.dto.FileUploadDto;
import com.easybbs.entity.dto.SysSetting4AuditDto;
import com.easybbs.entity.enums.*;
import com.easybbs.entity.po.ForumArticleAttachment;
import com.easybbs.entity.po.ForumBoard;
import com.easybbs.entity.query.ForumArticleAttachmentQuery;
import com.easybbs.exception.BusinessException;
import com.easybbs.mappers.ForumArticleAttachmentMapper;
import com.easybbs.service.ForumBoardService;
import com.easybbs.service.UserInfoService;
import com.easybbs.utils.FileUtils;
import com.easybbs.utils.ImageUtils;
import com.easybbs.utils.SysCacheUtils;
import com.sun.xml.internal.ws.api.message.Attachment;
import org.springframework.stereotype.Service;

import com.easybbs.entity.query.ForumArticleQuery;
import com.easybbs.entity.po.ForumArticle;
import com.easybbs.entity.vo.PaginationResultVO;
import com.easybbs.entity.query.SimplePage;
import com.easybbs.mappers.ForumArticleMapper;
import com.easybbs.service.ForumArticleService;
import com.easybbs.utils.StringTools;
import org.springframework.web.multipart.MultipartFile;


/**
 * 文章信息 业务接口实现
 */
@Service("forumArticleService")
public class ForumArticleServiceImpl implements ForumArticleService {

	@Resource
	private ForumArticleMapper<ForumArticle, ForumArticleQuery> forumArticleMapper;

	@Resource
	private ForumBoardService forumBoardService;

	@Resource
	private FileUtils fileUtils;

	@Resource
	private ForumArticleAttachmentMapper<ForumArticleAttachment, ForumArticleAttachmentQuery> attachmentMapper;

	@Resource
	private UserInfoService userInfoService;

	@Resource
	private ImageUtils imageUtils;

	@Resource
	private AppConfig appConfig;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<ForumArticle> findListByParam(ForumArticleQuery param) {
		return this.forumArticleMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(ForumArticleQuery param) {
		return this.forumArticleMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<ForumArticle> findListByPage(ForumArticleQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<ForumArticle> list = this.findListByParam(param);
		PaginationResultVO<ForumArticle> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(ForumArticle bean) {
		return this.forumArticleMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<ForumArticle> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.forumArticleMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<ForumArticle> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.forumArticleMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(ForumArticle bean, ForumArticleQuery param) {
		StringTools.checkParam(param);
		return this.forumArticleMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(ForumArticleQuery param) {
		StringTools.checkParam(param);
		return this.forumArticleMapper.deleteByParam(param);
	}

	/**
	 * 根据ArticleId获取对象
	 */
	@Override
	public ForumArticle getForumArticleByArticleId(String articleId) {
		return this.forumArticleMapper.selectByArticleId(articleId);
	}

	/**
	 * 根据ArticleId修改
	 */
	@Override
	public Integer updateForumArticleByArticleId(ForumArticle bean, String articleId) {
		return this.forumArticleMapper.updateByArticleId(bean, articleId);
	}

	/**
	 * 根据ArticleId删除
	 */
	@Override
	public Integer deleteForumArticleByArticleId(String articleId) {
		return this.forumArticleMapper.deleteByArticleId(articleId);
	}

	//根据ArticleId更新文章阅读数
	@Override
	public ForumArticle readArticle(String articleId) {
		ForumArticle forumArticle = this.forumArticleMapper.selectByArticleId(articleId);
		//判断是否为空
		if(forumArticle == null){
			throw new BusinessException(ResponseCodeEnum.CODE_404);
		}
		//判断是否已审核
		if(ArticleStatusEnum.AUDIT.getStatus().equals(forumArticle.getStatus())){
			//更新文章阅读数
			this.forumArticleMapper.updateArticleCount(UpdateArticleCountTypeEnum.READ_COUNT.getType(), Constants.ONE,articleId);
		}
		return forumArticle;
	}

	//文章上传
	@Override
	public void postArticle(Boolean isAdmin, ForumArticle article, ForumArticleAttachment articleAttachment, MultipartFile cover, MultipartFile attachment) {
		resetBoardInfo(isAdmin,article);

		Date curData = new Date();
		String articleId = StringTools.getRandomString(Constants.LENGTH_15);
		article.setArticleId(articleId);
		article.setPostTime(curData);
		article.setLastUpdateTime(curData);

		//文章封面上传
		if(cover != null) {
			FileUploadDto fileUploadDto = fileUtils.uploadFile2Local(cover, Constants.FILE_FOLDER_IMAGE,FileUploadTypeEnum.ARTICLE_COVER);
			article.setCover(fileUploadDto.getLocalPath());
		}

		//附件上传
		if(attachment != null) {
			uploadAttachment(article,articleAttachment,attachment,false);
			article.setAttachmentType(ArticleAttachmentTypeEnum.HAVE.getType());
		}else {
			article.setAttachmentType(ArticleAttachmentTypeEnum.NO.getType());
		}
		//设置审核信息(是否需要审核)
		if(isAdmin){
			article.setStatus(ArticleStatusEnum.AUDIT.getStatus());
		}else {
			SysSetting4AuditDto auditDto = SysCacheUtils.getSysSetting().getAuditSetting();
			article.setStatus(auditDto.getPostAudit() ? ArticleStatusEnum.NO_AUDIT.getStatus() : ArticleStatusEnum.AUDIT.getStatus());
		}

		//替换图片源(将临时目录中的图片复制到真实目录中去)
		String content = article.getContent();
		if(!StringTools.isEmpty(content))
		{
			String month = imageUtils.resetImageHtml(content);
			String replaceMonth = "/" + month + "/";
			content = content.replace(Constants.FILE_FOLDER_TEMP,replaceMonth);
			article.setContent(content);
			String markdownContent = article.getMarkdownContent();
			if(!StringTools.isEmpty(markdownContent))
			{
				markdownContent = markdownContent.replace(Constants.FILE_FOLDER_TEMP,replaceMonth);
				article.setMarkdownContent(markdownContent);
			}
		}

		this.forumArticleMapper.insert(article);

		//增加积分
		Integer postIntegral = SysCacheUtils.getSysSetting().getPostSetting().getPostIntegral();
		if(postIntegral > 0 && ArticleStatusEnum.AUDIT.getStatus().equals(article.getStatus())){
			userInfoService.updateUserIntegral(article.getUserId(),UserIntegralOperTypeEnum.POST_ARTICLE,UserIntegralChangeTypeEnum.ADD.getChangeType(), postIntegral);
		}
	}

	//更新文章
	@Override
	public void updateArticle(Boolean isAdmin, ForumArticle article, ForumArticleAttachment articleAttachment, MultipartFile cover, MultipartFile attachment) {
		ForumArticle dbInfo  = forumArticleMapper.selectByArticleId(article.getArticleId());
		//修改者与作者是否相同
		if(!isAdmin && !dbInfo.getUserId().equals(article.getUserId())){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		article.setLastUpdateTime(new Date());

		resetBoardInfo(isAdmin,article);
		//文章封面更新
		if(cover != null){
			FileUploadDto fileUploadDto = fileUtils.uploadFile2Local(cover, Constants.FILE_FOLDER_IMAGE,FileUploadTypeEnum.ARTICLE_COVER);
			article.setCover(fileUploadDto.getLocalPath());
		}
		//附件更新
		if(attachment != null) {
			uploadAttachment(article, articleAttachment, attachment, true);
			article.setAttachmentType(ArticleAttachmentTypeEnum.HAVE.getType());
		}

		ForumArticleAttachment dbAttachment = null;
		ForumArticleAttachmentQuery query = new ForumArticleAttachmentQuery();
		query.setArticleId(article.getArticleId());
		//获取原文章信息
		List<ForumArticleAttachment> articleAttachmentList = this.attachmentMapper.selectList(query);
		if(!articleAttachmentList.isEmpty()){
			dbAttachment = articleAttachmentList.get(0);
		}
		if(dbAttachment != null){
			//不修改并删除附件,并清除数据库中记录
			if(article.getAttachmentType() == Constants.ZERO){
				new File(appConfig.getProjectFolder()+Constants.FILE_FOLDER_FILE+Constants.FILE_FOLDER_ATTACHMENT+dbAttachment.getFilePath()).delete();
				this.attachmentMapper.deleteByFileId(dbAttachment.getFileId());
			}else {
				//更新积分
				if(!dbAttachment.getIntegral().equals(articleAttachment.getIntegral())){
					ForumArticleAttachment integralUpdate = new ForumArticleAttachment();
					integralUpdate.setIntegral(articleAttachment.getIntegral());
					this.attachmentMapper.updateByFileId(integralUpdate, dbAttachment.getFileId());
				}
			}
		}
		if(isAdmin){
			article.setStatus(ArticleStatusEnum.AUDIT.getStatus());
		}else{
			SysSetting4AuditDto auditDto = SysCacheUtils.getSysSetting().getAuditSetting();
			article.setStatus(auditDto.getPostAudit() ? ArticleStatusEnum.NO_AUDIT.getStatus() : ArticleStatusEnum.AUDIT.getStatus());
		}

		//替换图片源(将临时目录中的图片复制到真实目录中去)
		String content = article.getContent();
		if(!StringTools.isEmpty(content))
		{
			String month = imageUtils.resetImageHtml(content);
			String replaceMonth = "/" + month + "/";
			content = content.replace(Constants.FILE_FOLDER_TEMP,replaceMonth);
			article.setContent(content);
			String markdownContent = article.getMarkdownContent();
			if(!StringTools.isEmpty(markdownContent))
			{
				markdownContent = markdownContent.replace(Constants.FILE_FOLDER_TEMP,replaceMonth);
				article.setMarkdownContent(markdownContent);
			}
		}

		this.forumArticleMapper.updateByArticleId(article,article.getArticleId());
	}


	private void resetBoardInfo(Boolean isAdmin, ForumArticle article){
		ForumBoard pBoard = forumBoardService.getForumBoardByBoardId(article.getpBoardId());
		//校验一级板块
		if(pBoard == null || pBoard.getPostType() == Constants.ZERO && !isAdmin){
			throw new BusinessException("一级板块不存在");
		}
		article.setpBoardName(pBoard.getBoardName());
		//校验二级板块
		if(article.getBoardId() != null && article.getBoardId() != 0){
			ForumBoard board = forumBoardService.getForumBoardByBoardId(article.getBoardId());
			if(board == null || board.getPostType() == Constants.ZERO && !isAdmin){
				throw new BusinessException("二级板块不存在");
			}
			article.setBoardName(board.getBoardName());
		}else{
			article.setBoardId(0);
			article.setBoardName("");
		}
	}

	//上传附件
	public void uploadAttachment(ForumArticle article,ForumArticleAttachment articleAttachment,MultipartFile file,Boolean isUpdate){
		//限制文件大小
		Integer allowSizeMb = SysCacheUtils.getSysSetting().getPostSetting().getAttachmentSize();
		long allowSize = (long) allowSizeMb *Constants.FILE_SIZE_1M;
		if(file.getSize() > allowSize)
		{
			throw new BusinessException("附件最大只能上传"+allowSize+"MB");
		}
		//修改附件
		ForumArticleAttachment dbInfo = null;
		if(isUpdate){
			ForumArticleAttachmentQuery query = new ForumArticleAttachmentQuery();
			query.setArticleId(article.getArticleId());
			//获取原文章信息
			List<ForumArticleAttachment> articleAttachmentList = this.attachmentMapper.selectList(query);
			//如果附件存在则删除原附件
			if(!articleAttachmentList.isEmpty()){
				dbInfo = articleAttachmentList.get(0);
				new File(appConfig.getProjectFolder()+Constants.FILE_FOLDER_FILE+Constants.FILE_FOLDER_ATTACHMENT+dbInfo.getFilePath()).delete();
			}
		}
		FileUploadDto fileUploadDto = fileUtils.uploadFile2Local(file,Constants.FILE_FOLDER_ATTACHMENT,FileUploadTypeEnum.ARTICLE_ATTACHMENT);

		if(dbInfo == null)
		{
			//初始化附件记录
			articleAttachment.setFileId(StringTools.getRandomNumber(Constants.LENGTH_15));
			articleAttachment.setArticleId(article.getArticleId());
			articleAttachment.setFileName(fileUploadDto.getOriginalFileName());
			articleAttachment.setFilePath(fileUploadDto.getLocalPath());
			articleAttachment.setFileSize(file.getSize());
			articleAttachment.setDownloadCount(Constants.ZERO);
			articleAttachment.setFileType(AttachmentFileTypeEnum.ZIP.getType());
			articleAttachment.setUserId(article.getUserId());
			attachmentMapper.insert(articleAttachment);
		}else {
			//修改附件记录
			ForumArticleAttachment updateInfo = new ForumArticleAttachment();
			updateInfo.setFileName(fileUploadDto.getOriginalFileName());
			updateInfo.setFileSize(file.getSize());
			updateInfo.setFilePath(fileUploadDto.getLocalPath());
			attachmentMapper.updateByFileId(updateInfo, dbInfo.getFileId());
		}
	}
}