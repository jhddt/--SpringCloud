package com.education.file.service;

import com.education.file.config.MinIOConfig;
import com.education.common.exception.BusinessException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.http.Method;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {
    
    private final MinioClient minioClient;
    private final MinIOConfig minIOConfig;
    
    public String uploadFile(MultipartFile file, String folder) {
        try {
            // 检查bucket是否存在
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(minIOConfig.getBucketName())
                    .build());
            
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(minIOConfig.getBucketName())
                        .build());
            }
            
            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String fileName = folder + "/" + UUID.randomUUID() + extension;
            
            // 上传文件
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minIOConfig.getBucketName())
                    .object(fileName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            
            // 返回访问URL
            // 方案1：使用后端代理 URL（推荐，更可靠，不依赖 MinIO 的访问策略）
            // 方案2：使用 presigned URL（如果方案1不行）
            // 方案3：使用直接访问 URL（需要 bucket 为 public）
            
            // 方案1：使用后端代理 URL（通过 file-service 代理访问）
            // 格式：/api/file/view?path=bucket-name/object-name
            // 注意：需要对 path 参数进行 URL 编码
            String path = minIOConfig.getBucketName() + "/" + fileName;
            try {
                // URL 编码 path 参数
                String encodedPath = java.net.URLEncoder.encode(path, "UTF-8");
                String proxyUrl = "/api/file/view?path=" + encodedPath;
                log.info("文件上传成功，代理访问 URL: {}", proxyUrl);
                return proxyUrl;
            } catch (java.io.UnsupportedEncodingException e) {
                // 如果编码失败，使用原始路径
                String proxyUrl = "/api/file/view?path=" + path;
                log.warn("URL 编码失败，使用原始路径: {}", proxyUrl);
                return proxyUrl;
            }
            
            // 如果方案1不行，可以尝试方案2或方案3（取消下面的注释）
            /*
            // 方案2：使用 presigned URL（有效期 7 天）
            try {
                String presignedUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(minIOConfig.getBucketName())
                        .object(fileName)
                        .expiry(7, TimeUnit.DAYS)
                        .build()
                );
                log.info("文件上传成功，Presigned URL: {}", presignedUrl);
                return presignedUrl;
            } catch (Exception e) {
                // 如果 presigned URL 失败，使用直接访问 URL
                log.warn("生成 presigned URL 失败，使用直接访问 URL: {}", e.getMessage());
                String endpoint = minIOConfig.getEndpoint();
                // 去掉端口 80（因为 80 是默认端口）
                if (endpoint.endsWith(":80")) {
                    endpoint = endpoint.replace(":80", "");
                }
                // 构建完整的访问 URL（需要 bucket 为 public）
                String url = endpoint + "/" + minIOConfig.getBucketName() + "/" + fileName;
                log.info("文件上传成功，直接访问 URL: {}", url);
                return url;
            }
            */
            
        } catch (Exception e) {
            log.error("文件上传失败: endpoint={}, bucket={}, error={}", 
                    minIOConfig.getEndpoint(), minIOConfig.getBucketName(), e.getMessage(), e);
            String errorMsg = "文件上传失败";
            if (e.getMessage() != null) {
                if (e.getMessage().contains("404")) {
                    errorMsg = "文件服务连接失败，请检查 MinIO 服务是否正常运行";
                } else if (e.getMessage().contains("Connection refused")) {
                    errorMsg = "无法连接到文件服务器，请检查网络连接";
                } else {
                    errorMsg = "文件上传失败：" + e.getMessage();
                }
            }
            throw new BusinessException(500, errorMsg);
        }
    }
    
    public InputStream downloadFile(String fileName) {
        return downloadFileFromBucket(minIOConfig.getBucketName(), fileName);
    }
    
    /**
     * 从指定 bucket 下载文件
     */
    public InputStream downloadFileFromBucket(String bucketName, String objectName) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            log.error("文件下载失败: bucket={}, object={}, error={}", bucketName, objectName, e.getMessage(), e);
            throw new BusinessException(500, "文件下载失败：" + e.getMessage());
        }
    }
    
    public void deleteFile(String fileName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minIOConfig.getBucketName())
                    .object(fileName)
                    .build());
        } catch (Exception e) {
            log.error("文件删除失败", e);
            throw new BusinessException(500, "文件删除失败：" + e.getMessage());
        }
    }
    
    public String uploadAvatar(MultipartFile file) {
        return uploadFile(file, "avatars");
    }
    
    public String uploadCourseCover(MultipartFile file) {
        return uploadFile(file, "course-covers");
    }
}

