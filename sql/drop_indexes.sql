-- ========================================
-- 删除性能优化索引脚本
-- ========================================
-- 用途：如果需要重新创建索引，先运行此脚本
-- 使用方法：mysql -u root -p569811 message_service_db < drop_indexes.sql

USE message_service_db;

-- 查看当前索引
SHOW INDEX FROM message;

-- ========================================
-- 删除索引
-- ========================================
-- 注意：如果索引不存在会报错，这是正常的

DROP INDEX idx_sender_receiver ON message;
DROP INDEX idx_receiver_status ON message;
DROP INDEX idx_scope ON message;
DROP INDEX idx_message_type ON message;
DROP INDEX idx_role_mask ON message;
DROP INDEX idx_sender_type ON message;
DROP INDEX idx_scope_role ON message;

-- ========================================
-- 验证删除结果
-- ========================================
SHOW INDEX FROM message;

-- ========================================
-- 删除完成
-- ========================================
