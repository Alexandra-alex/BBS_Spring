package com.easybbs.entity.dto;

//发帖设置
public class SysSetting4PostDto {
    //发帖积分
    private Integer postIntegral;
    //一天发帖数量
    private Integer postDayCountThreshold;
    //一天上传图片数量
    private Integer dayImageUploadCount;
    //附件大小
    private Integer attachmentSize;
    public Integer getPostIntegral() {return postIntegral;}
    public void setPostIntegral(Integer postIntegral) {this.postIntegral = postIntegral;}
    public Integer getPostDayCountThreshold() {return postDayCountThreshold;}
    public void setPostDayCountThreshold(Integer postDayCountThreshold){this.postDayCountThreshold = postDayCountThreshold;}
    public Integer getDayImageUploadCount(){return dayImageUploadCount;}
    public void setDayImageUploadCount(Integer dayImageUploadCount){this.dayImageUploadCount = dayImageUploadCount;}
    public Integer getAttachmentSize() {return attachmentSize;}
    public void setAttachmentSize(Integer attachmentSize) {this.attachmentSize= attachmentSize;}
}
