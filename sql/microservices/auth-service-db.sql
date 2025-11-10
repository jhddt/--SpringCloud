-- ============================================
-- auth-service 数据库初始化脚本（可选）
-- 数据库名：auth_service_db
-- 功能：统一管理所有用户的登录凭证
-- 说明：如果选择统一管理，可以将user_credentials表放在这里
-- ============================================

-- 注意：当前设计是将user_credentials表放在user_service_db中统一管理
-- 如果未来需要完全解耦，可以考虑创建独立的auth_service_db

-- 当前方案：user_credentials表在user_service_db中，auth-service通过服务间调用查询

