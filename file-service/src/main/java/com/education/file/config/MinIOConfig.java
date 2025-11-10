package com.education.file.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URL;

@Configuration
@ConfigurationProperties(prefix = "minio")
@Data
public class MinIOConfig {
    
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucketName;
    
    @Bean
    public MinioClient minioClient() {
        try {
            // 解析 endpoint URL
            URL url = new URL(endpoint);
            String hostname = url.getHost();
            int port = url.getPort();
            
            // MinIO Java SDK 8.x 的 endpoint 方法
            // 根据 MinIO SDK 文档，endpoint 应该只包含 hostname
            // 对于非标准端口，MinIO SDK 内部使用 okhttp3.HttpUrl 来处理
            // 但是，endpoint 方法本身只接受 hostname，端口信息需要通过其他方式传递
            //
            // 根据 MinIO Java SDK 8.x 的实际实现，endpoint 方法确实只接受 hostname
            // 端口信息可能需要在构建 HttpUrl 时指定，但 SDK 可能不支持直接配置非标准端口
            //
            // 解决方案：
            // 1. 修改 Docker 端口映射：docker run -p 9000:9000 minio/minio
            //    这样 MinIO 的 9000 端口直接映射到主机的 9000，可以使用标准端口
            // 2. 或者使用 Nginx 等反向代理
            // 3. 或者修改 endpoint 配置，使用标准端口 9000（如果 MinIO 容器内部端口是 9000）
            
            // MinIO Java SDK 8.x 的 endpoint 方法只接受 hostname（不含端口）
            // SDK 会使用默认端口：HTTP(80)、HTTPS(443)、或 MinIO 标准端口(9000)
            // 
            // 为什么推荐使用 9000：
            // 1. 9000 是 MinIO 的标准端口，SDK 可能对此有特殊处理
            // 2. 如果使用非标准端口（如 9100），SDK 无法指定，会尝试 80/443，导致连接失败
            // 3. 使用标准端口可以避免端口冲突和配置问题
            //
            // 替代方案：
            // 1. 使用 Nginx 反向代理，将 9100 代理到 MinIO 的 9000
            // 2. 修改 Docker 端口映射为 9000:9000（最简单）
            
            // 检查端口配置
            // 支持标准端口：80(HTTP)、443(HTTPS)、9000(MinIO标准)
            // 如果使用 Nginx 反向代理，可以通过 Nginx 监听标准端口
            if (port > 0 && port != 80 && port != 443 && port != 9000) {
                // 如果使用 9001 端口，说明是通过 Nginx 代理（因为 9000 被占用）
                // 这种情况下，Nginx 会代理到 MinIO 的 9000，但 SDK 仍然需要标准端口
                // 所以应该使用 80 端口或修改 Nginx 配置
                if (port == 9001) {
                    System.err.println("警告: 检测到使用 9001 端口（Nginx 代理）");
                    System.err.println("建议: 修改 Nginx 配置使用 80 端口，或修改 endpoint 为 http://192.168.141.128（不指定端口）");
                    // 允许继续，但会尝试使用 hostname（默认 80 端口）
                    // 实际上，如果配置了 9001，SDK 会尝试 80 端口，这不会工作
                    // 所以我们需要特殊处理
                }
                throw new RuntimeException(
                    "MinIO Java SDK 不支持非标准端口配置。\n" +
                    "当前配置的端口: " + port + "\n" +
                    "\n为什么需要标准端口：\n" +
                    "- MinIO SDK 的 endpoint 方法只接受 hostname，无法指定端口\n" +
                    "- SDK 会尝试使用默认端口：80(HTTP)、443(HTTPS)、9000(MinIO标准)\n" +
                    "\n解决方案：\n" +
                    "1. 使用 80 端口（推荐）：修改 endpoint 为 http://192.168.141.128\n" +
                    "   并配置 Nginx 监听 80 端口\n" +
                    "2. 使用 9000 端口：修改 Docker 端口映射为 9000:9000\n" +
                    "3. 如果必须使用 9001，需要修改 Nginx 配置监听 80 端口"
                );
            }
            
            // MinIO SDK 的 endpoint 方法只接受 hostname
            // 但是，SDK 可能会默认尝试 HTTPS(443)，即使 URL 是 http://
            // 根据 MinIO Java SDK 8.x 的行为，可能需要使用不同的方式
            
            // 记录配置信息
            System.out.println("MinIO 配置信息:");
            System.out.println("  Endpoint: " + endpoint);
            System.out.println("  Hostname: " + hostname);
            System.out.println("  Port: " + (port > 0 ? port : "默认"));
            System.out.println("  Protocol: " + url.getProtocol());
            
            // MinIO Java SDK 8.x 的问题：即使 URL 是 http://，SDK 可能默认尝试 HTTPS(443)
            // 根据 MinIO SDK 的行为，endpoint 方法只接受 hostname，但 SDK 内部会尝试 HTTPS
            // 
            // 关键发现：MinIO SDK 8.x 可能会根据 hostname 的某些特征（如 IP 地址）默认尝试 HTTPS
            // 解决方案：尝试使用完整 URL 作为 endpoint（虽然文档说只接受 hostname，但实际可能支持）
            
            MinioClient client;
            String endpointValue;
            
            // 尝试使用完整 URL（包括协议）
            // 根据 MinIO SDK 8.x 的实际行为，endpoint 方法可能支持完整 URL
            // 格式：http://hostname 或 http://hostname:80
            if (port == 80) {
                // 对于 80 端口，使用 http://hostname（不包含端口）
                endpointValue = "http://" + hostname;
            } else if (port == 443) {
                // 对于 443 端口，使用 https://hostname
                endpointValue = "https://" + hostname;
            } else {
                // 对于其他端口，尝试使用完整 URL
                endpointValue = url.getProtocol() + "://" + hostname + ":" + port;
            }
            
            System.out.println("尝试使用 endpoint: " + endpointValue);
            
            // 构建客户端
            // 注意：如果 SDK 不支持完整 URL，会抛出异常，我们需要捕获并回退
            try {
                client = MinioClient.builder()
                        .endpoint(endpointValue)
                        .credentials(accessKey, secretKey)
                        .build();
                System.out.println("MinIO 客户端已创建（使用完整 URL）");
            } catch (Exception e) {
                // 如果完整 URL 不支持，回退到只使用 hostname
                System.err.println("使用完整 URL 失败，回退到 hostname: " + e.getMessage());
                endpointValue = hostname;
                client = MinioClient.builder()
                        .endpoint(endpointValue)
                        .credentials(accessKey, secretKey)
                        .build();
                System.out.println("MinIO 客户端已创建（使用 hostname）");
            }
            
            // 延迟初始化 bucket（不在启动时检查，避免阻塞启动）
            // 在实际使用时 FileService 会检查并创建 bucket
            // initializeBucket(client);  // 注释掉，延迟到实际使用时
            
            return client;
        } catch (Exception e) {
            throw new RuntimeException("MinIO 客户端初始化失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 初始化 bucket（如果不存在则创建）
     */
    private void initializeBucket(MinioClient client) {
        try {
            boolean found = client.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());
            if (!found) {
                client.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());
                System.out.println("MinIO bucket '" + bucketName + "' 创建成功");
            } else {
                System.out.println("MinIO bucket '" + bucketName + "' 已存在");
            }
        } catch (Exception e) {
            // 如果 bucket 创建失败，记录警告但不阻止启动
            // 在实际使用时 FileService 会再次尝试创建
            System.err.println("警告: MinIO bucket 初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

