package com.easybbs.mappers;

import org.apache.ibatis.annotations.Param;

/**
 * 评论 数据库操作接口
 */
public interface ForumCommentMapper<T,P> extends BaseMapper<T,P> {

	/**
	 * 根据CommentId更新
	 */
	 Integer updateByCommentId(@Param("bean") T t,@Param("commentId") Integer commentId);


	/**
	 * 根据CommentId删除
	 */
	 Integer deleteByCommentId(@Param("commentId") Integer commentId);


	/**
	 * 根据CommentId获取对象
	 */
	 T selectByCommentId(@Param("commentId") Integer commentId);

	//根据commentId更新评论点赞数
	void updateCommentGoodCount(@Param("changeCount") Integer changeCount,
							@Param("commentId") Integer commentId);

	//根据articleId更新置顶
	void updateTopTypeByArticleId(@Param("articleId") String articleId);
}
