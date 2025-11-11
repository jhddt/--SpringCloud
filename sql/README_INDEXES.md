# 数据库索引优化脚本使用说明

## 📁 文件说明

### 1. `performance_optimization_indexes_compatible.sql` ✅ **推荐使用**
- **用途**：创建性能优化索引（兼容旧版 MySQL）
- **特点**：不使用 `IF NOT EXISTS` 语法，兼容性最好
- **使用场景**：首次创建索引

### 2. `drop_indexes.sql`
- **用途**：删除已创建的索引
- **使用场景**：需要重新创建索引时先运行此脚本

### 3. `performance_optimization_indexes.sql`（已废弃）
- **问题**：使用了 `IF NOT EXISTS` 语法，不兼容旧版 MySQL
- **建议**：使用 `performance_optimization_indexes_compatible.sql` 替代

---

## 🚀 使用方法

### 方法一：使用 optimize.bat 脚本（推荐）

```bash
cd d:\EducationManagent-SpringCloud
optimize.bat
```

脚本会自动执行索引优化。

### 方法二：手动执行 SQL

#### 步骤 1：首次创建索引

```bash
mysql -u root -p569811 message_service_db < sql\performance_optimization_indexes_compatible.sql
```

#### 步骤 2：如果需要重新创建索引

先删除旧索引：
```bash
mysql -u root -p569811 message_service_db < sql\drop_indexes.sql
```

再创建新索引：
```bash
mysql -u root -p569811 message_service_db < sql\performance_optimization_indexes_compatible.sql
```

### 方法三：在 IDE 中执行

1. 打开 `performance_optimization_indexes_compatible.sql`
2. 连接到 `message_service_db` 数据库
3. 逐条执行 SQL 语句
4. 如果某条语句报错"索引已存在"，可以忽略

---

## ⚠️ 常见问题

### 问题 1：表不存在
```
[42S02][1146] Table 'message_service_db.message' doesn't exist
```

**解决方案**：
1. 确认数据库名称是否正确
2. 检查表名是否为 `message`（可能是 `t_message` 或其他）
3. 运行 `SHOW TABLES;` 查看实际表名

### 问题 2：索引已存在
```
Duplicate key name 'idx_sender_receiver'
```

**解决方案**：
- 这是正常的，说明索引已经创建过了
- 可以忽略此错误
- 或者先运行 `drop_indexes.sql` 删除旧索引

### 问题 3：MySQL 语法错误
```
You have an error in your SQL syntax
```

**解决方案**：
- 确保使用 `performance_optimization_indexes_compatible.sql`（兼容版本）
- 不要使用 `performance_optimization_indexes.sql`（旧版本）

### 问题 4：MySQL 客户端未安装
```
'mysql' 不是内部或外部命令
```

**解决方案**：
1. 安装 MySQL 客户端
2. 将 MySQL bin 目录添加到系统 PATH
3. 或者在 IDE 中手动执行 SQL 脚本

---

## 📊 创建的索引列表

| 索引名称 | 字段 | 用途 |
|---------|------|------|
| `idx_sender_receiver` | sender_id, receiver_id, created_at | 优化私聊消息查询 |
| `idx_receiver_status` | receiver_id, status, created_at | 优化未读消息查询 |
| `idx_scope` | scope_type, scope_id, created_at | 优化课程/群组消息查询 |
| `idx_message_type` | message_type, created_at | 优化按消息类型筛选 |
| `idx_role_mask` | scope_type, role_mask(100), created_at | 优化全局公告查询 |
| `idx_sender_type` | sender_type, created_at | 优化按发送者角色查询 |
| `idx_scope_role` | scope_type, role_mask(50) | 优化全局公告的角色过滤 |

---

## ✅ 验证索引是否创建成功

```sql
USE message_service_db;
SHOW INDEX FROM message;
```

应该能看到上述 7 个索引。

---

## 📈 预期效果

- **查询速度提升**：80-90%
- **未读消息查询**：从 2-5 秒降至 0.2-0.5 秒
- **私聊消息查询**：从 1-3 秒降至 0.1-0.3 秒
- **全局公告查询**：从 3-8 秒降至 0.3-0.8 秒

---

## 📞 技术支持

如果遇到问题，请查看：
- `docs/PERFORMANCE_OPTIMIZATION.md` - 完整优化方案
- `docs/QUICK_START_OPTIMIZATION.md` - 快速优化指南
- `docs/TROUBLESHOOT_503_ERROR.md` - 常见问题排查
