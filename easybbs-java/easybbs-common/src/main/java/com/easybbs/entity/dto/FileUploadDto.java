package com.easybbs.entity.dto;

public class FileUploadDto {
    //本地路径
    private String localPath;
    //真实名称
    private String originalFileName;

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }
}
