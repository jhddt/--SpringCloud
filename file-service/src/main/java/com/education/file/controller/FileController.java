package com.education.file.controller;

import com.education.common.result.Result;
import com.education.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {
    
    private final FileService fileService;
    
    @PostMapping("/upload/avatar")
    public Result<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String url = fileService.uploadAvatar(file);
        Map<String, String> result = new HashMap<>();
        result.put("url", url);
        return Result.success("上传成功", result);
    }
    
    @PostMapping("/upload/course-cover")
    public Result<Map<String, String>> uploadCourseCover(@RequestParam("file") MultipartFile file) {
        String url = fileService.uploadCourseCover(file);
        Map<String, String> result = new HashMap<>();
        result.put("url", url);
        return Result.success("上传成功", result);
    }
    
    @GetMapping("/download")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.InputStreamResource> download(
            @RequestParam("fileName") String fileName) {
        InputStream inputStream = fileService.downloadFile(fileName);
        return org.springframework.http.ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new org.springframework.core.io.InputStreamResource(inputStream));
    }
    
    /**
     * 通过后端代理访问文件（用于显示图片）
     * 路径格式：/file/view?path=bucket-name/object-name
     */
    @GetMapping("/view")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.InputStreamResource> viewFile(
            @RequestParam("path") String path) {
        try {
            // path 格式：bucket-name/object-name 或 bucket-name/folder/object-name
            // 注意：path 参数会自动进行 URL 解码
            System.out.println("收到文件查看请求，path: " + path);
            
            String[] parts = path.split("/", 2);
            if (parts.length < 2) {
                System.err.println("路径格式错误，parts.length: " + parts.length);
                return org.springframework.http.ResponseEntity.notFound().build();
            }
            
            String bucketName = parts[0];
            String objectName = parts[1];
            
            System.out.println("解析结果 - bucketName: " + bucketName + ", objectName: " + objectName);
            
            InputStream inputStream = fileService.downloadFileFromBucket(bucketName, objectName);
            
            // 根据文件扩展名设置 Content-Type
            String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            String lowerObjectName = objectName.toLowerCase();
            if (lowerObjectName.endsWith(".jpg") || lowerObjectName.endsWith(".jpeg")) {
                contentType = MediaType.IMAGE_JPEG_VALUE;
            } else if (lowerObjectName.endsWith(".png")) {
                contentType = MediaType.IMAGE_PNG_VALUE;
            } else if (lowerObjectName.endsWith(".gif")) {
                contentType = MediaType.IMAGE_GIF_VALUE;
            } else if (lowerObjectName.endsWith(".webp")) {
                contentType = "image/webp";
            }
            
            System.out.println("返回文件，Content-Type: " + contentType);
            
            return org.springframework.http.ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header("Cache-Control", "public, max-age=31536000") // 缓存 1 年
                    .body(new org.springframework.core.io.InputStreamResource(inputStream));
        } catch (Exception e) {
            System.err.println("文件查看失败: " + e.getMessage());
            e.printStackTrace();
            return org.springframework.http.ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/delete")
    public Result<?> delete(@RequestParam("fileName") String fileName) {
        fileService.deleteFile(fileName);
        return Result.success("删除成功");
    }
}

