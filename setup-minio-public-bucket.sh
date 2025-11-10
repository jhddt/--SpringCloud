#!/bin/bash
# 配置 MinIO bucket 为 public 的脚本

echo "=== 配置 MinIO bucket 为 public ==="

# 使用 MinIO 客户端配置 bucket 策略
# 需要先安装 MinIO 客户端：mc

# 如果已安装 mc，执行以下命令：
# mc alias set myminio http://192.168.141.128:9100 minio minio123456
# mc anonymous set public myminio/education-files

# 或者通过 MinIO 控制台配置：
# 1. 访问 http://192.168.141.128:9091
# 2. 登录（minio/minio123456）
# 3. 进入 education-files bucket
# 4. 设置 Access Policy 为 "Public"

echo "请通过 MinIO 控制台配置 bucket 为 public："
echo "1. 访问 http://192.168.141.128:9091"
echo "2. 登录（用户名: minio, 密码: minio123456）"
echo "3. 进入 education-files bucket"
echo "4. 点击 'Access Policy' -> 'Public'"

