package com.easybbs.entity.vo.web;

public class ForumArticleDetailVO {
    //文章详情页
    private ForumArticleVO forumArticle;
    //附件
    private ForumArticleAttachmentVO attachment;
    //是否可以点赞
    private Boolean haveLike;

    public ForumArticleVO getForumArticle() {
        return forumArticle;
    }

    public void setForumArticle(ForumArticleVO forumArticle) {
        this.forumArticle = forumArticle;
    }

    public ForumArticleAttachmentVO getAttachment() {
        return attachment;
    }

    public void setAttachment(ForumArticleAttachmentVO attachment) {
        this.attachment = attachment;
    }

    public Boolean getHaveLike() {
        return haveLike;
    }

    public void setHaveLike(Boolean haveLike) {
        this.haveLike = haveLike;
    }
}
