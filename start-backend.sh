#!/bin/bash
# =============================================
# 系统百科平台 - 一键启动脚本
# 使用方法: chmod +x start.sh && ./start.sh
# =============================================

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}     系统百科平台 - 启动脚本${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# ---- 1. 检查 Java ----
echo -e "${YELLOW}[1/5] 检查 Java 环境...${NC}"
if ! command -v java &> /dev/null; then
    echo -e "${RED}错误: 未找到 Java，请安装 JDK 11+${NC}"
    exit 1
fi
java -version 2>&1 | head -1
echo ""

# ---- 2. 检查/安装 Maven ----
echo -e "${YELLOW}[2/5] 检查 Maven 环境...${NC}"
if ! command -v mvn &> /dev/null; then
    echo -e "${YELLOW}Maven 未安装，正在通过 Homebrew 安装...${NC}"
    if command -v brew &> /dev/null; then
        brew install maven
    else
        echo -e "${RED}错误: 未找到 Maven 且未安装 Homebrew${NC}"
        echo -e "${RED}请手动安装 Maven: https://maven.apache.org/install.html${NC}"
        exit 1
    fi
fi
mvn -version 2>&1 | head -1
echo ""

# ---- 3. 检查 MySQL ----
echo -e "${YELLOW}[3/5] 检查 MySQL 连接...${NC}"
if command -v mysql &> /dev/null; then
    if mysql -uroot -proot -e "SELECT 1" &> /dev/null; then
        echo -e "${GREEN}MySQL 连接成功${NC}"
        echo -e "${YELLOW}正在初始化数据库...${NC}"
        mysql -uroot -proot < "$PROJECT_DIR/syswiki-backend/src/main/resources/init.sql" 2>/dev/null || true
        echo -e "${GREEN}数据库初始化完成${NC}"
    else
        echo -e "${YELLOW}MySQL 连接失败，请确保 MySQL 已启动 (root/root)${NC}"
    fi
else
    echo -e "${YELLOW}mysql 命令不可用，请手动执行 init.sql 初始化数据库${NC}"
fi
echo ""

# ---- 4. 构建后端 ----
echo -e "${YELLOW}[4/5] 构建后端项目...${NC}"
cd "$PROJECT_DIR/syswiki-backend"
mvn clean package -DskipTests -q
echo -e "${GREEN}后端构建成功${NC}"
echo ""

# ---- 5. 启动服务 ----
echo -e "${YELLOW}[5/5] 启动后端服务...${NC}"
echo -e "${GREEN}后端地址: http://localhost:8080/syswiki${NC}"
echo -e "${GREEN}按 Ctrl+C 停止服务${NC}"
echo ""

java -jar target/syswiki-backend-1.0.0.jar
