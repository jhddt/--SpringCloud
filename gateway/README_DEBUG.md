# 网关调试说明

## 问题：文件查看接口返回 401 错误

### 解决步骤

1. **重新编译网关服务**
   ```bash
   cd gateway
   mvn clean compile
   ```

2. **重启网关服务**
   - 停止当前运行的网关服务
   - 重新启动网关服务

3. **查看日志**
   访问图片时，应该能看到以下日志：
   ```
   === 网关过滤器执行 === path=/api/file/view, fullUri=http://localhost:8888/api/file/view?path=...
   白名单检查结果: isWhiteList=true, path=/api/file/view
   ✓ 白名单路径，直接放行: path=/api/file/view, fullUri=...
   ```

### 如果仍然无法访问

请检查：
1. 网关服务是否真的重启了（查看启动时间）
2. 代码是否重新编译了（查看 target/classes 目录的修改时间）
3. 是否有其他过滤器在拦截请求

### 测试方法

在浏览器中直接访问：
```
http://localhost:8888/api/file/view?path=education-files/avatars/test.jpg
```

如果返回 401，说明网关过滤器仍在拦截。
如果返回 404 或其他错误，说明已经通过了网关过滤器。

