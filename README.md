# 教务选课系统（微服务架构）

## 项目概述

本项目是一个基于Spring Cloud微服务架构的教务选课系统，实现了学生、教师、课程、选课的全流程管理。系统采用前后端分离架构，后端使用Spring Cloud微服务，前端使用Vue3 + Element Plus。

## 技术栈

### 后端
- Spring Boot 3.2.0
- Spring Cloud 2023.0.0
- Spring Cloud Alibaba 2022.0.0.0
- Nacos（服务注册与配置中心）
- Spring Cloud Gateway（API网关）
- MyBatis Plus（ORM框架）
- Redis（缓存）
- RabbitMQ（消息队列）
- MinIO（对象存储）
- MySQL 8.0（数据库）
- JWT（身份认证）

### 前端
- Vue 3
- Element Plus
- Vue Router
- Pinia
- Axios
- WebSocket（实时通信）

## 项目结构

```
EducationManagent-SpringCloud/
├── common/                    # 公共模块
├── gateway/                   # 网关服务
├── auth-service/              # 认证服务
├── user-service/              # 用户服务
├── student-service/           # 学生服务
├── teacher-service/           # 教师服务
├── course-service/            # 课程服务
├── selection-service/        # 选课服务
├── file-service/             # 文件服务
├── message-service/          # 消息服务
├── frontend/                 # 前端项目
├── sql/                      # 数据库脚本
└── docker-compose.yml        # Docker Compose配置
```

## 环境要求

- JDK 17+
- Maven 3.6+
- Node.js 16+
- Docker & Docker Compose
- MySQL 8.0
- Redis 7.0
- RabbitMQ
- MinIO
- Nacos

## 快速开始

### 1. 启动基础服务

使用Docker Compose启动所有基础服务：

```bash
docker-compose up -d
```

这将启动以下服务：
- MySQL (端口: 3306)
- Redis (端口: 6379)
- RabbitMQ (端口: 5672, 管理界面: 15672)
- MinIO (端口: 9000, 控制台: 9100)
- Portainer (端口: 9443)

### 2. 启动Nacos

Nacos需要单独启动，访问 http://localhost:8848/nacos 进行配置管理（控制台端口为8080，访问 http://localhost:8080）。

### 3. 初始化数据库

执行 `sql/init.sql` 脚本初始化数据库。

### 4. 启动后端服务

按以下顺序启动后端服务：

1. common（公共模块，需要先编译）
2. gateway（网关服务，端口: 8888）
3. auth-service（认证服务，端口: 8081）
4. student-service（学生服务，端口: 8082）
5. teacher-service（教师服务，端口: 8083）
6. course-service（课程服务，端口: 8085）
7. selection-service（选课服务，端口: 8086）
8. file-service（文件服务，端口: 8087）
9. message-service（消息服务，端口: 8088）

### 5. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端服务将在 http://localhost:3000 启动。

## 默认账号

- 管理员：admin / 123456
- 教师：teacher1 / 123456
- 学生：student1 / 123456

## 功能特性

### 后端功能
- ✅ 用户认证与授权（JWT）
- ✅ 学生信息管理（CRUD + Redis缓存）
- ✅ 教师信息管理（CRUD + 角色绑定）
- ✅ 课程信息管理（分页、搜索）
- ✅ 选课管理（RabbitMQ异步处理、并发控制）
- ✅ 文件上传（MinIO，支持头像和课程封面）
- ✅ WebSocket实时消息通信

### 前端功能
- ✅ 管理员端与用户端双入口登录
- ✅ 学生信息管理
- ✅ 教师信息管理
- ✅ 课程浏览与选课
- ✅ 选课审批
- ✅ 实时消息通信
- ✅ 头像上传

## API文档

所有API通过网关统一访问，基础路径：`http://localhost:8888/api`

### 认证接口
- POST `/api/auth/login` - 用户登录
- POST `/api/auth/register` - 用户注册

### 学生接口
- GET `/api/student/{id}` - 获取学生信息
- GET `/api/student/page` - 分页查询学生
- POST `/api/student` - 创建学生
- PUT `/api/student/{id}` - 更新学生
- DELETE `/api/student/{id}` - 删除学生

### 教师接口
- GET `/api/teacher/{id}` - 获取教师信息
- GET `/api/teacher/page` - 分页查询教师
- POST `/api/teacher` - 创建教师
- PUT `/api/teacher/{id}` - 更新教师
- DELETE `/api/teacher/{id}` - 删除教师

### 课程接口
- GET `/api/course/{id}` - 获取课程信息
- GET `/api/course/page` - 分页查询课程
- POST `/api/course` - 创建课程
- PUT `/api/course/{id}` - 更新课程
- DELETE `/api/course/{id}` - 删除课程

### 选课接口
- POST `/api/selection/select` - 选课
- GET `/api/selection/page` - 分页查询选课记录
- PUT `/api/selection/{id}/approve` - 审核选课
- DELETE `/api/selection/{id}` - 取消选课

### 文件接口
- POST `/api/file/upload/avatar` - 上传头像
- POST `/api/file/upload/course-cover` - 上传课程封面
- GET `/api/file/download` - 下载文件

### 消息接口
- POST `/api/message/send` - 发送消息
- GET `/api/message/list` - 获取消息列表
- PUT `/api/message/{id}/read` - 标记已读
- GET `/api/message/unread-count` - 获取未读消息数

## 开发计划

本项目按照6周开发计划完成，详细内容请参考项目开发计划书。

## 许可证

MIT License

## 作者

周朝坤

日期：2025年11月

