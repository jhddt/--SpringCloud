#!/bin/bash
# Docker Nginx 反向代理 MinIO 配置脚本（使用 80 端口）

echo "=== Docker Nginx 反向代理 MinIO 配置（80端口）==="

# 检查 80 端口占用
echo "检查 80 端口占用情况..."
if netstat -tuln | grep -q ":80 "; then
    echo "警告: 80 端口已被占用！"
    echo "占用进程:"
    netstat -tuln | grep ":80 "
    exit 1
fi

# 创建 Nginx 配置目录
mkdir -p /opt/nginx/conf.d

# 创建 Nginx 配置文件（使用 80 端口）
cat > /opt/nginx/conf.d/minio.conf <<'EOF'
upstream minio_backend {
    # 代理到 MinIO 容器（通过 Docker 网络）
    server minio:9000;
}

server {
    listen 80;
    server_name _;

    # 允许大文件上传
    client_max_body_size 1000M;

    # MinIO API 代理
    location / {
        proxy_pass http://minio_backend;
        proxy_set_header Host $http_host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # WebSocket 支持
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        
        # 超时设置
        proxy_connect_timeout 300;
        proxy_send_timeout 300;
        proxy_read_timeout 300;
        
        # 缓冲设置
        proxy_buffering off;
        proxy_request_buffering off;
    }
}
EOF

echo "Nginx 配置文件已创建: /opt/nginx/conf.d/minio.conf"

# 停止并删除旧的 Nginx 容器（如果存在）
docker stop nginx-minio 2>/dev/null
docker rm nginx-minio 2>/dev/null

# 启动 Nginx 容器
echo "启动 Nginx 容器..."
docker run -d \
  --name nginx-minio \
  --network bridge \
  -p 80:80 \
  -v /opt/nginx/conf.d:/etc/nginx/conf.d:ro \
  --restart unless-stopped \
  nginx:alpine

echo ""
echo "=== 配置完成 ==="
echo "Nginx 容器已启动，监听端口: 80"
echo "测试命令: curl http://192.168.141.128"
echo ""
echo "查看容器日志: docker logs nginx-minio"
echo "查看容器状态: docker ps | grep nginx-minio"

