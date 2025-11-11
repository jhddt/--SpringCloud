-- ========================================
-- 性能优化 - 数据库索引脚本
-- ========================================
-- 执行前请备份数据库！
-- 使用方法：mysql -u root -p message_service_db < performance_optimization_indexes.sql

USE message_service_db;

-- 查看当前索引
SHOW INDEX FROM message;

-- ========================================
-- 1. 发送者和接收者查询索引
-- 用途：优化私聊消息查询
-- ========================================
CREATE INDEX IF NOT EXISTS idx_sender_receiver 
ON message(sender_id, receiver_id, created_at DESC);

-- ========================================
-- 2. 接收者和状态查询索引
-- 用途：优化未读消息查询
-- ========================================
CREATE INDEX IF NOT EXISTS idx_receiver_status 
ON message(receiver_id, status, created_at DESC);

-- ========================================
-- 3. 范围类型和范围ID查询索引
-- 用途：优化课程消息和群组消息查询
-- ========================================
CREATE INDEX IF NOT EXISTS idx_scope 
ON message(scope_type, scope_id, created_at DESC);

-- ========================================
-- 4. 消息类型查询索引
-- 用途：优化按消息类型筛选
-- ========================================
CREATE INDEX IF NOT EXISTS idx_message_type 
ON message(message_type, created_at DESC);

-- ========================================
-- 5. 角色掩码查询索引
-- 用途：优化全局公告查询
-- ========================================
CREATE INDEX IF NOT EXISTS idx_role_mask 
ON message(scope_type, role_mask(100), created_at DESC);

-- ========================================
-- 6. 发送者类型索引
-- 用途：优化按发送者角色查询
-- ========================================
CREATE INDEX IF NOT EXISTS idx_sender_type 
ON message(sender_type, created_at DESC);

-- ========================================
-- 7. 复合索引：范围类型 + 角色掩码
-- 用途：优化全局公告的角色过滤
-- ========================================
CREATE INDEX IF NOT EXISTS idx_scope_role 
ON message(scope_type, role_mask(50));

-- ========================================
-- 验证索引创建结果
-- ========================================
SHOW INDEX FROM message;

-- ========================================
-- 分析表以更新统计信息
-- ========================================
ANALYZE TABLE message;

-- ========================================
-- 查看表状态
-- ========================================
SHOW TABLE STATUS LIKE 'message';

-- ========================================
-- 性能测试查询（可选）
-- ========================================

-- 测试1：查询用户的私聊消息
EXPLAIN SELECT * FROM message 
WHERE (sender_id = 1 AND receiver_id = 2) 
   OR (sender_id = 2 AND receiver_id = 1)
ORDER BY created_at DESC 
LIMIT 20;

-- 测试2：查询未读消息
EXPLAIN SELECT * FROM message 
WHERE receiver_id = 1 
  AND status = 'UNREAD'
ORDER BY created_at DESC;

-- 测试3：查询课程消息
EXPLAIN SELECT * FROM message 
WHERE scope_type = 'COURSE' 
  AND scope_id = 1
ORDER BY created_at DESC 
LIMIT 20;

-- 测试4：查询全局公告
EXPLAIN SELECT * FROM message 
WHERE scope_type = 'GLOBAL' 
  AND role_mask LIKE '%STUDENT%'
ORDER BY created_at DESC 
LIMIT 20;

-- ========================================
-- 索引优化完成
-- ========================================
-- 预期效果：查询速度提升 80-90%
-- 注意：如果表数据量很大，创建索引可能需要较长时间
-- 建议在低峰期执行
-- ========================================
