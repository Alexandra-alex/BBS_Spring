package com.easybbs.entity.enums;

//SysSetting枚举
public enum SysSettingCodeEnum {
    AUDIT("audit","com.easybbs.entity.dto.SysSetting4AuditDto","auditSetting","审核设置"),
    COMMENT("comment","com.easybbs.entity.dto.SysSetting4CommentDto","commentSetting","评论设置"),
    POST("post","com.easybbs.entity.dto.SysSetting4PostDto","postSetting","帖子设置"),
    LIKE("like","com.easybbs.entity.dto.SysSetting4LikeDto","likeSetting","点赞设置"),
    REGISTER("register","com.easybbs.entity.dto.SysSetting4RegisterDto","registerSetting","注册设置"),
    EMAIL("email","com.easybbs.entity.dto.SysSetting4EmailDto","emailSetting","邮件设置");

    private final String code;
    private final String clazz;
    //类
    private final String propName;
    //描述
    private final String desc;

    public static SysSettingCodeEnum getByCode(String code) {
        for(SysSettingCodeEnum item:SysSettingCodeEnum.values()){
            //比较code值并返回item
            if(item.getCode().equals(code)){
                return item;
            }
        }
        return null;
    }

    SysSettingCodeEnum(String code, String clazz, String propName, String desc){
        this.code = code;
        this.clazz  = clazz;
        this.propName = propName;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getClazz() {
        return clazz;
    }

    public String getPropName() {
        return propName;
    }

    public String getDesc() {
        return desc;
    }
}
