package com.easybbs.entity.enums;

public enum AttachmentFileTypeEnum {
    ZIP(0, new String[]{".zip", ".rar"}, "压缩包");

    private Integer type;
    private String[] suffixes;
    private String desc;

    AttachmentFileTypeEnum(Integer type, String[] suffixes, String desc)
    {
        this.type = type;
        this.suffixes = suffixes;
        this.desc = desc;
    }

    public static AttachmentFileTypeEnum getByType(Integer type){
        for(AttachmentFileTypeEnum item : AttachmentFileTypeEnum.values()){
            if(item.getType().equals(type)){
                return item;
            }
        }
        return null;
    }

    public Integer getType() {
        return type;
    }

    public String[] getSuffixes() {
        return suffixes;
    }

    public String getDesc() {
        return desc;
    }
}
