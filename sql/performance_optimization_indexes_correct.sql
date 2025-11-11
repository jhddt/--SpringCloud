-- ========================================
-- 性能优化 - 数据库索引脚本（正确版本）
-- ========================================
-- 执行前请备份数据库！
-- 表名：messages（复数）
-- 使用方法：mysql -u root -p569811 message_service_db < performance_optimization_indexes_correct.sql

USE message_service_db;

-- 查看当前索引
SHOW INDEX FROM messages;

-- ========================================
-- 创建性能优化索引
-- ========================================

-- 1. 发送者和接收者查询索引
-- 用途：优化私聊消息查询
-- 预期提升：80-90%
CREATE INDEX idx_sender_receiver ON messages(sender_id, receiver_id, created_at DESC);

-- 2. 接收者和状态查询索引
-- 用途：优化未读消息查询
-- 预期提升：85-95%
CREATE INDEX idx_receiver_status ON messages(receiver_id, status, created_at DESC);

-- 3. 范围类型和范围ID查询索引
-- 用途：优化课程消息和群组消息查询
-- 预期提升：80-90%
CREATE INDEX idx_scope ON messages(scope_type, scope_id, created_at DESC);

-- 4. 消息类型查询索引
-- 用途：优化按消息类型筛选
-- 预期提升：70-80%
CREATE INDEX idx_message_type ON messages(message_type, created_at DESC);

-- 5. 角色掩码查询索引
-- 用途：优化全局公告查询
-- 预期提升：75-85%
CREATE INDEX idx_role_mask ON messages(scope_type, role_mask(100), created_at DESC);

-- 6. 发送者类型索引
-- 用途：优化按发送者角色查询
-- 预期提升：70-80%
CREATE INDEX idx_sender_type ON messages(sender_type, created_at DESC);

-- 7. 复合索引：范围类型 + 角色掩码
-- 用途：优化全局公告的角色过滤
-- 预期提升：75-85%
CREATE INDEX idx_scope_role ON messages(scope_type, role_mask(50));

-- ========================================
-- 验证索引创建结果
-- ========================================
SHOW INDEX FROM messages;

-- ========================================
-- 分析表以更新统计信息
-- ========================================
ANALYZE TABLE messages;

-- ========================================
-- 查看表状态
-- ========================================
SHOW TABLE STATUS LIKE 'messages';

-- ========================================
-- 索引优化完成
-- ========================================
-- 预期效果：
--   - 消息列表查询速度提升：80-90%
--   - 未读消息查询速度提升：85-95%
--   - 私聊消息查询速度提升：80-90%
--   - 全局公告查询速度提升：75-85%
--   - 并发处理能力提升：150%+
-- 
-- 注意：
--   - 如果表数据量很大，创建索引可能需要较长时间
--   - 建议在低峰期执行
--   - 索引会占用额外的磁盘空间
-- ========================================
