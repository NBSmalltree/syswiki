# 系统百科平台

银行内网统一系统知识管理平台，旨在打破信息孤岛，将分散的系统文档、技术架构、服务器配置、运维SQL等信息集中管理，支持多系统入驻和 AI 智能问答。

## 核心功能

**结构化知识管理**
- 多系统空间隔离，每个系统拥有独立的百科空间
- Markdown 驱动的内容管理，支持在线编辑、离线导入导出
- 轻量版本轨迹，每次修改自动记录版本号和操作人

**可视化黄金链路**
- 基于 ECharts 的动态拓扑图，展示系统上下游调用关系
- 支持节点拖拽、缩放，点击连线查看接口级/报文级详情

**运维 SQL 库**
- 分类管理常用查询、数据校对、应急冲正、性能监控等 SQL
- 参数化占位符 + 输入框自动替换 + 一键复制

**AI 智能问答**
- 基于 RAG 技术，结合系统知识库进行精准问答
- 支持快速问答和深度推理两种模式
- 回答后推荐相关问题，持续引导探索

**用户权限管理**
- 三级角色：超级管理员（ADMIN）、系统管理员（EDITOR）、访客（VIEWER）
- 系统级权限隔离，EDITOR 只能管理被授权的系统
- JWT Token 认证，支持密码修改和管理员重置

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 + TypeScript + Vite + Element Plus + ECharts + Pinia |
| 后端 | Java 11 + Spring Boot 2.7 + MyBatis-Plus + JWT |
| 数据库 | MySQL 8.0（开发）/ Oracle 11g（生产） |
| AI | OpenAI 兼容协议（支持 MiMo / DeepSeek 等模型） |
| 部署 | Nginx 反向代理 + Spring Boot 内嵌 Tomcat |

## 快速开始

### 环境要求

- JDK 11+
- Node.js 16+
- MySQL 5.7+ 或 8.0
- Maven 3.6+

### 1. 配置环境变量

系统**必须**通过环境变量注入敏感配置，应用启动前请确保以下变量已设置：

```bash
# 复制环境变量模板
cp .env.example .env

# 编辑 .env，填入实际值
vi .env

# 加载环境变量
source .env
```

**必需的环境变量：**

| 环境变量 | 说明 |
|---------|------|
| `JWT_SECRET` | JWT 签名密钥（至少 32 字节）。生成方法：`openssl rand -hex 32` |
| `AI_BASE_URL` | AI 服务 API 地址（OpenAI 兼容协议） |
| `AI_API_KEY` | AI 服务 API Key |

**可选的环境变量：**

| 环境变量 | 说明 | 默认值 |
|---------|------|--------|
| `AI_FLASH_MODEL` | 快速问答模型名称 | `mimo-v2.5-pro` |
| `AI_THINK_MODEL` | 深度推理模型名称 | `mimo-v2.5-pro` |

### 2. 初始化数据库

```bash
mysql -uroot -p < syswiki-backend/src/main/resources/init.sql
```

### 3. 启动后端

```bash
cd syswiki-backend
mvn clean package -DskipTests
java -jar target/syswiki-backend-1.0.0.jar
```

> **注意：** 启动前必须确保环境变量已加载，否则应用将因缺少必需配置（JWT_SECRET、AI_BASE_URL、AI_API_KEY）而启动失败。

后端启动后访问：http://localhost:8080/syswiki

### 4. 启动前端

```bash
cd syswiki-frontend
npm install
npm run dev
```

前端启动后访问：http://localhost:3000

### 5. 登录

| 账号 | 密码 | 角色 |
|------|------|------|
| admin | 123456 | 超级管理员 |

> 首次启动时，应用会自动设置 admin 密码为 123456。**请在首次登录后立即修改密码**。

### 6. 加载演示数据（可选）

```bash
mysql -uroot -proot syswiki < syswiki-backend/db/demo-data.sql
```

## 项目结构

```
SysWiki/
├── syswiki-backend/                     # 后端 Spring Boot 工程
│   ├── pom.xml
│   ├── db/                              # 演示数据脚本
│   └── src/main/
│       ├── java/com/syswiki/
│       │   ├── SysWikiApplication.java  # 启动类
│       │   ├── auth/                    # JWT 认证 + 权限校验
│       │   ├── config/                  # 配置类
│       │   ├── controller/              # REST 接口
│       │   ├── mapper/                  # MyBatis Mapper
│       │   ├── model/                   # 实体 / DTO / VO
│       │   ├── rag/                     # AI 问答 + Prompt 管理
│       │   ├── service/                 # 业务逻辑
│       │   ├── util/                    # 工具类
│       │   └── exception/               # 异常处理
│       └── resources/
│           ├── application.yml          # 主配置
│           ├── application-dev.yml      # MySQL 开发环境
│           ├── application-prod.yml     # Oracle 生产环境
│           ├── init.sql                 # MySQL 建表脚本
│           └── init_oracle.sql          # Oracle 建表脚本
│
├── syswiki-frontend/                    # 前端 Vue 3 工程
│   ├── package.json
│   ├── vite.config.ts
│   └── src/
│       ├── api/                         # API 调用层
│       ├── components/                  # 公共组件
│       ├── composables/                 # 组合式函数
│       ├── router/                      # 路由配置
│       ├── stores/                      # Pinia 状态管理
│       ├── types/                       # TypeScript 类型
│       ├── utils/                       # 工具函数
│       └── views/                       # 页面视图
│           ├── admin/                   # 用户管理 / 成员管理
│           └── space/                   # 系统百科各功能页
│
├── .env.example                         # 环境变量配置模板
├── 系统概要设计文档.md
├── 前端详细设计文档.md
├── 后端详细设计文档.md
├── 系统使用说明书.md
└── 部署文档.md
```

## 配置说明

### 环境变量配置（必需）

系统通过环境变量注入敏感配置，`application.yml` 中使用 `${变量名}` 语法引用：

```bash
# 必需环境变量
export JWT_SECRET=$(openssl rand -hex 32)       # JWT 签名密钥
export AI_BASE_URL=https://api.openai.com/v1    # AI 服务地址
export AI_API_KEY=your-api-key                   # AI 服务密钥
```

或使用 `.env` 文件（推荐）：

```bash
cp .env.example .env
vi .env
source .env
```

### 后端配置

核心配置位于 `syswiki-backend/src/main/resources/application.yml`，敏感字段通过环境变量注入：

```yaml
syswiki:
  jwt:
    secret: ${JWT_SECRET}          # 从环境变量读取
    expiration: 14400000           # Token 有效期 4 小时
  ai:
    base-url: ${AI_BASE_URL}       # 从环境变量读取
    api-key: ${AI_API_KEY}         # 从环境变量读取
    flash-model: ${AI_FLASH_MODEL:mimo-v2.5-pro}   # 可选，有默认值
    think-model: ${AI_THINK_MODEL:mimo-v2.5-pro}   # 可选，有默认值
```

### 数据库切换

修改 `application.yml` 中的 `spring.profiles.active`：

- `dev` — MySQL（开发环境）
- `prod` — Oracle（生产环境）

### 安全配置说明

| 配置项 | 要求 | 说明 |
|--------|------|------|
| `JWT_SECRET` | 必须，至少 32 字节 | 用于 JWT Token 签发和验证，泄露将导致 Token 被伪造 |
| `AI_API_KEY` | 必须 | AI 服务认证密钥 |
| 密码策略 | 最低 6 位 | 用户密码最低长度为 6 位 |
| 首次登录 | 立即修改密码 | admin 默认密码为 123456，首次登录后应立即修改 |

## 部署

详见 [部署文档.md](部署文档.md)，包含：

- Nginx 反向代理配置
- systemd 服务化配置
- 生产环境安全建议

## 权限模型

| 能力 | 超级管理员 | 系统管理员 | 访客 |
|------|:-:|:-:|:-:|
| 浏览所有系统 | ✅ | ✅ | ✅ |
| 创建系统 | ✅ | ✅ | ❌ |
| 编辑被授权系统 | ✅（全部） | ✅（仅授权的） | ❌ |
| 管理系统成员 | ✅ | ✅（仅自己的系统） | ❌ |
| 用户管理 | ✅ | ❌ | ❌ |
| AI 问答 | ✅ | ✅ | ✅ |

## 设计文档

- [系统概要设计文档.md](系统概要设计文档.md) — 总体架构、功能模块、数据库概要
- [前端详细设计文档.md](前端详细设计文档.md) — Vue3 工程结构、路由、组件设计
- [后端详细设计文档.md](后端详细设计文档.md) — Spring Boot 工程结构、DDL、API 定义
- [系统使用说明书.md](系统使用说明书.md) — 各功能模块操作指引

## License

MIT
