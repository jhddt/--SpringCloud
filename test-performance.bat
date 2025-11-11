@echo off
chcp 65001 >nul
echo ========================================
echo 性能测试脚本
echo ========================================
echo.

echo [测试 1/5] 检查服务状态...
echo.

echo 检查 message-service (8088)...
curl -s http://localhost:8088/actuator/health >nul 2>&1
if %errorlevel% EQU 0 (
    echo ✓ message-service 运行正常
) else (
    echo ✗ message-service 未运行或无响应
    echo   请先启动 message-service
)

echo.
echo 检查 gateway (8888)...
curl -s http://localhost:8888/actuator/health >nul 2>&1
if %errorlevel% EQU 0 (
    echo ✓ gateway 运行正常
) else (
    echo ✗ gateway 未运行或无响应
    echo   请先启动 gateway
)

echo.
echo ========================================
echo [测试 2/5] 检查 Druid 监控
echo ========================================
echo.
echo Druid 监控地址: http://localhost:8088/druid/
echo 用户名: admin
echo 密码: admin123
echo.
echo 请手动访问 Druid 监控面板，检查：
echo   - SQL 监控
echo   - 连接池监控
echo   - 慢SQL统计
echo.
set /p druid_ok="Druid 监控是否正常？(Y/N): "

echo.
echo ========================================
echo [测试 3/5] 测试接口响应时间
echo ========================================
echo.
echo 提示：需要有效的 Token 才能测试
echo.
set /p token="请输入 JWT Token (或按 Enter 跳过): "

if "%token%"=="" (
    echo 已跳过接口测试
    goto :skip_api_test
)

echo.
echo 测试消息列表接口...
echo 请求: GET /api/message/page?current=1^&size=20
echo.

curl -w "\n响应时间: %%{time_total}s\n" ^
  -H "Authorization: Bearer %token%" ^
  -H "X-User-Id: 1" ^
  -H "X-Role: TEACHER" ^
  "http://localhost:8888/api/message/page?current=1&size=20"

echo.
echo 预期响应时间: ^< 1秒
echo 如果响应时间 ^> 2秒，说明优化效果不明显

:skip_api_test
echo.
echo ========================================
echo [测试 4/5] 检查 Redis 缓存
echo ========================================
echo.
echo 连接到 Redis...
echo 地址: 192.168.141.128:6379
echo.

set /p check_redis="是否检查 Redis 缓存？(Y/N): "
if /i "%check_redis%" NEQ "Y" goto :skip_redis

echo.
echo 执行以下命令检查缓存：
echo   redis-cli -h 192.168.141.128 -p 6379 -a 123456
echo   KEYS message:cache:*
echo   INFO stats
echo.
echo 预期结果：
echo   - 应该能看到 message:cache:user:name:* 的缓存键
echo   - keyspace_hits 应该 ^> 0（缓存命中）
echo.

:skip_redis
echo.
echo ========================================
echo [测试 5/5] 数据库索引验证
echo ========================================
echo.
set /p check_db="是否检查数据库索引？(Y/N): "
if /i "%check_db%" NEQ "Y" goto :skip_db_check

echo.
echo 执行以下 SQL 检查索引：
echo.
echo   USE message_service_db;
echo   SHOW INDEX FROM message;
echo.
echo 预期结果：应该看到以下索引：
echo   - idx_sender_receiver
echo   - idx_receiver_status
echo   - idx_scope
echo   - idx_message_type
echo   - idx_role_mask
echo   - idx_sender_type
echo   - idx_scope_role
echo.

:skip_db_check
echo.
echo ========================================
echo 性能测试完成
echo ========================================
echo.
echo 📊 性能指标对比
echo.
echo 优化前：
echo   - 消息列表查询: 2-5秒
echo   - 用户信息查询: 100-200ms
echo   - 并发能力: 20 TPS
echo.
echo 优化后（预期）：
echo   - 消息列表查询: 0.2-0.5秒 (提升 80-90%%)
echo   - 用户信息查询: 10-20ms (提升 90%%+)
echo   - 并发能力: 50+ TPS (提升 150%%+)
echo.
echo 🔍 进一步测试建议：
echo   1. 使用 JMeter 或 Apache Bench 进行压力测试
echo   2. 监控 Druid 面板的 SQL 执行时间
echo   3. 查看 Redis 缓存命中率
echo   4. 检查服务器资源使用情况
echo.
echo 📚 相关文档：
echo   - docs\PERFORMANCE_OPTIMIZATION.md
echo   - docs\QUICK_START_OPTIMIZATION.md
echo   - docs\OPTIMIZATION_SUMMARY.md
echo.

pause
