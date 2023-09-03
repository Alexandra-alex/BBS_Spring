package com.easybbs.entity.dto;

//点赞设置
public class SysSetting4LikeDto {
    //点赞数量阈值
    private Integer likeDayCountThreshold;
    public Integer getLikeDayCountThreshold(){return likeDayCountThreshold;}
    public void setLikeDayCountThreshold(Integer likeDayCountThreshold){this.likeDayCountThreshold = likeDayCountThreshold;}
}
