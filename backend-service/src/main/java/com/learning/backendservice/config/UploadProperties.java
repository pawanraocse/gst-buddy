package com.learning.backendservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

@Component
@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {

    private DataSize maxFileSize = DataSize.ofMegabytes(10);
    private int maxFiles = 20;
    private int maxConcurrentUploads = 2;

    public DataSize getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(DataSize maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public int getMaxFiles() {
        return maxFiles;
    }

    public void setMaxFiles(int maxFiles) {
        this.maxFiles = maxFiles;
    }

    public int getMaxConcurrentUploads() {
        return maxConcurrentUploads;
    }

    public void setMaxConcurrentUploads(int maxConcurrentUploads) {
        this.maxConcurrentUploads = maxConcurrentUploads;
    }
}
