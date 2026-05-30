-- =============================================
-- 系统百科平台 - MySQL 建库建表脚本
-- 执行方式: mysql -uroot -proot < init.sql
-- =============================================

CREATE DATABASE IF NOT EXISTS syswiki DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE syswiki;

-- =============================================
-- 1. 系统空间主表
-- =============================================
DROP TABLE IF EXISTS sys_ency_content_version;
DROP TABLE IF EXISTS sys_ency_content;
DROP TABLE IF EXISTS sys_ency_topology;
DROP TABLE IF EXISTS sys_ency_sql_lib;
DROP TABLE IF EXISTS sys_ency_space;

CREATE TABLE sys_ency_space (
    system_id       VARCHAR(32)     NOT NULL            COMMENT '系统唯一编号（主键）',
    system_name     VARCHAR(128)    NOT NULL            COMMENT '系统名称',
    system_code     VARCHAR(32)     NOT NULL            COMMENT '系统代号（唯一）',
    owner           VARCHAR(64)     NOT NULL            COMMENT '系统负责人',
    description     VARCHAR(512)    DEFAULT NULL        COMMENT '系统简要描述',
    status          VARCHAR(16)     NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/DISABLED',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (system_id),
    UNIQUE KEY uk_space_code (system_code),
    KEY idx_space_owner (owner)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统百科-空间主表';

-- =============================================
-- 2. 百科内容明细表
-- =============================================
CREATE TABLE sys_ency_content (
    content_id      VARCHAR(32)     NOT NULL            COMMENT '内容唯一编号（主键）',
    system_id       VARCHAR(32)     NOT NULL            COMMENT '外键-系统编号',
    module_type     VARCHAR(32)     NOT NULL            COMMENT '模块类型',
    md_content      LONGTEXT        DEFAULT NULL        COMMENT 'Markdown源码内容',
    version         INT             NOT NULL DEFAULT 1  COMMENT '版本号',
    operator        VARCHAR(64)     DEFAULT NULL        COMMENT '最近操作人',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (content_id),
    UNIQUE KEY uk_content_module (system_id, module_type),
    KEY idx_content_system (system_id),
    KEY idx_content_module (module_type),
    CONSTRAINT fk_content_space FOREIGN KEY (system_id) REFERENCES sys_ency_space(system_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统百科-内容明细表';

-- =============================================
-- 3. 内容版本历史表
-- =============================================
CREATE TABLE sys_ency_content_version (
    version_id      VARCHAR(32)     NOT NULL            COMMENT '版本记录唯一编号',
    content_id      VARCHAR(32)     NOT NULL            COMMENT '关联内容编号',
    system_id       VARCHAR(32)     NOT NULL            COMMENT '系统编号',
    module_type     VARCHAR(32)     NOT NULL            COMMENT '模块类型',
    version         INT             NOT NULL            COMMENT '版本号',
    md_content      LONGTEXT        DEFAULT NULL        COMMENT '该版本的Markdown内容',
    operator        VARCHAR(64)     DEFAULT NULL        COMMENT '操作人',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (version_id),
    KEY idx_version_content (content_id),
    KEY idx_version_system (system_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统百科-内容版本历史表';

-- =============================================
-- 4. 拓扑链路配置表
-- =============================================
CREATE TABLE sys_ency_topology (
    link_id             VARCHAR(32)     NOT NULL        COMMENT '链路唯一编号',
    system_id           VARCHAR(32)     NOT NULL        COMMENT '外键-系统编号',
    from_node           VARCHAR(64)     NOT NULL        COMMENT '起始节点',
    to_node             VARCHAR(64)     NOT NULL        COMMENT '目标节点',
    protocol            VARCHAR(32)     DEFAULT NULL    COMMENT '通信协议',
    interface_name      VARCHAR(128)    DEFAULT NULL    COMMENT '接口名称/交易码',
    interface_details   LONGTEXT        DEFAULT NULL    COMMENT '接口报文详情',
    sort_order          INT             DEFAULT 0       COMMENT '排序序号',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (link_id),
    KEY idx_topo_system (system_id),
    CONSTRAINT fk_topo_space FOREIGN KEY (system_id) REFERENCES sys_ency_space(system_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统百科-拓扑链路配置表';

-- =============================================
-- 5. 运维SQL库表
-- =============================================
CREATE TABLE sys_ency_sql_lib (
    sql_id          VARCHAR(32)     NOT NULL            COMMENT 'SQL条目唯一编号',
    system_id       VARCHAR(32)     NOT NULL            COMMENT '外键-系统编号',
    title           VARCHAR(128)    NOT NULL            COMMENT 'SQL标题',
    category        VARCHAR(32)     NOT NULL            COMMENT '分类',
    sql_template    LONGTEXT        NOT NULL            COMMENT 'SQL模板',
    description     VARCHAR(512)    DEFAULT NULL        COMMENT '使用说明',
    params_json     LONGTEXT        DEFAULT NULL        COMMENT '参数定义JSON',
    sort_order      INT             DEFAULT 0           COMMENT '排序序号',
    operator        VARCHAR(64)     DEFAULT NULL        COMMENT '操作人',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (sql_id),
    KEY idx_sql_system (system_id),
    KEY idx_sql_category (system_id, category),
    CONSTRAINT fk_sql_space FOREIGN KEY (system_id) REFERENCES sys_ency_space(system_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统百科-运维SQL与指令库';

-- =============================================
-- 插入一条示例数据
-- =============================================
INSERT INTO sys_ency_space (system_id, system_name, system_code, owner, description)
VALUES ('SP20260530000001', '示例核心系统', 'DEMO', '系统管理员', '系统百科平台示例空间，用于功能演示');

INSERT INTO sys_ency_content (content_id, system_id, module_type, md_content, version, operator)
VALUES ('CT20260530000001', 'SP20260530000001', 'INTRO',
'# 系统简介

本系统是系统百科平台的示例核心系统，用于演示平台各项功能。

## 技术栈

- Java 11
- Spring Boot 2.7
- MySQL 8.0
- Vue 3
- ECharts 5.x',
1, '系统管理员');
